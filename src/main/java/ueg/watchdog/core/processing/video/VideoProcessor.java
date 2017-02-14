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

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacv.*;
import ueg.watchdog.Constants;
import ueg.watchdog.api.*;
import ueg.watchdog.api.GenderDetector.Gender;
import ueg.watchdog.core.configuration.WatchDogContext;
import ueg.watchdog.core.processing.age.CNNAgeDetector;
import ueg.watchdog.core.processing.face.FisherFacesPersonRecognizer;
import ueg.watchdog.core.processing.face.HaarFaceDetector;
import ueg.watchdog.core.processing.gender.FisherFacesGenderDetector;
import ueg.watchdog.model.ProcessedFrameStat;
import ueg.watchdog.model.Profile;
import ueg.watchdog.model.Video;
import ueg.watchdog.util.ImageUtils;
import ueg.watchdog.util.WatchDogUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.bytedeco.javacpp.opencv_core.Point;
import static org.bytedeco.javacpp.opencv_core.Scalar;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_imgproc.putText;
import static ueg.watchdog.Constants.VideoConstants.RECORDING_FRAME_THRESHOLD;

/**
 * Class to process videos.
 *
 * @author Erandi Ganepola
 */
public class VideoProcessor extends AbstractWatchDogElement {

    private ExecutorService executorService;

    private FrameGrabber grabber;
    private FrameRecorder recorder;
    private GenderDetector genderDetector;
    private AgeDetector ageDetector;
    private FaceDetector faceDetector;
    private PersonRecognizer personRecognizer;

    private OpenCVFrameConverter.ToMat toMatConverter;

    public VideoProcessor(WatchDogContext context) {
        super(context);
        toMatConverter = new OpenCVFrameConverter.ToMat();

        genderDetector = new FisherFacesGenderDetector(Constants.FISHERFACES_GENDER_MODEL);
        ageDetector = new CNNAgeDetector();
        faceDetector = new HaarFaceDetector();

        personRecognizer = new FisherFacesPersonRecognizer(Constants.PROFILE_PICTURE_DIR);

        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void startElement() {
        logger.info("Starting video processor");

        executorService.submit((Runnable) () -> {
            for (; ; ) {
                if (State.STARTED.compareTo(getState()) < 0) {
                    logger.warn("State is {}. Stopping...", getState());
                    break;
                }

                logger.debug("Loading unprocessed videos from database");
                List<Video> videos = Video.getVideos(false, false);
                Optional<Video> videoOptional = videos.stream().sorted(Comparator.comparingInt(Video::getId)).findFirst();
                if (videoOptional.isPresent()) {
                    Video video = videoOptional.get();
                    logger.debug("Selected video with id : {}, path : {}", video.getId(), video.getFilePath());
                    if (!video.exists()) {
                        logger.debug("Video ({}) doesn't exist. Marking as deleted", video.getFilePath());
                        video.softDelete();
                    } else {
                        try {
                            processVideo(video);
                        } catch (Exception e) {
                            logger.error("Error occurred when processing video : {}", video.getFilePath(), e);
                        } finally {
                            if (grabber != null) {
                                try { grabber.stop(); grabber.release(); } catch (Exception ignored) { }
                            }
                            if (recorder != null) {
                                try { recorder.stop(); recorder.release(); } catch (Exception ignored) { }
                            }
                        }
                    }
                } else {
                    logger.debug("No video found to be processed");
                    try { Thread.sleep(10000); } catch (InterruptedException ignored) { }
                }
            }
        });
    }

    private void processVideo(Video video) throws Exception {
        grabber = new FFmpegFrameGrabber(new File(video.getFilePath()));
        grabber.start();

        String processedFilePath = WatchDogUtils.getProcessedVideoStoragePath(video.getFilePath());
        WatchDogUtils.createDirectoryIfNotExist(processedFilePath.substring(0, processedFilePath.lastIndexOf(Constants.SEPARATOR)));

        recorder = new FFmpegFrameRecorder(processedFilePath, watchDogContext.getFrameWidth(),
                watchDogContext.getFrameHeight(), grabber.getAudioChannels());
        recorder.setVideoOption("preset", watchDogContext.getCompressionMode());
        recorder.setVideoCodec(watchDogContext.getVideoCodec());
        recorder.setFormat(watchDogContext.getVideoFormat());
        recorder.setFrameRate(watchDogContext.getFrameRate());
        recorder.start();
        // TODO: 12/30/16 Add more options like bit rate to optimize

        int counter = RECORDING_FRAME_THRESHOLD;
        LocalDateTime timestamp = video.getFrom();
        long offsetNanos = (long) ((1 / grabber.getFrameRate()) * 1000000000);

        Frame frame;
        while ((frame = grabber.grab()) != null && State.STARTED.compareTo(getState()) >= 0) {
            timestamp = timestamp.plusNanos(offsetNanos);
            final LocalDateTime finalTimestamp = timestamp;

            //detections
            FrameData frameData = faceDetector.detect(frame);
            Map<Mat, Gender> genders = genderDetector.predictGender(frameData);
            Map<Mat, String> ages = ageDetector.predictAge(frameData);

            frameData.getFaces().entrySet().forEach(entry -> {
                String caption = String.format("%s:[%s]", genders.get(entry.getValue()), ages.get(entry.getValue()));
                logger.debug("Caption : {}", caption);

                // Recognize and add recognized people to the database
                Profile profile = personRecognizer.recognize(entry.getValue());
                ProcessedFrameStat stat = new ProcessedFrameStat(
                        video.getId(),
                        finalTimestamp,
                        caption,
                        ImageUtils.toBufferedImage(entry.getValue()),
                        profile != null ? String.valueOf(profile.getId()) : null);
                stat.checkForDuplicatesAndSave();

                int posX = Math.max(entry.getKey().x() - 10, 0);
                int posY = Math.max(entry.getKey().y() - 10, 0);
                // And now put it into the image:
                putText(frameData.getOriginalMat(), caption, new Point(posX, posY), CV_FONT_HERSHEY_PLAIN, 1.0,
                        new Scalar(255, 255, 255, 2.0));

            });

            putText(frameData.getOriginalMat(), timestamp.toString(), new Point(10, 20), CV_FONT_HERSHEY_PLAIN, 1.0,
                    new Scalar(0, 255, 255, 2.0));

            try {
                //                if (grabber.getTimestamp() > recorder.getTimestamp()) {
                //                    recorder.setTimestamp(grabber.getTimestamp());
                //                }

                if (watchDogContext.getOperatingMode() == WatchDogContext.OperatingMode.HUMAN_PRESENCE_AWARE) {
                    if (frameData.getNoOfPeople() > 0) {
                        counter = RECORDING_FRAME_THRESHOLD; // set to 50
                        Frame processedFrame = toMatConverter.convert(frameData.getOriginalMat());
                        recorder.record(processedFrame);
                    } else if (counter > 0) {
                        counter--;
                        Frame processedFrame = toMatConverter.convert(frameData.getOriginalMat());
                        recorder.record(processedFrame);
                    }
                } else {
                    Frame processedFrame = toMatConverter.convert(frameData.getOriginalMat());
                    recorder.record(processedFrame);
                }
            } catch (Exception e) {
                logger.warn("Error occurred when recording video : {}", e);
            }
        }

        logger.debug("Finished processing video : {}", video.getFilePath());
        try { grabber.stop(); grabber.release(); } catch (Exception ignored) { }
        try { recorder.stop(); recorder.release(); } catch (Exception ignored) { }

        if (State.STARTED.equals(getState())) {
            logger.debug("Marking video ({}) as processed", video.getFilePath());
            video.markProcessed(true);
            logger.debug("Removing raw video file({}) since the video is processed", video.getFilePath());
            video.deleteRawFile();
        }
    }

    @Override
    protected void stopElement() {

    }
}
