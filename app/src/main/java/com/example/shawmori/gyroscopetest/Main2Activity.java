package com.example.shawmori.gyroscopetest;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main2Activity extends AppCompatActivity implements SensorEventListener{

    private ArrayList<Float> serverData = new ArrayList<>();
    private ArrayList<Float> localData = new ArrayList<>();

    //Statistic variables
    private String username = "";

    //Sensor elements
    private Sensor sensor;
    private SensorManager sm;

    //Debugging
    private static final String TAG = "Main2Activity";

    //Server
    private String mUrlPost = "https://sigbackontrack.herokuapp.com/postuser";
    private String mUrlVib = "https://sigbackontrack.herokuapp.com/badposture";

    private int localScanInterval = 20;
    private int serverScanInterval = 5;
    private int badPostureCount = 0;

    private TextView text, usernameBanner;
    private Button startStop, logout;

    private ScheduledExecutorService exec;
    private Runnable avgRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        usernameBanner = (TextView) findViewById(R.id.usernameBanner);
        username = getIntent().getStringExtra("user");
        usernameBanner.setText("Logged in as: " + username);
        text = (TextView)findViewById(R.id.textView);
        startStop = (Button) findViewById(R.id.startStop);
        startStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startStopPressed();
            }
        });
        logout = (Button) findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logoutPressed();
            }
        });

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        setTitle("Back on Track");

        //Initialise sensor
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        //Run sendPostEvent() every serverScanInterval (in seconds)
        avgRunnable = new Runnable() {
            @Override
            public void run() {
                sendPostEvent();
            }
        };
        exec = Executors.newScheduledThreadPool(1);
        exec.scheduleAtFixedRate(avgRunnable, serverScanInterval, serverScanInterval, TimeUnit.SECONDS);


    }

    private void logoutPressed() {
        sm.unregisterListener(this);
        exec.shutdown();
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
    }

    private void startStopPressed() {
        Log.d(TAG, "START STOP PRESSED");
        if (startStop.getText().equals("Stop Tracking")) {
            sm.unregisterListener(this);
            exec.shutdown();
            startStop.setText("Start Tracking");
        }else if(startStop.getText().equals("Start Tracking")){
            sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            exec = Executors.newScheduledThreadPool(1);
            exec.scheduleAtFixedRate(avgRunnable, serverScanInterval, serverScanInterval, TimeUnit.SECONDS);
            startStop.setText("Stop Tracking");
        }
        Log.d(TAG, "START STOP END");
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        exec.shutdown();
        sm.unregisterListener(this);
    }

    private float average(ArrayList<Float> data) {
        float total = 0;
        for (float f : data) {
            total += f;
        }
        float average = total / data.size();
        return average;

    }

    private void sendPostEvent() {
        int average = Math.round(average(serverData));
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        new HttpAsyncTask().execute(mUrlPost, "User", username, "Val", Integer.toString(average+5), "Dat", date, "Tim", time);
        serverData.clear();
    }

    private void sendVibrateEvent() {
        float average = average(localData);
        if (average < -2 || average > 5) {
            Log.d(TAG, badPostureCount + "");
            badPostureCount++;
            if(badPostureCount == 5){
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(1000);
                Toast.makeText(getApplicationContext(), "Bad Posture", Toast.LENGTH_LONG).show();
                String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                new HttpAsyncTask().execute(mUrlVib, "Userid", username, "Date", date, "Vib", "1");
                badPostureCount = 0;
            }
        } else {
            badPostureCount = 0;
        }
        localData.clear();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        float z = sensorEvent.values[2];
        text.setText("Tracking your posture!\n" + String.valueOf(z));
        serverData.add(z);
        localData.add(z);

        if (localData.size() == localScanInterval) {
            sendVibrateEvent();
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return POST(urls);
        }

        @Override
        protected void onPostExecute(String result) {

        }
    }

    private String POST(String... url) {
        InputStream inputStream = null;
        String response = "";
        DataOutputStream outputStream = null;
        try {
            URL urlObj = new URL(url[0]);
            HttpURLConnection urlConnection = (HttpURLConnection) urlObj.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            outputStream = new DataOutputStream(urlConnection.getOutputStream());

            JSONObject data = new JSONObject();

            for(int i = 1; i < url.length; i++){
                data.put(url[i], url[i + 1]);
                i++;
            }


            outputStream.writeBytes(data.toString());
            outputStream.flush();
            outputStream.close();

            int status = urlConnection.getResponseCode();
            Log.d(TAG, Integer.toString(status));
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
