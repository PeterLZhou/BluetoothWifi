package com.peterlzhou.bluetoothwifi;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    public static final String USER_TYPE = "com.peterlzhou.bluetoothwifi.USER_TYPE";
    public static final int SERVER = 0;
    public static final int CLIENT = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button wifiServerButton = (Button) findViewById(R.id.wifi);
        Button bluetoothServerButton = (Button) findViewById(R.id.bluetooth);
        Button bluetoothLEServerButton = (Button) findViewById(R.id.bluetoothle);
        Button wifiClientButton = (Button) findViewById(R.id.wifi2);
        Button bluetoothClientButton = (Button) findViewById(R.id.bluetooth2);
        Button bluetoothLEClientButton = (Button) findViewById(R.id.bluetoothle2);

        //Starts the wifi listening section
        wifiServerButton.setOnClickListener(
                new Button.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        startWifi(v, SERVER);
                    }
                }
        );

        //Starts the bluetooth listening section
        bluetoothServerButton.setOnClickListener(
                new Button.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        startBluetooth(v, SERVER);
                    }
                }
        );

        //Starts the bluetooth listening section
        bluetoothLEServerButton.setOnClickListener(
                new Button.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        startBluetoothLE(v, SERVER);
                    }
                }
        );


        //Starts the wifi sending section
        wifiClientButton.setOnClickListener(
                new Button.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        startWifi(v, CLIENT);
                    }
                }
        );

        //Starts the bluetooth sending section
        bluetoothClientButton.setOnClickListener(
                new Button.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        startBluetooth(v, CLIENT);
                    }
                }
        );

        //Starts the bluetooth sending section
        bluetoothLEClientButton.setOnClickListener(
                new Button.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        startBluetoothLE(v, CLIENT);
                    }
                }
        );
    }

    public void startWifi(View view, int flag){
        Intent intent = new Intent(this, WifiActivity.class);
        intent.putExtra(USER_TYPE, flag);
        startActivity(intent);
    }

    public void startBluetooth(View view, int flag){
        Intent intent = new Intent(this, BluetoothActivity.class);
        intent.putExtra(USER_TYPE, flag);
        startActivity(intent);
    }

    public void startBluetoothLE(View view, int flag){
        Intent intent = new Intent(this, BluetoothLEActivity.class);
        intent.putExtra(USER_TYPE, flag);
        startActivity(intent);
    }
}

