package com.unileeds.Android_App;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.w3c.dom.Text;

import java.util.Objects;

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
    private Vector3D final_coords = new Vector3D();

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
        init_coords = (Vector3D) Objects.requireNonNull(intent.getExtras()).getSerializable("init_coords");

        // Locked so that screen rotation doesn't affect any calculations
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Call get functions
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        steps = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        // Get UI buttons / text views
        TextView coords_view = (TextView) findViewById(R.id.init_coords);
        Button orientation_button = (Button) findViewById(R.id.orientation_button);
        Button distance_button = (Button) findViewById(R.id.distance_button);
        TextView distance_text = (TextView) findViewById(R.id.distance_text);
        Button send_button = (Button) findViewById(R.id.send_new_button);
        TextView send_text = (TextView) findViewById(R.id.send_text);

        String str = "(%f, %f, %f)";
        // Default locale shouldn't give us any bugs, so can suppress lint
        @SuppressLint("DefaultLocale") String coords = String.format(str, init_coords.x, init_coords.y, init_coords.z);
        coords_view.setText(coords);

        // Listen for button clicks
        orientation_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(orientation_button.getText().toString().equals(getString(R.string.Start))) {
                    orientation_button.setText(R.string.Stop);
                    is_orientation_stage = true;
                    is_distance_stage = false;
                }
                else if(orientation_button.getText().toString().equals(getString(R.string.Stop))) {
                    orientation_button.setText(R.string.Start);
                    is_orientation_stage = false;
                    is_distance_stage = false;
                    distance_button.setVisibility(View.VISIBLE);
                    distance_text.setVisibility(View.VISIBLE);
                }
                else {
                    // Something has gone wrong
                    Log.e("Orientation button", "Unexpected value: " + orientation_button.getText());
                }
            }
        });

        distance_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(distance_button.getText().toString().equals(getString(R.string.Start))) {
                    distance_button.setText(R.string.Stop);
                    is_orientation_stage = false;
                    is_distance_stage = true;
                    steps_data = 0.0f;
                }
                else if(distance_button.getText().toString().equals(getString(R.string.Stop))) {
                    distance_button.setText(R.string.Start);
                    is_orientation_stage = false;
                    is_distance_stage = false;
                    send_button.setVisibility(View.VISIBLE);
                    send_text.setVisibility(View.VISIBLE);

                    calculateFinalCoords();

                    String str = "New coordinates: (%f, %f, %f)";
                    // Default locale shouldn't give us any bugs, so can suppress lint
                    @SuppressLint("DefaultLocale") String new_text = String.format(str, final_coords.x, final_coords.y, final_coords.z);
                    send_text.setText(new_text);
                }
                else {
                    // Something has gone wrong
                    Log.e("Distance button", "Unexpected value: " + distance_button.getText());
                }
            }
        });

        send_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Publish coordinates on MQTT topic
                String str = "(%f, %f, %f)";
                // Default locale shouldn't give us any bugs, so can suppress lint
                // Flip axis here to match the CrazyFlie axis
                @SuppressLint("DefaultLocale") String msg = String.format(str, init_coords.x, init_coords.z, init_coords.y);

                MQTTClient mqtt_client = new MQTTClient(getApplicationContext());
                mqtt_client.publish(msg);
            }
        });
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

        // I would be surprised if azimuth is ever exactly 0.0f, so
        if(init_azimuth == 0.0f) {
            init_azimuth = orientation_angles[0];
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
        float step_length = 0.5288f;
        distance_travelled = steps_data * step_length;
    }

    private void calculateFinalCoords() {
        double angle = current_azimuth - init_azimuth;

        // Apply distance to final_coords
        final_coords.x =  init_coords.x + (distance_travelled * (float) Math.cos(angle));
        final_coords.y = init_coords.y;
        final_coords.z = init_coords.z + (-distance_travelled * (float) Math.sin(angle)); // flip axis
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
            if(is_orientation_stage) {
                // Copy data
                accelerometer_data = event.values.clone();

                calculateAzimuth();
                Log.d("Accelerometer", "Calculating azimuth");
            }
        }
        else if(type == Sensor.TYPE_MAGNETIC_FIELD) {
            if(is_orientation_stage) {
                // Copy data
                magnetometer_data = event.values.clone();

                calculateAzimuth();
                Log.d("Magnetometer", "Calculating azimuth");
            }
        }
        else if(type == Sensor.TYPE_STEP_DETECTOR) {
            if(is_distance_stage) {
                // Copy data
                steps_data++;

                calculateDistance();
                Log.d("Step counter", "Calculating distance");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not implementing this method
    }
}