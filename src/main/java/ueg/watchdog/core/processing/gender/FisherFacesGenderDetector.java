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
package ueg.watchdog.core.processing.gender;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_face;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ueg.watchdog.api.FrameData;
import ueg.watchdog.api.GenderDetector;
import ueg.watchdog.core.exception.WatchDogRuntitimeException;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.bytedeco.javacpp.opencv_face.createFisherFaceRecognizer;
import static org.bytedeco.javacpp.opencv_imgproc.*;

/**
 * Gender detection using fisher faces algorithm
 *
 * @author Erandi Ganepola
 */
public class FisherFacesGenderDetector implements GenderDetector {

    private static final Logger logger = LoggerFactory.getLogger(FisherFacesGenderDetector.class);
    private opencv_face.FaceRecognizer genderRecognizer;

    /**
     * Constructor will load the model from the file.
     *
     * @param modelFile saved model file
     */
    public FisherFacesGenderDetector(String modelFile) {
        genderRecognizer = createFisherFaceRecognizer();
        try {
            genderRecognizer.load(new File(this.getClass().getClassLoader().getResource(modelFile).toURI()).getAbsolutePath());
        } catch (Exception e) {
            logger.error("Error occurred when creating gender recognizer", e);
            throw new WatchDogRuntitimeException("Unable to create gender recognizer", e);
        }
    }

    @Override
    public Map<Mat, Gender> predictGender(FrameData frameData) {
        Map<Mat, Gender> genderMap = new HashMap<>();

        for (Map.Entry<opencv_core.CvRect, Mat> face : frameData.getFaces().entrySet()) {
            Mat croppedMat = new Mat();
            cvtColor(face.getValue(), croppedMat, COLOR_BGR2GRAY);
            //face should be 92,112px size
            resize(croppedMat, croppedMat, new opencv_core.Size(92, 112));

            int[] label = new int[]{-1};
            double[] confidence = new double[]{0.0};
            genderRecognizer.predict(croppedMat, label, confidence);
            logger.debug("Prediction is : {}, confidence : {}", label[0], confidence[0]);
            genderMap.put(face.getValue(), label[0] == 0 ? Gender.FEMALE : Gender.MALE);
        }
        return genderMap;

    }
}
