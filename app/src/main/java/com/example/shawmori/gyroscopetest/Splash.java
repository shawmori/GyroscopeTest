package com.example.shawmori.gyroscopetest;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class Splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {;

            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_splash);

            int secondsDelayed = 2;
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    startActivity(new Intent(Splash.this, LoginActivity.class));
                    finish();
                }
            }, secondsDelayed * 1000);
        }
    }
