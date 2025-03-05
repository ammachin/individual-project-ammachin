package com.unileeds.Android_App;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class StepCounter extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if(isGranted) {
                    Log.d("Permission", "Granted");
                }
                else {
                    Log.d("Permission", "denied");
                }
            });

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_step_counter);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
             Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Intent intent = getIntent();

        // Request permission to recognise activity
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_GRANTED) {
            stepCounterInit();
        }
        else {
            requestPermissionLauncher.launch(
                    Manifest.permission.ACTIVITY_RECOGNITION
            );
        }
    }

    private void stepCounterInit() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor step_counter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        Sensor magnetometer = sensorManager.getDefaultSensor((Sensor.TYPE_MAGNETIC_FIELD));
        Sensor gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if(event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                    TextView step_text = (TextView) findViewById(R.id.step_text);
                    step_text.setText("Steps: " + event.values[0]);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                //TODO: Come back to this
            }
        }, step_counter, SensorManager.SENSOR_DELAY_UI);

        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                TextView magnet_text = (TextView) findViewById(R.id.magnet_text);
                magnet_text.setText("Magnetometer reading: " + event.values[0]);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                //TODO: Come back to this
            }
        }, magnetometer, SensorManager.SENSOR_DELAY_UI);

        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                TextView gyro_text = (TextView) findViewById(R.id.gyro_text);
                gyro_text.setText("Gyroscope reading: " + event.values[0]);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // TODO: Come back to this
            }
        }, gyroscope, SensorManager.SENSOR_DELAY_UI);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        // TODO: Come back to this
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //TODO: come back to this
    }
}

