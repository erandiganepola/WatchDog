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

package ueg.watchdog.core.configuration;

import org.bytedeco.javacpp.avcodec;

import static ueg.watchdog.Constants.VideoConstants.*;

/**
 * The configurations used by the whole watch dog system. This configuration will be shared among all the watch dog member
 * classes
 *
 * @author Erandi Ganepola
 */
public class WatchDogContext {

    private OperatingMode operatingMode = OperatingMode.HUMAN_PRESENCE_AWARE;
    private String storagePath = VIDEO_STORAGE_PATH;
    private String compressionMode = "ultrafast";
    private double frameRate = 5.0;
    private int frameWidth = VIDEO_WIDTH;
    private int frameHeight = VIDEO_HEIGHT;
    /**
     * 2000 kb/s, reasonable "sane" area for 720
     */
    private int videoBitRate = 600000;
    private int videoCodec = avcodec.AV_CODEC_ID_H264;
    private String videoFormat = VIDEO_EXTENSION;
    /**
     * Number between 1-51. 1 means loss-less with low compression ratio. 51 means great compression with great loss
     */
    private int videoQuality = 40;

    private static WatchDogContext instance;

    public OperatingMode getOperatingMode() {
        return operatingMode;
    }

    public void setOperatingMode(OperatingMode operatingMode) {
        this.operatingMode = operatingMode;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getCompressionMode() {
        return compressionMode;
    }

    public void setCompressionMode(String compressionMode) {
        this.compressionMode = compressionMode;
    }

    public double getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(double frameRate) {
        this.frameRate = frameRate;
    }

    public int getVideoBitRate() {
        return videoBitRate;
    }

    public void setVideoBitRate(int videoBitRate) {
        this.videoBitRate = videoBitRate;
    }

    public int getVideoCodec() {
        return videoCodec;
    }

    public void setVideoCodec(int videoCodec) {
        this.videoCodec = videoCodec;
    }

    public String getVideoFormat() {
        return videoFormat;
    }

    public void setVideoFormat(String videoFormat) {
        this.videoFormat = videoFormat;
    }

    public int getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(int videoQuality) {
        this.videoQuality = videoQuality;
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public void setFrameWidth(int frameWidth) {
        this.frameWidth = frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    public void setFrameHeight(int frameHeight) {
        this.frameHeight = frameHeight;
    }

    public static WatchDogContext getInstance() {
        if (instance == null) {
            instance = new WatchDogContext();
        }
        return instance;
    }

    public static void setInstance(WatchDogContext newInstance) {
        instance = newInstance;
    }

    /**
     * Whether WatchDog is operating in normal surveillance camera mode or in storage efficient mode with human presence
     * recognition.
     */
    public enum OperatingMode {
        NORMAL,
        HUMAN_PRESENCE_AWARE
    }
}