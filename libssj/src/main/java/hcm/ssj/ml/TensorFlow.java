/*
 * TensorFlow.java
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

package hcm.ssj.ml;

import android.util.Xml;

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.nio.FloatBuffer;
import java.util.Arrays;

import hcm.ssj.core.Log;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.stream.Stream;

/**
 * TensorFlow model.
 * Supports prediction using tensor flow's Android API.
 * Requires pre-trained frozen graph, e.g. using SSI.
 *
 * @author Vitaly Krumins
 */

public class TensorFlow extends Model {
    static {
        System.loadLibrary("tensorflow_inference");
    }

    public Options options = new Options();

    private Graph graph;
    private Session session;

    public TensorFlow() {
        _name = "TensorFlow";
    }

    @Override
    public Model.Options getOptions() {
        return options;
    }

    @Override
    void init(int input_dim, int output_dim, String[] outputNames) {

    }

    @Override
    protected float[] forward(Stream stream) {
        if (!isTrained) {
            Log.w("not trained");
            return null;
        }

        float[] floatValues = stream.ptrF();
        float[] probabilities = makePrediction(floatValues);

        return probabilities;
    }

    @Override
    protected void loadOption(File file) {
        XmlPullParser parser = Xml.newPullParser();

        try {
            parser.setInput(new FileReader(file));
            parser.next();

            int eventType = parser.getEventType();

            // Check if option file is of the right format.
            if (eventType != XmlPullParser.START_TAG || !parser.getName().equalsIgnoreCase("options")) {
                Log.w("unknown or malformed trainer file");
                return;
            }

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equalsIgnoreCase("item")) {
                        String optionName = parser.getAttributeValue(null, "name");
                        String optionValue = parser.getAttributeValue(null, "value");

                        Object currentValue = options.getOptionValue(optionName);
                        if (currentValue == null)
                            options.setOptionValue(optionName, optionValue);
                    }
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            Log.e(e.getMessage());
        }
    }

    @Override
    protected void loadModel(File file) {
        FileInputStream fileInputStream;
        byte[] fileBytes = new byte[(int) file.length()];

        try {
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(fileBytes);
            fileInputStream.close();

            graph = new Graph();
            graph.importGraphDef(fileBytes);
            session = new Session(graph);
        } catch (Exception e) {
            Log.e("Error while importing the model: " + e.getMessage());
            return;
        }

        isTrained = true;
    }

    /**
     * Makes prediction about the given image data.
     *
     * @param floatValues RGB float data.
     * @return Probability array.
     */
    private float[] makePrediction(float[] floatValues) {
        Tensor input = Tensor.create(options.shape.get(), FloatBuffer.wrap(floatValues));
        Tensor result = session.runner()
                .feed(options.inputNode.get(), input)
                .fetch(options.outputNode.get())
                .run().get(0);

        long[] rshape = result.shape();

        if (result.numDimensions() != 2 || rshape[0] != 1) {
            throw new RuntimeException(
                    String.format(
                            "Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
                            Arrays.toString(rshape)));
        }

        int nlabels = (int) rshape[1];
        return result.copyTo(new float[1][nlabels])[0];
    }

    /**
     * Parses string representation of input tensor shape.
     *
     * @param shape String that represents tensor shape.
     * @return shape of the input tensor as a n-dimensional array.
     */
    private long[] parseTensorShape(String shape) {
        // Delete square brackets and white spaces.
        String formatted = shape.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "");

        // Separate each dimension value.
        String[] shapeArray = formatted.split(",");

        long[] tensorShape = new long[shapeArray.length];

        for (int i = 0; i < shapeArray.length; i++) {
            tensorShape[i] = Integer.parseInt(shapeArray[i]);
        }

        return tensorShape;
    }

    public class Options extends Model.Options {
        public final Option<String> inputNode = new Option<>("input", "input", String.class, "name of the input node");
        public final Option<String> outputNode = new Option<>("output", "output", String.class, "name of the output node");
        public final Option<long[]> shape = new Option<>("shape", null, long[].class, "shape of the input tensor");

        private Options() {
            super();
            addOptions();
        }
    }
}
