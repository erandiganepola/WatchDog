package ueg.watchdog.core.processing;

import org.bytedeco.javacv.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ueg.watchdog.Constants;
import ueg.watchdog.api.*;
import ueg.watchdog.core.WatchDog;
import ueg.watchdog.core.processing.face.FisherFacesPersonRecognizer;
import ueg.watchdog.core.processing.face.HaarFaceDetector;
import ueg.watchdog.model.Profile;
import ueg.watchdog.util.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Erandi Ganepola
 */
public class PersonDetectionProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PersonDetectionProcessor.class);

    private ExecutorService threadPool;
    private FrameListener frameListener;

    private int counter = 0;
    private Map<Profile, Integer> recognitions = new HashMap<>();

    private FaceDetector hfd = new HaarFaceDetector();
    private PersonRecognizer personRecognizer = new FisherFacesPersonRecognizer(Constants.PROFILE_PICTURE_DIR);

    public PersonDetectionProcessor() {
        threadPool = Executors.newSingleThreadExecutor();
    }

    public void showCapturingImage(JPanel videoPanel, PersonRecognizedCallback callback) {
        if (frameListener != null) {
            logger.warn("Attempting to re-run Capturing. Ignoring");
            return;
        }

        frameListener = new FrameListener() {
            @Override
            public void frameAdded(Frame frame, long timestamp) {
                threadPool.submit(() -> {
                    FrameData frameData = hfd.detect(frame);
                    Graphics graphics = videoPanel.getGraphics();
                    BufferedImage resizedImage = ImageUtils.getResizedBufferedImage(frameData.getProcessedFrame(), videoPanel);
                    //UI thread invoking
                    SwingUtilities.invokeLater(() -> {
                        graphics.drawImage(resizedImage, 0, 0, videoPanel);
                    });

                    if (frameListener != null && frameData.getNoOfPeople() > 0) {
                        counter++;
                        frameData.getFaces().values().stream()
                                .map(face -> personRecognizer.recognize(face))
                                .forEach(profile -> {
                                    int count = recognitions.computeIfAbsent(profile, key -> 0);
                                    recognitions.put(profile, ++count);
                                });

                        if (counter > 10) {
                            stopCapturingImage();

                            Optional<Profile> profileOptional = recognitions.keySet().stream()
                                    .filter(Objects::nonNull)
                                    .sorted(Comparator.comparingInt(p -> recognitions.get(p)).reversed())
                                    .findFirst();
                            recognitions.clear();

                            if (profileOptional.isPresent()) {
                                logger.debug("Profile identified");
                                callback.onRecognized(profileOptional.get());
                            } else {
                                logger.debug("Unable to identify profile");
                                callback.onNotRecognized();
                            }
                        }
                    }
                });
            }
        }

        ;

        logger.info("Starting image capturing");
        WatchDog.getInstance().

                getFrameManager().

                addFrameListener(frameListener);

    }

    public void stopCapturingImage() {
        logger.info("Stopping Capturing Image");
        WatchDog.getInstance().getFrameManager().removeFrameListener(frameListener);
        frameListener = null;
        counter = 0;
    }
}
