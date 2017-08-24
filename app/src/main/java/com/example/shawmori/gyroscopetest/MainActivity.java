package com.example.shawmori.gyroscopetest;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private TextView gyroX, gyroY, gyroZ, countData;
    private Button seeData, toggle, clear, dataSizeOk;
    private EditText dataSizeChooser;

    private Sensor sensor;
    private SensorManager sm;

    private int dataSize = 100;
    private int count = 0;
    private Coordinate[] coordinates;
    private int toastShow = 1;

    private boolean sensorToggle = true;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        coordinates = new Coordinate[dataSize];

        //Checks features that the phone has
        PackageManager packageManager = getPackageManager();
        boolean hasGyro = packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE);
        boolean hasAcc = packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER);

        sm = (SensorManager) getSystemService(SENSOR_SERVICE);

        //Sets gyroscope first as this is what we really want
        if(hasGyro) {
            Log.d(TAG, "Gyroscope added");
            sensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        //If there is no gyroscope use accelerometer
        else if(hasAcc){
            Log.d(TAG, "Acclerometer added");
            sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        //Neither of these features show a toast
        if(sensor == null){
            Log.d(TAG, "No sensor -- quit");
            Process.killProcess(Process.myPid());
        }
        //Sets UI elements
        countData = (TextView) findViewById(R.id.countData);
        gyroX = (TextView) findViewById(R.id.gyroX);
        gyroY = (TextView) findViewById(R.id.gyroY);
        gyroZ = (TextView) findViewById(R.id.gyroZ);
        seeData = (Button) findViewById(R.id.seeData);
        seeData.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                seeDataActivity();
            }
        });
        toggle = (Button) findViewById(R.id.toggle);
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleSensor();
            }
        });
        clear = (Button)findViewById(R.id.clear);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearData();
            }
        });
        dataSizeChooser = (EditText) findViewById(R.id.dataSizeChooser);
        dataSizeOk = (Button) findViewById(R.id.dataSizeOk);
        dataSizeOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDataSize();
            }
        });

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //Boolean to toggle the sensor
        if(sensorToggle) {

            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            countData.setText("Items Stored: " + count);
            gyroX.setText(String.valueOf(x));
            gyroY.setText(String.valueOf(y));
            gyroZ.setText(String.valueOf(z));

            //When data is full display toast and stop more data being added.
            //toastShow ensures toast only shows once.
            if (toastShow == 1) {
                if (count == dataSize) {
                    Toast.makeText(getApplicationContext(), "Data entry complete.\n" + dataSize + " pieces of data added.", Toast.LENGTH_SHORT).show();
                    toastShow = 0;
                } else {
                    Coordinate coord = new Coordinate(x, y, z);
                    coordinates[count] = coord;
                    count++;
                }
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void seeDataActivity(){
        if(count != dataSize){
            Toast.makeText(getApplicationContext(), "Wait for data collection to finish before viewing.", Toast.LENGTH_LONG).show();
        }else {
            Intent i = new Intent(this, DataActivity.class);
            i.putExtra("data", coordinates);
            startActivity(i);
        }
    }

    public void toggleSensor(){
        if(sensorToggle){
            sensorToggle = false;
            toggle.setText("START");
        }else{
            sensorToggle = true;
            toggle.setText("STOP");
        }
    }

    public void clearData(){
        if (sensorToggle) {
            Toast.makeText(getApplicationContext(), "Stop sensor before clearing data!", Toast.LENGTH_SHORT).show();
        }else{
            resetData();
            coordinates = new Coordinate[dataSize];
        }
    }

    public void setDataSize(){
        if (sensorToggle) {
            Toast.makeText(getApplicationContext(), "Stop sensor before resetting size!", Toast.LENGTH_SHORT).show();
        }else{
            dataSize = Integer.parseInt(dataSizeChooser.getText().toString());
            dataSizeChooser.setText("");
            resetData();
            coordinates = new Coordinate[dataSize];
        }
    }

    private void resetData(){
        count = 0;
        countData.setText("0");
        toastShow = 1;
    }
}
