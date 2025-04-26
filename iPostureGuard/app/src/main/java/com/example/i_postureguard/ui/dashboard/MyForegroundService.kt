package com.example.i_postureguard.ui.dashboard
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PointF
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
import com.example.i_postureguard.Utils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MyForegroundService : LifecycleService() {
    val buffer = 0 //TODO: Access from user profile
    private val CHANNEL_ID = "MyForegroundServiceChannel"
    private lateinit var cameraExecutor: ExecutorService
    private var lastUpdateTime = 0L
    private val detectionDelay = Utils.detectionDelay// For adjust the frequency of detection
    private var mediaPlayer: MediaPlayer? = null
    private var accelerometer: GetAccelerometer? = null//initialize GetAccelerometer activity
    var focalLength = 0f
    var sensorWidth = 0f
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
        accelerometer = GetAccelerometer.getInstance(this)
        accelerometer?.startListening()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        accelerometer?.stopListening()
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
                                    var rotX=0.0f
                                    var rotY=0.0f
                                    var rotZ=0.0f
                                    var face1: Face? =null

                                    for (face in faces) {

                                        val bounds = face.boundingBox


                                            rotX = face.headEulerAngleX
                                            rotY =
                                                face.headEulerAngleY // Head is rotated to the right rotY degrees
                                            rotZ =
                                                face.headEulerAngleZ // Head is tilted sideways rotZ degrees
                                            msg += "X: $rotX\nY: $rotY\nZ: $rotZ\n\n"

                                            if (face.rightEyeOpenProbability != null) {
                                                msg += "\nRight eye open: ${face.rightEyeOpenProbability}"
                                            }
                                            if (face.leftEyeOpenProbability != null) {
                                                msg += "\nLeft eye open: ${face.leftEyeOpenProbability}"
                                            }
                                        face1=face
                                    }
                                    if (face1 != null) {
                                        checkPosture(rotX,rotY,rotZ,getAccelerometerData(),face1)
                                    }
                                    Log.e("hi","I am still running")
                                    //Log.e("update",msg)

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
                        var cameraInfo=GetFrontCameraInfo.getCameraInfo(this)
                        focalLength=cameraInfo[0]
                        sensorWidth=cameraInfo[1]
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
    private fun getAccelerometerData(): String {
        return accelerometer?.getData() ?: "No accelerometer data"
    }
    fun checkPosture(rotX:Float, rotY: Float, rotZ:Float,accelerometerData: String,face:Face){
        var msg = ""
        val regex = """[-\d]+\.\d+""".toRegex()
        val accelData =
            regex.findAll(accelerometerData).map { it.value.trim() }.mapNotNull { it.toFloatOrNull() }
                .toMutableList()
        var distance:Float=getDistance(focalLength, sensorWidth, face)
        var type = -1
        Log.i("Distance: ",distance.toString()+" cm")
        if (accelData[2] < 5 && rotX < (0 - buffer) || (accelData[2] > 5 && rotX < (12 - buffer))) { //text neck
            playMp3(R.raw.text_neck)
            msg += "Text-neck posture\n"
            type = 0
        }else if (rotZ < (-10 - buffer)) {  //左傾
            playMp3(R.raw.tilt_left)
            msg += "Head tilting left (Scoliosis)\n"
            type = 1
        }else if (rotZ > (10 + buffer)) { //右傾
            playMp3(R.raw.tilt_right)
            msg += "Head tilting right (Scoliosis)\n"
            type = 2
        }

        if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT&&
            rotZ > -20 && rotZ < 20 && (accelData[0] > 7 || accelData[0] < -7)) {
            playMp3(R.raw.side_sleep)
            msg += "Sleep on side while using mobile phone\n"
            type = 4

        } else if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE&&
            rotZ > -20 && rotZ < 20 && (accelData[1] > 7 || accelData[1] < -7)){
            playMp3(R.raw.side_sleep)
            msg += "Sleep on side while using mobile phone\n"
            type = 4
        } else if (accelData[2] < -7) {
            playMp3(R.raw.sleep)
            msg += "Sleep on back while using mobile phone\n"
            type = 3
        } else {

            if (accelData[2] < 5 && rotX < (0 - buffer) || (accelData[2] > 5 && rotX < (12 - buffer))) {
                playMp3(R.raw.text_neck)
                msg += "Text-neck posture\n"
                type = 0
            }
            else if (rotZ < (-10 - buffer)) {
                playMp3(R.raw.tilt_left)
                msg += "Head tilting left (Scoliosis)\n"
                type = 1
            }
            else if (rotZ > (10 + buffer)) {
                playMp3(R.raw.tilt_right)
                msg += "Head tilting right (Scoliosis)\n"
                type = 2
            }
            else if(distance<30){
                playMp3(R.raw.close)
                msg ="Unsafety distance\nToo close, please keep the distance\n"
                type = 5

            }
        }
        if(type != -1) {
            if(Utils.isLogin(applicationContext)){
                Utils.firebaseAddData(applicationContext, "p", type)
            } else {
                Utils.localAddData(applicationContext, "p", type)
            }
        }


        Log.e("update",msg)
    }

    fun  getDistance(focalLength:Float, sensorWidth: Float, face: Face):Float{
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position


        if (leftEye != null && rightEye != null) {

            val eyeDistance = distanceBetweenPoints(leftEye, rightEye)
            // Known average eye distance in real life (e.g., 6.2 cm)
            val knownEyeDistance = 6.2f

            // Calculate the distance using the formula
            val distance = (focalLength * knownEyeDistance) / (eyeDistance * sensorWidth)*0.75f*1000
            Log.i("eyeDistance",eyeDistance.toString())
            Log.i("focalLength",focalLength.toString())
            Log.i("sensorWidth",sensorWidth.toString())
            return distance
        }
        return 0f

    }
    private fun distanceBetweenPoints(p1: PointF, p2: PointF): Float {
        return Math.sqrt(((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y)).toDouble())
            .toFloat()
    }


}