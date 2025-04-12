package com.example.i_postureguard.ui.exercise

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.i_postureguard.R
import com.example.i_postureguard.ui.dashboard.MyForegroundService

class fragment_exercise_shoulder : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.fragment_exercise_shoulder)
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
        val serviceIntent = Intent(this, MyForegroundService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }
    fun cameraOff(){
        val serviceIntent = Intent(this, MyForegroundService::class.java)
        stopService(serviceIntent)
    }
}