/*
 * SvmTest.java
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

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import hcm.ssj.androidSensor.AndroidSensor;
import hcm.ssj.androidSensor.AndroidSensorChannel;
import hcm.ssj.androidSensor.SensorType;
import hcm.ssj.body.AccelerationFeatures;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.Provider;
import hcm.ssj.file.FileWriter;
import hcm.ssj.ml.ClassifierT;
import hcm.ssj.ml.SVM;
import hcm.ssj.test.Logger;

import static androidx.test.InstrumentationRegistry.getContext;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class SvmTest {
    @Test
    public void testSVM() throws Exception {
        // Resources
        File dir = getContext().getFilesDir();
        String modelName = "search_model.trainer";
        TestHelper.copyAssetToFile(modelName, new File(dir, modelName));
        TestHelper.copyAssetToFile(modelName + ".SVM.model", new File(dir, modelName + ".SVM.model"));
        TestHelper.copyAssetToFile(modelName + ".SVM.option", new File(dir, modelName + ".SVM.option"));
        String outputFileName = getClass().getSimpleName() + ".test";
        File outputFile = new File(dir, outputFileName);

        // Setup
        Pipeline frame = Pipeline.getInstance();
        frame.options.bufferSize.set(10.0f);

        // Sensor
        AndroidSensor accSensor = new AndroidSensor();
        AndroidSensor gyrSensor = new AndroidSensor();

        // Channel
        AndroidSensorChannel accChannel = new AndroidSensorChannel();
        accChannel.options.sensorType.set(SensorType.LINEAR_ACCELERATION);
        accChannel.options.sampleRate.set(40);
        frame.addSensor(accSensor, accChannel);

        AndroidSensorChannel gyrChannel = new AndroidSensorChannel();
        gyrChannel.options.sensorType.set(SensorType.GYROSCOPE);
        gyrChannel.options.sampleRate.set(40);
        frame.addSensor(gyrSensor, gyrChannel);

        // Transformer
        AccelerationFeatures accFeatures = new AccelerationFeatures();
        frame.addTransformer(accFeatures, accChannel, 2, 2);

        AccelerationFeatures gyrFeatures = new AccelerationFeatures();
        frame.addTransformer(gyrFeatures, gyrChannel, 2, 2);

        // SVM
        SVM svm = new SVM();
        svm.options.file.setValue(dir.getAbsolutePath() + File.separator + modelName);
        frame.addModel(svm);

        ClassifierT classifier = new ClassifierT();
        classifier.setModel(svm);
        frame.addTransformer(classifier, new Provider[]{accFeatures, gyrFeatures}, 2, 0);

        // Consumer
        Logger log = new Logger();
        frame.addConsumer(log, classifier, 2, 0);

        FileWriter svmWriter = new FileWriter();
        svmWriter.options.filePath.setValue(dir.getAbsolutePath());
        svmWriter.options.fileName.set(outputFileName);
        frame.addConsumer(svmWriter, classifier, 2, 0);

        // Start framework
        frame.start();

        // Wait duration
        try {
            Thread.sleep(TestHelper.DUR_TEST_NORMAL);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Stop framework
        frame.stop();
        frame.clear();

        // Get data file
        File data = new File(dir, outputFileName + "~");

        // Verify
        Assert.assertTrue(outputFile.length() > 100);
        Assert.assertTrue(data.length() > 100);

        if (outputFile.exists()) outputFile.delete();
        if (data.exists()) data.delete();
    }
}
