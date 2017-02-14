package ueg.watchdog.core.processing.gender;

import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ueg.watchdog.api.FrameData;
import ueg.watchdog.api.GenderDetector;
import ueg.watchdog.core.exception.WatchDogRuntitimeException;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_dnn.*;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

/**
 * The class responsible for recognizing gender. This class use the concept of CNN (Convolution Neural Networks) to
 * identify the gender of a detected face.
 *
 * @author Erandi Ganepola
 */
public class CNNGenderDetector implements GenderDetector {

    private static final Logger logger = LoggerFactory.getLogger(CNNGenderDetector.class);

    private Net genderNet;

    public CNNGenderDetector() {
        try {
            genderNet = new Net();
            File protobuf = new File(getClass().getResource("/caffe/deploy_gendernet.prototxt").toURI());
            File caffeModel = new File(getClass().getResource("/caffe/gender_net.caffemodel").toURI());
            Importer importer = createCaffeImporter(protobuf.getAbsolutePath(), caffeModel.getAbsolutePath());
            importer.populateNet(genderNet);
            importer.close();
        } catch (Exception e) {
            logger.error("Error reading prototxt", e);
            throw new WatchDogRuntitimeException("Unable to start CNNGenderDetector", e);
        }
    }

    @Override
    public Map<Mat, Gender> predictGender(FrameData frameData) {
        Map<Mat, Gender> genderMap = new HashMap<>();

        for (Map.Entry<CvRect, Mat> face : frameData.getFaces().entrySet()) {
            try {
                Mat croppedMat = new Mat();
                resize(face.getValue(), croppedMat, new Size(256, 256));
                normalize(croppedMat, croppedMat, 0, Math.pow(2, frameData.getOriginalFrame().imageDepth),
                        NORM_MINMAX, -1, null);

                Blob inputBlob = new Blob(croppedMat);
                genderNet.setBlob(".data", inputBlob);
                genderNet.forward();
                Blob prob = genderNet.getBlob("prob");

                Indexer indexer = prob.matRefConst().createIndexer();
                logger.debug("CNN results {},{}", indexer.getDouble(0, 0), indexer.getDouble(0, 1));
                if (indexer.getDouble(0, 0) > indexer.getDouble(0, 1)) {
                    logger.debug("Male detected");
                    genderMap.put(face.getValue(), Gender.MALE);
                } else {
                    logger.debug("Female detected");
                    genderMap.put(face.getValue(), Gender.FEMALE);
                }
            } catch (Exception e) {
                logger.error("Error when processing gender", e);
            }
        }
        return genderMap;
    }
}
