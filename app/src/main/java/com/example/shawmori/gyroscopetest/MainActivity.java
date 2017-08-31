package com.example.shawmori.gyroscopetest;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Process;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends AppCompatActivity implements SensorEventListener{

    //UI elements
    private TextView gyroX, gyroY, gyroZ, countData;
    private Button seeData, toggle, clear, dataSizeOk, sendData;
    private EditText dataSizeChooser;

    //Sensor elements
    private Sensor sensor;
    private SensorManager sm;

    //Data variables
    private int dataSize = 100;
    private int count = 0;
    private Coordinate[] coordinates;
    private int toastShow = 1;
    private boolean sensorToggle = true;

    //Debugging
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
        Log.d(TAG, hasGyro+" ");

        sm = (SensorManager) getSystemService(SENSOR_SERVICE);

        //Strict mode for server connectivity
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

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
        clear = (Button)findViewById(R.id.clear);
        toggle = (Button) findViewById(R.id.toggle);
        dataSizeChooser = (EditText) findViewById(R.id.dataSizeChooser);
        dataSizeOk = (Button) findViewById(R.id.dataSizeOk);
        sendData = (Button) findViewById(R.id.sendData);
        sendData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Button pressed...");
                new HttpAsyncTask().execute("http://192.168.88.155:8081/post");
            }
        });
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleSensor();
            }
        });
        seeData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                seeDataActivity();
            }
        });
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearData();
            }
        });
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

    /**
     * Changes activity from MainActivity to DataActivity and sends the coordinates for the new activity to use
     */
    public void seeDataActivity(){
        if(count != dataSize){
            Toast.makeText(getApplicationContext(), "Wait for data collection to finish before viewing.", Toast.LENGTH_LONG).show();
        }else {
            Intent i = new Intent(this, DataActivity.class);
            i.putExtra("data", coordinates);
            startActivity(i);
        }
    }

    /**
     * When the START/STOP button is pushed this method is called. It starts and stops the sensor.
     */
    public void toggleSensor(){
        if(sensorToggle){
            sensorToggle = false;
            toggle.setText("START");
        }else{
            sensorToggle = true;
            toggle.setText("STOP");
        }
    }

    /**
     * Clears all stored data. Sensor must be stopped for it to work.
     */
    public void clearData(){
        if (sensorToggle) {
            Toast.makeText(getApplicationContext(), "Stop sensor before clearing data!", Toast.LENGTH_SHORT).show();
        }else{
            resetData();
            coordinates = new Coordinate[dataSize];
        }
    }

    /**
     * Sets the size of the data storage array. Clears any previously stored data. Sensor must be stopped for it to work.
     */
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

    /**
     * Helper method to reset the data.
     */
    private void resetData(){
        count = 0;
        countData.setText("0");
        toastShow = 1;
    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            Log.d(TAG, "doInBackground...");
            return POST(urls[0]);
        }
        @Override
        protected void onPostExecute(String result) {

        }
    }

    private String POST(String url) {
        InputStream inputStream = null;
        String response = "";
        DataOutputStream outputStream = null;
        Log.d(TAG, "POST start");
        try {
            URL urlObj = new URL(url);
            HttpURLConnection urlConnection = (HttpURLConnection) urlObj.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            Log.d(TAG, "URL connections established");
            outputStream= new DataOutputStream(urlConnection.getOutputStream());
            Log.d(TAG, "after output");

            Log.d(TAG, "Stream and BW");

            // THIS IS THE DATA THAT WE ARE SENDING FROM THE APP TO THE SERVER

            //This should be the ID of the sensor
            String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

            JSONObject data = new JSONObject();
            data.put("task", id);
            data.put("x", 1);
            data.put("y", 2);
            data.put("z", 3);

            Log.d(TAG, "Sending:" + data);

            outputStream.writeBytes(data.toString());
            outputStream.flush();
            outputStream.close();

            int status = urlConnection.getResponseCode();
            Log.d(TAG, "The status code is " + status);
            if (status == 200) {
                Log.d(TAG, "Success");
            } else {
                Log.d(TAG, "Unable to send Data");
            }

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return response;

    }

}
