package com.example.i_postureguard.ui.dashboard;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;

public class GetFrontCameraInfo {
    private static final String TAG = "CameraInfo";

    public static void getCameraInfo(Context context) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            // Get the list of available cameras
            String[] cameraIds = cameraManager.getCameraIdList();
            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);

                // Check if this is the front-facing camera
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                    // Get the focal length
                    Float focalLength = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)[0];
                    Log.d(TAG, "Front Camera Focal Length: " + focalLength + " mm");

                    // Get the sensor width
                    Float sensorWidth = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE).getWidth();
                    Log.d(TAG, "Front Camera Sensor Width: " + sensorWidth + " mm");

                    // Exit after finding the front camera
                    break;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error accessing camera: " + e.getMessage());
        }
    }
}
