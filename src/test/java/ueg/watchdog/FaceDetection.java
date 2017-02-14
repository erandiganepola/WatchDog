package ueg.watchdog;

import static org.bytedeco.javacpp.helper.opencv_objdetect.cvHaarDetectObjects;
import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.IplImage;
import static org.bytedeco.javacpp.opencv_core.cvClearMemStorage;
import static org.bytedeco.javacpp.opencv_core.cvGetSeqElem;
import static org.bytedeco.javacpp.opencv_core.cvLoad;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_highgui.cvShowImage;
import static org.bytedeco.javacpp.opencv_highgui.cvWaitKey;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;
import static org.bytedeco.javacpp.opencv_imgproc.CV_AA;
import static org.bytedeco.javacpp.opencv_imgproc.cvRectangle;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;
import org.bytedeco.javacpp.opencv_objdetect.CvHaarClassifierCascade;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author erandi
 */
public class FaceDetection{

	public static final String XML_FILE = 
			"/home/erandi/NetBeansProjects/WatchDog/src/main/resources/face/haarcascade_frontalface_alt.xml";

	public static void main(String[] args){

		IplImage img1 = cvLoadImage("/home/erandi/NetBeansProjects/WatchDog/src/main/resources/6.jpg");
                IplImage img2 = cvLoadImage("/home/erandi/NetBeansProjects/WatchDog/src/main/resources/5.jpg");
                IplImage img3 = cvLoadImage("/home/erandi/NetBeansProjects/WatchDog/src/main/resources/2.jpg");
		detect(img1);
                detect(img2);
                detect(img3);
                		
	}	

	public static void detect(IplImage src){

		CvHaarClassifierCascade cascade = new 
				CvHaarClassifierCascade(cvLoad(XML_FILE));
		CvMemStorage storage = CvMemStorage.create();
		CvSeq sign = cvHaarDetectObjects(
				src,
				cascade,
				storage,
				1.5,
				3,
				CV_HAAR_DO_CANNY_PRUNING);

		cvClearMemStorage(storage);

		int total_Faces = sign.total();		

		for(int i = 0; i < total_Faces; i++){
			CvRect r = new CvRect(cvGetSeqElem(sign, i));
			cvRectangle (
					src,
					cvPoint(r.x(), r.y()),
					cvPoint(r.width() + r.x(), r.height() + r.y()),
					CvScalar.RED,
					2,
					CV_AA,
					0);

		}

		cvShowImage("Result", src);
		cvWaitKey(0);

	}
}
