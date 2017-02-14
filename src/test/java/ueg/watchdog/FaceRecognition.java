package ueg.watchdog;/*
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

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import ueg.watchdog.Constants;
import ueg.watchdog.api.FaceDetector;
import ueg.watchdog.api.FrameData;
import ueg.watchdog.api.PersonRecognizer;
import ueg.watchdog.core.processing.face.FisherFacesPersonRecognizer;
import ueg.watchdog.core.processing.face.HaarFaceDetector;

public class FaceRecognition {

    public static void main(String[] args) throws FrameGrabber.Exception {
        PersonRecognizer personRecognizer = new FisherFacesPersonRecognizer(Constants.PROFILE_PICTURE_DIR);
        FaceDetector faceDetector = new HaarFaceDetector();

        FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber("/dev/video1");
        frameGrabber.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                frameGrabber.stop();
                frameGrabber.release();
            } catch (FrameGrabber.Exception ignored) { }
        }));

        while (true) {
            Frame frame = frameGrabber.grab();
            FrameData frameData = faceDetector.detect(frame);
            frameData.getFaces().values().forEach(personRecognizer::recognize);
        }
    }
}
