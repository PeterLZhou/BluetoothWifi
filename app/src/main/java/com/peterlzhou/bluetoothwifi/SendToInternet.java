package com.peterlzhou.bluetoothwifi;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Created by peterlzhou on 4/28/17.
 */

public class SendToInternet extends IntentService {
    StringBuilder sb = new StringBuilder();

    public SendToInternet() {
        super("SendToInternet");
    }

    /*@Override
    public void onCreate() {
        // TODO Auto-generated method stub

        System.out.println("Service started!");
        super.onCreate();
        mHandler = new Handler();
    }*/
    /*
     * (non-Javadoc)
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        System.out.println("Handling Intent");
        HttpURLConnection client = null;
        try {
            URL url = new URL("https://bluetoothwifi.herokuapp.com/ping");
            client = (HttpURLConnection) url.openConnection();
            client.setRequestMethod("POST");
            //Not sure how these three lines work yet
            client.setRequestProperty("Content-Type", "application/json");
            //Specify the length of my post request?
            //client.setRequestProperty("Content-Length", "");
            client.setRequestProperty("Accept", "application/json");
            //Allow urlconnection to write output???
            //This is redundant with client.setRequestMethod("POST")
            client.setDoOutput(true);
            client.connect();
            //Create the JSON object
            JSONObject node = WifiActivity.getCurrentJSON();
            //Specify the attributes of the JSON object:
            //pokemonName is a string, latitude and longitude are doubles, captureTime is a long in milliseconds
            node.put("srcip", node.get("destIP"));
            node.put("port", node.get("destPort"));
            node.put("message", node.get("body"));
            //This is for debugging purposes
            //System.out.println(node.toString(4));
            //Open up the output stream so we can write our JSON object into the server
            OutputStreamWriter output = new OutputStreamWriter(client.getOutputStream());
            //Write the JSON object
            output.write(node.toString());
            output.flush();
            //close the output stream
            output.close();
            int HttpResult =client.getResponseCode();
            System.out.println("The response code is " + HttpResult);
            if(HttpResult ==HttpURLConnection.HTTP_OK){
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        client.getInputStream(),"utf-8"));
                String line = null;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();

                System.out.println(""+sb.toString());

            }else{
                System.out.println(client.getResponseMessage());
            }
        }
        catch (IOException | JSONException e) {
            e.printStackTrace();
            System.out.println("Error");
        }
        finally{
            System.out.println("Done");
        }

    }
}
