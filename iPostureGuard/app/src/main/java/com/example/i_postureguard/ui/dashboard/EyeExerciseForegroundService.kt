package com.example.i_postureguard.ui.dashboard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
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

class EyeExerciseForegroundService : LifecycleService() {
    private val CHANNEL_ID = "EyeExerciseForegroundServiceChannel"
    private lateinit var cameraExecutor: ExecutorService
    private var lastUpdateTime = 0L
    private val detectionDelay = 1000 // For adjust the frequency of detection
    private var mediaPlayer: MediaPlayer? = null

    // Exercise state variables
    private enum class ExerciseState {
        LEFT_EYE_EXERCISE,
        RIGHT_EYE_EXERCISE,
        COMPLETED
    }
    private var currentState = ExerciseState.LEFT_EYE_EXERCISE
    private var exerciseTimer: CountDownTimer? = null
    private var exerciseTime = 10 // seconds for each eye
    private var exerciseSuccessful = false
    private var faceDetected = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        playMp3(R.raw.left_eye_exercise)

        cameraExecutor = Executors.newSingleThreadExecutor()

        startCameraAnalysis()
        startExerciseTimer()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Eye Exercise Service")
            .setContentText(getExerciseInstruction())
            .setSmallIcon(R.drawable.logo)
            .build()

        startForeground(1, notification)
        Log.i("ForegroundService", "Eye Exercise Service Activated")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        exerciseTimer?.cancel()
        cameraExecutor.shutdown()
        mediaPlayer?.release()
        Log.i("ForegroundService", "Eye Exercise Service Shutdown")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Eye Exercise Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun startExerciseTimer() {
        exerciseTimer = object : CountDownTimer((exerciseTime * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                updateNotification("${getExerciseInstruction()} - $secondsRemaining seconds remaining")

                // Check if we have face data and if exercise is being performed correctly
                if (faceDetected) {
                    when (currentState) {
                        ExerciseState.LEFT_EYE_EXERCISE -> {
                            if (!exerciseSuccessful) {
                                playMp3(R.raw.incorrect) // Play sound if not performing correctly
                            }
                        }
                        ExerciseState.RIGHT_EYE_EXERCISE -> {
                            if (!exerciseSuccessful) {
                                playMp3(R.raw.incorrect) // Play sound if not performing correctly
                            }
                        }
                        else -> {}
                    }
                }

                faceDetected = false // Reset for next detection
                exerciseSuccessful = false // Reset for next detection
            }

            override fun onFinish() {
                when (currentState) {
                    ExerciseState.LEFT_EYE_EXERCISE -> {
                        currentState = ExerciseState.RIGHT_EYE_EXERCISE
                        playMp3(R.raw.right_eye_exercise)
                        startExerciseTimer() // Start timer for right eye
                    }
                    ExerciseState.RIGHT_EYE_EXERCISE -> {
                        currentState = ExerciseState.COMPLETED
                        playMp3(R.raw.exercise_complete)
                        updateNotification("Exercise completed! Closing in 5 seconds...")

                        // Use a postDelayed handler to stop the service after delay
                        Handler(Looper.getMainLooper()).postDelayed({
                            stopSelf()
                        }, 5000) // 5 seconds delay
                    }
                    else -> stopSelf()
                }
            }
        }.start()
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

                        if (mediaImage != null && currentTime - lastUpdateTime >= detectionDelay) {
                            lastUpdateTime = currentTime
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            faceDetector.process(image)
                                .addOnSuccessListener { faces ->
                                    if (faces.isNotEmpty()) {
                                        faceDetected = true
                                        val face = faces[0] // Get the first face

                                        when (currentState) {
                                            ExerciseState.LEFT_EYE_EXERCISE -> {
                                                // Check if left eye is closed
                                                if (face.rightEyeOpenProbability != null &&
                                                    face.rightEyeOpenProbability!! < 0.5f) {
                                                    exerciseSuccessful = true
                                                }
                                            }
                                            ExerciseState.RIGHT_EYE_EXERCISE -> {
                                                // Check if right eye is closed
                                                if (face.leftEyeOpenProbability != null &&
                                                    face.leftEyeOpenProbability!! < 0.5f) {
                                                    exerciseSuccessful = true
                                                }
                                            }
                                            else -> {}
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("EyeExerciseService", "Face detection failed: $e")
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
                    this,
                    cameraSelector,
                    imageAnalysis
                )

            } catch (exc: Exception) {
                Log.e("EyeExerciseService", "Camera binding failed: $exc")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun playMp3(resourceId: Int) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, resourceId).apply {
            start()
        }
    }

    private fun getExerciseInstruction(): String {
        return when (currentState) {
            ExerciseState.LEFT_EYE_EXERCISE -> "Close your LEFT eye for $exerciseTime seconds"
            ExerciseState.RIGHT_EYE_EXERCISE -> "Close your RIGHT eye for $exerciseTime seconds"
            ExerciseState.COMPLETED -> "Exercise completed!"
        }
    }

    private fun updateNotification(text: String) {
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Eye Exercise Service")
            .setContentText(text)
            .setSmallIcon(R.drawable.logo)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }
}