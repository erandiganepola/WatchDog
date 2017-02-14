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

import org.bytedeco.javacv.Frame;
import ueg.watchdog.api.AbstractWatchDogElement;
import ueg.watchdog.api.FrameListener;
import ueg.watchdog.core.configuration.WatchDogContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class responsible for keeping track of frames grabbed by the {@link WatchDog}
 * class. Also, this class will notify the frames newly received to the
 * listeners.
 *
 * @author Erandi Ganepola
 */
public class FrameManager extends AbstractWatchDogElement {

    private final Set<FrameListener> frameListeners;
    private ExecutorService notifier;

    public FrameManager(WatchDogContext context) {
        super(context);
        frameListeners = new HashSet<>();
    }

    @Override
    protected void startElement() {
        logger.info("Starting frame manager");
        notifier = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void stopElement() {
        logger.info("Stopping frame manager");
        notifier.shutdownNow();
        synchronized (frameListeners) {
            frameListeners.clear();
        }
    }

    /**
     * This method is called when an object needs to be registered as a Listener.
     * Since all the Listeners in the set will receive frames after grabbing them by FrameGrabber.
     */
    public void addFrameListener(FrameListener listener) {
        synchronized (frameListeners) {
            frameListeners.add(listener);
        }
    }

    /**
     * This method is called when an object needs to be removed from the Listener set.
     */
    public void removeFrameListener(FrameListener listener) {
        frameListeners.remove(listener);
    }

    /**
     * This method is called after grabbing frames by FrameGrabber in WatchDog class.
     * Those frames are submitted to already registered FrameListeners.
     *
     * @param frame the newly captured frame
     */
    public void addFrame(Frame frame, long timestamp) {
        notifier.submit(() -> {
            synchronized (frameListeners) {
                frameListeners.forEach(frameListener -> frameListener.frameAdded(frame, timestamp));
            }
        });
    }
}
