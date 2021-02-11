/*
 * Energy.java
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

package hcm.ssj.audio;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Audio energy algorithms extracted from TarsosDSP
 * Created by Johnny on 05.03.2015.
 */
public class Energy extends Transformer {

    public final Options options = new Options();

    public Energy() {
        _name = "Energy";
    }

    /**
     * Calculates and returns the root mean square of the signal. Please
     * cache the result since it is calculated every time.
     *
     * @param floatBuffer The audio buffer to calculate the RMS for.
     * @return The <a
     * href="http://en.wikipedia.org/wiki/Root_mean_square">RMS</a> of
     * the signal present in the current buffer.
     */
    public static double calculateRMS(float[] floatBuffer) {
        double rms = 0.0;
        for (int i = 0; i < floatBuffer.length; i++) {
            rms += floatBuffer[i] * floatBuffer[i];
        }
        rms = rms / Double.valueOf(floatBuffer.length);
        rms = Math.sqrt(rms);
        return rms;
    }

    @Override
    public OptionList getOptions() {
        return options;
    }

    @Override
    public void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException {
        Stream audio = null;
        for (Stream s : stream_in) {
            if (s.findDataClass("Audio") >= 0) {
                audio = s;
            }
        }
        if (audio == null) {
            Log.w("invalid input stream");
            return;
        }
    }

    @Override
    public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException {
        float[] data = stream_in[0].ptrF();
        float[] out = stream_out.ptrF();

        int dim = 0;
        if (options.computeRMS.get()) {
            out[dim++] = (float) calculateRMS(data);
        }

        if (options.computeSPL.get() || options.computeSilence.get()) {
            double SPL = soundPressureLevel(data);
            out[dim++] = (float) SPL;

            if (options.computeSilence.get()) {
                float silence = ((SPL < options.silenceThreshold.get()) ? 1 : 0);
                out[dim++] = silence;
            }
        }
    }

    @Override
    public void flush(Stream[] stream_in, Stream stream_out) throws SSJFatalException {
    }

    @Override
    public int getSampleDimension(Stream[] stream_in) {
        int dim = 0;

        if (options.computeRMS.get()) dim++;
        if (options.computeSPL.get()) dim++;
        if (options.computeSilence.get()) dim++;

        return dim;
    }

    @Override
    public int getSampleNumber(int sampleNumber_in) {
        return 1;
    }

    @Override
    public int getSampleBytes(Stream[] stream_in) {
        if (stream_in[0].bytes != 4)
            Log.e("Unsupported input stream type");

        return 4;
    }

    @Override
    public Cons.Type getSampleType(Stream[] stream_in) {
        if (stream_in[0].type != Cons.Type.FLOAT)
            Log.e("Unsupported input stream type");

        return Cons.Type.FLOAT;
    }

    @Override
    public void describeOutput(Stream[] stream_in, Stream stream_out) {
        stream_out.desc = new String[stream_out.dim];

        int i = 0;
        if (options.computeRMS.get()) stream_out.desc[i++] = "RMS";
        if (options.computeSPL.get()) stream_out.desc[i++] = "SPL";
        if (options.computeSilence.get()) stream_out.desc[i++] = "Silence";
    }

    /****************************************************
     * Original code taken from TarsosDSP
     * file: be.tarsos.dsp.AudioEvent
     *
     * TarsosDSP is developed by Joren Six at IPEM, University Ghent
     *
     *  Info: http://0110.be/tag/TarsosDSP
     *  Github: https://github.com/JorenSix/TarsosDSP
     *  Releases: http://0110.be/releases/TarsosDSP/
     ****************************************************/

    /**
     * Returns the dBSPL for a buffer.
     *
     * @param buffer The buffer with audio information.
     * @return The dBSPL level for the buffer.
     */
    private double soundPressureLevel(float[] buffer) {
        double value = Math.pow(localEnergy(buffer), 0.5);
        value = value / buffer.length;
        return linearToDecibel(value);
    }

    /**
     * Calculates the local (linear) energy of an audio buffer.
     *
     * @param buffer The audio buffer.
     * @return The local (linear) energy of an audio buffer.
     */
    private double localEnergy(float[] buffer) {
        double power = 0.0D;
        for (float element : buffer) {
            power += element * element;
        }
        return power;
    }

    /**
     * Converts a linear to a dB value.
     *
     * @param value The value to convert.
     * @return The converted value.
     */
    private double linearToDecibel(double value) {
        return 20.0 * Math.log10(value);
    }

    public class Options extends OptionList {
        public final Option<Boolean> computeRMS = new Option<>("computeRMS", false, Boolean.class, "");
        public final Option<Boolean> computeSPL = new Option<>("computeSPL", true, Boolean.class, "");
        public final Option<Boolean> computeSilence = new Option<>("computeSilence", false, Boolean.class, "");
        public final Option<Double> silenceThreshold = new Option<>("silenceThreshold", -70.0, Double.class, "in DB, default of -70 defined in TarsosDSP: be.tarsos.dsp.SilenceDetector");
        public final Option<Boolean> inputIsSigned = new Option<>("inputIsSigned", true, Boolean.class, "");
        public final Option<Boolean> inputIsBigEndian = new Option<>("inputIsBigEndian", false, Boolean.class, "");

        /**
         *
         */
        private Options() {
            addOptions();
        }
    }

}
