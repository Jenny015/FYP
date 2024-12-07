package com.example.i_postureguard

import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.i_postureguard.databinding.ActivityMainBinding
import com.google.firebase.Firebase
import com.google.firebase.database.database


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_information, R.id.navigation_exercise, R.id.navigation_dashboard, R.id.navigation_ranking, R.id.navigation_profile
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        val database = Firebase.database
        val myRef = database.getReference("/Login/Count")
        myRef.get().addOnSuccessListener {
            Log.i("firebase", "Got value ${it.value}")
            val newCount = (it.value as Long).toInt() + 1
            myRef.setValue(newCount)
        }.addOnFailureListener{
            Log.e("firebase", "Error getting data",it)
            }
    }
}