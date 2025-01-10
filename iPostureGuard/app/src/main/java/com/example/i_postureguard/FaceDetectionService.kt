package com.example.i_postureguard

import FaceDetectionUtils.detectionDelay
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.os.Handler
import androidx.core.app.NotificationCompat

class FaceDetectionService : Service() {

    companion object {
        const val CHANNEL_ID = "FaceDetectionServiceChannel"
        const val NOTIFICATION_ID = 1
        const val TAG = "FaceDetectionService"
    }

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, getNotification("Face Detection Service is starting..."))
        handler = Handler(Looper.getMainLooper())
        runnable = Runnable {
            updateNotification("Face Detection Service is running...")
            handler.postDelayed(runnable, detectionDelay.toLong())
        }
        handler.post(runnable)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Face Detection Service")
            .setContentText("Running face detection in background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
        startFaceDetection()

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        Log.d(TAG, "Creating notification channel")
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Face Detection Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun getNotification(message: String): android.app.Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Face Detection Service")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Makes the notification unremovable
            .build()
    }

    private fun updateNotification(message: String) {
        Log.d(TAG, "Updating notification: $message")
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = getNotification(message)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun startFaceDetection() {
        Log.d(TAG, "Starting face detection")
        FaceDetectionUtils.cameraOn(this) { msg ->
            Log.d(TAG, "FaceDetectionUtils callback message: $msg")
            updateNotification(msg)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FaceDetectionUtils.cameraOff(this)
        handler.removeCallbacks(runnable)
        Log.d(TAG, "Service onDestroy")
    }
}