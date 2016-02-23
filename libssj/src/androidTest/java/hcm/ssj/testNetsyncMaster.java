/*
 * testBluetoothServer.java
 * Copyright (c) 2015
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler
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
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package hcm.ssj;

import android.app.Application;
import android.test.ApplicationTestCase;
import android.util.Log;

import hcm.ssj.audio.AudioProvider;
import hcm.ssj.audio.Microphone;
import hcm.ssj.core.TheFramework;
import hcm.ssj.test.Logger;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class testNetsyncMaster extends ApplicationTestCase<Application> {

    String _name = "SSJ_test_NetyncMaster";

    public testNetsyncMaster() {
        super(Application.class);
    }

    public void test() throws Exception
    {
        TheFramework frame = TheFramework.getFramework();
        frame.options.bufferSize = 10.0f;
        frame.options.netSync = true;
        frame.options.netSyncListen = false;

//        BluetoothReader blr = new BluetoothReader();
//        blr.options.connectionType = BluetoothConnection.Type.SERVER;
//        blr.options.connectionName = "stream";
//        BluetoothProvider data = new BluetoothProvider();
//        data.options.dim = 1;
//        data.options.type = Cons.Type.FLOAT;
//        data.options.sr = 16000;
//        frame.addSensor(blr);
//        blr.addProvider(data);

        Microphone mic = new Microphone();
        AudioProvider audio = new AudioProvider();
        audio.options.sampleRate = 16000;
        audio.options.scale = true;
        mic.addProvider(audio);
        frame.addSensor(mic);

        Logger dummy = new Logger();
        dummy.options.reduceNum = true;
        frame.addConsumer(dummy, audio, 0.1, 0);

//        BluetoothEventReader bler = new BluetoothEventReader();
//        bler.options.connectionType = BluetoothConnection.Type.SERVER;
//        bler.options.connectionName = "event";
//        frame.addComponent(bler);
//        EventChannel ch = frame.registerEventProvider(bler);
//
//        EventLogger evlog = new EventLogger();
//        frame.addComponent(evlog);
//        frame.registerEventListener(evlog, ch);

        try {
            frame.Start();

            long start = System.currentTimeMillis();
            while(true)
            {
                if(System.currentTimeMillis() > start + 3 * 60 * 1000)
                    break;

                Thread.sleep(1);
            }

            frame.Stop();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        Log.i(_name, "test finished");
    }
}