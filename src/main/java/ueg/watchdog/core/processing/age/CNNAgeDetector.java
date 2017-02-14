/*
 * The MIT License (MIT)
 * Copyright (c) 2016 Erandi Ganepola
 * <p>
 * Permission is hereby granted, free of charge,
 * to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS",
 * WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ueg.watchdog.core.processing.age;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.opencv_dnn;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ueg.watchdog.api.AgeDetector;
import ueg.watchdog.api.FrameData;
import ueg.watchdog.core.exception.WatchDogRuntitimeException;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_dnn.*;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

/**
 * Age predictor using Convolution Neural Networks
 *
 * @author Erandi Ganepola
 */
public class CNNAgeDetector implements AgeDetector {

    private static final Logger logger = LoggerFactory.getLogger(CNNAgeDetector.class);

    private static final String[] AGES = new String[]{"0-2", "4-6", "8-13", "15-20", "25-32", "38-43", "48-53", "60-"};

    //Net is the OpenCV class that represents NN
    private Net ageNet;

    public CNNAgeDetector() {
        try {
            ageNet = new Net();
            File protobuf = new File(getClass().getResource("/caffe/deploy_agenet.prototxt").toURI());
            File caffeModel = new File(getClass().getResource("/caffe/age_net.caffemodel").toURI());

            //create NN from the caffe model. OpenCV supports caffe framework.
            Importer importer = createCaffeImporter(protobuf.getAbsolutePath(), caffeModel.getAbsolutePath());
            importer.populateNet(ageNet);
            importer.close();
        } catch (URISyntaxException e) {
            logger.error("Unable to load the caffe model", e);
            throw new WatchDogRuntitimeException("Unable to load the caffe model", e);
        }
    }

    @Override
    public Map<Mat, String> predictAge(FrameData frameData) {
        Map<Mat, String> ageMap = new HashMap<>();

        for (Map.Entry<CvRect, Mat> face : frameData.getFaces().entrySet()) {
            try {
                Mat resizedMat = new Mat();
                resize(face.getValue(), resizedMat, new Size(256, 256));
                normalize(resizedMat, resizedMat, 0, Math.pow(2, frameData.getOriginalFrame().imageDepth),
                        NORM_MINMAX, -1, null);

                opencv_dnn.Blob inputBlob = new opencv_dnn.Blob(resizedMat);
                ageNet.setBlob(".data", inputBlob);
                ageNet.forward();
                //output layer
                opencv_dnn.Blob prob = ageNet.getBlob("prob");

                DoublePointer pointer = new DoublePointer(new double[1]);
                Point max = new Point();
                minMaxLoc(prob.matRefConst(), null, pointer, null, max, null);
                ageMap.put(face.getValue(), AGES[max.x()]);
            } catch (Exception e) {
                logger.error("Error when processing gender", e);
            }
        }
        return ageMap;
    }
}
