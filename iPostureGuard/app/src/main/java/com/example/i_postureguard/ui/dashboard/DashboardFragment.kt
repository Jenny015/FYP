package com.example.i_postureguard.ui.dashboard

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ToggleButton
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.i_postureguard.databinding.FragmentDashboardBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions


const val REQUEST_CAMERA_PERMISSION = 1001
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    //for testing
    private lateinit var textViewCameraData: TextView

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
            } else {
                cameraOff()
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
                                        val bounds = face.boundingBox
                                        val rotX = face.headEulerAngleX
                                        val rotY =
                                            face.headEulerAngleY // Head is rotated to the right rotY degrees
                                        val rotZ =
                                            face.headEulerAngleZ // Head is tilted sideways rotZ degrees
                                        var msg = "X: $rotX\nY: $rotY\nZ: $rotZ\n"

                                        if (face.rightEyeOpenProbability != null) {
                                            val rightEyeOpenProb = face.rightEyeOpenProbability
                                            msg += "\nRight eye open: $rightEyeOpenProb"
                                        }
                                        if (face.leftEyeOpenProbability != null) {
                                            val leftEyeOpenProb = face.leftEyeOpenProbability
                                            msg += "\nLeft eye open: $leftEyeOpenProb"
                                        }
                                        textViewCameraData.text = msg
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
}