package com.example.i_postureguard.ui.dashboard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.i_postureguard.R
import kotlin.concurrent.thread

class MyForegroundService : Service() {

    private val CHANNEL_ID = "MyForegroundServiceChannel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Create a notification for the foreground service
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Posture Correction Service")
            .setContentText("Service is running...")
            .setSmallIcon(R.drawable.logo) // Replace with your app's icon
            .build()

        // Start the service in the foreground
        startForeground(1, notification)

        thread {
            while (true) {
                Log.e("Service", "Service is running...")
                try {
                    Thread.sleep(2000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }

        return START_STICKY // Use START_STICKY to restart if the service gets terminated
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}