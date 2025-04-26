package com.example.i_postureguard.ui.exercise

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ToggleButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.i_postureguard.R
import com.example.i_postureguard.databinding.FragmentExerciseEyesBinding // Make sure this import matches your actual binding class
import com.example.i_postureguard.ui.dashboard.EyeExerciseForegroundService
import com.example.i_postureguard.ui.dashboard.MyForegroundService

class FragmentExerciseEyesActivity : AppCompatActivity() {
    private lateinit var binding: FragmentExerciseEyesBinding // Declare the binding variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the binding
        binding = FragmentExerciseEyesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


    }

    fun onBackButtonClicked(view: View) {
        finish()
    }
    fun onExerciseButtonClicked(view: View){
        val button = view as Button  // Cast the View to Button
        if (button.text.toString() == getString(R.string.on)) {
            button.text = getString(R.string.off)
            cameraOff()
        } else {
            button.text = getString(R.string.on)
            cameraOn()
        }
    }
    fun cameraOn(){
        val serviceIntent = Intent(this, EyeExerciseForegroundService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }
    fun cameraOff(){
        val serviceIntent = Intent(this,EyeExerciseForegroundService::class.java)
        stopService(serviceIntent)
    }
}