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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static java.sql.Types.NULL;

/**
 * Created by peterlzhou on 4/18/17.
 */

public class WifiActivity extends AppCompatActivity implements WifiP2pManager.PeerListListener{
    private int PACKETSTHRESHOLD = 10000;
    private static final String TEMPDESTIP = "0.0.0.0";
    private static final String TEMPDESTPORT = "8888";
    private String seenMapFile = "seenMapFile";
    // private File seenMapFile = new File("seenMapFile.txt");
    //Maps packet ID to tuple<source ip, source port, dest ip, dest port, timestamp>
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

    public WifiActivity() throws FileNotFoundException {
    }

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
        Button sendToInternet = (Button) findViewById(R.id.sendtointernet);
        //Starts the wifi listening section
        sendAsClient.setOnClickListener(
                new Button.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        EditText text = (EditText) findViewById(R.id.message);
                        EditText ip = (EditText) findViewById(R.id.dest_ip);
                        EditText port = (EditText) findViewById(R.id.dest_port);
                        String message = text.getText().toString();
                        String ipText = ip.getText().toString();
                        String portText = port.getText().toString();
                        if (message == ""){
                            message = "Hello World!";
                        }
                        if (ipText == "") {
                            ipText = TEMPDESTIP;
                            System.out.println("IP Default used");
                        }
                        if (portText == "") {
                            portText = TEMPDESTPORT;
                            System.out.println("Port Default used");
                        }

                        try{
                            sendData(message, ipText, Integer.parseInt(portText));
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
        sendToInternet.setOnClickListener(
                new Button.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        reachWeb();
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
        try {
            //System.out.println("Peerlist available");
            //System.out.println(peerlist);
            //System.out.println(peerlist.getDeviceList());
            Iterator<WifiP2pDevice> mIterator = peerlist.getDeviceList().iterator();
            WifiP2pDevice device;
            while (mIterator.hasNext()) {
                device = mIterator.next();
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
        }
        catch (NoSuchElementException e){
            System.out.println(e);
        }
    }

    public void sendData(String message, String ip, int port){
        System.out.println("sendasclient");
        Intent serviceIntent = new Intent(this, FileTransferService.class);
        serviceIntent.putExtra("MESSAGE", message);
        serviceIntent.putExtra("go_host", "172.27.83.183");
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
        System.out.println("Sending to web");
        Intent serviceIntent2 = new Intent(this, SendToInternet.class);
        this.startService(serviceIntent2);
    }


    public static void addToSeen(String s, Integer i) {
        seenPacketsMap.put(s, i);

        System.out.println("Added " + s + ":" + i + " to packet map");
    }

    public void cleanSeen() {
        for(String s: seenPacketsMap.keySet()) {
            if(Calendar.getInstance().getTimeInMillis() - seenPacketsMap.get(s) > PACKETSTHRESHOLD) {
                seenPacketsMap.remove(s);
            }
        }

        System.out.println("Packet Map Cleaned");
        for (String s : seenPacketsMap.keySet()) {
            System.out.println("Key: " + s + ", Value: " + seenPacketsMap.get(s));
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
            FileOutputStream outStream = openFileOutput(seenMapFile, Context.MODE_PRIVATE);
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

        System.out.println("Packet Map Saved");
        for (String s : seenPacketsMap.keySet()) {
            System.out.println("Key: " + s + ", Value: " + seenPacketsMap.get(s));
        }
    }

    public void loadSeen() {
        try {
            FileInputStream is = openFileInput(seenMapFile);
            BufferedReader reader;

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
            is.close();

            System.out.println("Packet Map Loaded");
            for (String s : seenPacketsMap.keySet()) {
                System.out.println("Key: " + s + ", Value: " + seenPacketsMap.get(s));
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
