package com.example.i_postureguard.ui.dashboard

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.PointF
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.i_postureguard.R
import com.example.i_postureguard.databinding.FragmentDashboardBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.io.File
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.i_postureguard.Utils // Ensure this import is correct for your Utils class
import com.example.i_postureguard.ui.login.FragmentLoginActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener


const val REQUEST_CAMERA_PERMISSION = 1001
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private var accelerometer: GetAccelerometer? = null//initialize GetAccelerometer activity
    private val detectionDelay = 10000 // For adjust the frequency of detection
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var dailyPostureCount: TextView
    private lateinit var weeklyPostureCount: TextView
    private lateinit var database: DatabaseReference

    //for testing
    private lateinit var textViewCameraData: TextView
    private var lastUpdateTime = 0L

    private val postureImages = listOf(
        R.drawable.p0, // Index 0
        R.drawable.p1, // Index 1
        R.drawable.p2, // Index 2
        R.drawable.p3, // Index 3
        R.drawable.p4, // Index 4
        R.drawable.p5  // Index 5
    )

    override fun onCreateView(

        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.e("DashboardFragment", "is Created")

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
        val textViewTodayCount: TextView = binding.textViewDailyPostureCount
        val textViewTodayPercentage: TextView = binding.textViewTodayCount
        val textViewWeekCount: TextView = binding.textViewWeeklyPostureCount
        val textViewWeekPercentage: TextView = binding.textViewWeekCount

        dailyPostureCount = binding.textViewDailyPostureCount
        weeklyPostureCount = binding.textViewWeeklyPostureCount

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference

        // Load posture data
        loadPostureData()

        return root
    }

    private fun loadPostureData() {
        val phoneNumber = Utils.getString(requireContext(), "phone", "")
        Log.d("Dashboard", "Phone number: $phoneNumber")

        if (phoneNumber.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Phone number not found. Please log in again.",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(requireContext(), FragmentLoginActivity::class.java))
            requireActivity().finish()
            return
        }

        // Formatter for Firebase date format (e.g., 27-04-2025)
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        database.child("users").child(phoneNumber).child("data")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("Dashboard", "Snapshot exists: ${snapshot.exists()}, Value: ${snapshot.value}")
                    if (snapshot.exists()) {
                        var dailyCount = 0
                        var weeklyCount = 0
                        val currentDate = LocalDate.now() // Mocked to 27-04-2025
                        var todayPostureData: List<Int>? = null

                        snapshot.children.forEach { dateSnapshot ->
                            try {
                                val date = LocalDate.parse(dateSnapshot.key, formatter)
                                val postureData = dateSnapshot.child("posture").children
                                    .mapNotNull { it.value as? Long }
                                    .map { it.toInt() }
                                Log.d("Dashboard", "Date: ${dateSnapshot.key}, Posture: $postureData")

                                if (date.isEqual(currentDate)) {
                                    dailyCount += postureData.sum()
                                    todayPostureData = postureData
                                }
                                if (date.isEqual(currentDate) || date.isAfter(currentDate.minusDays(7))) {
                                    weeklyCount += postureData.sum()
                                }
                            } catch (e: Exception) {
                                Log.e("Dashboard", "Error parsing date '${dateSnapshot.key}': ${e.message}")
                            }
                        }

                        dailyPostureCount.text = "$dailyCount"
                        weeklyPostureCount.text = "$weeklyCount"
                        Log.d("Dashboard", "Daily count: $dailyCount, Weekly count: $weeklyCount, Today posture: $todayPostureData")

                        updatePostureImages(todayPostureData)
                    } else {
                        dailyPostureCount.text = "0"
                        weeklyPostureCount.text = "0"
                        hideAllPostureImages()
                        Log.d("Dashboard", "No posture data found for phone: $phoneNumber")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        requireContext(),
                        "Error fetching data: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("Dashboard", "Firebase error: ${error.message}")
                }
            })
    }

    private fun updatePostureImages(postureData: List<Int>?) {
        hideAllPostureImages()

        if (postureData.isNullOrEmpty() || postureData.all { it == 0 }) {
            Log.d("Dashboard", "No posture data or all zeros, hiding images")
            return
        }

        val imageViews = listOf(
            binding.postureImage1,
            binding.postureImage2,
            binding.postureImage3,
            binding.postureImage4,
            binding.postureImage5,
            binding.postureImage6
        )

        var imageCount = 0
        postureData.forEachIndexed { index, value ->
            if (value != 0 && index < postureImages.size && imageCount < 6) {
                imageViews[imageCount].setImageResource(postureImages[index])
                imageViews[imageCount].visibility = View.VISIBLE
                imageCount++
            }
        }

        Log.d("Dashboard", "Displayed $imageCount posture images for data: $postureData")
    }

    private fun hideAllPostureImages() {
        binding.postureImage1.visibility = View.GONE
        binding.postureImage2.visibility = View.GONE
        binding.postureImage3.visibility = View.GONE
        binding.postureImage4.visibility = View.GONE
        binding.postureImage5.visibility = View.GONE
        binding.postureImage6.visibility = View.GONE
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
        var serviceIntent= Intent(
            requireContext(),
            MyForegroundService::class.java
        )
        ContextCompat.startForegroundService(requireContext(),serviceIntent)
    }

    private fun cameraOff() {
        textViewCameraData.text = "Camera Off"
        var serviceIntent= Intent(
            requireContext(),
            MyForegroundService::class.java
        )
        requireContext().stopService(serviceIntent)
    }

    private fun checkCameraPermission(activity: Activity) {
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

    private fun calculateDistance(fragment: Fragment, face: Face, onDistanceCalculated: (Float) -> Unit) {
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position


        if (leftEye != null && rightEye != null) {
            val eyeDistance = distanceBetweenPoints(leftEye, rightEye)

            // Retrieve the camera info (focal length, sensor width)
            getCameraInfo(fragment.requireContext(), fragment) { cameraInfo ->
                if (cameraInfo != null) {
                    val focalLength = cameraInfo.focalLength
                    val sensorWidth = cameraInfo.sensorWidth

                    // Known average eye distance in real life (e.g., 6.2 cm)
                    val knownEyeDistance = 6.2f

                    // Calculate the distance using the formula
                    val distance = (focalLength * knownEyeDistance) / (eyeDistance * sensorWidth)/2*1000

                    // Return the calculated distance
                    onDistanceCalculated(distance)
                } else {
                    onDistanceCalculated(-1f)
                }
            }
        } else {
            onDistanceCalculated(-1f)
        }
    }

    private fun distanceBetweenPoints(p1: PointF, p2: PointF): Float {
        return Math.sqrt(((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y)).toDouble())
            .toFloat()
    }

    data class CameraInfo(val focalLength: Float, val sensorWidth: Float)

    private fun getCameraInfo(
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

            if (focalLength != null && sensorWidth != null) {
                onCameraInfoAvailable(CameraInfo(focalLength, sensorWidth))
            } else {
                onCameraInfoAvailable(null)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    private fun getAccelerometerData(): String {
        return accelerometer?.getData() ?: "No accelerometer data"
    }

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

    private fun checkPosture(head: List<Float>, eyeopen: List<Float?>, accelerometer: String) {
        Toast.makeText(this.requireContext(), "For dev: Detect", Toast.LENGTH_SHORT).show() //For Testing
        val buffer = 0 //TODO: Access from user profile
        // Phrase accelerometer data from String into List
        val regex = """[-\d]+\.\d+""".toRegex()
        val accelData =
            regex.findAll(accelerometer).map { it.value.trim() }.mapNotNull { it.toFloatOrNull() }
                .toMutableList()
        // TODO: Calculate actual head angle with accelerometer data
//            collectData(head, accelData);
        // TODO: Set kinds of bad posture will be detected
        //Head position: Tilt Front (Text-neck), Tilt left(scoliosis), Tile Right(scoliosis)
        //Phone position: Phone horizontal & Head horizontal (Sleep on side with phone), Phone face down with face detected (Sleep on back with phone)
        // TODO: Set threshold for different kinds of bad posture
        var msg = ""
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT&&
            head[2] > -20 && head[2] < 20 && (accelData[0] > 7 || accelData[0] < -7)) {
            playAudio(R.raw.sleep)
            msg += "Sleep on side while using mobile phone\n"

        } else if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE&&
            head[2] > -20 && head[2] < 20 && (accelData[1] > 7 || accelData[1] < -7)){

            playAudio(R.raw.sleep)
            msg += "Sleep on side while using mobile phone\n"
        } else if (accelData[2] < -7) {
            playAudio(R.raw.sleep)
            msg += "Sleep on back while using mobile phone\n"
        } else {
            if (accelData[2] < 5 && head[0] < (0 - buffer) || (accelData[2] > 5 && head[0] < (12 - buffer))) {
                playAudio(R.raw.text_neck)
                msg += "Text-neck posture\n"
            }
            if (head[2] < (-10 - buffer)) {
                playAudio(R.raw.tilt_left)
                msg += "Head tilting left (Scoliosis)\n"
            }
            if (head[2] > (10 + buffer)) {
                playAudio(R.raw.tilt_right)
                msg += "Head tilting right (Scoliosis)\n"
            }
            if(head[3]<30){
                playAudio(R.raw.close)
                msg ="Unsafety distance\nToo close, please keep the distance\n"
            }
        }
        if(msg.isNotEmpty()){
            showDialog(msg)
        }
    }

    private fun playAudio(resId: Int) {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }

        mediaPlayer = MediaPlayer.create(this.requireContext(), resId)
        mediaPlayer.start()
    }
    private fun showDialog(msg: String){
        val builder = AlertDialog.Builder(this.requireContext())
        builder.setTitle("Posture Reminder")
        builder.setMessage(msg)
        builder.setPositiveButton("OK") { dialog, which ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }
}