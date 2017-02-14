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
package ueg.watchdog;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.indexer.IntIndexer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_objdetect;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Stream;

import static org.bytedeco.javacpp.opencv_core.CV_32S;
import static org.bytedeco.javacpp.opencv_face.FaceRecognizer;
import static org.bytedeco.javacpp.opencv_face.createFisherFaceRecognizer;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

public class FisherFacesGenderDetectionTraining {

    private static final Logger logger = LoggerFactory.getLogger(FisherFacesGenderDetectionTraining.class);

    private static final List<Integer> females = Arrays.asList(1, 8, 10, 12, 18, 28, 32, 35, 41, 43, 46);
    private static final String trainingDir = "att_faces";

    private FaceRecognizer faceRecognizer;

    public void train() throws URISyntaxException {
        OpenCVFrameConverter.ToIplImage iplImageConverter = new OpenCVFrameConverter.ToIplImage();
        OpenCVFrameConverter.ToMat matImageConverter = new OpenCVFrameConverter.ToMat();

        logger.info("Starting training fisher faces model");
        Map<Integer, List<opencv_core.Mat>> faceLabelMap = new HashMap<>();

        File[] imageDirs = new File(this.getClass().getClassLoader().getResource(trainingDir).toURI()).listFiles();

        Stream.of(imageDirs).forEach(imageDir -> {
            File[] imageFiles = imageDir.listFiles();
            Stream.of(imageFiles).forEach(imageFile -> {
                logger.debug("Loading image file : {}-{}", imageDir.getName(), imageFile.getName());
                opencv_core.IplImage image = cvLoadImage(imageFile.getAbsolutePath(), CV_LOAD_IMAGE_GRAYSCALE);
                Frame frame = iplImageConverter.convert(image);

                int subject = Integer.parseInt(imageDir.getName().substring(imageDir.getName().lastIndexOf("s") + 1));
                List<opencv_core.Mat> mats = faceLabelMap.computeIfAbsent(females.contains(subject) ? 0 : 1, key -> new ArrayList<>());
                opencv_core.Mat mat = matImageConverter.convert(frame);
                resize(mat, mat, new opencv_core.Size(92, 112));
                mats.add(mat);
            });
        });

        /*
        * size = number of training sample images and create a mat vector for that number
        * lables = mat vector for lables (n by 1 integer metrix)
        * create indexer for loooping (as required in C++ way)
         */
        int size = faceLabelMap.values().stream().mapToInt(List::size).sum();
        opencv_core.MatVector images = new opencv_core.MatVector(size);
        opencv_core.Mat labels = new opencv_core.Mat(size, 1, CV_32S);
        IntIndexer indexer = labels.createIndexer();
        logger.debug("Found person's with ids : {}", faceLabelMap.keySet());

        //put our sample images and lables in to above created vectors
        int counter = 0;
        for (Map.Entry<Integer, List<opencv_core.Mat>> entry : faceLabelMap.entrySet()) {
            for (opencv_core.Mat face : entry.getValue()) {
                images.put(counter, face);
                indexer.put(counter, 0, entry.getKey());
                counter++;
            }
        }

        //train FisherFaces algorithm using sample images
        faceRecognizer = createFisherFaceRecognizer();
        faceRecognizer.train(images, labels);
        faceRecognizer.save("fisherfaces_gender.model");
    }

    public static void main(String[] args) {
        Loader.load(opencv_objdetect.class);
        try {
            new FisherFacesGenderDetectionTraining().train();
        } catch (URISyntaxException e) {
            logger.error("Error occurred when training model", e);
        }
    }
}
