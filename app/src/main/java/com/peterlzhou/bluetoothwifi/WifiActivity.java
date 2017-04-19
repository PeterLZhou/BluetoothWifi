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

import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by peterlzhou on 4/18/17.
 */

public class WifiActivity extends AppCompatActivity implements WifiP2pManager.PeerListListener{
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private List<WifiP2pDevice> peersConnect = new ArrayList<WifiP2pDevice>();
    private ArrayList<String> peersName = new ArrayList<String>();
    private final IntentFilter mIntentFilter = new IntentFilter();
    WifiP2pManager.Channel mChannel;
    WifiP2pManager mManager;
    BroadcastReceiver mReceiver;
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }
    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerlist){
        System.out.println("Peerlist available");
        //nSystem.out.println(peerlist);
        System.out.println(peerlist.getDeviceList());
        WifiP2pDevice device = peerlist.getDeviceList().iterator().next();
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                if (USER_TYPE == 0){
                    System.out.println("Server-side code");
                }
                else if (USER_TYPE == 1){
                    System.out.println("Client-side code");
                }
                else{
                    System.out.println("Error");
                }
                System.out.println("Successful connection!");
            }

            @Override
            public void onFailure(int reason) {
                System.out.println("Failure with reason " + reason);
            }
        });

    }
}
