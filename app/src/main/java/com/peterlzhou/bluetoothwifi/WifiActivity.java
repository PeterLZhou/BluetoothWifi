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
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.sql.Types.NULL;

/**
 * Created by peterlzhou on 4/18/17.
 */

public class WifiActivity extends AppCompatActivity implements WifiP2pManager.PeerListListener{
    private int PACKETSTHRESHOLD = 10000; // 10 seconds until packet is stale
    private static final String TEMPDESTIP = "0.0.0.0"; // Default Dest IP if none given
    private static final String TEMPDESTPORT = "8888"; // Default Dest Port if none given
    private String NATFile = "NATFile"; // File to store the NAT into

    // Maps packet ID to tuple<source ip, timestamp>. Used to match unique packet ID to IP address
    private static HashMap<String, Pair<String, Long>> NAT = new HashMap<String, Pair<String, Long>>();

    // List of all peers, connected peers, and names of peers
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private List<WifiP2pDevice> peersConnect = new ArrayList<WifiP2pDevice>();
    private ArrayList<String> peersName = new ArrayList<String>();

    private final IntentFilter mIntentFilter = new IntentFilter();
    WifiP2pManager.Channel mChannel;
    WifiP2pManager mManager;
    BroadcastReceiver mReceiver;
    WifiP2pConfig config = new WifiP2pConfig();

    private String uniqueFile = "uniqueFile"; // File to store the unique packets map into
    // Used by Device C to receive only 1 of every packet
    private static HashMap<String, Long> uniquePacketsMap = new HashMap<String, Long>();

    private String sentWaitingAckFile = "sentWaitingAckFile"; // File to store the unique packets map into
    // Used by Device A to track which packets are sent but awaiting Acks
    public static HashMap<String, Pair<JSONObject, Long>> sentWaitingAckMap = new HashMap<String, Pair<JSONObject, Long>>();
    //Empty JSON object - will be populated with current received packet
    private static JSONObject currentJSON = new JSONObject();

    int USER_TYPE;

    // Constructor
    public WifiActivity() throws FileNotFoundException {}

    @Override
    protected void onCreate(Bundle savedInstanceState){
        System.out.println("Wifi Activity started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifilayout);

        Intent intent = getIntent();
        USER_TYPE = intent.getIntExtra(MainActivity.USER_TYPE, 0);

        //Initialize WifiP2pManager, Channel, and Receiver
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new Receiver(mManager, mChannel, this);
        //Add states to listen for
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // Discover WiFi Direct Peers
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

        //Schedules cleanup every 10 seconds
        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {
                        cleanNAT();
                        cleanUnique();
                        //Resend packets if ack not received
                        try {
                            sendBufferedPackets();
                        }
                        catch (JSONException e){
                            System.out.println("Invalid JSON");
                        }
                    }
                }, 0, 10, TimeUnit.SECONDS);

        // Buttons to determine action
        Button sendAsClient = (Button) findViewById(R.id.sendstuff);
        Button listenAsServer = (Button) findViewById(R.id.listen);
        Button sendToInternet = (Button) findViewById(R.id.sendtointernet);

        // Starts the wifi listening section
        sendAsClient.setOnClickListener(
                new Button.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        // Get input from UI
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

                        // Send intended message to WiFi Direct server
                        try{
                            long pack_id = System.currentTimeMillis();
                            sendData(message, ipText, Integer.parseInt(portText), false, pack_id);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        // We are acting as the WiFi Direct Server
        listenAsServer.setOnClickListener(
                new Button.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        receiveData();
                    }
                }
        );

        // We are acting as the internet client
        sendToInternet.setOnClickListener(
                new Button.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        reachWeb();
                    }
                }
        );

        // Load and clean tables from file
        loadNAT();
        cleanNAT();
        loadUnique();
        cleanUnique();
        loadSent();
        cleanSent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);

        // Load and clean tables from file
        loadNAT();
        cleanNAT();
        loadUnique();
        cleanUnique();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);

        // Save the tables to files
        try {
            saveNAT();
            saveUnique();
            saveSent();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Save the tables to files
        try {
            saveNAT();
            saveUnique();
            saveSent();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerlist) {
        try {
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

    public void sendData(String message, String ip, int port, boolean ack, long pack_id){
        System.out.println("sendasclient");

        // Give the necessary data to FileTransferService to send to the WiFi Direct Recipient
        Intent serviceIntent = new Intent(this, FileTransferService.class);
        serviceIntent.putExtra("MESSAGE", message);
        //Set this to be the IP of device B
        serviceIntent.putExtra("go_host", "172.27.83.183");
        serviceIntent.putExtra("go_port", 8888);
        serviceIntent.putExtra("dest_host", ip);
        serviceIntent.putExtra("dest_port", port);
        serviceIntent.putExtra("ack", ack);
        serviceIntent.putExtra("pack_id", pack_id);
        this.startService(serviceIntent);
    }

    public void receiveData(){
        // Process data using the FileServerAsyncTask
        FileServerAsyncTask FileServerobj = new FileServerAsyncTask(this);
        FileServerobj.execute();
    }

    public void reachWeb(){
        // Give the necessary data to SendToInternet service to send to the Internet Recipient
        System.out.println("Sending to web");
        Intent serviceIntent2 = new Intent(this, SendToInternet.class);
        this.startService(serviceIntent2);
    }

    /*
     * Adds the values to unique packets map if the string is not already an id in the map.
     * Return true if value added.
     * Otherwise, return false.
     */
    public static boolean addToUniquePacketsIfNot(String s, Long l) {
        if (WifiActivity.uniquePacketsMap.keySet().contains(s)) {
            return false;
        }

        WifiActivity.uniquePacketsMap.put(s,l);
        return true;
    }


    public static void addToNAT(String s, String i) {
        // Add an entry to the NAT table
        WifiActivity.NAT.put(s, new Pair<String, Long>(i, Calendar.getInstance().getTimeInMillis()));

        System.out.println("Added " + s + ":" + i + " to NAT map");
    }

    public static void addToSent(String s, JSONObject jo) {
        // Add an entry to the NAT table
        WifiActivity.sentWaitingAckMap.put(s, new Pair<JSONObject, Long>(jo, Calendar.getInstance().getTimeInMillis()));

        System.out.println("Added " + s + " to Sent map");
    }

    public void cleanUnique() {
        // Clean stale packets
        for(String s: uniquePacketsMap.keySet()) {
            if(Calendar.getInstance().getTimeInMillis() - uniquePacketsMap.get(s) > PACKETSTHRESHOLD) {
                uniquePacketsMap.remove(s);
            }
        }

        System.out.println("uniquePacketsMap Map Cleaned");
        for (String s : uniquePacketsMap.keySet()) {
            System.out.println("Key: " + s + ", Value: " + uniquePacketsMap.get(s));
        }
    }

    public void cleanSent() {
        // Clean stale packets
        for(String s: sentWaitingAckMap.keySet()) {
            if(Calendar.getInstance().getTimeInMillis() - sentWaitingAckMap.get(s).second > PACKETSTHRESHOLD) {
                sentWaitingAckMap.remove(s);
            }
        }

        System.out.println("sentWaitingAckMap Map Cleaned");
        for (String s : sentWaitingAckMap.keySet()) {
            System.out.println("Key: " + s + ", Value: " + sentWaitingAckMap.get(s));
        }
    }

    public void cleanNAT() {
        // Clean stale packets
        for(String s: NAT.keySet()) {
            if(Calendar.getInstance().getTimeInMillis() - NAT.get(s).second > PACKETSTHRESHOLD) {
                NAT.remove(s);
            }
        }

        System.out.println("NAT Map Cleaned");
        for (String s : NAT.keySet()) {
            System.out.println("Key: " + s + ", Value: " + NAT.get(s));
        }
    }

    public void saveNAT() throws IOException {
        // Save NAT to a file
        try {
            FileOutputStream outStream = openFileOutput(NATFile, Context.MODE_PRIVATE);
            OutputStreamWriter outWriter = new OutputStreamWriter(outStream);

            for (String s : NAT.keySet()) {
                outWriter.append(s);
                outWriter.append("\n\r");
                outWriter.append(NAT.get(s).first.toString());
                outWriter.append("\n\r");
                outWriter.append(NAT.get(s).second.toString());
                outWriter.append("\n\r");
            }

            outWriter.close();
            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("NAT Map Saved");
        for (String s : NAT.keySet()) {
            System.out.println("Key: " + s + ", Value: " + NAT.get(s));
        }
    }

    public void saveUnique() throws IOException {
        // Save the map of unique packets received to a file
        try {
            FileOutputStream outStream = openFileOutput(uniqueFile, Context.MODE_PRIVATE);
            OutputStreamWriter outWriter = new OutputStreamWriter(outStream);

            for (String s : uniquePacketsMap.keySet()) {
                outWriter.append(s);
                outWriter.append("\n\r");
                outWriter.append(uniquePacketsMap.get(s).toString());
                outWriter.append("\n\r");
            }

            outWriter.close();
            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Unique Map Saved");
        for (String s : uniquePacketsMap.keySet()) {
            System.out.println("Key: " + s + ", Value: " + uniquePacketsMap.get(s));
        }
    }

    public void saveSent() throws IOException {
        // Save the map of sent packets to a file
        try {
            FileOutputStream outStream = openFileOutput(uniqueFile, Context.MODE_PRIVATE);
            OutputStreamWriter outWriter = new OutputStreamWriter(outStream);

            for (String s : sentWaitingAckMap.keySet()) {
                outWriter.append(s);
                outWriter.append("\n\r");
                outWriter.append(sentWaitingAckMap.get(s).second.toString());
                outWriter.append("\n\r");

                JSONObject pack = sentWaitingAckMap.get(s).first;
                Iterator<String> iter = pack.keys();
                String packet_host = null;

                String packetID = null;
                while (iter.hasNext()) {
                    String key = iter.next();

                    outWriter.append(key);
                    outWriter.append("\n\r");
                    outWriter.append(pack.get(key).toString());
                    outWriter.append("\n\r");
                }
                outWriter.append("\n\r");
            }

            outWriter.close();
            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Sent Map Saved");
        for (String s : sentWaitingAckMap.keySet()) {
            System.out.println("Key: " + s + ", Value: " + sentWaitingAckMap.get(s));
        }
    }

    public void loadNAT() {
        // Load up the NAT table from a file
        try {
            FileInputStream is = openFileInput(NATFile);
            BufferedReader reader;

            cleanNAT();
            is = new FileInputStream(NATFile);
            reader = new BufferedReader(new InputStreamReader(is));
            String id = reader.readLine();
            String host;
            long time;
            String tempTime;
            while(id != null){
                host = reader.readLine();
                if(host == null) {
                    break;
                }

                tempTime = reader.readLine();
                if(tempTime == null) {
                    break;
                }
                time = Long.parseLong(tempTime);

                NAT.put(id,new Pair<String, Long>(host, time));
            }
            is.close();

            System.out.println("NAT Map Loaded");
            for (String s : NAT.keySet()) {
                System.out.println("Key: " + s + ", Value: " + NAT.get(s));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadUnique() {
        // Load up the table of unique packets received from a file
        try {
            FileInputStream is = openFileInput(uniqueFile);
            BufferedReader reader;

            cleanUnique();
            is = new FileInputStream(uniqueFile);
            reader = new BufferedReader(new InputStreamReader(is));
            String id = reader.readLine();
            long time;
            String temp;
            while(id != null){
                temp = reader.readLine();
                if(temp == null) {
                    break;
                }
                time = Long.parseLong(temp);
                uniquePacketsMap.put(id,time);
            }
            is.close();

            System.out.println("uniquePacketsMap Map Loaded");
            for (String s : uniquePacketsMap.keySet()) {
                System.out.println("Key: " + s + ", Value: " + uniquePacketsMap.get(s));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadSent() {
        // Load up the table of sent packets from a file
        try {
            FileInputStream is = openFileInput(sentWaitingAckFile);
            BufferedReader reader;

            cleanUnique();
            is = new FileInputStream(sentWaitingAckFile);
            reader = new BufferedReader(new InputStreamReader(is));
            String id = reader.readLine();
            long time;
            String temp;
            String temp2;

            JSONObject pack = new JSONObject();
            while(id != null){
                temp = reader.readLine();
                if(temp == null) {
                    break;
                }
                time = Long.parseLong(temp);

                while(!(temp = reader.readLine()).equals("\n\r")) {
                    if (temp.equals("destPort")) {
                        pack.put(temp, Integer.parseInt(reader.readLine()));
                    } else if (temp.equals("ack")) {
                        temp2 = reader.readLine();
                        if(temp2.equals("true")) {
                            pack.put(temp, true);
                        } else {
                            pack.put(temp, false);
                        }
                    } else {
                        pack.put(temp, reader.readLine());
                    }
                }

                sentWaitingAckMap.put(id,new Pair<JSONObject, Long>(pack, time));
            }
            is.close();

            System.out.println("sentWaitingAckMap Map Loaded");
            for (String s : sentWaitingAckMap.keySet()) {
                System.out.println("Key: " + s + ", Value: " + sentWaitingAckMap.get(s));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendBufferedPackets() throws JSONException {
        cleanSent();

        // Check un-ack'd packet map and send anything that still needs to be sent
        for (String s : sentWaitingAckMap.keySet()) {
            sendData((String) sentWaitingAckMap.get(s).first.get("body"),
                    (String) sentWaitingAckMap.get(s).first.get("destIP"),
                    (Integer) sentWaitingAckMap.get(s).first.get("destPort"),
                    (Boolean) sentWaitingAckMap.get(s).first.get("ack"),
                    (Long) sentWaitingAckMap.get(s).first.get("ID"));
        }
    }

    public static void setCurrentJSON(JSONObject js){
        WifiActivity.currentJSON = js;
    }

    public static JSONObject getCurrentJSON(){
        return WifiActivity.currentJSON;
    }



}
