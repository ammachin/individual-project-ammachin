package com.unileeds.Android_App;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PDR extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;

    // Sensors
    private Sensor accelerometer;
    private Sensor magnetometer;
    private Sensor steps;

    // Variables to store sensor data as it changes
    private float[] accelerometer_data = new float[3];
    private float[] magnetometer_data = new float[3];
    private float steps_data;

    // We want to do orientation and distance in two stages for simplification
    // So keep track with a boolean
    private boolean is_orientation_stage = false;
    private boolean is_distance_stage = false;

    private float distance_travelled = 0.0f;

    // Azimuth to calculate orientations
    private double init_azimuth = 0.0f;
    private double prev_azimuth = 0.0f;
    private double current_azimuth = 0.0f;

    private Vector3D init_coords;
    private Vector3D final_coords;

    // For permissions
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if(isGranted) {
                    Log.d("Permission", "Granted");
                }
                else {
                    Log.d("Permission", "Denied");
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdr);

        // Get intent from previous activity
        Intent intent = getIntent();

        // Locked so that screen rotation doesn't affect any calculations
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Call get functions
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        steps = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

    }

    private void calculateAzimuth() {
        // Generate rotation matrix from new data
        float[] rotation_matrix = new float[9];

        // If false, sensor data is unreliable and so should not be used
        boolean success = SensorManager.getRotationMatrix(rotation_matrix, null, accelerometer_data, magnetometer_data);

        // Azimuth, pitch, roll
        float[] orientation_angles = new float[3];
        if(success) {
            SensorManager.getOrientation(rotation_matrix, orientation_angles);
        }

        current_azimuth = orientation_angles[0];
        // Azimuth is in the range -Pi to Pi,
        // Which can lead to some big jumps, so let's try to remove that
        if(prev_azimuth != 0.0f) {
            double difference = current_azimuth - prev_azimuth;
            if(difference > Math.PI) {
                current_azimuth -= (2 * Math.PI);
            }
            else if(difference < -Math.PI) {
                current_azimuth += (2 * Math.PI);
            }
        }
    }

    private void calculateDistance() {
        // Using a constant for step length based off of my own step length
        // to simplify the problem
        float step_length = 52.88f;
        distance_travelled += steps_data * step_length;
    }

    private Vector3D calculateUnitVector(Vector3D v) {
        Vector3D unit = new Vector3D();
        double magnitude = Math.sqrt((Math.pow(v.x, 2)) + (Math.pow(v.y, 2)) + (Math.pow(v.z, 2)));

        unit.x = (float) (v.x / magnitude);
        unit.y = (float) (v.y / magnitude);
        unit.z = (float) (v.z / magnitude);
        return unit;
    }

    private void calculateFinalCoords() {
        // TODO: should probably change Vector3D to doubles instead
        Vector3D unit = calculateUnitVector(init_coords);

        double angle = current_azimuth - init_azimuth;

        // Create rotation matrix around the z-axis
        // In rows
        Vector3D[] rotation_matrix = new Vector3D[3];

        rotation_matrix[0].x = (float) Math.cos(angle);
        rotation_matrix[0].y = (float) -Math.sin(angle);
        rotation_matrix[0].z = 0.0f;

        rotation_matrix[1].x = (float) Math.sin(angle);
        rotation_matrix[1].y = (float) Math.cos(angle);
        rotation_matrix[1].z = 0.0f;

        rotation_matrix[1].x = 0.0f;
        rotation_matrix[1].y = 0.0f;
        rotation_matrix[1].z = 1.0f;

        // Apply rotation matrix to init_coords
        // Just doing this manually for now
        final_coords.x = ((rotation_matrix[0].x * init_coords.x) + (rotation_matrix[0].y * init_coords.y) + (rotation_matrix[0].z * init_coords.z));
        final_coords.y = ((rotation_matrix[1].x * init_coords.x) + (rotation_matrix[1].y * init_coords.y) + (rotation_matrix[1].z * init_coords.z));
        final_coords.z = ((rotation_matrix[2].x * init_coords.x) + (rotation_matrix[2].y * init_coords.y) + (rotation_matrix[2].z * init_coords.z));

        // Apply distance to final_coords
        final_coords.x = final_coords.x * distance_travelled * unit.x;
        final_coords.y = final_coords.y * distance_travelled * unit.y;
        final_coords.z = final_coords.z * distance_travelled * unit.z;
    }

    @Override
    protected void onStart() {
        super.onStart();

        // If sensor is available, register listeners on activity start
        if(accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        else {
            Log.d("Sensor", "Accelerometer unavailable");
        }

        if(magnetometer != null) {
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        else {
            Log.d("Sensor", "Magnetometer unavailable");
        }

        if(steps != null) {
            sensorManager.registerListener(this, steps, SensorManager.SENSOR_DELAY_NORMAL);
        }
        else {
            Log.d("Sensor", "Step counter unavailable");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();

        if(type == Sensor.TYPE_ACCELEROMETER) {
            // Copy data
            accelerometer_data = event.values.clone();

            if(is_orientation_stage) {
                calculateAzimuth();
            }
        }
        else if(type == Sensor.TYPE_MAGNETIC_FIELD) {
            // Copy data
            magnetometer_data = event.values.clone();

            if(is_orientation_stage) {
                calculateAzimuth();
            }
        }
        else if(type == Sensor.TYPE_STEP_COUNTER) {
            // Copy data
            steps_data = event.values[0];

            if(is_distance_stage) {
                calculateDistance();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not implementing this method
    }
}