package com.cmartosreyes.waterquality;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.UUID;

public class HomeActivity extends AppCompatActivity {

    TextView mEmail;
    Button mLogOutBtn, mBtNavButton;
    Button mButtonWeb;
    String email;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //setTheme(R.style.Theme_WaterQuality);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Bundle bundle = getIntent().getExtras();
        email = bundle.getString("email");

        mEmail = findViewById(R.id.emailTextView);
        mLogOutBtn = findViewById(R.id.logOutButton);
        mBtNavButton = findViewById(R.id.btNavButton);

        mButtonWeb = findViewById(R.id.buttonWeb);

        setup(email);
    }

    private void setup(String email){

        setTitle("Inicio");
        mEmail.setText(email);

        mLogOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                showLogin();
            }
        });

        mBtNavButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBtScreen();
            }
        });

        mButtonWeb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWeb();
            }
        });

    }

    private void showWeb() {
        Intent WebIntent = new Intent(this, WebActivity.class);
        startActivity(WebIntent);
    }

    private void showBtScreen() {
        Intent btIntent = new Intent(this, BtActivity.class);
        btIntent.putExtra("email",email);
        startActivity(btIntent);
    }

    private void showLogin(){
        Intent loginIntent = new Intent(this, AuthActivity.class);
        startActivity(loginIntent);
    }

    @Override
    public void onBackPressed() {

    }

}