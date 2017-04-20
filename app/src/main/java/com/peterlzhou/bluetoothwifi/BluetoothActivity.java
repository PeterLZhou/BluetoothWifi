package com.peterlzhou.bluetoothwifi;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by robertrtung on 4/20/17.
 */

public class BluetoothActivity extends Activity {
    private final static String TAG = BluetoothActivity.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    ListView deviceListView;
    ArrayAdapter arrayAdapter;
    String newDeviceName;
    String newDeviceAddress;

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                newDeviceName = device.getName();
                newDeviceAddress = device.getAddress(); // MAC address
                BluetoothActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        arrayAdapter.add(newDeviceName + ": " + newDeviceAddress);
                        arrayAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
    };
    HashSet<Device> pairedDevicesSet = new HashSet<Device>();
    ArrayList<String> discoveredDevicesAddresses = new ArrayList<String>();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView( R.layout.bluetoothlayout );

        deviceListView = (ListView) findViewById(R.id.DeviceList);

        arrayAdapter = new ArrayAdapter(this, R.layout.bluetoothdevice,
                R.id.device_in_list, discoveredDevicesAddresses);

        deviceListView.setAdapter(arrayAdapter);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // OnCreate, list paired devices and nearby devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            Device d = new Device();
            d.name = device.getName();
            d.address = device.getAddress();
            pairedDevicesSet.add(d);
        }

        // DISCOVERING DEVICES NEEDS THIS
        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        mBluetoothAdapter.startDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    static class Device {
        String name;
        String address;
    }
}
