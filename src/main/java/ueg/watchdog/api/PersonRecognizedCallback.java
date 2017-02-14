package ueg.watchdog.api;

import ueg.watchdog.model.Profile;

/**
 * @author Erandi Ganepola
 */
public interface PersonRecognizedCallback {

    void onRecognized(Profile profile);

    void onNotRecognized();
}