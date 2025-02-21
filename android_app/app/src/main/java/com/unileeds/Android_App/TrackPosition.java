package com.unileeds.Android_App;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

public class TrackPosition extends Activity implements SensorEventListener {

    private SensorManager sensorManager;

    // sensors
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor magnetometer;

    // sensor readings
    private float[] acc_val = new float[3];
    private float[] gyro_val = new float[3];
    private float[] magnet_val = new float[3];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);
        Intent intent = getIntent();

        // initialise sensors
        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, acc_val, 0, acc_val.length);
                    //Log.i("accelerometer", String.format("(%f, %f, %f)", acc_val[0], acc_val[1], acc_val[2]));
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        }, accelerometer, SensorManager.SENSOR_DELAY_UI);

        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    System.arraycopy(event.values, 0, gyro_val, 0, gyro_val.length);
                    //Log.i("gyroscope", String.format("(%f, %f, %f)", gyro_val[0], gyro_val[1], gyro_val[2]));
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        }, gyroscope, SensorManager.SENSOR_DELAY_UI);

        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, magnet_val, 0, magnet_val.length);
                    Log.i("magnetometer", String.format("(%f, %f, %f)", magnet_val[0], magnet_val[1], magnet_val[2]));
                }
            } 

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        }, magnetometer, SensorManager.SENSOR_DELAY_UI);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // TODO ?
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //TODO: Come back to this
    }
}
