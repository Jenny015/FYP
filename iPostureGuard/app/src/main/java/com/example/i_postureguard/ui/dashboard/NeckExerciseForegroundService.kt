package com.example.i_postureguard.ui.dashboard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
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
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class NeckExerciseForegroundService : LifecycleService() {
    private val CHANNEL_ID = "NeckExerciseForegroundServiceChannel"
    private lateinit var cameraExecutor: ExecutorService
    private var lastUpdateTime = 0L
    private val detectionDelay = 1000 // For adjusting the frequency of detection
    private var mediaPlayer: MediaPlayer? = null

    // Exercise state variables
    private enum class ExerciseState {
        LEFT_NECK_EXERCISE,
        RIGHT_NECK_EXERCISE,
        COMPLETED
    }
    private var currentState = ExerciseState.LEFT_NECK_EXERCISE
    private var exerciseTime = 10 // seconds for each neck exercise
    private var remainingTime = exerciseTime // Tracks remaining time in seconds
    private var exerciseSuccessful = false
    private var faceDetected = false
    private var isTimerRunning = false
    private val handler = Handler(Looper.getMainLooper())

    // Custom timer runnable
    private val timerRunnable = object : Runnable {
        override fun run() {
            Log.d("NeckExerciseService", "Timer tick: state=$currentState, remainingTime=$remainingTime, faceDetected=$faceDetected")
            if (faceDetected && remainingTime > 0) {
                remainingTime--
                updateNotification("${getExerciseInstruction()} - $remainingTime seconds remaining")

                // Check if exercise is being performed correctly
                if (!exerciseSuccessful) {
                    playMp3(R.raw.incorrect)
                }

                faceDetected = false // Reset for next detection
                exerciseSuccessful = false // Reset for next detection
            }

            if (remainingTime <= 0) {
                Log.d("NeckExerciseService", "Timer finished for state: $currentState")
                when (currentState) {
                    ExerciseState.LEFT_NECK_EXERCISE -> {
                        currentState = ExerciseState.RIGHT_NECK_EXERCISE
                        remainingTime = exerciseTime // Reset for right neck
                        Log.d("NeckExerciseService", "Transitioning to RIGHT_NECK_EXERCISE")
                        isTimerRunning = false // Stop current timer
                        playMp3(R.raw.tilt_neck_right_exercise) {
                            Log.d("NeckExerciseService", "Right neck audio completed, starting timer")
                            startTimer() // Start timer for right neck
                        }
                    }
                    ExerciseState.RIGHT_NECK_EXERCISE -> {
                        currentState = ExerciseState.COMPLETED
                        Log.d("NeckExerciseService", "Transitioning to COMPLETED")
                        playMp3(R.raw.exercise_complete)
                        updateNotification("Exercise completed! Closing in 5 seconds...")
                        handler.postDelayed({ stopSelf() }, 5000) // 5 seconds delay
                    }
                    else -> {
                        Log.d("NeckExerciseService", "Stopping service in COMPLETED state")
                        stopSelf()
                    }
                }
            } else {
                // Schedule the next tick
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Play the initial audio and start camera analysis only after it completes
        playMp3(R.raw.tilt_neck_left_exercise) {
            Log.d("NeckExerciseService", "Left neck audio completed, starting camera and timer")
            startCameraAnalysis()
            startTimer()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Neck Exercise Service") // Fixed title to reflect neck exercise
            .setContentText(getExerciseInstruction())
            .setSmallIcon(R.drawable.logo)
            .build()

        startForeground(1, notification)
        Log.i("ForegroundService", "Neck Exercise Service Activated")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
        cameraExecutor.shutdown()
        mediaPlayer?.release()
        Log.i("ForegroundService", "Neck Exercise Service Shutdown")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Neck Exercise Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun startTimer() {
        if (!isTimerRunning) {
            isTimerRunning = true
            Log.d("NeckExerciseService", "Starting timer for state: $currentState, remainingTime: $remainingTime")
            handler.post(timerRunnable)
        } else {
            Log.d("NeckExerciseService", "Timer already running, skipping start")
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

                        if (mediaImage != null && currentTime - lastUpdateTime >= detectionDelay) {
                            lastUpdateTime = currentTime
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            faceDetector.process(image)
                                .addOnSuccessListener { faces ->
                                    faceDetected = faces.isNotEmpty()
                                    Log.d("NeckExerciseService", "Face detection: faceDetected=$faceDetected")
                                    if (faceDetected) {
                                        val face = faces[0] // Get the first face

                                        when (currentState) {
                                            ExerciseState.LEFT_NECK_EXERCISE -> {
                                                // Check if head is tilted left (negative Z angle)
                                                if (face.headEulerAngleZ < -10) {
                                                    exerciseSuccessful = true
                                                    Log.d("NeckExerciseService", "Left neck exercise successful")
                                                }
                                            }
                                            ExerciseState.RIGHT_NECK_EXERCISE -> {
                                                // Check if head is tilted right (positive Z angle)
                                                if (face.headEulerAngleZ > 10) {
                                                    exerciseSuccessful = true
                                                    Log.d("NeckExerciseService", "Right neck exercise successful")
                                                }
                                            }
                                            else -> {}
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("NeckExerciseService", "Face detection failed: $e")
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
                Log.e("NeckExerciseService", "Camera binding failed: $exc")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun playMp3(resourceId: Int, onComplete: (() -> Unit)? = null) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, resourceId)?.apply {
            setOnCompletionListener {
                Log.d("NeckExerciseService", "Audio playback completed for resource: $resourceId")
                onComplete?.invoke() // Call the onComplete callback
                release() // Release the MediaPlayer
            }
            setOnErrorListener { _, what, extra ->
                Log.e("NeckExerciseService", "MediaPlayer error: what=$what, extra=$extra")
                onComplete?.invoke() // Ensure callback is called even on error
                release()
                true
            }
            try {
                start()
                Log.d("NeckExerciseService", "Audio playback started for resource: $resourceId")
            } catch (e: Exception) {
                Log.e("NeckExerciseService", "Failed to start audio: $e")
                onComplete?.invoke() // Call callback on failure
                release()
            }
        } ?: run {
            Log.e("NeckExerciseService", "Failed to create MediaPlayer for resource: $resourceId")
            onComplete?.invoke() // Call callback if MediaPlayer creation fails
            null // Explicitly return null to match MediaPlayer? type
        }
    }

    private fun getExerciseInstruction(): String {
        return when (currentState) {
            ExerciseState.LEFT_NECK_EXERCISE -> "Tilt your head LEFT for $exerciseTime seconds"
            ExerciseState.RIGHT_NECK_EXERCISE -> "Tilt your head RIGHT for $exerciseTime seconds"
            ExerciseState.COMPLETED -> "Exercise completed!"
        }
    }

    private fun updateNotification(text: String) {
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Neck Exercise Service")
            .setContentText(text)
            .setSmallIcon(R.drawable.logo)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }
}