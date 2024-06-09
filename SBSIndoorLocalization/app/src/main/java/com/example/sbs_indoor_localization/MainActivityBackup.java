package com.example.sbs_indoor_localization;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivityBackup extends AppCompatActivity implements SensorEventListener {

    private EditText logTextArea;

    private static final int REQUEST_CODE_ACTIVITY_RECOGNITION = 123;
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private TextView stepCountTextView;

    private int stepCount = 0;
    private boolean stepCountBool = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // UI binding
        logTextArea = findViewById(R.id.logTextArea);
        logTextArea.setEnabled(false);
        Button stepCountButton = findViewById(R.id.button1);
        Button timeRecordingButton = findViewById(R.id.button2);

        // Button Logging
        stepCountButton.setOnClickListener(v -> {
            appendLog("Step Count");
        });
        timeRecordingButton.setOnClickListener(v -> {
            appendLog("Time Recording");
        });


        // Sensor Manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);


        // Step Sensor
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        // Permission Request
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, REQUEST_CODE_ACTIVITY_RECOGNITION);
        } else {
            // 권한이 이미 granted 된 경우, 센서 등록
            registerStepCounterSensor();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_ACTIVITY_RECOGNITION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 granted 된 경우, 센서 등록
                registerStepCounterSensor();
            } else {
                // 권한이 denied 된 경우, 사용자에게 알림 표시
                appendLog("Activity Recognition Permission Request Denied");
            }
        }
    }

    private void registerStepCounterSensor() {
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
            appendLog("Step Counter Registered!");
        } else {
            appendLog("No Step Sensor");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 센서 이벤트 리스너 해제
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            // 걸음 수 업데이트
            stepCount = (int) event.values[0];
            appendLog("Step Count : " + stepCount);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 센서 정확도 변경 시 처리
    }

    private void appendLog(String msg) {
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        logTextArea.append(currentTime + "   " + msg + "\n");
    }
}