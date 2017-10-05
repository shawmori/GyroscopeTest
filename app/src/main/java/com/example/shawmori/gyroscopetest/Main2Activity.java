package com.example.shawmori.gyroscopetest;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main2Activity extends AppCompatActivity implements SensorEventListener{

    private ArrayList<Float> data = new ArrayList<>();

    //Statistic variables
    private int mTotalEvents = 0;
    private int mAverage = 0;
    private String username = "";

    //Sensor elements
    private Sensor sensor;
    private SensorManager sm;

    //Debugging
    private static final String TAG = "Main2Activity";

    //Server
    private String mUrl = "http://130.195.6.134:8081/postuser";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        username = getIntent().getStringExtra("user");

        //Initialise sensor
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        //Run average() every minute
        Runnable avgRunnable = new Runnable() {
            @Override
            public void run() {
                average();
            }
        };
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
        exec.scheduleAtFixedRate(avgRunnable, 3, 3, TimeUnit.SECONDS);
    }

    private void average() {
        Log.d(TAG, Integer.toString(data.size()));
        float total = 0;
        for (float f : data) {
            total += f;
        }
        float average = total / data.size();
        mAverage = Math.round(average);
        new HttpAsyncTask().execute(mUrl);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        float z = sensorEvent.values[2];
        data.add(z);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return POST(urls[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            data.clear();
        }
    }

    private String POST(String url) {
        InputStream inputStream = null;
        String response = "";
        DataOutputStream outputStream = null;
        try {
            URL urlObj = new URL(url);
            HttpURLConnection urlConnection = (HttpURLConnection) urlObj.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            outputStream = new DataOutputStream(urlConnection.getOutputStream());

            JSONObject data = new JSONObject();
            data.put("User", username);
            data.put("Val", mAverage+5);
            Log.d(TAG, Integer.toString(mAverage));

            outputStream.writeBytes(data.toString());
            outputStream.flush();
            outputStream.close();

            int status = urlConnection.getResponseCode();
            if (status == 202) {
                response = "success";
            } else if(status == 404) {
                response = "fail";
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return response;

    }
}
