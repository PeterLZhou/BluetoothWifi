package com.peterlzhou.bluetoothwifi;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button wifiButton = (Button) findViewById(R.id.wifi);
        Button bluetoothButton = (Button) findViewById(R.id.bluetooth);

        //Starts the wifi listening/sending section
        wifiButton.setOnClickListener(
                new Button.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        startWifi(v);
                    }
                }
        );

        //Starts the bluetooth listening/sending section
        bluetoothButton.setOnClickListener(
                new Button.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        startBluetooth(v);
                    }
                }
        );
    }

    public void startWifi(View view){
        Intent intent = new Intent(this, WifiActivity.class);
        startActivity(intent);
    }

    public void startBluetooth(View view){
        Intent intent = new Intent(this, BluetoothActivity.class);
        startActivity(intent);
    }
}

