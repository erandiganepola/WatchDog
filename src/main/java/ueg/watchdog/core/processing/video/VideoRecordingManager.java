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

package ueg.watchdog.core.processing.video;

import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;
import ueg.watchdog.api.AbstractWatchDogElement;
import ueg.watchdog.api.FrameListener;
import ueg.watchdog.core.configuration.WatchDogContext;
import ueg.watchdog.core.exception.WatchDogRuntitimeException;
import ueg.watchdog.core.WatchDog;
import ueg.watchdog.model.Video;
import ueg.watchdog.util.WatchDogUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ueg.watchdog.Constants.SEPARATOR;

/**
 * The class responsible for saving video efficiently.
 *
 * @author Erandi Ganepola
 */
public class VideoRecordingManager extends AbstractWatchDogElement implements FrameListener {

    private FFmpegFrameRecorder frameRecorder;
    private String currentVideoFile;
    private ExecutorService executorService;
    private String storagePath;

    public VideoRecordingManager(WatchDogContext context) {
        super(context);
    }

    @Override
    protected void startElement() {
        storagePath = WatchDogUtils.getVideoStoragePath(watchDogContext.getStoragePath());
        currentVideoFile = storagePath + SEPARATOR + WatchDogUtils.getTimestampedVideoFileName() + "." + watchDogContext.getVideoFormat();
        logger.info("Video manager is using storagePath : {}, currentVideoFileName : {}", storagePath, currentVideoFile);
        WatchDogUtils.createDirectoryIfNotExist(storagePath);
        logger.debug("Created video storage directory");

        WatchDog.getInstance().getFrameManager().addFrameListener(this);

        executorService = Executors.newSingleThreadExecutor();

        frameRecorder = new FFmpegFrameRecorder(currentVideoFile, watchDogContext.getFrameWidth(),
                watchDogContext.getFrameHeight(), 0);

        frameRecorder.setVideoOption("preset", watchDogContext.getCompressionMode());
        frameRecorder.setVideoCodec(watchDogContext.getVideoCodec());
        frameRecorder.setFormat(watchDogContext.getVideoFormat());
        frameRecorder.setFrameRate(watchDogContext.getFrameRate());

        try {
            frameRecorder.start();
        } catch (FrameRecorder.Exception e) {
            logger.error("Error when starting the frame recorder", e);
            throw new WatchDogRuntitimeException("Error when starting the frame recorder", e);
        }
    }

    @Override
    protected void stopElement() {
        WatchDog.getInstance().getFrameManager().removeFrameListener(this);
        executorService.shutdown();

        try {
            frameRecorder.stop();
            frameRecorder.release();
        } catch (FrameRecorder.Exception e) {
            logger.error("Error when stopping frame recorder", e);
        }

        logger.debug("Finalizing video file name");
        String finalName = WatchDogUtils.finalizeVideo(currentVideoFile);
        if (finalName == null) {
            logger.error("Unable to finalize video file name");
        } else {
            logger.debug("Finalized name to : {}", finalName);
            Video video = Video.createVideo(finalName, storagePath + SEPARATOR + finalName);
            if (video.save()) {
                logger.info("Saved video({}) info to database", finalName);
            } else {
                logger.warn("Unable to save video ({}) to database", finalName);
            }
        }
    }

    @Override
    public void frameAdded(Frame frame, long timestamp) {
        executorService.submit(() -> {
            try {
                frameRecorder.setTimestamp(timestamp);
                frameRecorder.record(frame);
            } catch (FrameRecorder.Exception e) {
                logger.error("Error occurred when video a frame", e);
            }
        });
    }
}
