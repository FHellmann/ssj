/*
 * PSD.java
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

import org.jtransforms.fft.FloatFFT_1D;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Calculates probability density function (PSD) or entropy. <br>
 * Created by Frank Gaibler on 10.01.2017.
 */
public class PSD extends Transformer {
    public final Options options = new Options();
    //helper variables
    private FloatFFT_1D fft;
    private float[] copy, psd;
    /**
     *
     */
    public PSD() {
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
        if (stream_in.length != 1 || stream_in[0].dim != 1 || stream_in[0].type != Cons.Type.FLOAT) {
            Log.e("invalid input stream");
        }
        fft = new FloatFFT_1D(stream_in[0].num);
        copy = new float[stream_in[0].num];
        psd = new float[stream_in[0].num / 2 + 1];
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    public void flush(Stream[] stream_in, Stream stream_out) throws SSJFatalException {
        super.flush(stream_in, stream_out);
        fft = null;
        copy = null;
        psd = null;
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException {
        int rfft = psd.length;
        float[] ptr_in = stream_in[0].ptrF(), ptr_out = stream_out.ptrF();
        float fde = 0;
        // Copy data for FFT
        System.arraycopy(ptr_in, 0, copy, 0, stream_in[0].num);
        // 1. Calculate FFT
        fft.realForward(copy);
        // Format values like in SSI
        joinFFT(copy);
        if (rfft > 0) {
            // 2. Calculate Power Spectral Density
            for (int i = 0; i < rfft; i++) {
                psd[i] = (float) Math.pow(psd[i], 2) / (float) (rfft);
            }
            if (options.entropy.get() || options.normalize.get()) {
                float psdSum = getSum(psd);
                if (psdSum > 0) {
                    if (options.entropy.get() || options.normalize.get()) {
                        // 3. Normalize calculated PSD so that it can be viewed as a Probability Density Function
                        for (int i = 0; i < rfft; i++) {
                            psd[i] = psd[i] / psdSum;
                        }
                    }
                    if (options.entropy.get()) {
                        // 4. Calculate the Frequency Domain Entropy
                        for (float val : psd) {
                            if (val != 0) {
                                fde += val * Math.log(val);
                            }
                        }
                        fde *= -1;
                    }
                }
            }
            if (!options.entropy.get()) {
                //return psd
                System.arraycopy(psd, 0, ptr_out, 0, rfft);
            }
        }
        //return entropy
        if (options.entropy.get()) {
            ptr_out[0] = fde;
        }
    }

    /**
     * calculates the sum of all values
     *
     * @param values float[]
     * @return float
     */
    private float getSum(float[] values) {
        float sum = 0;
        for (float val : values) {
            sum += val;
        }
        return sum;
    }

    /**
     * Helper function to format fft values similar to SSI
     *
     * @param fft float[]
     */
    private void joinFFT(float[] fft) {
        for (int i = 0; i < fft.length; i += 2) {
            if (i == 0) {
                psd[0] = fft[0];
                psd[1] = fft[1];
            } else {
                psd[i / 2 + 1] = (float) Math.sqrt(Math.pow(fft[i], 2) + Math.pow(fft[i + 1], 2));
            }
        }
    }

    /**
     * @param stream_in Stream[]
     * @return int
     */
    @Override
    public int getSampleDimension(Stream[] stream_in) {
        return 1;
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
        if (options.entropy.get()) {
            return 1;
        } else {
            return sampleNumber_in / 2 + 1;
        }
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    protected void describeOutput(Stream[] stream_in, Stream stream_out) {
        int overallDimension = getSampleDimension(stream_in);
        stream_out.desc = new String[overallDimension];
        if (options.outputClass.get() != null && overallDimension == options.outputClass.get().length) {
            System.arraycopy(options.outputClass.get(), 0, stream_out.desc, 0, options.outputClass.get().length);
        } else {
            if (options.outputClass.get() != null && overallDimension != options.outputClass.get().length) {
                Log.w("invalid option outputClass length");
            }
            for (int i = 0; i < overallDimension; i++) {
                stream_out.desc[i] = "psd" + i;
            }
        }
    }

    /**
     * All options for the transformer
     */
    public class Options extends OptionList {
        public final Option<String[]> outputClass = new Option<>("outputClass", null, String[].class, "Describes the output names for every dimension in e.g. a graph");
        public final Option<Boolean> entropy = new Option<>("entropy", false, Boolean.class, "Calculate entropy instead of PSD");
        public final Option<Boolean> normalize = new Option<>("normalize", false, Boolean.class, "Normalize PSD");

        /**
         *
         */
        private Options() {
            addOptions();
        }
    }
}