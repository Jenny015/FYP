import android.content.Context
import android.content.res.Configuration
import android.graphics.PointF
import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.i_postureguard.R
import com.example.i_postureguard.ui.dashboard.DashboardFragment.Companion.cameraInfo
import com.example.i_postureguard.ui.dashboard.GetAccelerometer
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark

object FaceDetectionUtils {
    private lateinit var mediaPlayer: MediaPlayer
    private var lastUpdateTime: Long = 0
    val detectionDelay: Long = 10000 // For adjusting the frequency of detection

    @OptIn(ExperimentalGetImage::class)
    fun cameraOn(context: Context, updateNotificationCallback: (String) -> Unit) {
        Log.d("FaceDetectionUtils", "cameraOn method is being called")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            Log.d("FaceDetectionUtils", "Camera provider obtained")

            val faceDetectorOptions = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .build()
            val faceDetector = FaceDetection.getClient(faceDetectorOptions)

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                        val currentTime = System.currentTimeMillis()
                        Log.d("FaceDetectionUtils", "Analyzer started")
                        if (currentTime - lastUpdateTime >= detectionDelay) {
                            lastUpdateTime = currentTime

                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )

                                faceDetector.process(image)
                                    .addOnSuccessListener { faces ->
                                        Log.d("FaceDetectionUtils", "Faces detected: ${faces.size}")
                                        for (face in faces) {
                                            calculateDistance(face) { distance ->
                                                val rotX = face.headEulerAngleX
                                                val rotY = face.headEulerAngleY
                                                val rotZ = face.headEulerAngleZ
                                                val accelData = getAccelerometerData(context)
                                                Log.d("FaceDetectionUtils", "Rotation: $rotX, $rotY, $rotZ")
                                                updateNotificationCallback(checkPosture(context,
                                                    listOf(rotX, rotY, rotZ, distance % 1000),
                                                    listOf(
                                                        face.leftEyeOpenProbability,
                                                        face.rightEyeOpenProbability
                                                    ), accelData
                                                ))
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("FaceDetectionUtils", "Face detection failed: $e")
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                Log.e("FaceDetectionUtils", "Media image is null")
                            }
                        } else {
                            imageProxy.close()
                        }
                    }
                }
        }, ContextCompat.getMainExecutor(context))
    }

    fun cameraOff(context: Context) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun calculateDistance(face: Face, onDistanceCalculated: (Float) -> Unit) {
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position

        if (leftEye != null && rightEye != null) {
            val eyeDistance = distanceBetweenPoints(leftEye, rightEye)

            // Retrieve the camera info (focal length, sensor width)
            val focalLength = cameraInfo["focalLength"] ?: 0f
            val sensorWidth = cameraInfo["sensorWidth"] ?: 1f  // Use 1f to avoid division by zero

            // Known average eye distance in real life (e.g., 6.2 cm)
            val knownEyeDistance = 6.2f

            // Calculate the distance using the formula
            val distance = (focalLength * knownEyeDistance) / (eyeDistance * sensorWidth) / 2 * 1000

            // Return the calculated distance
            onDistanceCalculated(distance)
        }
    }

    private fun distanceBetweenPoints(p1: PointF, p2: PointF): Float {
        return Math.sqrt(((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y)).toDouble())
            .toFloat()
    }

    private fun getAccelerometerData(context: Context): String {
        var accelerometer: GetAccelerometer? = GetAccelerometer.getInstance(context)
        return accelerometer?.getData() ?: "No accelerometer data"
    }

    private fun checkPosture(context: Context, head: List<Float>, eyeopen: List<Float?>, accelerometer: String): String {
        val buffer = 0 //TODO: Access from user profile
        // Phrase accelerometer data from String into List
        val regex = """[-\d]+\.\d+""".toRegex()
        val accelData =
            regex.findAll(accelerometer).map { it.value.trim() }.mapNotNull { it.toFloatOrNull() }
                .toMutableList()
        //Head position: Tilt Front (Text-neck), Tilt left(scoliosis), Tile Right(scoliosis)
        //Phone position: Phone horizontal & Head horizontal (Sleep on side with phone), Phone face down with face detected (Sleep on back with phone)
        var msg = ""
        Log.d("CheckPostureData", "Head:" + head[0] + ", " + head[1] + ", " + head[2] + "\nAccelerometer:" + accelerometer[0] + ", " + accelerometer[1] + ", " + accelerometer[2])
        if (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT &&
            head[2] > -20 && head[2] < 20 && (accelData[0] > 7 || accelData[0] < -7)) {
            playAudio(context, R.raw.sleep)
            msg += "Sleep on side while using mobile phone\n"

        } else if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
            head[2] > -20 && head[2] < 20 && (accelData[1] > 7 || accelData[1] < -7)) {

            playAudio(context, R.raw.sleep)
            msg += "Sleep on side while using mobile phone\n"
        } else if (accelData[2] < -7) {
            playAudio(context, R.raw.sleep)
            msg += "Sleep on back while using mobile phone\n"
        } else {
            if (accelData[2] < 5 && head[0] < (0 - buffer) || (accelData[2] > 5 && head[0] < (12 - buffer))) {
                playAudio(context, R.raw.text_neck)
                msg += "Text-neck posture\n"
            }
            if (head[2] < (-10 - buffer)) {
                playAudio(context, R.raw.tilt_left)
                msg += "Head tilting left (Scoliosis)\n"
            }
            if (head[2] > (10 + buffer)) {
                playAudio(context, R.raw.tilt_right)
                msg += "Head tilting right (Scoliosis)\n"
            }
            if (head[3] < 30) {
                playAudio(context, R.raw.close)
                msg = "Unsafety distance\nToo close, please keep the distance\n"
            }
        }
        Log.d("CheckPostureResult", msg)
        return msg
    }

    private fun playAudio(context: Context, resId: Int) {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }

        mediaPlayer = MediaPlayer.create(context, resId)
        mediaPlayer.start()
    }
}
