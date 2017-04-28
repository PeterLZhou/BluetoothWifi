package com.peterlzhou.bluetoothwifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import static java.sql.Types.NULL;

/**
 * Created by peterlzhou on 4/18/17.
 */

public class WifiActivity extends AppCompatActivity implements WifiP2pManager.PeerListListener{
    private int PACKETSTHRESHOLD = 10000;
    // private String seenMapFile = "seenMapFile";
    private File seenMapFile = new File("seenMapFile.txt");
    private HashMap<String, Integer> seenPacketsMap = new HashMap<String, Integer>();
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private List<WifiP2pDevice> peersConnect = new ArrayList<WifiP2pDevice>();
    private ArrayList<String> peersName = new ArrayList<String>();
    private final IntentFilter mIntentFilter = new IntentFilter();
    WifiP2pManager.Channel mChannel;
    WifiP2pManager mManager;
    BroadcastReceiver mReceiver;
    WifiP2pConfig config = new WifiP2pConfig();

    private static JSONObject currentJSON = new JSONObject();

    int USER_TYPE;
    WifiP2pManager.PeerListListener mPeerListListener;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        System.out.println("Wifi Activity started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifilayout);

        Intent intent = getIntent();
        USER_TYPE = intent.getIntExtra(MainActivity.USER_TYPE, 0);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new Receiver(mManager, mChannel, this);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                System.out.println("Found em'!");
            }

            @Override
            public void onFailure(int reasonCode) {
                System.out.println("Error code" + reasonCode);
            }
        });
        Button sendAsClient = (Button) findViewById(R.id.sendstuff);
        Button listenAsServer = (Button) findViewById(R.id.listen);
        //Starts the wifi listening section
        sendAsClient.setOnClickListener(
                new Button.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        EditText text = (EditText) findViewById(R.id.message);
                        EditText ip = (EditText) findViewById(R.id.dest_ip);
                        EditText port = (EditText) findViewById(R.id.dest_port);
                        String message = text.getText().toString();
                        if (message == ""){
                            message = "Hello World!";
                        }

                        try{
                            sendData(message, ip.getText().toString(), Integer.parseInt(port.getText().toString()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        listenAsServer.setOnClickListener(
                new Button.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        receiveData();
                    }
                }
        );

        loadSeen();
        cleanSeen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);

        loadSeen();
        cleanSeen();
    }
    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);

        try {
            saveSeen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            saveSeen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerlist) {
        //System.out.println("Peerlist available");
        //System.out.println(peerlist);
        //System.out.println(peerlist.getDeviceList());
        WifiP2pDevice device = peerlist.getDeviceList().iterator().next();
        config.deviceAddress = device.deviceAddress;
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                if (USER_TYPE == 0) {
                    //System.out.println("Server-side code");
                    //System.out.println(config.deviceAddress);
                } else if (USER_TYPE == 1) {
                    System.out.println("Client-side code");
                } else {
                    System.out.println("Error");
                }
                //System.out.println("Successful connection!");
            }

            @Override
            public void onFailure(int reason) {
                System.out.println("Failure with reason " + reason);
            }
        });
    }

    public void sendData(String message, String ip, int port){
        System.out.println("sendasclient");
        Intent serviceIntent = new Intent(this, FileTransferService.class);
        serviceIntent.putExtra("MESSAGE", message);
        serviceIntent.putExtra("go_host", "172.27.90.60");
        serviceIntent.putExtra("go_port", 8888);
        serviceIntent.putExtra("dest_host", ip);
        serviceIntent.putExtra("dest_port", port);
        this.startService(serviceIntent);
    }

    public void receiveData(){
        FileServerAsyncTask FileServerobj = new FileServerAsyncTask(this);
        FileServerobj.execute();
    }

    public void reachWeb(){
        System.out.println("placeholder");
    }


    public void addToSeen(String s, Integer i) {
        seenPacketsMap.put(s, i);
    }

    public void cleanSeen() {
        for(String s: seenPacketsMap.keySet()) {
            if(Calendar.getInstance().getTimeInMillis() - seenPacketsMap.get(s) > PACKETSTHRESHOLD) {
                seenPacketsMap.remove(s);
            }
        }
    }

    public boolean checkSeen(String s) {
        if (seenPacketsMap.keySet().contains(s)) {
            return true;
        }
        return false;
    }

    public void saveSeen() throws IOException {
        try {
            FileOutputStream outStream = new FileOutputStream(seenMapFile);
            OutputStreamWriter outWriter = new OutputStreamWriter(outStream);

            for (String s : seenPacketsMap.keySet()) {
                outWriter.append(s);
                outWriter.append("\n\r");
                outWriter.append(seenPacketsMap.get(s).toString());
                outWriter.append("\n\r");
            }

            outWriter.close();
            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadSeen() {
        FileInputStream is;
        BufferedReader reader;

        try {
            if (seenMapFile.exists()) {
                cleanSeen();
                is = new FileInputStream(seenMapFile);
                reader = new BufferedReader(new InputStreamReader(is));
                String id = reader.readLine();
                int time;
                String temp;
                while(id != null){
                    temp = reader.readLine();
                    if(temp == null) {
                        break;
                    }
                    time = Integer.parseInt(temp);
                    seenPacketsMap.put(id,time);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setCurrentJSON(JSONObject js){
        WifiActivity.currentJSON = js;
    }

    public static JSONObject getCurrentJSON(){
        return WifiActivity.currentJSON;
    }



}
