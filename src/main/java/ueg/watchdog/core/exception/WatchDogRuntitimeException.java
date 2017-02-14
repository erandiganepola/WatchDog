package ueg.watchdog.core.exception;

/**
 * A runtime exception to be thrown on error occasions in startup and stop.
 *
 * @author Erandi Ganepola
 */
public class WatchDogRuntitimeException extends RuntimeException {

    public WatchDogRuntitimeException(String msg) {
        super(msg);
    }

    public WatchDogRuntitimeException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
