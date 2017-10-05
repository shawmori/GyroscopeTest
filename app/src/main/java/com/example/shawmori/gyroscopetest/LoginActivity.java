package com.example.shawmori.gyroscopetest;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameField, passwordField;
    private Button login;

    private String username, password = null;
    private String mUrl = "http://130.195.6.134:8081/CheckUser";

    private final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        usernameField = (EditText) findViewById(R.id.username);
        passwordField = (EditText) findViewById(R.id.password);
        login = (Button) findViewById(R.id.loginButton);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkLogin();
            }
        });
    }

    private void checkLogin() {
        username = usernameField.getText().toString();
        password = passwordField.getText().toString();

        new HttpAsyncTask().execute(mUrl);

    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return POST(urls[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            if(result.equals("success")) {
                Intent mIntent = new Intent(getApplicationContext(), BLEActivity.class);
                mIntent.putExtra("user", username);
                startActivity(mIntent);
            }
        }
    }

        private String POST(String url) {
            Log.d(TAG, "Start of POST");
            InputStream inputStream = null;
            String response = "";
            DataOutputStream outputStream = null;
            try {
                Log.d(TAG, "Start of setting URL");
                URL urlObj = new URL(url);
                HttpURLConnection urlConnection = (HttpURLConnection) urlObj.openConnection();
                urlConnection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                outputStream = new DataOutputStream(urlConnection.getOutputStream());

                Log.d(TAG, "Create JSON");
                JSONObject data = new JSONObject();
                data.put("User", username);
                data.put("Password", password);

                Log.d(TAG, "Send JSON");
                outputStream.writeBytes(data.toString());
                outputStream.flush();
                outputStream.close();

                int status = urlConnection.getResponseCode();
                Log.d(TAG, Integer.toString(status));
                if (status == 202) {
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Successfully logged in...\nWelcome Back " + username + "!", Toast.LENGTH_SHORT).show();
                        }
                    });
                    response = "success";
                } else if(status == 201){
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Successfully added new user!", Toast.LENGTH_SHORT).show();
                        }
                    });
                    response = "success";
                } else if(status == 404) {
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Incorrect username or password!", Toast.LENGTH_SHORT).show();
                        }
                    });
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
