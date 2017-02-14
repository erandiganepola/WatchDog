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

package ueg.watchdog.core;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import ueg.watchdog.Constants;
import ueg.watchdog.api.AbstractWatchDogElement;
import ueg.watchdog.api.PersonRecognizedCallback;
import ueg.watchdog.core.configuration.WatchDogContext;
import ueg.watchdog.core.processing.PersonDetectionProcessor;
import ueg.watchdog.core.processing.LiveFeedProcessor;
import ueg.watchdog.core.processing.video.VideoProcessor;
import ueg.watchdog.core.processing.video.VideoRecordingManager;
import ueg.watchdog.util.WatchDogUtils;

import javax.swing.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This is the main class responsible for processing Video captured through the camera. This class captures frame by
 * frame and then queue it for processing. At the same time, it can show the current feed.
 *
 * @author Erandi Ganepola
 */
public class WatchDog extends AbstractWatchDogElement {
    // TODO: 1/8/17 Add confidence to person recognition
    private static WatchDog instance;

    private FrameManager frameManager;
    private VideoRecordingManager videoRecordingManager;
    private LiveFeedProcessor liveFeedProcessor;
    private PersonDetectionProcessor detectPersonProcessor;
    private VideoProcessor videoProcessor;

    private FFmpegFrameGrabber frameGrabber;
    private ExecutorService executor;

    private WatchDog(WatchDogContext context) {
        super(context);
        frameManager = new FrameManager(context);
        videoRecordingManager = new VideoRecordingManager(context);
        liveFeedProcessor = new LiveFeedProcessor();
        detectPersonProcessor = new PersonDetectionProcessor();
        videoProcessor = new VideoProcessor(context);

        logger.debug("Creating data directories if not exist");
        WatchDogUtils.createDirectoryIfNotExist(Constants.DATA_DIR);
        WatchDogUtils.createDirectoryIfNotExist(Constants.PROFILE_PICTURE_DIR);

        logger.debug("Starting background tasks");
        //start background processing
        startBackgroundTasks();
    }

    private void startBackgroundTasks() {
        logger.debug("Starting video processor");
        videoProcessor.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            videoProcessor.stop();
        }));
    }

    public static WatchDog getInstance(WatchDogContext context) {
        if (instance == null) {
            instance = new WatchDog(context);
        }
        return instance;
    }

    public static WatchDog getInstance() {
        return instance;
    }

    @Override
    protected void startElement() {
        logger.info("Initializing WatchDog");

        frameGrabber = new FFmpegFrameGrabber("/dev/video0");
        frameGrabber.setFormat("video4linux2");
        frameGrabber.setFrameRate(watchDogContext.getFrameRate());
        frameGrabber.setImageWidth(watchDogContext.getFrameWidth());
        frameGrabber.setImageHeight(watchDogContext.getFrameHeight());

        executor = Executors.newSingleThreadExecutor();

        logger.debug("Starting frame grabber");
        try {
            frameGrabber.start();
            logger.debug("Started frame grabber with image width-height : {}-{}", frameGrabber.getImageWidth(), frameGrabber.getImageHeight());
        } catch (FrameGrabber.Exception e) {
            logger.error("Error when initializing the frame grabber");
            throw new RuntimeException("Unable to start the FrameGrabber");
        }

        logger.debug("Starting the video recorder");
        videoRecordingManager.start();

        logger.debug("Starting frame manager");
        frameManager.start();

        logger.debug("Starting video frame grabbing");
        executor.submit(() -> {
            long waitTime = (long) ((1.0 / watchDogContext.getFrameRate()) * 1000);
            for (; ; ) {
                try {
                    /*
                     * First, we grab a frame from the camera. Then, we add it to a frame queue where multiple parties
                     * can use those stored frames.
                     */
                    Frame frame = frameGrabber.grab();
                    if (frame != null) {
                        frameManager.addFrame(frame, frameGrabber.getTimestamp());
                    }

                    /*
                     * Wait some time to match the frame rate
                     */
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        logger.error("Interrupted", e);
                    }

                } catch (FrameGrabber.Exception e) {
                    logger.error("Error when grabbing the frame", e);
                }

                if (state != State.STARTED) {
                    logger.warn("State is : {}. Stopping frame grabbing", state);
                    break;
                }
            }
            logger.info("Stopped frame grabbing");
        });
    }


    public void showLiveFeed(JPanel videoPanel) {
        liveFeedProcessor.showLiveFeed(videoPanel);
    }

    public void detectPerson(JPanel jPanel, PersonRecognizedCallback callback) {
        detectPersonProcessor.showCapturingImage(jPanel, callback);
    }

    public void stopDetectPerson() {
        detectPersonProcessor.stopCapturingImage();
    }

    public void stopLiveFeed() {
        liveFeedProcessor.stopLiveFeed();
    }

    @Override
    protected synchronized void stopElement() {
        executor.shutdownNow();

        logger.debug("Stopping the video manager");
        videoRecordingManager.stop();

        logger.debug("Stopping the frame manager");
        frameManager.stop();

        try {
            logger.debug("Releasing and stopping FrameGrabber");
            frameGrabber.release();
            frameGrabber.stop();
        } catch (FrameGrabber.Exception e) {
            logger.error("Error occurred when stopping the FrameGrabber", e);
        }
    }

    public FrameManager getFrameManager() {
        return frameManager;
    }

    public VideoRecordingManager getVideoRecordingManager() {
        return videoRecordingManager;
    }

    public LiveFeedProcessor getLiveFeedProcessor() {
        return liveFeedProcessor;
    }
}
