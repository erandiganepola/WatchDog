package ueg.watchdog;

import net.coobird.thumbnailator.Thumbnails;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_dnn;
import org.bytedeco.javacpp.opencv_objdetect;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ueg.watchdog.api.FrameData;
import ueg.watchdog.core.exception.WatchDogRuntitimeException;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.bytedeco.javacpp.helper.opencv_objdetect.cvHaarDetectObjects;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_dnn.createCaffeImporter;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;

/**
 * Created by erandi on 12/11/16.
 *
 * @author Erandi
 */
@SuppressWarnings("ALL") public class GenderDetection {

    private static final Logger logger = LoggerFactory.getLogger(GenderDetection.class);

    private FrameGrabber frameGrabber;
    private opencv_objdetect.CvHaarClassifierCascade haarClassifierCascade;
    private opencv_core.CvMemStorage storage;
    private OpenCVFrameConverter.ToIplImage iplImageConverter = new OpenCVFrameConverter.ToIplImage();

    private OpenCVFrameConverter.ToMat toMatConverter = new OpenCVFrameConverter.ToMat();
    private Java2DFrameConverter bufferedImageConverter = new Java2DFrameConverter();
    private JFrame window;

    private JPanel videoPanel;
    private JPanel origPhotoPanel;
    private JPanel resizedPhotoPanel;
    private JLabel label;

    private opencv_dnn.Net genderNet;
    private opencv_dnn.Net ageNet;

    private String[] ages = new String[]{"0-2", "4-6", "8-13", "15-20", "25-32", "38-43", "48-53", "60-"};

    private void start() {
        frameGrabber = new OpenCVFrameGrabber(0);
        frameGrabber.setImageWidth(1366);
        frameGrabber.setImageHeight(768);

        window = new JFrame();
        JPanel basePanel = new JPanel();
        basePanel.setLayout(new BoxLayout(basePanel, BoxLayout.X_AXIS));

        videoPanel = new JPanel();
        origPhotoPanel = new JPanel();
        resizedPhotoPanel = new JPanel();
        label = new JLabel("");

        basePanel.add(origPhotoPanel);
        basePanel.add(resizedPhotoPanel);
        basePanel.add(videoPanel);
        basePanel.add(label);
        window.add(basePanel);

        window.setSize(new Dimension(1000, 500));
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setVisible(true);

        try {
            genderNet = new opencv_dnn.Net();
            File protobuf = new File(getClass().getResource("/caffe/deploy_gendernet.prototxt").toURI());
            File caffeModel = new File(getClass().getResource("/caffe/gender_net.caffemodel").toURI());
            opencv_dnn.Importer importer = createCaffeImporter(protobuf.getAbsolutePath(), caffeModel.getAbsolutePath());
            importer.populateNet(genderNet);
            importer.close();

            ageNet = new opencv_dnn.Net();
            protobuf = new File(getClass().getResource("/caffe/deploy_agenet.prototxt").toURI());
            caffeModel = new File(getClass().getResource("/caffe/age_net.caffemodel").toURI());

            importer = createCaffeImporter(protobuf.getAbsolutePath(), caffeModel.getAbsolutePath());
            importer.populateNet(ageNet);
            importer.close();

        } catch (Exception e) {
            logger.error("Error reading prototxt", e);
            throw new WatchDogRuntitimeException("Unable to start CNNGenderDetector", e);
        }

        try {
            File haarCascade = new File(this.getClass().getResource(Constants.HAAR_CASCADE_FILE).toURI());
            logger.debug("Using Haar Cascade file located at : {}", haarCascade.getAbsolutePath());
            haarClassifierCascade = new opencv_objdetect.CvHaarClassifierCascade(cvLoad(haarCascade.getAbsolutePath()));
        } catch (Exception e) {
            logger.error("Error when trying to get the haar cascade", e);
            throw new WatchDogRuntitimeException("Unable to load the Haar Cascade", e);
        }
        storage = opencv_core.CvMemStorage.create();

        try {
            frameGrabber.start();
        } catch (FrameGrabber.Exception e) {
            logger.error("Error when initializing the frame grabber");
            throw new RuntimeException("Unable to start the FrameGrabber");
        }

        for (; ; ) {
            try {
                Frame frame = frameGrabber.grab();
                processFrame(frame);
            } catch (FrameGrabber.Exception e) {
                logger.error("Error when grabbing the frame", e);
            }
        }

        //        try {
        //            File imagesDir = new File(getClass().getResource("/").toURI());
        //            for (File imageFile : imagesDir.listFiles()) {
        //                IplImage image = cvLoadImage(imageFile.getAbsolutePath());
        //                if (image == null) continue;
        //                processFrame(iplImageConverter.convert(image));
        //            }
        //        } catch (URISyntaxException e) {
        //            logger.error("Error", e);
        //        }
    }

    private void processFrame(Frame frame) {
        FrameData metaData = new FrameData(frame.clone());

        opencv_core.IplImage iplImage = iplImageConverter.convert(frame);
        metaData.setOriginalIplImage(cvCloneImage(iplImage));

        //Cv sequence is like a list
        opencv_core.CvSeq detectObjects = cvHaarDetectObjects(iplImage, haarClassifierCascade, storage, 1.5, 3, CV_HAAR_DO_CANNY_PRUNING);
        metaData.setDetectionResults(detectObjects);

        int numberOfPeople = detectObjects.total();
        metaData.setNoOfPeople(numberOfPeople);

        Mat matImage = toMatConverter.convert(metaData.getOriginalFrame());

        for (int i = 0; i < numberOfPeople; i++) {
            opencv_core.CvRect rect = new opencv_core.CvRect(cvGetSeqElem(detectObjects, i));
            Mat mat = matImage.apply(new Rect(rect.x(), rect.y(), rect.width(), rect.height()));
            metaData.addFace(rect, mat);

            cvRectangle(iplImage, cvPoint(rect.x(), rect.y()), cvPoint(rect.width() + rect.x(), rect.height() + rect.y()),
                    CvScalar.RED, 2, CV_AA, 0);
        }
        metaData.setProcessedIplImage(iplImage);

        Frame processedFrame = iplImageConverter.convert(iplImage);
        metaData.setProcessedFrame(processedFrame);

        if (metaData.getNoOfPeople() > 0) {
            logger.debug("Found {} people. Processing ...", metaData.getNoOfPeople());
        }

        drawImage(videoPanel, metaData.getProcessedFrame());

        for (Map.Entry<CvRect, Mat> face : metaData.getFaces().entrySet()) {
            label.setText("");
            try {
                drawImage(origPhotoPanel, toMatConverter.convert(face.getValue()));

                Mat croppedMat = new Mat();
                resize(face.getValue(), croppedMat, new Size(256, 256));
                Mat normalizedMat = new Mat(croppedMat.size());
                normalize(croppedMat, normalizedMat, 0, Math.pow(2, processedFrame.imageDepth), NORM_MINMAX, -1, null);

                drawImage(resizedPhotoPanel, toMatConverter.convert(normalizedMat));
                drawImage(videoPanel, metaData.getProcessedFrame());

                opencv_dnn.Blob inputBlob = new opencv_dnn.Blob(normalizedMat);
                genderNet.setBlob(".data", inputBlob);
                genderNet.forward();
                opencv_dnn.Blob genderProb = genderNet.getBlob("prob");

                ageNet.setBlob(".data", inputBlob);
                ageNet.forward();
                opencv_dnn.Blob ageProb = ageNet.getBlob("prob");

                Mat probMat = genderProb.matRefConst();
                Indexer indexer = probMat.createIndexer();

                DoublePointer pointer = new DoublePointer(new double[1]);
                opencv_core.Point max = new opencv_core.Point();
                minMaxLoc(ageProb.matRefConst(), null, pointer, null, max, null);
                logger.debug("CNN results {},{}", indexer.getDouble(0, 0), indexer.getDouble(0, 1));
                String gender;
                if (indexer.getDouble(0, 0) > indexer.getDouble(0, 1)) {
                    gender = "Male";
                } else {
                    gender = "Female";
                }
                putText(normalizedMat, gender + " : " + ages[max.x()], new opencv_core.Point(0, 0),
                        FONT_HERSHEY_PLAIN, 2.0, new Scalar(100), 2, 8, false);
                drawImage(resizedPhotoPanel, toMatConverter.convert(normalizedMat));
                logger.debug(gender + " : " + ages[max.x()]);
                Thread.sleep(5000);
            } catch (Exception e) {
                logger.error("Error when processing gender", e);
            }
        }
    }

    private void drawImage(JPanel panel, Frame frame) {
        Graphics graphics = panel.getGraphics();
        try {
            BufferedImage resizedImage = Thumbnails.of(bufferedImageConverter.getBufferedImage(frame))
                    .size(Math.min(frame.imageWidth, panel.getWidth()), Math.min(frame.imageHeight, panel.getHeight()))
                    .asBufferedImage();

            SwingUtilities.invokeLater(() -> {
                graphics.drawImage(resizedImage, 0, 0, panel);
            });
        } catch (IOException e) {
            logger.error("Unable to convert the image to buffered image", e);
        }
    }

    public static void main(String[] args) {
        new GenderDetection().start();
    }
}
