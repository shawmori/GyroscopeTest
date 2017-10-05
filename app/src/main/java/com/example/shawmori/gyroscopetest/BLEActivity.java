package com.example.shawmori.gyroscopetest;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BLEActivity extends AppCompatActivity implements View.OnClickListener {

    private Map<String, BleDevice> mBluetoothMap;
    private ArrayList<BleDevice> mBluetoothList;
    private BleScanner mScanner;

    private BleListAdapter listAdapter;
    private ListView listView;

    private String username = "";

    private BluetoothAdapter mAdapter;

    private Button btnScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);

        mBluetoothList = new ArrayList<>();
        mBluetoothMap = new HashMap<>();

        username = getIntent().getStringExtra("user");
        Log.d("123123", username);

        btnScan = (Button) findViewById(R.id.btnScan);
        btnScan.setOnClickListener(this);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        setTitle("Scan for Device");

        //Check device is BLE capable
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(getApplicationContext(), "Your device does not support BLE!", Toast.LENGTH_SHORT).show();
            finish();
        }

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        mScanner = new BleScanner(this, 7500, -75);

        listAdapter = new BleListAdapter(this, R.layout.ble_device_list_item, mBluetoothList);
        listView = (ListView) findViewById(R.id.bleListView);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(getApplicationContext(), "Connected to " + mBluetoothList.get(i).getName(), Toast.LENGTH_LONG).show();
                Intent mIntent = new Intent(BLEActivity.this, Main2Activity.class);
                mIntent.putExtra("user", username);
                startActivity(mIntent);
                finish();
            }
        });

    }

    @Override
    protected void onStop(){
        super.onStop();
        stopScan();
    }

    @Override
    protected void onPause(){
        super.onPause();
        stopScan();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.btnScan:
                Toast.makeText(getApplicationContext(), "Scan button pressed", Toast.LENGTH_SHORT).show();

                if (!mScanner.isScanning()) {
                    startScan();
                }
                else{
                    stopScan();
                }

            //case R.id.bleListView:
              //  Intent i = new Intent(this, MainActivity.class);
              //  startActivity(i);
        }
    }

    public class BleScanner {
        private final String TAG = "BLEActivity";

        private BLEActivity mainActivity;
        private boolean mScanning;
        private Handler mHandler;

        private long scanPeriod;
        private int signalStrength;
        BleScanner(BLEActivity mainActivity, long scanPeriod, int signalStrength) {
            this.mainActivity = mainActivity;
            this.scanPeriod = scanPeriod;
            this.signalStrength = signalStrength;

            mHandler = new Handler();

            final BluetoothManager bluetoothManager =
                    (BluetoothManager) mainActivity.getSystemService(Context.BLUETOOTH_SERVICE);

            mAdapter = bluetoothManager.getAdapter();
        }

        public boolean isScanning(){
            return mScanning;
        }

        public void start(){
            if (mAdapter == null || !mAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
                mainActivity.stopScan();
            }
            else{
                scanLeDevice(true);
            }
        }

        public void stop(){
            scanLeDevice(false);
        }

        private void scanLeDevice(final boolean enable) {

            if(enable && !mScanning) {
                Toast.makeText(getApplicationContext(), "Scanning for BLE devices...", Toast.LENGTH_LONG).show();

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Stopping BLE Scanner...", Toast.LENGTH_LONG).show();

                        mScanning = false;
                        mAdapter.stopLeScan(mLeScanCallback);

                        mainActivity.stopScan();
                    }
                }, scanPeriod);

                mScanning = true;
                mAdapter.startLeScan(mLeScanCallback);
            }
        }

        private BluetoothAdapter.LeScanCallback mLeScanCallback =
                new BluetoothAdapter.LeScanCallback(){
                    @Override
                    public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord){
                        final int newRssi = rssi;
                        if (rssi > signalStrength) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(TAG, "Adding device to device list");
                                    Log.d(TAG, "Address: " + device.getAddress() + "\nName: " + device.getName());
                                    mainActivity.addDevice(device, newRssi);
                                }
                            });
                        }
                    }
                };
    }

    private void addDevice(BluetoothDevice device, int newRssi) {

        String address = device.getAddress();

        if (!mBluetoothMap.containsKey(address)) {
            BleDevice newDevice = new BleDevice(device, newRssi);
            mBluetoothMap.put(address, newDevice);
            mBluetoothList.add(newDevice);
        }
        else{
            mBluetoothMap.get(address).setRssi(newRssi);
        }

    }

    private void startScan(){
        btnScan.setText("Scanning...");

        mBluetoothList.clear();
        mBluetoothMap.clear();

        mScanner.start();
    }

    private void stopScan() {
        btnScan.setText("Start Scanning");
        mScanner.stop();
    }

    public class BleListAdapter extends ArrayAdapter<BleDevice> {

        private Activity activity;
        private int layoutResourceId;
        private ArrayList<BleDevice> devices;

        public BleListAdapter(Activity activity, int resource, ArrayList<BleDevice> objects){
            super(activity.getApplicationContext(), resource, objects);

            devices = objects;
            this.activity = activity;
            layoutResourceId = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                LayoutInflater inflater =
                        (LayoutInflater) activity.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(layoutResourceId, parent, false);
            }

            BleDevice device = devices.get(position);
            String name = device.getName();
            String address = device.getAddress();
            int rssi = device.getRssi();

            Log.d("BLEActivity", "PUTTING SENSOR INTO LISTVIEW\nName: " + name + "\naddress: " + address + "\nRSSI: " + rssi);

            TextView tvName = (TextView) convertView.findViewById(R.id.tvName);
            if (name != null && name.length() > 0) {
                tvName.setText(device.getName());
            }
            else{
                tvName.setText("No Name");
            }

            TextView tvRssi = (TextView) convertView.findViewById(R.id.tvRssi);
            if (name != null && name.length() > 0) {
                tvRssi.setText("RSSI: " + device.getRssi());
            }
            else{
                tvRssi.setText("No RSSI");
            }

            TextView tvMac = (TextView) convertView.findViewById(R.id.tvMac);
            if (name != null && name.length() > 0) {
                tvMac.setText("Mac Address: " + device.getAddress());
            }
            else{
                tvMac.setText("No Mac Address");
            }
            return convertView;
        }

        public BleDevice getItem(int pos) {
            return devices.get(pos);
        }
    }

}
