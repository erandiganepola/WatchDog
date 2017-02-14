package ueg.watchdog.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;

import static javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE;

/**
 * Base abstract class that extends JFrame. Handles basic actions for the user
 * input events
 *
 * @author Erandi Ganepola
 */
public abstract class WatchDogBaseFrame extends JFrame {

    private WatchDogBaseFrame parentFrame;
    protected Logger logger;

    public WatchDogBaseFrame(WatchDogBaseFrame parentFrame) {
        this.parentFrame = parentFrame;  //relevant 'UI instance' parameter becomes the parentFrame
        logger = LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Sets what happen when the close button of the window is pressed.
     */
    protected void setCloseOperation() {
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); //nothing happens for the default close operation
        this.addWindowListener(new WindowAdapter() {    //adding a window listener
            @Override
            public void windowClosing(WindowEvent e) {
                if (WatchDogBaseFrame.this.parentFrame == null) {   //calls when user tries to close 'Main' window
                    System.exit(0);
                } else {
                    WatchDogBaseFrame.this.parentFrame.setVisible(true);
                    WatchDogBaseFrame.this.setVisible(false);
                }
            }
        });
    }

    /**
     * This method should be called when the current frame required to
     * exitToParent. The boolean <code>hardExit</code> will determine whether
     * the application should exitToParent or just go to the parent view.
     *
     * @param hardExit whether the application should exitToParent
     */
    public void exitToParent(boolean hardExit) {
        if (hardExit) {
            System.exit(0);
        } else {
            this.setVisible(false);
            this.parentFrame.setVisible(true);
        }
    }

    /**
     * Opens the specified given window and sets the current window as not
     * visible. Sets the given window as visible while hiding the current one.
     *
     * @param frame The child window to be opened
     */
    protected void openGivenWindow(WatchDogBaseFrame frame) {
        this.setVisible(false);
        frame.setVisible(true);
    }
}
