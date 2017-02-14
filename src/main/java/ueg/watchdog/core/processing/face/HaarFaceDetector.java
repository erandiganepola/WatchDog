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

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_objdetect;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ueg.watchdog.api.FaceDetector;
import ueg.watchdog.api.FrameData;
import ueg.watchdog.Constants;
import ueg.watchdog.core.exception.WatchDogRuntitimeException;

import java.io.File;

import static org.bytedeco.javacpp.helper.opencv_objdetect.cvHaarDetectObjects;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_imgproc.CV_AA;
import static org.bytedeco.javacpp.opencv_imgproc.cvRectangle;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;

/**
 * Face detector using haar classifier cascades
 *
 * @author Erandi Ganepola
 */
public class HaarFaceDetector implements FaceDetector {

    private static final Logger logger = LoggerFactory.getLogger(HaarFaceDetector.class);

    private opencv_objdetect.CvHaarClassifierCascade haarClassifierCascade;
    private opencv_core.CvMemStorage storage;
    private OpenCVFrameConverter.ToIplImage iplImageConverter;
    private OpenCVFrameConverter.ToMat toMatConverter;

    public HaarFaceDetector() {
        iplImageConverter = new OpenCVFrameConverter.ToIplImage();
        toMatConverter = new OpenCVFrameConverter.ToMat();

        try {
            File haarCascade = new File(this.getClass().getResource(Constants.HAAR_CASCADE_FILE).toURI());
            logger.debug("Using Haar Cascade file located at : {}", haarCascade.getAbsolutePath());
            haarClassifierCascade = new opencv_objdetect.CvHaarClassifierCascade(cvLoad(haarCascade.getAbsolutePath()));
        } catch (Exception e) {
            logger.error("Error when trying to get the haar cascade", e);
            throw new WatchDogRuntitimeException("Unable to load the Haar Cascade", e);
        }
        storage = opencv_core.CvMemStorage.create();
    }

    @Override
    public synchronized FrameData detect(Frame frame) {
        FrameData frameData = new FrameData(frame);

        opencv_core.IplImage iplImage = iplImageConverter.convert(frame);
        frameData.setOriginalIplImage(cvCloneImage(iplImage));

        /*
         * return a CV Sequence (kind of a list) with coordinates of rectangle face area.
         * (returns coordinates of left top corner & right bottom corner)
         */
        opencv_core.CvSeq detectObjects = cvHaarDetectObjects(iplImage, haarClassifierCascade, storage, 1.5, 3, CV_HAAR_DO_CANNY_PRUNING);
        frameData.setDetectionResults(detectObjects);

        int numberOfPeople = detectObjects.total();
        frameData.setNoOfPeople(numberOfPeople);

        Mat matImage = toMatConverter.convert(frame);
        frameData.setOriginalMat(matImage);

        for (int i = 0; i < numberOfPeople; i++) {
            opencv_core.CvRect rect = new opencv_core.CvRect(cvGetSeqElem(detectObjects, i));
            Mat croppedMat = matImage.apply(new Rect(rect.x(), rect.y(), rect.width(), rect.height()));

            frameData.addFace(rect, croppedMat);

            cvRectangle(iplImage, cvPoint(rect.x(), rect.y()), cvPoint(rect.width() + rect.x(), rect.height() + rect.y()),
                    CvScalar.RED, 2, CV_AA, 0);
        }
        frameData.setProcessedIplImage(iplImage);

        Frame processedFrame = iplImageConverter.convert(iplImage);
        frameData.setProcessedFrame(processedFrame);
        return frameData;
    }

    @Override
    public void finalize() {
        cvReleaseMemStorage(storage);
    }
}
