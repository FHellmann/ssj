/*
 * Intensity.java
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
 * Computes audio intensity.
 * Based on code from the PRAAT Toolbox by Paul Boersma and David Weenink.
 * http://www.fon.hum.uva.nl/praat/
 */
public class Intensity extends Transformer {

    public final Options options = new Options();
    double[] amplitude = null;
    double[] window = null;
    private double myDuration, windowDuration, halfWindowDuration, outStep;
    private int halfWindowSamples, numberOfFrames;

    public Intensity() {
        _name = "Intensity";
    }

    public static double computeWindowDuration(double minimumPitch) {
        return 6.4 / minimumPitch;
    }

    public static int computeNumberOfFrames(double frameDuration, double windowDuration, double timeStep) {
        return (int) (Math.floor((frameDuration - windowDuration) / timeStep) + 1);
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
        if (audio == null || audio.type != Cons.Type.FLOAT) {
            Log.e("invalid input stream");
            return;
        }

        /*
         * Preconditions.
         */
        if (options.timeStep.get() < 0.0)
            throw new IllegalArgumentException("(Sound-to-Intensity:) Time step should be zero or positive instead of " + options.timeStep.get() + ".");
        if (options.minPitch.get() <= 0.0)
            throw new IllegalArgumentException("(Sound-to-Intensity:) Minimum pitch should be positive.");
        if (audio.step <= 0.0)
            throw new IllegalArgumentException("(Sound-to-Intensity:) The Sound's time step should be positive.");

        /*
         * Defaults.
         */
        halfWindowDuration = 0.5 * windowDuration;
        halfWindowSamples = (int) (halfWindowDuration / audio.step);
        amplitude = new double[2 * halfWindowSamples + 1];
        window = new double[2 * halfWindowSamples + 1];

        double x, root;
        for (int i = -halfWindowSamples; i <= halfWindowSamples; i++) {
            x = i * audio.step / halfWindowDuration;
            root = 1 - x * x;
            window[i + halfWindowSamples] = root <= 0.0 ? 0.0 : AudioUtil.bessel_i0_f((2 * Math.PI * Math.PI + 0.5) * Math.sqrt(root));
        }

    }

    @Override
    public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException {
        Stream in = stream_in[0];
        Stream out = stream_out;
        float[] data = in.ptrF();
        float[] outf = out.ptrF();

        double intensitySum = 0;

        for (int iframe = 0; iframe < numberOfFrames; iframe++) {
            double midTime = in.time + iframe * outStep;
            int midSample = (int) (Math.round((midTime - in.time) / in.step + 1.0));
            int leftSample = midSample - halfWindowSamples, rightSample = midSample + halfWindowSamples;
            double sumxw = 0.0, sumw = 0.0, intensity;
            if (leftSample < 0) leftSample = 0;
            if (rightSample >= in.num) rightSample = in.num - 1;

            for (int channel = 0; channel < in.dim; channel++) {
                for (int i = leftSample; i <= rightSample; i++) {
                    amplitude[i - midSample + halfWindowSamples] = data[i * in.dim + channel];
                }
                if (options.subtractMeanPressure.get()) {
                    double sum = 0.0;
                    for (int i = leftSample; i <= rightSample; i++) {
                        sum += amplitude[i - midSample + halfWindowSamples];
                    }
                    double mean = sum / (rightSample - leftSample + 1);
                    for (int i = leftSample; i <= rightSample; i++) {
                        amplitude[i - midSample + halfWindowSamples] -= mean;
                    }
                }
                for (int i = leftSample; i <= rightSample; i++) {
                    sumxw += amplitude[i - midSample + halfWindowSamples] * amplitude[i - midSample + halfWindowSamples] * window[i - midSample + halfWindowSamples];
                    sumw += window[i - midSample + halfWindowSamples];
                }
            }
            intensity = sumxw / sumw;
            if (intensity != 0.0) intensity /= 4e-10;
            intensity = intensity < 1e-30 ? -300 : 10 * Math.log10(intensity);

            if (!options.mean.get())
                outf[iframe] = (float) intensity;
            else
                intensitySum += intensity;
        }

        if (options.mean.get())
            outf[0] = (float) (intensitySum / numberOfFrames);
    }

    @Override
    public void flush(Stream[] stream_in, Stream stream_out) throws SSJFatalException {
    }

    @Override
    public int getSampleDimension(Stream[] stream_in) {
        return 1;
    }

    @Override
    public void init(double frame, double delta) {
        if (options.timeStep.get() == 0)
            options.timeStep.set(0.8 / options.minPitch.get()); // default: four times oversampling Hanning-wise

        myDuration = frame + delta;

        windowDuration = computeWindowDuration(options.minPitch.get());
        if (windowDuration <= 0.0 || windowDuration > myDuration)
            Log.e("invalid processing window duration");

        numberOfFrames = computeNumberOfFrames(myDuration, windowDuration, options.timeStep.get());
        if (numberOfFrames < 1)
            Log.e("The duration of the sound in an intensity analysis should be at least 6.4 divided by the minimum pitch (" + options.minPitch.get() + " Hz), " +
                    "i.e. at least " + 6.4 / options.minPitch.get() + " s, instead of " + myDuration + " s.");

        outStep = frame / numberOfFrames;
    }

    @Override
    public int getSampleNumber(int sampleNumber_in) {
        return (options.mean.get()) ? 1 : numberOfFrames;
    }

    @Override
    public int getSampleBytes(Stream[] stream_in) {
        if (stream_in[0].bytes != 4) //float
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
        stream_out.desc[0] = "Intensity";
    }

    public class Options extends OptionList {
        public final Option<Double> minPitch = new Option<>("minPitch", 50., Double.class, "");
        public final Option<Double> timeStep = new Option<>("timeStep", 0., Double.class, "");
        public final Option<Boolean> subtractMeanPressure = new Option<>("subtractMeanPressure", true, Boolean.class, "");
        public final Option<Boolean> mean = new Option<>("mean", false, Boolean.class, "output mean intensity over entire window");

        /**
         *
         */
        private Options() {
            addOptions();
        }
    }
}
