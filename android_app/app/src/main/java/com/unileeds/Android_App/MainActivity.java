package com.unileeds.Android_App;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private WifiManager wifiManager;
    private BroadcastReceiver wifiReceiver;

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

        Log.i("Success", "Initial");

        // Set up WiFi manager and receiver for later scanning
        // Using Android's "Wi-Fi scanning overview for help"
        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        wifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                if(success) {
                    //onScanSuccess();
                }
                else {
                    onScanFailure();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        this.registerReceiver(wifiReceiver, intentFilter);
    }

    private void scanForWifi() {
        // Requesting permissions
        // For ease, let's just do them one-by-one for now
        if(ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.ACCESS_WIFI_STATE) ==
                PackageManager.PERMISSION_GRANTED) {
            // Skip
        }
        else {
            requestPermissions(new String[] {Manifest.permission.ACCESS_WIFI_STATE},1);
        }
        if(ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.CHANGE_WIFI_STATE) ==
                PackageManager.PERMISSION_GRANTED) {
            // Skip
        }
        else {
            requestPermissions(new String[] {Manifest.permission.CHANGE_WIFI_STATE},1);
        }
        if(ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            // Skip
        }
        else {
            requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},1);
        }
        if(ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            // Skip
        }
        else {
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION},1);
        }

        Log.d("Permissions", "Should be granted?");
        boolean success = wifiManager.startScan();
        if(success) {
            onScanSuccess();
        }
        else {
            onScanFailure();
        }
    }

    private void onScanSuccess() {
        // Ask for permission later i cba
        Log.i("Success", "Scan");

        // Since this is just a test application
        // let's just assume the user hasn't removed their permissions.
        @SuppressLint("MissingPermission") List<ScanResult> access_points = wifiManager.getScanResults();
        for(ScanResult point : access_points) {
            Log.i("Access point found", String.format("Name: %s, RSSI: %d", point.BSSID, point.level));
        }
    }

    private void onScanFailure() {
        Log.i("Failure", "Scan");
    }

    // Overriding menu methods so we can access scan button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.scan_button) {
            Log.i("Success", "Button");
            scanForWifi();
        }
        return super.onOptionsItemSelected(item);
    }
}