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

import java.io.File;

/**
 * @author Erandi Ganepola
 */
public final class Constants {

    public static final String SEPARATOR = File.separator;
    public static final String HAAR_CASCADE_FILE = SEPARATOR + "detection" + SEPARATOR + "haarcascade_frontalface_alt.xml";
    public static final String UPPER_BODY_HAAR_CASCADE_FILE = SEPARATOR + "detection" + SEPARATOR + "haarcascade_upperbody.xml";

    public static final String DATA_DIR = "data";
    public static final String PROFILE_PICTURE_DIR = DATA_DIR + SEPARATOR + "photos" + SEPARATOR + "profiles";

    public static final String FISHERFACES_GENDER_MODEL = "detection" + SEPARATOR + "fisherfaces_gender.model";

    public static final String MAIN_REPORT_PATH = "reports" + SEPARATOR + "MainReport.jrxml";
    public static final String RECOGNIZED_PERSON_REPORT_PATH = "reports" + SEPARATOR + "RecognizedPersonReport.jrxml";

    public static final String UNAUTHORIZED = "Not Authorized";

    public static final class VideoConstants {
        public static final String VIDEO_COMPRESSION_MODE = "ultrafast";
        public static final String VIDEO_STORAGE_PATH = DATA_DIR + SEPARATOR + "feeds";
        public static final String VIDEO_NAME_PREFIX = "video";
        public static final String VIDEO_EXTENSION = "mkv";

        public static final int VIDEO_WIDTH = 1280;
        public static final int VIDEO_HEIGHT = 720;

        public static final int RECORDING_FRAME_THRESHOLD = 50;

        public static final String PROCESSES_VIDEO_DIR = "processed";

        public static final String DATE_FORMAT = "yyyy-MM-dd";
        public static final String TIME_FORMAT = "HH:mm:ss.fffffff";
        public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    }
}
