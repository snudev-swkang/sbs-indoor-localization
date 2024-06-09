package com.example.sbs_indoor_localization;

import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.os.Environment;
import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
//import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity{

    // CSV File Related
    private File csvFile;
    private FileWriter csvWriter;

    // Permission Codes
    private static final int REQUEST_CODE_ACTIVITY_RECOGNITION = 123;
    private static final int REQUEST_CODE_LOCATION = 789;
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 456;

    // Sensors
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private Sensor accelSensor, gyroSensor;
    private Sensor magSensor, humidSensor;
    private SensorEventListener stepCounterListener;
    private SensorEventListener accelGyroListener;
    private SensorEventListener magHumidListener;

    // UIs
    private EditText logTextArea;
//    private TextView stepCountTextView;
    private TextView turnCountView;
    private TextView turnDegreeView;
    private TextView magXView;
    private TextView magYView;
    private TextView magZView;
//    private TextView humidView;


    // Data Related
    private int stepCount = 0;
    private boolean stepCountBool = false;
    private long movingTime = 0;
    private long movingStartTime = 0;
    private long movingEndTime = 0;
    private float rotationSum = 0;
    private int turnCount = 0;
    private boolean accelGyroBool = false;
    private static final float ROTATION_THRESHOLD = 0.785f; // 방향전환 감지를 위한 Threshold
    private static final float NANO_TO_SEC = 1.0f / 1000000000.0f;
    private static final int SENSOR_DELAY_CUSTOM = 1000000; // 1sec interval (1,000,000 mirosec)
    private long prevGyroTimestamp = 0;
    private float[] magneticData = new float[3];
//    private float humidityData = 0;
    private boolean magHumidBool = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // UI binding
        logTextArea = findViewById(R.id.logTextArea);
        logTextArea.setEnabled(false);
        Button stepCountButton = findViewById(R.id.button1);
        Button accelGyroRecordingButton = findViewById(R.id.button2);
        Button magHumidButton = findViewById(R.id.button3);
        turnCountView = findViewById(R.id.textView1);
        turnDegreeView = findViewById(R.id.textView2);
        magXView = findViewById(R.id.textView3);
        magYView = findViewById(R.id.textView4);
        magZView = findViewById(R.id.textView5);
//        humidView = findViewById(R.id.textView6);


        // Button Logging
        stepCountButton.setOnClickListener(v -> {
//            String msg = "Step Count ";
            String msg = "Step/Turn Count ";
            if(!stepCountBool) {
                // Step Count Part
                startStepCounterListener();

                // Gyro. Part
                startAccelGyroListener();
                movingStartTime = System.currentTimeMillis();
                msg += "Started";
            } else {
                movingEndTime = System.currentTimeMillis();
                movingTime = movingEndTime - movingStartTime;
                appendLog(String.valueOf(movingTime));

                openOrCreateCSVFile("T");
                writeToCSV(stepCount, movingTime, turnCount, rotationSum, "T");
                closeCSVFile();

                movingStartTime = 0;
                movingEndTime = 0;
                movingTime = 0;
                stopStepCounterListener();
                stopAccelGyroListener();
                msg += "Stopped";
            }
            appendLog(msg);
        });
        accelGyroRecordingButton.setOnClickListener(v -> {
            String msg = "Turn Count ";
//            if(!accelGyroBool) {
//                startAccelGyroListener();
//                msg += "Started";
//            } else {
//                turnCount = 0;
//                rotationSum = 0;
//                stopAccelGyroListener();
//                msg += "Stopped";
//            }
            appendLog(msg);
        });
        magHumidButton.setOnClickListener(v -> {
            String msg = "Mag. and Humid ";
            if(!magHumidBool) {
                openOrCreateCSVFile("D");
                startMagHumidListener();
                msg += "Started";
            } else {
                closeCSVFile();
                stopMagHumidListener();
                msg += "Stopped";
            }
            appendLog(msg);
        });


        // Sensor Manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Sensor Initialization
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
//        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

//        humidSensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
//        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_RELATIVE_HUMIDITY);
//        if(!sensorList.isEmpty()) appendLog("There is Humidity Sensor");
//        else appendLog("No Humidity Sensor");



        // Permission Request
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, REQUEST_CODE_ACTIVITY_RECOGNITION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION);
        }

    }

    private void startStepCounterListener() {
        stepCounterListener = new SensorEventListener() {
            // Step Counter Logics
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
                    stepCount++;
                    appendLog("Step Count : " + stepCount);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        // Sensor Register
        if(stepCounterSensor != null) {
            sensorManager.registerListener(stepCounterListener, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
            appendLog("Step Counter Registered!");
            stepCountBool = true;
        } else {
            appendLog("No Step Sensor");
        }
    }

    private void stopStepCounterListener() {
        sensorManager.unregisterListener(stepCounterListener);
        stepCounterListener = null;
        appendLog("Step Counter Unregistered!");
        stepCountBool = false;
        stepCount = 0;
    }


    private void startAccelGyroListener() {
        accelGyroListener = new SensorEventListener() {
            // Accel. and Gyro. Logics
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    float[] rotation = event.values;
                    long currentGyroTimestamp = event.timestamp;

                    if (prevGyroTimestamp != 0) {
                        float timeDelta = (currentGyroTimestamp - prevGyroTimestamp) * NANO_TO_SEC;
                        detectTurnChange(rotation, timeDelta);
                    }

                    prevGyroTimestamp = currentGyroTimestamp;
//                    turnDegreeView.setText(String.valueOf(rotation[1]));
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        // Sensor Register
//        if((accelSensor != null) && (gyroSensor != null)) {
        if(gyroSensor != null) {
//            sensorManager.registerListener(accelGyroListener, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(accelGyroListener, gyroSensor, SENSOR_DELAY_CUSTOM);
            appendLog("Accel Gyro Registered!");
            accelGyroBool = true;
        } else {
            appendLog("No Accel Gyro Sensor");
        }
    }

    private void stopAccelGyroListener() {
        sensorManager.unregisterListener(accelGyroListener);
        accelGyroListener = null;
        appendLog("Accel Gyro Unregistered!");
        accelGyroBool = false;
        turnCount = 0;
        rotationSum = 0;
    }

    private void detectTurnChange(float[] rotation, float timeDelta) {
        float rotationX = rotation[0] * timeDelta;
        float rotationY = rotation[1] * ((float) SENSOR_DELAY_CUSTOM /1000000);
        float rotationZ = rotation[2] * timeDelta;

        if (Math.abs(rotationX) > ROTATION_THRESHOLD) {
            // X-axis Detected
            appendLog("X-axis rotation detected");
        }

        if (Math.abs(rotationY) > ROTATION_THRESHOLD) {
            // Y-axis Detected
            turnCount++;
            rotationSum += rotationY;
            turnCountView.setText(String.valueOf(turnCount));
            turnDegreeView.setText(String.valueOf(rotationSum));
            appendLog("Y-axis rotation detected");
        }

        if (Math.abs(rotationZ) > ROTATION_THRESHOLD) {
            // Z-axis Detected
            appendLog("Z-axis rotation detected");
        }
    }

    private void startMagHumidListener() {
        magHumidListener = new SensorEventListener() {
            // Magnetic and Humidity Logics
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    float[] magneticField = event.values;
                    magXView.setText(String.valueOf(magneticField[0]));
//                    magneticData[0] = magneticField[0];
                    magYView.setText(String.valueOf(magneticField[1]));
//                    magneticData[1] = magneticField[1];
                    magZView.setText(String.valueOf(magneticField[2]));
//                    magneticData[2] = magneticField[2];
                    writeMagToCSV(magneticField[0], magneticField[1], magneticField[2], "D", "D2");
//                     appendLog("Magnetic Detected");
                }

//                if (event.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
//                    float humidity = event.values[0];
//                    humidView.setText(String.valueOf(humidity));
//                    appendLog("Humidity Detected");
//                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        // Sensor Register
        if(magSensor != null) {
            sensorManager.registerListener(magHumidListener, magSensor, SENSOR_DELAY_CUSTOM);
            appendLog("Magnetic Registered!");
            magHumidBool = true;
        } else {
//            magHumidBool = false;
            appendLog("No Magnetic Sensor");
        }

//        if(humidSensor != null) {
//            sensorManager.registerListener(magHumidListener, humidSensor, SensorManager.SENSOR_DELAY_NORMAL);
//            appendLog("Humidity Registered!");
//            magHumidBool = true;
//        } else {
//            magHumidBool = false;
//            appendLog("No Humidity Sensor");
//        }
    }

    private void stopMagHumidListener() {
        sensorManager.unregisterListener(magHumidListener);
        magHumidListener = null;
        appendLog("Mag. and Humid. Unregistered!");
        magHumidBool = false;
        magneticData[0] = 0;
        magneticData[1] = 0;
        magneticData[2] = 0;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_ACTIVITY_RECOGNITION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                appendLog("Activity Recognition Permission Request Granted");
            } else {
                // Permission Denied
                appendLog("Activity Recognition Permission Request Denied");
            }
        }
        if (requestCode == REQUEST_CODE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                appendLog("Location Permission Request Granted");
            } else {
                // Permission Denied
                appendLog("Location Permission Request Denied");
            }
        }
    }


    private void appendLog(String msg) {
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        logTextArea.append(currentTime + "   " + msg + "\n");
    }

    private void openOrCreateCSVFile(String areaCode) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
        }

        try {
            String fileName = "moving_data_" + areaCode + ".csv";
//            String fileName = "magnetic_data.csv";
            File externalDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            csvFile = new File(externalDir, fileName);
            if (csvFile.exists()) {
                csvWriter = new FileWriter(csvFile, true); // 기존 파일에 추가 모드로 열기
            } else {
                csvWriter = new FileWriter(csvFile);
                csvWriter.write("Timestamp,StepCount,MovingTime,TurnCount,LastRotation,Area\n"); // 새 파일인 경우 헤더 행 작성
//                csvWriter.write("Timestamp,MagX,MagY,MagZ,AreaCode,LocationCode\n"); // 새 파일인 경우 헤더 행 작성
            }
            appendLog("File Recording Started");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeToCSV(int stepCount, long movingTime, int turnCount, float lastRotation, String areaCode) {
        try {
            long timestamp = System.currentTimeMillis();
            csvWriter.write(timestamp + "," + stepCount + "," + movingTime + "," + turnCount + "," + lastRotation + "," + areaCode + "\n");
            csvWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
            closeCSVFile();
        }
    }

    private void writeMagToCSV(float magX, float magY, float magZ, String areaCode, String locationCode) {
        try {
            long timestamp = System.currentTimeMillis();
            csvWriter.write(timestamp + "," + magX + "," + magY + "," + magZ + "," + areaCode + "," + locationCode + "\n");
            csvWriter.flush();
            appendLog("Magnetic Data Flushed");
        } catch (IOException e) {
            e.printStackTrace();
            closeCSVFile();
        }
    }


    private void closeCSVFile() {
        try {
            csvFile = null;
            csvWriter.close();
            csvWriter = null;
            appendLog("File Recording Stopped");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}