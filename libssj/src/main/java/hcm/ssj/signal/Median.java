/*
 * Median.java
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

package hcm.ssj.signal;

import java.util.Arrays;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * A general transformer to calculate the median for every dimension in the provided streams.<br>
 * Created by Frank Gaibler on 09.09.2015.
 */
public class Median extends Transformer {
    public final Options options = new Options();
    //helper variables
    private float[][] floats;
    private float[][] dimensions;
    /**
     *
     */
    public Median() {
        _name = this.getClass().getSimpleName();
    }

    @Override
    public OptionList getOptions() {
        return options;
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    public void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException {
        //no check for a specific type to allow for different providers
        if (stream_in.length < 1 || stream_in[0].dim < 1) {
            Log.e("invalid input stream");
            return;
        }
        floats = new float[stream_in.length][];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = new float[stream_in[i].num * stream_in[i].dim];
        }
        dimensions = new float[stream_in.length][];
        for (int i = 0; i < dimensions.length; i++) {
            dimensions[i] = new float[stream_in[i].num];
        }
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    public void flush(Stream[] stream_in, Stream stream_out) throws SSJFatalException {
        super.flush(stream_in, stream_out);
        floats = null;
        dimensions = null;
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException {
        float[] out = stream_out.ptrF();
        for (int i = 0, t = 0; i < stream_in.length; i++) {
            Util.castStreamPointerToFloat(stream_in[i], floats[i]);
            if (stream_in[i].dim > 1) {
                int size = stream_in[i].num * stream_in[i].dim;
                for (int j = 0; j < stream_in[i].dim; j++) {
                    for (int k = j, l = 0; k < size; k += stream_in[i].dim, l++) {
                        dimensions[i][l] = floats[i][k];
                    }
                    out[t++] = getMedian(dimensions[i]);
                }
            } else {
                out[t++] = getMedian(floats[i]);
            }
        }
    }

    /**
     * @param in float[]
     * @return float
     */
    private float getMedian(float[] in) {
        Arrays.sort(in);
        int n = in.length;
        if (n % 2 == 0) {
            return (in[n / 2] + in[(n / 2) - 1]) / 2;
        } else {
            return in[(n - 1) / 2];
        }
    }

    /**
     * @param stream_in Stream[]
     * @return int
     */
    @Override
    public int getSampleDimension(Stream[] stream_in) {
        int overallDimension = 0;
        for (Stream stream : stream_in) {
            overallDimension += stream.dim;
        }
        return overallDimension;
    }

    /**
     * @param stream_in Stream[]
     * @return int
     */
    @Override
    public int getSampleBytes(Stream[] stream_in) {
        return Util.sizeOf(Cons.Type.FLOAT);
    }

    /**
     * @param stream_in Stream[]
     * @return Cons.Type
     */
    @Override
    public Cons.Type getSampleType(Stream[] stream_in) {
        return Cons.Type.FLOAT;
    }

    /**
     * @param sampleNumber_in int
     * @return int
     */
    @Override
    public int getSampleNumber(int sampleNumber_in) {
        return 1;
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    protected void describeOutput(Stream[] stream_in, Stream stream_out) {
        int overallDimension = getSampleDimension(stream_in);
        stream_out.desc = new String[overallDimension];
        if (options.outputClass.get() != null) {
            if (overallDimension == options.outputClass.get().length) {
                System.arraycopy(options.outputClass.get(), 0, stream_out.desc, 0, options.outputClass.get().length);
                return;
            } else {
                Log.w("invalid option outputClass length");
            }
        }
        for (int i = 0, k = 0; i < stream_in.length; i++) {
            for (int j = 0; j < stream_in[i].dim; j++, k++) {
                stream_out.desc[k] = "median" + i + "." + j;
            }
        }
    }

    /**
     * All options for the transformer
     */
    public class Options extends OptionList {
        public final Option<String[]> outputClass = new Option<>("outputClass", null, String[].class, "Describes the output names for every dimension in e.g. a graph");

        /**
         *
         */
        private Options() {
            addOptions();
        }
    }
}