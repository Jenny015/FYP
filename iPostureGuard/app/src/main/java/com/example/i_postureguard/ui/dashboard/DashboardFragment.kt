package com.example.i_postureguard.ui.dashboard

import com.example.i_postureguard.FaceDetectionService
import FaceDetectionUtils
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ToggleButton
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.i_postureguard.databinding.FragmentDashboardBinding
import java.io.File


const val REQUEST_CAMERA_PERMISSION = 1001
class DashboardFragment : Fragment() {
    companion object {
        var cameraInfo: Map<String, Float> = mapOf(
            "focalLength" to 0.0F,
            "sensorWidth" to 0.0F
        );
    }
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private var accelerometer: GetAccelerometer? = null//initialize GetAccelerometer activity

    //for testing
    private lateinit var textViewCameraData: TextView
    private lateinit var preview: PreviewView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root
        preview = binding.cameraPreview
        textViewCameraData = binding.cameraData

        val toggleButton: ToggleButton = binding.toggleButton
        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if(checkCameraPermission(requireActivity())){
                    getCameraInfo(this);
                    startFaceDetectionService()
                }
            } else {
                stopFaceDetectionService()
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
        stopFaceDetectionService()
        _binding = null
    }

    private fun startFaceDetectionService() {
        textViewCameraData.text = "Camera On"
        accelerometer = GetAccelerometer.getInstance(requireContext())
        accelerometer?.startListening() // Use safe call
        val serviceIntent = Intent(requireContext(), FaceDetectionService::class.java)
        ContextCompat.startForegroundService(requireContext(), serviceIntent)
    }

    private fun stopFaceDetectionService() {
        textViewCameraData.text = "Camera Off"
        val serviceIntent = Intent(requireContext(), FaceDetectionService::class.java)
        requireContext().stopService(serviceIntent)
    }

    private fun checkCameraPermission(activity: Activity): Boolean {
        return if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
            false
        } else {
            true
        }
    }


    @OptIn(ExperimentalCamera2Interop::class)
    private fun getCameraInfo(lifecycleOwner:Fragment) {
        val cameraProviderFuture = context?.let { ProcessCameraProvider.getInstance(it) }

        context?.let { ContextCompat.getMainExecutor(it) }?.let {
            cameraProviderFuture?.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)

                val camera2CameraInfo = Camera2CameraInfo.from(camera.cameraInfo)
                val cameraManager = context?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val cameraId = camera2CameraInfo.cameraId
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)

                val focalLength = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.get(0)
                val sensorWidth = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)?.width

                if (focalLength != null && sensorWidth != null) {
                    cameraInfo = mapOf(
                        "focalLength" to focalLength,
                        "sensorWidth" to sensorWidth
                    );
                }
            }, it)
        }
    }
    //TODO: collect data
    fun collectData(cameraData: List<Float>, accelData: List<Float>) {
        var label = -1
        val combinedData = accelData + cameraData + listOf(label)
        val dataLine = combinedData.joinToString(separator = ",")
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "posture_data.csv"
        )
        file.appendText("$dataLine\n")
    }
}