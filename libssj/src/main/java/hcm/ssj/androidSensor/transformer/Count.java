/*
 * Count.java
 * Copyright (c) 2016
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken
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

package hcm.ssj.androidSensor.transformer;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.Util;
import hcm.ssj.core.stream.Stream;

/**
 * Counts high values.
 * Created by Frank Gaibler on 31.08.2015.
 */
public class Count extends Transformer
{
    /**
     * All options for the transformer
     */
    public class Options
    {
        /**
         * Describes the output names for every dimension in e.g. a graph.
         */
        public String[] outputClass = null;
        /**
         * Global or frame count
         */
        public boolean frameCount = true;
        /**
         * Divide through number of values to be comparable
         */
        public boolean divideByNumber = false;
    }

    public Options options = new Options();
    //helper variables
    private float[] oldValues;
    private int counter = 0;

    /**
     *
     */
    public Count()
    {
        _name = "SSJ_transformer_" + this.getClass().getSimpleName();
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    public void enter(Stream[] stream_in, Stream stream_out)
    {
        //no check for a specific type to allow for different providers
        if (stream_in.length < 1 || stream_in[0].dim < 1)
        {
            Log.e("invalid input stream");
        }
        //every stream should have the same sample number
        int num = stream_in[0].num;
        for (int i = 1; i < stream_in.length; i++)
        {
            if (num != stream_in[i].num)
            {
                Log.e("invalid input stream num for stream " + i);
            }
        }
        oldValues = new float[stream_out.dim];
        counter = 0;
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    public void transform(Stream[] stream_in, Stream stream_out)
    {
        float[] out = stream_out.ptrF();
        if (options.frameCount)
        {
            oldValues = new float[stream_out.dim];
        }
        for (int i = 0; i < stream_in[0].num; i++)
        {
            int t = 0;
            for (Stream aStream_in : stream_in)
            {
                for (int k = 0; k < aStream_in.dim; k++, t++)
                {
                    float value = aStream_in.ptrF()[i * aStream_in.dim + k];
                    if (value > 0)
                    {
                        oldValues[t]++;
                    }
                }
            }
        }
        //write to output
        if (options.divideByNumber)
        {
            float divider = options.frameCount ? stream_in[0].num : ++counter * stream_in[0].num;
            for (int i = 0; i < oldValues.length; i++)
            {
                out[i] = oldValues[i] / divider;
            }
        } else
        {
            System.arraycopy(oldValues, 0, out, 0, oldValues.length);
        }
    }

    /**
     * @param stream_in Stream[]
     * @return int
     */
    @Override
    public int getSampleDimension(Stream[] stream_in)
    {
        int overallDimension = 0;
        for (Stream stream : stream_in)
        {
            overallDimension += stream.dim;
        }
        return overallDimension;
    }

    /**
     * @param stream_in Stream[]
     * @return int
     */
    @Override
    public int getSampleBytes(Stream[] stream_in)
    {
        return Util.sizeOf(Cons.Type.FLOAT);
    }

    /**
     * @param stream_in Stream[]
     * @return Cons.Type
     */
    @Override
    public Cons.Type getSampleType(Stream[] stream_in)
    {
        return Cons.Type.FLOAT;
    }

    /**
     * @param sampleNumber_in int
     * @return int
     */
    @Override
    public int getSampleNumber(int sampleNumber_in)
    {
        return 1;
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    protected void defineOutputClasses(Stream[] stream_in, Stream stream_out)
    {
        int overallDimension = getSampleDimension(stream_in);
        stream_out.dataclass = new String[overallDimension];
        if (options.outputClass != null)
        {
            if (overallDimension == options.outputClass.length)
            {
                System.arraycopy(options.outputClass, 0, stream_out.dataclass, 0, options.outputClass.length);
                return;
            } else
            {
                Log.w("invalid option outputClass length");
            }
        }
        for (int i = 0, k = 0; i < stream_in.length; i++)
        {
            for (int j = 0; j < stream_in[i].dim; j++, k++)
            {
                stream_out.dataclass[k] = "cnt" + i + "." + j;
            }
        }
    }
}
