/*
 * InceptionTest.java
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

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import hcm.ssj.camera.CameraChannel;
import hcm.ssj.camera.CameraSensor;
import hcm.ssj.camera.ImageNormalizer;
import hcm.ssj.camera.ImageResizer;
import hcm.ssj.camera.NV21ToRGBDecoder;
import hcm.ssj.core.Cons;
import hcm.ssj.core.EventChannel;
import hcm.ssj.core.Pipeline;
import hcm.ssj.ml.Classifier;
import hcm.ssj.ml.TensorFlow;
import hcm.ssj.test.EventLogger;

/**
 * Tests setting up, loading, and evaluating object classification
 * with the Inception model.
 *
 * @author Vitaly
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class InceptionTest {
    @Test
    public void loadInceptionModel() throws Exception {
        String trainerName = "inception.trainer";
        String trainerURL = "https://hcm-lab.de/downloads/ssj/model";

        // Option parameters for camera sensor
        double sampleRate = 1;
        int width = 640;
        int height = 480;

        final float IMAGE_MEAN = 127.5f;
        final float IMAGE_STD = 1;
        final int CROP_SIZE = 224;
        final boolean MAINTAIN_ASPECT = true;

        // Get pipeline instance
        Pipeline frame = Pipeline.getInstance();
        frame.options.bufferSize.set(10.0f);

        // Instantiate camera sensor and set options
        CameraSensor cameraSensor = new CameraSensor();
        cameraSensor.options.cameraType.set(Cons.CameraType.BACK_CAMERA);
        cameraSensor.options.width.set(width);
        cameraSensor.options.height.set(height);
        cameraSensor.options.previewFpsRangeMin.set(15);
        cameraSensor.options.previewFpsRangeMax.set(15);

        // Add sensor to the pipeline
        CameraChannel cameraChannel = new CameraChannel();
        cameraChannel.options.sampleRate.set(sampleRate);
        frame.addSensor(cameraSensor, cameraChannel);

        // Set up a NV21 decoder
        NV21ToRGBDecoder decoder = new NV21ToRGBDecoder();
        frame.addTransformer(decoder, cameraChannel, 1, 0);

        // Add image resizer to the pipeline
        ImageResizer resizer = new ImageResizer();
        resizer.options.maintainAspect.set(MAINTAIN_ASPECT);
        resizer.options.size.set(CROP_SIZE);
        resizer.options.rotation.set(90);
        frame.addTransformer(resizer, decoder, 1, 0);

        // Add image pixel value normalizer to the pipeline
        ImageNormalizer imageNormalizer = new ImageNormalizer();
        imageNormalizer.options.imageMean.set(IMAGE_MEAN);
        imageNormalizer.options.imageStd.set(IMAGE_STD);
        frame.addTransformer(imageNormalizer, resizer, 1, 0);

        TensorFlow tensorFlow = new TensorFlow();
        tensorFlow.options.file.setValue(trainerURL + File.separator + trainerName);
        frame.addModel(tensorFlow);

        // Add classifier transformer to the pipeline
        Classifier classifier = new Classifier();
        classifier.setModel(tensorFlow);
        classifier.options.merge.set(false);
        classifier.options.bestMatchOnly.set(true);
        frame.addConsumer(classifier, imageNormalizer, 1, 0);

        // Log events
        EventChannel channel = classifier.getEventChannelOut();
        EventLogger log = new EventLogger();
        frame.registerEventListener(log, channel);

        // Start pipeline
        frame.start();

        // Wait duration
        try {
            Thread.sleep(TestHelper.DUR_TEST_NORMAL);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Stop pipeline
        frame.stop();
        frame.release();
    }
}
