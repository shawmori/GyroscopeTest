package com.example.shawmori.gyroscopetest;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Process;
import android.os.StrictMode;
import android.os.Vibrator;
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
import java.util.Arrays;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //UI elements
    private TextView gyroX, gyroY, gyroZ, countData;
    private Button seeData, toggle, clear, dataSizeOk, sendData;
    private EditText dataSizeChooser;

    //Sensor elements
    private Sensor sensor;
    private SensorManager sm;

    //Data variables
    private int dataSize = 100;
    private int userCount = 0;
    private Coordinate[] userCoordinates;
    private int toastShow = 1;
    private boolean sensorToggle = true;

    private BluetoothDevice mDevice;
    private BluetoothGatt mGatt;
    private BluetoothGattCharacteristic mCharacteristic;
    private BluetoothGattDescriptor mDescriptor;

    private static final UUID mUuid = UUID.fromString("06E40002-B5A3-F393-E0A9-E50E24DCCA9E");

    private int badPostureCount = 0;
    private int numDataItemsToAverage = 30;
    private Coordinate[] localCoordinateData = new Coordinate[numDataItemsToAverage];
    private int localCount = 0;

    //Debugging
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(getIntent().getExtras() != null) {
            mDevice = getIntent().getParcelableExtra("device");
        }
        mGatt = mDevice.connectGatt(this, false, mGattCallback);
        mGatt.discoverServices();
        mCharacteristic = findCharacteristic();
        //mGatt.setCharacteristicNotification(mCharacteristic, true);
        //mDescriptor = mCharacteristic.getDescriptor(mUuid);
        //mDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
      //  mGatt.writeDescriptor(mDescriptor);
        //if(readCharacteristic()){
           // Log.d(TAG, "Reading characteristics");
       // }


        userCoordinates = new Coordinate[dataSize];

        //Checks features that the phone has
        PackageManager packageManager = getPackageManager();
        boolean hasGyro = packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE);
        boolean hasAcc = packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER);
        Log.d(TAG, hasGyro + " ");

        sm = (SensorManager) getSystemService(SENSOR_SERVICE);

        //Strict mode for server connectivity
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //If there is no gyroscope use accelerometer
        if (hasAcc) {
            Log.d(TAG, "Acclerometer added");
            sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        //Neither of these features show a toast
        if (sensor == null) {
            Log.d(TAG, "No sensor -- quit");
            Process.killProcess(Process.myPid());
        }
        //Sets UI elements
        countData = (TextView) findViewById(R.id.countData);
        gyroX = (TextView) findViewById(R.id.gyroX);
        gyroY = (TextView) findViewById(R.id.gyroY);
        gyroZ = (TextView) findViewById(R.id.gyroZ);
        seeData = (Button) findViewById(R.id.seeData);
        clear = (Button) findViewById(R.id.clear);
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

    private boolean readCharacteristic() {
        if (mGatt != null) {
            return mGatt.readCharacteristic(mCharacteristic);
        }
        return false;
    }

    public BluetoothGattCharacteristic findCharacteristic() {
        Log.d(TAG, "IN METHOD!@#!@#!@#!@#!@#");
        if (mGatt == null) {
            Log.d(TAG, "NULL");
            return null;
        }

        for (BluetoothGattService service : mGatt.getServices()) {
            Log.d(TAG, service.getUuid().toString());
            BluetoothGattCharacteristic charac = service.getCharacteristic(mUuid);

        }
        return null;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Successfully connected to GATT");
            }else{
                Log.d(TAG, "FAILED TO GATYTY");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "In onCharacteristRead");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }
    };
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //Boolean to toggle the sensor
        if (sensorToggle) {

            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            countData.setText("Items Stored: " + userCount);
            gyroX.setText(String.valueOf(x));
            gyroY.setText(String.valueOf(y));
            gyroZ.setText(String.valueOf(z));

            //When data is full display toast and stop more data being added.
            //toastShow ensures toast only shows once. Used for debugging, very little user use.
            if (toastShow == 1) {
                if (userCount == dataSize) {
                    Toast.makeText(getApplicationContext(), "Data entry complete.\n" + dataSize + " pieces of data added.", Toast.LENGTH_SHORT).show();
                    toastShow = 0;
                } else {
                    Coordinate coord = new Coordinate(x, y, z);
                    userCoordinates[userCount] = coord;
                    userCount++;
                }
            }



            //Actual implementation
           if (badPostureCount == 5) {
                sendPostureEvent();
                badPostureCount = 0;
            } else if (localCount % numDataItemsToAverage == 0 && localCount != 0) {
                int currentCount = localCount;
                Log.d(TAG, "Bad posture count: " + badPostureCount);
                getZAverage(Arrays.copyOfRange(localCoordinateData, currentCount - numDataItemsToAverage, currentCount));
            }
            if (localCount == numDataItemsToAverage) {
                localCoordinateData = new Coordinate[numDataItemsToAverage];
                localCount = 0;
            }
                localCoordinateData[localCount] = new Coordinate(x, y, z);
                localCount++;
        }
    }

    private void sendPostureEvent() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(1500);
        Toast.makeText(getApplicationContext(), "Bad Posture", Toast.LENGTH_LONG).show();
    }

    private void getZAverage(Coordinate[] coordinates) {
        float zAv = 0;

        for (Coordinate c : coordinates) {
            zAv += c.getZ();
        }

        zAv = zAv / numDataItemsToAverage;

        if (zAv < 1 || zAv > 5) {
            badPostureCount++;
        } else {
            badPostureCount = 0;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /**
     * Changes activity from MainActivity to DataActivity and sends the userCoordinates for the new activity to use
     */
    public void seeDataActivity() {
        if (userCount != dataSize) {
            Toast.makeText(getApplicationContext(), "Wait for data collection to finish before viewing.", Toast.LENGTH_LONG).show();
        } else {
            Intent i = new Intent(this, DataActivity.class);
            i.putExtra("data", userCoordinates);
            startActivity(i);
        }
    }

    /**
     * When the START/STOP button is pushed this method is called. It starts and stops the sensor.
     */
    public void toggleSensor() {
        if (sensorToggle) {
            sensorToggle = false;
            toggle.setText("START");
        } else {
            sensorToggle = true;
            toggle.setText("STOP");
        }
    }

    /**
     * Clears all stored data. Sensor must be stopped for it to work.
     */
    public void clearData() {
        if (sensorToggle) {
            Toast.makeText(getApplicationContext(), "Stop sensor before clearing data!", Toast.LENGTH_SHORT).show();
        } else {
            resetData();
            userCoordinates = new Coordinate[dataSize];
        }
    }

    /**
     * Sets the size of the data storage array. Clears any previously stored data. Sensor must be stopped for it to work.
     */
    public void setDataSize() {
        if (sensorToggle) {
            Toast.makeText(getApplicationContext(), "Stop sensor before resetting size!", Toast.LENGTH_SHORT).show();
        } else {
            dataSize = Integer.parseInt(dataSizeChooser.getText().toString());
            dataSizeChooser.setText("");
            resetData();
            userCoordinates = new Coordinate[dataSize];
        }
    }

    /**
     * Helper method to reset the data.
     */
    private void resetData() {
        userCount = 0;
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
            outputStream = new DataOutputStream(urlConnection.getOutputStream());
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return response;

    }

}
