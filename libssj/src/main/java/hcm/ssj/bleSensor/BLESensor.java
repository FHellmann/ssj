/*
 * BLESensor.java
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

package hcm.ssj.bleSensor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;

import hcm.ssj.core.Log;
import hcm.ssj.core.SSJApplication;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;

public class BLESensor extends Sensor {
    public final BLESensor.Options options = new BLESensor.Options();
    protected boolean angelInitialized;
    protected BLESensorListener listener;
    BluetoothDevice bluetoothDevice;
    private BleDevicesScanner mBleScanner;
    public LeScanCallback mScanCallback = new LeScanCallback() {


        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (device.getName() != null) {
                Log.i("Bluetooth LE device found: " + device.getName());
                //mBleScanner.stopScan();
                if (device.getName() != null && device.getName().startsWith(options.sensorName.get())) {

                    bluetoothDevice = device;
                    //mBleScanner.stop();
                    mBleScanner.stop();
                    listener.initialize();
                    listener.connect(device.getAddress());
                    Log.i("connected to device " + device.getName());
                }
            }
        }
    };
    private BluetoothAdapter bluetoothAdapter;

    public BLESensor() {
        _name = "BLESensor";
        angelInitialized = false;
    }

    @Override
    public OptionList getOptions() {
        return options;
    }

    @Override
    public boolean connect() throws SSJFatalException {
        listener = new BLESensorListener(options.service.get(), options.characteristic.get());

        try {
            if (mBleScanner == null) {

                bluetoothAdapter = BleUtils.getBluetoothAdapter(SSJApplication.getAppContext());
                mBleScanner = new BleDevicesScanner(bluetoothAdapter, mScanCallback);
            }
        } catch (Exception e) {
            Log.e("Exception:", e);
        }

        mBleScanner.start();


        return true;
    }

    @Override
    public void disconnect() throws SSJFatalException {

    }

    public void didDiscoverDevice(BluetoothDevice bluetoothDevice, int rssi, boolean allowed) {

    }

    public class Options extends OptionList {
        public final Option<String> sensorName = new Option<>("sensorName", "HCM", String.class, "Sensor Name to connect to");
        //public final Option<String> service = new Option<>("service", "0000180d-0000-1000-8000-00805f9b34fb", String.class, "UUID of service" );//hr
        //public final Option<String> service = new Option<>("service", "481d178c-10dd-11e4-b514-b2227cce2b54", String.class, "UUID of service" ); //angel
        public final Option<String> service = new Option<>("service", "00002220-0000-1000-8000-00805f9b34fb", String.class, "UUID of service");// andys
        //public final Option<String> characteristic = new Option<>("characteristic", "00002a37-0000-1000-8000-00805f9b34fb", String.class, "UUID of characteristic"); //hr
        //public final Option<String> characteristic = new Option<>("characteristic", "334c0be8-76f9-458b-bb2e-7df2b486b4d7", String.class, "UUID of characteristic");//angel
        public final Option<String> characteristic = new Option<>("characteristic", "00002221-0000-1000-8000-00805f9b34fb", String.class, "UUID of characteristic"); // andys

        /**
         *
         */
        private Options() {
            addOptions();
        }
    }


}
