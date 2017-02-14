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

package ueg.watchdog.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ueg.watchdog.core.configuration.WatchDogContext;

/**
 * An abstract class for all the WatchDog related classes to manage its states and all the common components.
 *
 * @author Erandi Ganepola
 */
public abstract class AbstractWatchDogElement implements Startable {

    protected Logger logger;
    protected State state = State.NON_EXISTENT;
    protected WatchDogContext watchDogContext;

    public AbstractWatchDogElement(WatchDogContext context) {
        logger = LoggerFactory.getLogger(this.getClass());
        watchDogContext = context;
    }

    @Override
    public void start() {
        logger.debug("Starting watch dog element");
        this.state = State.STARTING;
        startElement();
        this.state = State.STARTED;
        logger.info("WatchDog element started successfully");
    }

    @Override
    public void stop() {
        if (state.compareTo(State.STOPPING) < 0) {
            logger.debug("Stopping WatchDog element");
            this.state = State.STOPPING;
            stopElement();
            this.state = State.STOPPED;
            logger.info("WatchDog element stopped successfully");
        } else {
            logger.warn("Skipping stopping since the state is : {}", state);
        }
    }

    public State getState() {
        return state;
    }

    protected abstract void startElement();

    protected abstract void stopElement();
}
