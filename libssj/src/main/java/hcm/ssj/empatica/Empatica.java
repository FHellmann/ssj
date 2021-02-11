/*
 * Empatica.java
 * Copyright (c) 2018
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken, Simon Flutura,
 * Vitalijs Krumins, Antonio Grieco
 * *****************************************************
 * This file is part of the Social Signal Interpretation for Java (SSJ) framework
 * developed at the Lab for Human Centered Multimedia of the University of Augsburg.
 *
 * SSJ has been inspired by the SSI (http://openssi.net) framework. SSJ is not a
 * one-to-one port of SSI to Java, it is an approximation. Nor does SSJ pretend
 * to offer SSI's comprehensive functionality and performance (this is java after all).
 * Nevertheless, SSJ borrows a lot of programming patterns from SSI.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package hcm.ssj.empatica;

import android.bluetooth.BluetoothDevice;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Base64;

import com.empatica.empalink.EmpaDeviceManager;
import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.config.EmpaSensorType;
import com.empatica.empalink.config.EmpaStatus;
import com.empatica.empalink.delegate.EmpaStatusDelegate;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJApplication;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;

/**
 * Created by Michael Dietz on 15.04.2015.
 */
public class Empatica extends Sensor implements EmpaStatusDelegate {
    public final Options options = new Options();
    protected EmpaDeviceManager deviceManager;
    protected EmpaticaListener listener;
    protected boolean empaticaInitialized;
    public Empatica() {
        _name = "Empatica";
        empaticaInitialized = false;
    }

    @Override
    public OptionList getOptions() {
        return options;
    }

    @Override
    public boolean connect() throws SSJFatalException {
        if (options.apiKey.get() == null || options.apiKey.get().length() == 0) {
            throw new SSJFatalException("invalid apiKey - you need to set the apiKey in the sensor options");
        }

        // Create data listener
        listener = new EmpaticaListener();

        //pre-validate empatica to avoid the certificate being bound to one app
        try {
            getCertificate();
            applyCertificate();
        } catch (IOException e) {
            throw new SSJFatalException("error in applying certificates - empatica needs to connect to the server once a month to validate", e);
        }

        // Empatica device manager must be initialized in the main ui thread
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            public void run() {
                try {
                    // Create device manager
                    deviceManager = new EmpaDeviceManager(SSJApplication.getAppContext(), listener, Empatica.this);

                    // Register the device manager using your API key. You need to have Internet access at this point.
                    deviceManager.authenticateWithAPIKey(options.apiKey.get());
                } catch (Exception e) {
                    Log.e("unable to create empatica device manager, is your api key correct?", e);
                }
            }
        }, 1);


        long time = SystemClock.elapsedRealtime();
        while (!_terminate && !listener.receivedData && SystemClock.elapsedRealtime() - time < _frame.options.waitSensorConnect.get() * 1000) {
            try {
                Thread.sleep(Cons.SLEEP_IN_LOOP);
            } catch (InterruptedException e) {
            }
        }

        if (!listener.receivedData) {
            Log.w("Unable to connect to empatica. Make sure it is on and NOT paired to your smartphone.");
            disconnect();
            return false;
        }

        return true;
    }

    private void applyCertificate() throws IOException {
        copyFile(new File(Environment.getExternalStorageDirectory(), "SSJ/empatica/profile"),
                new File(SSJApplication.getAppContext().getFilesDir(), "profile"));

        copyFile(new File(Environment.getExternalStorageDirectory(), "SSJ/empatica/signature"),
                new File(SSJApplication.getAppContext().getFilesDir(), "signature"));
    }

    private void getCertificate() {
        try {
            HashMap<String, String> p = new HashMap<>();
            p.put("api_key", options.apiKey.get());
            p.put("api_version", "AND_1.3");
            String json = (new GsonBuilder()).create().toJson(p, Map.class);

            //execute json
            URL url = new URL("https://www.empatica.com/connect/empalink/api_login.php");
            HttpURLConnection urlConn;
            DataOutputStream printout;

            urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setUseCaches(false);
            urlConn.setRequestProperty("Accept", "application/json");
            urlConn.setRequestProperty("Content-Type", "application/json");
            urlConn.connect();

            //send request
            printout = new DataOutputStream(urlConn.getOutputStream());
            printout.write(json.getBytes(StandardCharsets.UTF_8));
            printout.flush();
            printout.close();

            int HttpResult = urlConn.getResponseCode();
            if (HttpResult == HttpURLConnection.HTTP_OK) {
                //get response
                BufferedReader r = new BufferedReader(new InputStreamReader(urlConn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    response.append(line);
                }

                //check status
                JsonElement jelement = (new JsonParser()).parse(response.toString());
                JsonObject jobject = jelement.getAsJsonObject();
                String status = jobject.get("status").getAsString();
                if (!status.equals("ok"))
                    throw new IOException("status check failed");

                //save certificate
                if (jobject.has("empaconf") && jobject.has("empasign")) {
                    String empaconf = jobject.get("empaconf").getAsString();
                    String empasign = jobject.get("empasign").getAsString();
                    byte[] empaconfBytes = Base64.decode(empaconf, 0);
                    byte[] empasignBytes = Base64.decode(empasign, 0);

                    saveFile("profile", empaconfBytes);
                    saveFile("signature", empasignBytes);

                    Log.i("Successfully retrieved and saved new Empatica certificates");
                } else {
                    throw new IOException("Failed loading certificates. Empaconf and Empasign missing from http response.");
                }
            }
        } catch (IOException e) {
            Log.w("Failed to retrieve certificate", e);
        }
    }

    private void saveFile(String name, byte[] data) throws IOException {
        File dir = new File(Environment.getExternalStorageDirectory(), "SSJ/empatica");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, name);

        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data);
        fos.close();
    }

    public void copyFile(File src, File dst) throws IOException {
        FileInputStream inStream = new FileInputStream(src);
        FileOutputStream outStream = new FileOutputStream(dst);
        FileChannel inChannel = inStream.getChannel();
        FileChannel outChannel = outStream.getChannel();
        inChannel.transferTo(0, inChannel.size(), outChannel);
        inStream.close();
        outStream.close();
    }

    @Override
    public void disconnect() throws SSJFatalException {
        Log.i("disconnecting...");
        deviceManager.disconnect();
        deviceManager.cleanUp();
    }

    @Override
    public void didUpdateStatus(EmpaStatus empaStatus) {
        Log.i("Empatica status: " + empaStatus);

        try {
            switch (empaStatus) {
                case READY:
                    // Start scanning
                    deviceManager.startScanning();
                    break;

                case DISCONNECTED:
                    listener.reset();
                    break;

                case CONNECTING:
                    break;
                case CONNECTED:
                    break;
                case DISCONNECTING:
                    break;
                case DISCOVERING:
                    break;
            }
        } catch (Exception e) {
            Log.w("error reacting to status update", e);
        }
    }

    @Override
    public void didUpdateSensorStatus(EmpaSensorStatus empaSensorStatus, EmpaSensorType empaSensorType) {
        // Sensor status update
    }

    @Override
    public void didDiscoverDevice(BluetoothDevice bluetoothDevice, String deviceName, int rssi, boolean allowed) {
        // Stop scanning. The first allowed device will do.
        if (allowed) {
            try {
                deviceManager.stopScanning();

                // Connect to the device
                Log.i("Connecting to device: " + bluetoothDevice.getName() + "(MAC: " + bluetoothDevice.getAddress() + ")");

                deviceManager.connectDevice(bluetoothDevice);
                // Depending on your configuration profile, you might be unable to connect to a device.
                // This should happen only if you try to connect when allowed == false.

                empaticaInitialized = true;
            } catch (Exception e) {
                Log.e("Can't connect to device: " + bluetoothDevice.getName() + "(MAC: " + bluetoothDevice.getAddress() + ")");
            }
        } else {
            Log.w("Device " + deviceName + " not linked to specified api key");
        }
    }

    @Override
    public void didRequestEnableBluetooth() {
        Log.e("Bluetooth not enabled!");
    }

    public class Options extends OptionList {
        public final Option<String> apiKey = new Option<>("apiKey", null, String.class, "the api key from your empatica developer page");

        /**
         *
         */
        private Options() {
            addOptions();
        }
    }
}
