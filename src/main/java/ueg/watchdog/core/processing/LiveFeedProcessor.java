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

package ueg.watchdog.core.processing;

import net.coobird.thumbnailator.Thumbnails;
import org.bytedeco.javacv.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ueg.watchdog.api.FrameListener;
import ueg.watchdog.core.WatchDog;
import ueg.watchdog.util.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The class responsible for processing and showing live feed
 *
 * @author Erandi Ganepola
 */
public class LiveFeedProcessor {

    private static final Logger logger = LoggerFactory.getLogger(LiveFeedProcessor.class);

    private ExecutorService threadPool;
    private FrameListener frameListener;

    public LiveFeedProcessor() {
        threadPool = Executors.newSingleThreadExecutor();
    }

    public void showLiveFeed(JPanel videoPanel) {
        if (frameListener != null) {
            logger.warn("Attempting to re-run show live feed. Ignoring");
            return;
        }

        frameListener = new FrameListener() {
            @Override
            public void frameAdded(Frame frame, long timestamp) {
                threadPool.submit(() -> {
                    Graphics graphics = videoPanel.getGraphics();
                    BufferedImage resizedImage = ImageUtils.getResizedBufferedImage(frame, videoPanel);
                    SwingUtilities.invokeLater(() -> {
                        graphics.drawImage(resizedImage, 0, 0, videoPanel);
                    });
                });
            }
        };

        logger.info("Starting live feed processing");
        WatchDog.getInstance().getFrameManager().addFrameListener(frameListener);
    }

    public void stopLiveFeed() {
        logger.info("Stopping live feed");
        WatchDog.getInstance().getFrameManager().removeFrameListener(frameListener);
        frameListener = null;
    }
}
