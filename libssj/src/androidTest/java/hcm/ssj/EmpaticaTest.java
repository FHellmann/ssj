/*
 * EmpaticaTest.java
 * Copyright (c) 2017
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

package hcm.ssj;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import hcm.ssj.core.Log;
import hcm.ssj.core.Pipeline;
import hcm.ssj.empatica.AccelerationChannel;
import hcm.ssj.empatica.BVPChannel;
import hcm.ssj.empatica.Empatica;
import hcm.ssj.empatica.GSRChannel;
import hcm.ssj.empatica.IBIChannel;
import hcm.ssj.empatica.TemperatureChannel;
import hcm.ssj.test.Logger;

/**
 * Created by Michael Dietz on 15.04.2015.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class EmpaticaTest
{
	// Insert your API key here:
	String APIKEY = "";

	@Test
	public void testAcc() throws Exception
	{
		Pipeline frame = Pipeline.getInstance();

		Empatica empatica = new Empatica();
		empatica.options.apiKey.set(APIKEY);
		AccelerationChannel acc = new AccelerationChannel();
		frame.addSensor(empatica, acc);

		Logger dummy = new Logger();
		frame.addConsumer(dummy, acc, 0.1, 0);

		frame.start();

		// Wait duration
		try
		{
			Thread.sleep(TestHelper.DUR_TEST_NORMAL);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		frame.stop();
		Log.i("ACC test finished");
	}

	@Test
	public void testGsr() throws Exception
	{
		Pipeline frame = Pipeline.getInstance();

		Empatica empatica = new Empatica();
		empatica.options.apiKey.set(APIKEY);
		GSRChannel data = new GSRChannel();
		frame.addSensor(empatica, data);

		Logger dummy = new Logger();
		frame.addConsumer(dummy, data, 0.25, 0);

		frame.start();

		// Wait duration
		try
		{
			Thread.sleep(TestHelper.DUR_TEST_SHORT);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		frame.stop();
		Log.i("GSR test finished");
	}

	@Test
	public void testIBI() throws Exception
	{
		Pipeline frame = Pipeline.getInstance();

		Empatica empatica = new Empatica();
		empatica.options.apiKey.set(APIKEY);
		IBIChannel data = new IBIChannel();
		frame.addSensor(empatica, data);


		Logger dummy = new Logger();
		frame.addConsumer(dummy, data, 0.1, 0);

		frame.start();

		// Wait duration
		try
		{
			Thread.sleep(TestHelper.DUR_TEST_SHORT);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		frame.stop();
		Log.i("IBI test finished");
	}

	@Test
	public void testTemp() throws Exception
	{
		Pipeline frame = Pipeline.getInstance();

		Empatica empatica = new Empatica();
		empatica.options.apiKey.set(APIKEY);
		TemperatureChannel data = new TemperatureChannel();
		frame.addSensor(empatica, data);


		Logger dummy = new Logger();
		frame.addConsumer(dummy, data, 0.25, 0);

		frame.start();

		// Wait duration
		try
		{
			Thread.sleep(TestHelper.DUR_TEST_SHORT);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		frame.stop();
		Log.i("Temp test finished");
	}

	@Test
	public void testBVP() throws Exception
	{
		Pipeline frame = Pipeline.getInstance();

		Empatica empatica = new Empatica();
		empatica.options.apiKey.set(APIKEY);
		BVPChannel data = new BVPChannel();
		frame.addSensor(empatica, data);

		Logger dummy = new Logger();
		frame.addConsumer(dummy, data, 0.1, 0);

		frame.start();

		// Wait duration
		try
		{
			Thread.sleep(TestHelper.DUR_TEST_SHORT);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		frame.stop();
		Log.i("BVP test finished");
	}
}
