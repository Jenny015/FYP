package com.example.i_postureguard.ui.dashboard

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PointF
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ToggleButton
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.i_postureguard.databinding.FragmentDashboardBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark


const val REQUEST_CAMERA_PERMISSION = 1001
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    //for testing
    private lateinit var textViewCameraData: TextView
    private var accelerometer: GetAccelerometer? = null//initialize GetAccelerometer activity
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        textViewCameraData = binding.cameraData
        val toggleButton: ToggleButton = binding.toggleButton
        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkCameraPermission(requireActivity())
                cameraOn()
                accelerometer = GetAccelerometer.getInstance(requireContext())
                accelerometer?.startListening() // Use safe call
            } else {
                cameraOff()
                accelerometer?.stopListening() // Stop listening if toggled off
            }
        }

        // New TextViews
        val textViewTodayPosture: TextView = binding.textViewTodayPosture
        val textViewTodayCount: TextView = binding.textViewTodayCount
        val textViewTodayPercentage: TextView = binding.textViewTodayPercentage
        val textViewWeekCount: TextView = binding.textViewWeekCount
        val textViewWeekPercentage: TextView = binding.textViewWeekPercentage

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraOff()
        _binding = null
        accelerometer?.stopListening()
    }

    @OptIn(ExperimentalGetImage::class)
    private fun cameraOn() {
        textViewCameraData.text = "Camera On"
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

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
                    it.setAnalyzer(ContextCompat.getMainExecutor(requireContext())) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            faceDetector.process(image)
                                .addOnSuccessListener { faces ->
                                    for (face in faces) {
                                        var msg = ""
                                        val bounds = face.boundingBox

                                        calculateDistance(this, face) { distance ->
                                            msg += "Distance: ${distance * 1000} cm\n"

                                            val rotX = face.headEulerAngleX
                                            val rotY =
                                                face.headEulerAngleY // Head is rotated to the right rotY degrees
                                            val rotZ =
                                                face.headEulerAngleZ // Head is tilted sideways rotZ degrees
                                            msg += "X: $rotX\nY: $rotY\nZ: $rotZ\n\n"

                                            if (face.rightEyeOpenProbability != null) {
                                                val rightEyeOpenProb = face.rightEyeOpenProbability
                                                msg += "\nRight eye open: $rightEyeOpenProb"
                                            }
                                            if (face.leftEyeOpenProbability != null) {
                                                msg += "\nLeft eye open: ${face.leftEyeOpenProbability}"
                                            }

                                            // Safely access accelerometer data
                                            msg += "\n${accelerometer?.getData() ?: "No accelerometer data"}"
                                            textViewCameraData.text = msg
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    textViewCameraData.text = "Face detection failed: $e"
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        }
                    }
                }

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (exc: Exception) {
                textViewCameraData.text = "Camera binding failed: $exc"
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun cameraOff() {
        textViewCameraData.text = "Camera Off"
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    fun checkCameraPermission(activity: Activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            cameraOn()
        }
    }

    fun calculateDistance(fragment: Fragment, face: Face, onDistanceCalculated: (Float) -> Unit) {
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position

        // Log the eye positions
        println("Left Eye: $leftEye, Right Eye: $rightEye")

        if (leftEye != null && rightEye != null) {
            val eyeDistance = distanceBetweenPoints(leftEye, rightEye)

            // Log the distance between the eyes in pixels
            println("Eye Distance in Pixels: $eyeDistance")

            // Retrieve the camera info (focal length, sensor width)
            getCameraInfo(fragment.requireContext(), fragment) { cameraInfo ->
                if (cameraInfo != null) {
                    val focalLength = cameraInfo.focalLength
                    val sensorWidth = cameraInfo.sensorWidth

                    // Log the focal length and sensor width
                    println("Focal Length: $focalLength, Sensor Width: $sensorWidth")

                    // Known average eye distance in real life (e.g., 6.2 cm)
                    val knownEyeDistance = 6.2f

                    // Calculate the distance using the formula
                    val distance = (focalLength * knownEyeDistance) / (eyeDistance * sensorWidth)

                    // Log the calculated distance
                    println("Calculated Distance: $distance")

                    // Return the calculated distance
                    onDistanceCalculated(distance)
                } else {
                    // Log the failure to retrieve camera info
                    println("Unable to retrieve camera info.")
                    onDistanceCalculated(-1f)
                }
            }
        } else {
            // Log if the eye landmarks are not detected
            println("Eye landmarks are not detected.")
            onDistanceCalculated(-1f)
        }
    }

    fun distanceBetweenPoints(p1: PointF, p2: PointF): Float {
        return Math.sqrt(((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y)).toDouble())
            .toFloat()
    }

    data class CameraInfo(val focalLength: Float, val sensorWidth: Float)

    fun getCameraInfo(
        context: Context,
        lifecycleOwner: Fragment,
        onCameraInfoAvailable: (CameraInfo?) -> Unit
    ) {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Select the back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Bind to the lifecycle of the fragment (this)
            val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)

            // Opt-in to Camera2 interop experimental API
            @OptIn(ExperimentalCamera2Interop::class)
            val camera2CameraInfo = Camera2CameraInfo.from(camera.cameraInfo)

            // Access the Camera2 characteristics
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            // Opt-in to Camera2 interop experimental API for accessing the cameraId
            @OptIn(ExperimentalCamera2Interop::class)
            val cameraId = camera2CameraInfo.cameraId

            val characteristics = cameraManager.getCameraCharacteristics(cameraId)

            // Get focal length and sensor width
            val focalLength =
                characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.get(0)
            val sensorWidth =
                characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)?.width

            // Log retrieved camera info
            println("Camera Focal Length: $focalLength, Sensor Width: $sensorWidth")

            if (focalLength != null && sensorWidth != null) {
                onCameraInfoAvailable(CameraInfo(focalLength, sensorWidth))
            } else {
                // Log failure to retrieve the info
                println("Failed to retrieve focal length or sensor width.")
                onCameraInfoAvailable(null)
            }

        }, ContextCompat.getMainExecutor(context))
    }
}