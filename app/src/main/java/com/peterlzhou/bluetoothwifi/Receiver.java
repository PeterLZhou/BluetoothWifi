package com.peterlzhou.bluetoothwifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;

/**
 * Created by peterlzhou on 4/18/17.
 */

public class Receiver extends BroadcastReceiver {
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WifiActivity mActivity;
    //Constructor
    public Receiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       WifiActivity activity) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mActivity = activity;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Determine if Wifi P2P mode is enabled or not, alert
            // the Activity.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                System.out.println("Wifi is on");
            } else {
                System.out.println("Wifi is off");
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            //System.out.println("Found peers!");
            // The peer list has changed!  We should probably do something about
            // that.
            if (mManager != null) {
                mManager.requestPeers(mChannel, mActivity);
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            // Connection state changed!  We should probably do something about
            // that.

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            //Device's wifi state changed
        }
    }

}
