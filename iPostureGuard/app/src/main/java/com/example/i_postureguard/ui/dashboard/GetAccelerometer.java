package com.example.i_postureguard.ui.dashboard;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class GetAccelerometer implements SensorEventListener {
    public static GetAccelerometer instance;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private String data;

    // Private constructor to prevent instantiation
    private GetAccelerometer(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    // Public method to provide access to the instance
    public static synchronized GetAccelerometer getInstance(Context context) {
        if (instance == null) {
            instance = new GetAccelerometer(context.getApplicationContext());
        }
        return instance;
    }

    public void startListening() {
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void stopListening() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            data = "accelerometer:"+"\nX: " + x + "\nY: " + y + "\nZ: " + z;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle changes in sensor accuracy if needed
    }

    public String getData() {
        return data;
    }
}