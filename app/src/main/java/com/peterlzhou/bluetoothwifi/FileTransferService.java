package com.peterlzhou.bluetoothwifi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class FileTransferService extends IntentService {
    // private static final String TEMPDESTIP = "0.0.0.0";
    // private static final String TEMPDESTPORT = "8888";
    Handler mHandler;

    public static final int SOCKET_TIMEOUT = 5000;
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";

    public static  int PORT = 8888;
    public static final String inetaddress = "inetaddress";
    public static final int ByteSize = 512;
    public static final String Extension = "extension";
    public static final String Filelength = "filelength";
    public FileTransferService(String name) {
        super(name);
    }

    public FileTransferService() {
        super("FileTransferService");
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        System.out.println("Service started!");
        super.onCreate();
        mHandler = new Handler();
    }
    /*
     * (non-Javadoc)
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        System.out.println("Sending message!");
        Context context = getApplicationContext();
        String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
        Socket socket = new Socket();
        int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);
        String message = intent.getExtras().getString("MESSAGE");
        try {
            socket.bind(null);
            socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

            // Set Packet JSON
            JSONObject pack = new JSONObject();
            WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
            String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
            pack.put("srcIP", ip);
            pack.put("destIP", intent.getExtras().getString("dest_host"));
            pack.put("destPort", intent.getExtras().getInt("dest_port"));
            pack.put("ID", System.currentTimeMillis());
            pack.put("body", message);
            pack.put("ack", false);

            /*OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
            out.write(pack.toString());*/
            OutputStream stream = socket.getOutputStream();
            stream.write(pack.toString().getBytes(Charset.forName("UTF-8")));
            stream.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            //e.printStackTrace();
            //CommonMethods.e("Unable to connect host", "service socket error in wififiletransferservice class");
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                if (socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // Give up
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("Wrapping up!");
        }


    }
}