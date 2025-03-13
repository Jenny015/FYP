package com.example.i_postureguard.ui.dashboard
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.example.i_postureguard.R
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MyForegroundService : LifecycleService() {

    private val CHANNEL_ID = "MyForegroundServiceChannel"
    private lateinit var cameraExecutor: ExecutorService
    private var lastUpdateTime = 0L
    private val detectionDelay = 5000// For adjust the frequency of detection
    private var mediaPlayer: MediaPlayer? = null
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        cameraExecutor = Executors.newSingleThreadExecutor()

        startCameraAnalysis()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // Create and display a notification for the foreground service
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Posture Correction Service")
            .setContentText("Analyzing posture...")
            .setSmallIcon(R.drawable.logo) // Replace with your app's icon
            .build()

        startForeground(1, notification)
        Log.i("ForegroundService","Camera ForegroundService Activated")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        Log.i("ForegroundService","Camera ForegroundService Shutdown")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCameraAnalysis() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Configure face detector
            val faceDetectorOptions = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .build()
            val faceDetector = FaceDetection.getClient(faceDetectorOptions)

            // Set up ImageAnalysis
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        val currentTime = System.currentTimeMillis()
                        if (mediaImage != null && currentTime - lastUpdateTime >= detectionDelay){
                            lastUpdateTime = currentTime
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            faceDetector.process(image)
                                .addOnSuccessListener { faces ->
                                    var msg = ""
                                    for (face in faces) {

                                        val bounds = face.boundingBox


                                            val rotX = face.headEulerAngleX
                                            val rotY =
                                                face.headEulerAngleY // Head is rotated to the right rotY degrees
                                            val rotZ =
                                                face.headEulerAngleZ // Head is tilted sideways rotZ degrees
                                            msg += "X: $rotX\nY: $rotY\nZ: $rotZ\n\n"

                                            if (face.rightEyeOpenProbability != null) {
                                                msg += "\nRight eye open: ${face.rightEyeOpenProbability}"
                                            }
                                            if (face.leftEyeOpenProbability != null) {
                                                msg += "\nLeft eye open: ${face.leftEyeOpenProbability}"
                                            }
                                    }
                                    Log.e("hi","I am still running")
                                    Log.e("update",msg)
                                    playMp3(R.raw.warning)

                                }
                                .addOnFailureListener { e ->
                                    Log.e("MyForegroundService", "Face detection failed: $e")
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            // Select front camera
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()
                // Bind ImageAnalysis use case to the lifecycle of this service
                cameraProvider.bindToLifecycle(
                    this, // Now this works because LifecycleService is a LifecycleOwner
                    cameraSelector,
                    imageAnalysis
                )

            } catch (exc: Exception) {
                Log.e("MyForegroundService", "Camera binding failed: $exc")
            }
        }, ContextCompat.getMainExecutor(this))
    }
    fun playMp3(resourceId: Int) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, resourceId).apply {
            start()
        }
    }


}