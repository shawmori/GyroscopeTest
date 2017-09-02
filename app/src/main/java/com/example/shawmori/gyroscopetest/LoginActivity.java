package com.example.shawmori.gyroscopetest;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameField, passwordField;
    private Button login;

    private String username, password = null;
    private String defaultUser = "name";
    private String defaultPass = "";

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
                changeView();
            }
        });
    }

    private void changeView() {
        username = usernameField.getText().toString();
        password = passwordField.getText().toString();

        if (username.equalsIgnoreCase(defaultUser) && password.equals(defaultPass)) {
            Intent i = new Intent(this, BLEActivity.class);
            startActivity(i);
        }
        else{
            Toast.makeText(getApplicationContext(), "Incorrect username or password", Toast.LENGTH_LONG);
        }
    }

}
