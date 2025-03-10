package com.unileeds.Android_App;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.unileeds.Android_App.databinding.ActivityCalculateOrientationBinding;

import org.w3c.dom.Text;

public class CalculateOrientation extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;

    private Sensor accelerometer;
    private Sensor magnetometer;

    private float[] accelerometer_data = new float[3];
    private float[] magnetometer_data = new float[3];

    // UI stuff
    private TextView azimuth_text;
    private TextView pitch_text;
    private TextView roll_text;

    // Calibration vs current
    private float[] azimuth_vals = new float[2];
    private double prev_azimuth = 0;

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if(isGranted) {
                    Log.d("Permission", "Granted");
                }
                else {
                    Log.d("Permission", "denied");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculate_orientation);

        Intent intent = getIntent();

        // Locked so that screen rotation doesn't affect any calculations
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        azimuth_text = (TextView) findViewById(R.id.azimuth_text);
        pitch_text = (TextView) findViewById(R.id.pitch_text);
        roll_text = (TextView) findViewById(R.id.roll_text);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // If sensor is available, register listener
        if(accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        else {
            Log.d("Sensor", "Accelerometer unavailable");
        }
        if(magnetometer != null) {
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onStop() {
        // Unregister listeners when app closes
        super.onStop();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        if(type == Sensor.TYPE_ACCELEROMETER) {
            // Copy data
            accelerometer_data = event.values.clone();
        }
        else if(type == Sensor.TYPE_MAGNETIC_FIELD) {
            // Copy data
            magnetometer_data = event.values.clone();
        }

        // Generate rotation matrix from new data
        float[] rotation_matrix = new float[9];

        // If false, the sensor data is unreliable
        boolean success = SensorManager.getRotationMatrix(rotation_matrix, null, accelerometer_data, magnetometer_data);

        // Get orientation angles
        float[] orientation_angles = new float[3];
        if(success) {
            SensorManager.getOrientation(rotation_matrix, orientation_angles);
        }

        // Now separate the components of orientation_angles
        float azimuth = orientation_angles[0]; // direction
        float pitch = orientation_angles[1]; // top-to-bottom tilt
        float roll = orientation_angles[2]; // left-to-right tilt

        double azimuth_deg = Math.toDegrees(azimuth);

        // azimuth is in the range -180 and 180, so let's try to remove these negatives
        double difference = azimuth_deg - prev_azimuth;
        if(difference > 180f) {
            azimuth_deg = azimuth_deg - 360f;
        }
        else if(difference < -180f) {
            azimuth_deg = azimuth_deg + 360f;
        }


        azimuth_text.setText("Azimuth: " + Double.toString(azimuth_deg));
        pitch_text.setText("Pitch: " + Float.toString(pitch));
        roll_text.setText("Roll: " + Float.toString(roll));
        prev_azimuth = azimuth_deg;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}