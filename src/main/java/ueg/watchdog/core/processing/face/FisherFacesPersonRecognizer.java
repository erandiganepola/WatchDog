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
package ueg.watchdog.core.processing.face;

import org.bytedeco.javacpp.indexer.IntIndexer;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ueg.watchdog.api.PersonRecognizer;
import ueg.watchdog.model.Profile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_face.FaceRecognizer;
import static org.bytedeco.javacpp.opencv_face.createFisherFaceRecognizer;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;
import static org.bytedeco.javacpp.opencv_imgproc.*;

/**
 * Face recognizer using fisher faces
 *
 * @author Erandi Ganepola
 */
public class FisherFacesPersonRecognizer implements PersonRecognizer {

    private static final Logger logger = LoggerFactory.getLogger(FisherFacesPersonRecognizer.class);

    private boolean initialized = false;
    private FaceRecognizer faceRecognizer;

    public FisherFacesPersonRecognizer(String trainingImagesDir) {
        //        FaceDetector faceDetector = new HaarFaceDetector();
        OpenCVFrameConverter.ToIplImage iplImageConverter = new OpenCVFrameConverter.ToIplImage();
        OpenCVFrameConverter.ToMat matImageConverter = new OpenCVFrameConverter.ToMat();

        logger.info("Starting training fisher faces model");
        Map<Integer, List<Mat>> faceLabelMap = new HashMap<>();

        File[] imageDirs = new File(trainingImagesDir).listFiles();

        if (imageDirs != null) {
            Stream.of(imageDirs).forEach(imageDir -> {
                File[] imageFiles = imageDir.listFiles();
                if (imageFiles != null) {
                    int profileId = Integer.parseInt(imageDir.getName());
                    Stream.of(imageFiles).forEach(imageFile -> {
                        logger.debug("Loading image file : {}-{}", imageDir.getName(), imageFile.getName());
                        IplImage image = cvLoadImage(imageFile.getAbsolutePath(), CV_LOAD_IMAGE_GRAYSCALE);
                        Frame frame = iplImageConverter.convert(image);

                        List<Mat> mats = faceLabelMap.computeIfAbsent(profileId, key -> new ArrayList<>());

                        //                        FrameData frameData = faceDetector.detect(frame);
                        //                        logger.debug("Found {} people in image : {}-{}", frameData.getNoOfPeople(), imageDir.getName(), imageFile.getName());
                        //                        frameData.getFaces().values().stream()
                        Mat mat = matImageConverter.convert(frame);
                        //                                .map(mat -> {
                        resize(mat, mat, new Size(256, 256));
                        normalize(mat, mat, 0, Math.pow(2, mat.depth()), NORM_MINMAX, -1, null);
                        mats.add(mat);
                        //                            return mat;
                        //                        }).forEach(mats::add);
                    });
                }
            });
        }

        int size = faceLabelMap.values().stream().mapToInt(List::size).sum();
        MatVector images = new MatVector(size);
        Mat labels = new Mat(size, 1, CV_32S);
        IntIndexer indexer = labels.createIndexer();
        logger.debug("Found person's with ids : {}", faceLabelMap.keySet());

        int counter = 0;
        for (Map.Entry<Integer, List<Mat>> entry : faceLabelMap.entrySet()) {
            for (Mat face : entry.getValue()) {

                images.put(counter, face);
                indexer.put(counter, 0, entry.getKey());

                counter++;
            }
        }

        faceRecognizer = createFisherFaceRecognizer();
        faceRecognizer.train(images, labels);
    }

    @Override
    public Profile recognize(Mat face) {
        Mat preparedMat = new Mat();
        cvtColor(face, preparedMat, COLOR_BGR2GRAY);
        resize(preparedMat, preparedMat, new Size(256, 256));
        normalize(preparedMat, preparedMat, 0, Math.pow(2, preparedMat.depth()), NORM_MINMAX, -1, null);

        int[] label = new int[]{-1};
        double[] confidence = new double[]{0.0};
        faceRecognizer.predict(preparedMat, label, confidence);
        logger.debug("Prediction is : {}, confidence : {}", label[0], confidence[0]);
        if (confidence[0] < 12) {
            return null;
        }
        return Profile.getProfileById(label[0]);
    }
}
