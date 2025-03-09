package com.example.i_postureguard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.i_postureguard.databinding.ActivityMainBinding
import com.example.i_postureguard.ui.dashboard.MyForegroundService
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // 設置導航
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_exercise, R.id.navigation_dashboard, R.id.navigation_ranking, R.id.navigation_profile
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Firebase 數據庫設置
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("/Login/Count")
        myRef.get().addOnSuccessListener {
            Log.i("firebase", "Got value ${it.value}")
            val newCount = (it.value as Long).toInt() + 1
            myRef.setValue(newCount)
        }.addOnFailureListener {
            Log.e("firebase", "Error getting data", it)
        }

        // 啟動前台服務
        val serviceIntent = Intent(this, MyForegroundService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }
}