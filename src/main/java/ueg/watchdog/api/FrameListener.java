
package ueg.watchdog.api;

import org.bytedeco.javacv.Frame;

/**
 * All the listeners interested in receiving about the frames being grabbed in the runtime should implement this interface
 * and register in the corresponding frame manager
 *
 * @author Erandi Ganepola
 */
public interface FrameListener {

    void frameAdded(Frame frame, long timestamp);
}
