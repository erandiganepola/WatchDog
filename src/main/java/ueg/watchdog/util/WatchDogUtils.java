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

package ueg.watchdog.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static ueg.watchdog.Constants.SEPARATOR;
import static ueg.watchdog.Constants.VideoConstants.DATE_FORMAT;
import static ueg.watchdog.Constants.VideoConstants.PROCESSES_VIDEO_DIR;

/**
 * Main utility class for WatchDog
 *
 * @author Erandi Ganepola
 */
public final class WatchDogUtils {

    private static final Logger logger = LoggerFactory.getLogger(WatchDogUtils.class);

    private WatchDogUtils() { }

    public static void createDirectoryIfNotExist(String path) {
        File file = new File(path);
        file.mkdirs();
    }

    public static String getVideoStoragePath(String baseDir) {
        LocalDateTime dateTime = LocalDateTime.now();
        String date = dateTime.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        return baseDir + SEPARATOR + date;
    }

    public static String getProcessedVideoStoragePath(String videoPath) {
        String prefix = videoPath.substring(0, videoPath.lastIndexOf(SEPARATOR));
        String fileName = videoPath.substring(videoPath.lastIndexOf(SEPARATOR) + 1);
        return prefix + SEPARATOR + PROCESSES_VIDEO_DIR + SEPARATOR + fileName;
    }

    public static String getTimestampedVideoFileName() {
        LocalDateTime dateTime = LocalDateTime.now();
        return Timestamp.valueOf(dateTime).toString().split(" ")[1];
    }

    public static LocalDateTime getLocalDateTime(String timeString) {
        LocalDateTime dateTime = LocalDateTime.now();
        return Timestamp.valueOf(dateTime.format(DateTimeFormatter.ofPattern(DATE_FORMAT)) + " " + timeString).toLocalDateTime();
    }

    public static LocalDateTime fromMySQLDate(Timestamp timestamp) {
        return timestamp.toLocalDateTime();
    }

    public static Timestamp toMySQLDate(LocalDateTime dateTime) {
        return Timestamp.valueOf(dateTime);
    }

    public static String finalizeVideo(String videoPath) {
        File file = new File(videoPath);

        String extension = videoPath.substring(videoPath.lastIndexOf("."));
        String filePath = videoPath.substring(0, videoPath.lastIndexOf("."));
        String newFilePath = filePath + "-" + getTimestampedVideoFileName() + extension;

        try {
            Files.move(file.toPath(), new File(newFilePath).toPath());
        } catch (IOException e) {
            logger.error("Error when renaming video file at : {} to : {}", videoPath, newFilePath, e);
            return null;
        }
        return newFilePath.substring(newFilePath.lastIndexOf(SEPARATOR) + 1);
    }
}
