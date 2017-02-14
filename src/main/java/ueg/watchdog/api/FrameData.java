package ueg.watchdog.api;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.Frame;

import java.util.HashMap;
import java.util.Map;

/**
 * A class to hold on meta data about a frame through out the framework to be usd by the interested classes
 *
 * @author Erandi Ganepola
 */
public class FrameData {

    private Frame originalFrame;
    private Frame processedFrame;
    private opencv_core.Mat originalMat;
    private opencv_core.IplImage processedIplImage;
    private opencv_core.IplImage originalIplImage;
    private int noOfPeople;
    private opencv_core.CvSeq detectionResults;
    private Map<opencv_core.CvRect, opencv_core.Mat> faces = new HashMap<>();

    public FrameData(Frame originalFrame) {
        this.originalFrame = originalFrame;
    }

    public Frame getOriginalFrame() {
        return originalFrame;
    }

    public void setOriginalFrame(Frame originalFrame) {
        this.originalFrame = originalFrame;
    }

    public Frame getProcessedFrame() {
        return processedFrame;
    }

    public void setProcessedFrame(Frame processedFrame) {
        this.processedFrame = processedFrame;
    }

    public int getNoOfPeople() {
        return noOfPeople;
    }

    public void setNoOfPeople(int noOfPeople) {
        this.noOfPeople = noOfPeople;
    }

    public opencv_core.CvSeq getDetectionResults() {
        return detectionResults;
    }

    public void setDetectionResults(opencv_core.CvSeq detectionResults) {
        this.detectionResults = detectionResults;
    }

    public opencv_core.IplImage getProcessedIplImage() {
        return processedIplImage;
    }

    public void setProcessedIplImage(opencv_core.IplImage processedIplImage) {
        this.processedIplImage = processedIplImage;
    }

    public void addFace(opencv_core.CvRect rect, opencv_core.Mat faceMat) {
        faces.put(rect, faceMat);
    }

    public Map<opencv_core.CvRect, opencv_core.Mat> getFaces() {
        return faces;
    }

    public opencv_core.IplImage getOriginalIplImage() {
        return originalIplImage;
    }

    public void setOriginalIplImage(opencv_core.IplImage originalIplImage) {
        this.originalIplImage = originalIplImage;
    }

    public opencv_core.Mat getOriginalMat() {
        return originalMat;
    }

    public void setOriginalMat(opencv_core.Mat originalMat) {
        this.originalMat = originalMat;
    }
}
