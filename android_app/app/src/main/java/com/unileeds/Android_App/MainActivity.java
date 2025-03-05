package com.unileeds.Android_App;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private Vector3D init_coords = new Vector3D();

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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button calibrate_button = (Button) findViewById(R.id.calibrate_button);
        EditText edit_x = (EditText) findViewById(R.id.location_input);
        EditText edit_y = (EditText) findViewById(R.id.location_input_2);
        EditText edit_z = (EditText) findViewById(R.id.location_input_3);

        calibrate_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                init_coords.x = Float.parseFloat(edit_x.getText().toString());
                init_coords.y = Float.parseFloat(edit_y.getText().toString());
                init_coords.z = Float.parseFloat(edit_z.getText().toString());
                Log.d("Calibrate onClick", String.format("(%f, %f, %f)", init_coords.x, init_coords.y, init_coords.z));

                // Let's just publish these coordinates for now as a MQTT test
                String str = "(%f, %f, %f)";
                // Default locale shouldn't give us any bugs, so can suppress lint
                @SuppressLint("DefaultLocale") String msg = String.format(str, init_coords.x, init_coords.y, init_coords.z);

                MQTTClient mqtt_client = new MQTTClient(getApplicationContext());
                mqtt_client.publish(msg);
            }
        });

        Button readings_button = (Button) findViewById(R.id.readings_button);

        readings_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, TrackPosition.class);
//                MainActivity.this.startActivity(intent);

                Intent intent = new Intent(MainActivity.this, StepCounter.class);
                MainActivity.this.startActivity(intent);
            }
        });

    }
}