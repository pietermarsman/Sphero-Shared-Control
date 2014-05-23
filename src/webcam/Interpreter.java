package webcam;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvClearMemStorage;
import static com.googlecode.javacv.cpp.opencv_core.cvClearSeq;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateMemStorage;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSeqElem;
import static com.googlecode.javacv.cpp.opencv_core.cvPointFrom32f;
import static com.googlecode.javacv.cpp.opencv_core.cvReleaseImage;
import static com.googlecode.javacv.cpp.opencv_core.cvReleaseMemStorage;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_HOUGH_GRADIENT;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCanny;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvHoughCircles;

import java.util.Date;
import java.util.logging.Level;

import main.CircleObservation;
import main.Constant;
import main.StaticMethods;
import main.Sphero;

import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;
import com.googlecode.javacv.cpp.opencv_core.CvPoint3D32f;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

/**
 * @author Pieter Marsman
 */
public class Interpreter {

	Sphero sphero;
	CvPoint spheroCenter, spheroNewCenter;

	/**
	 * Initializes this class with reference to
	 * 
	 * @param sphero
	 */
	public Interpreter(Sphero sphero) {
		this.sphero = sphero;
	}

	/**
	 * Finds the best circle in an image with no further information
	 * 
	 * @param img Image object
	 * @return the best circle
	 */
	private CircleObservation findSphero(Image img) {
		CvMemStorage memHough = cvCreateMemStorage(0);
		IplImage graySmooth = img.getGray();
		int maxWindow = Math.max(img.getSize().width(), img.getSize().height());
		CvSeq circles = cvHoughCircles(graySmooth, memHough, CV_HOUGH_GRADIENT, 1.0, maxWindow,
				Constant.CIRCLE_CANNY_PARAMETER, Constant.CIRCLE_DETECTION_THRESHOLD, 5, maxWindow / 2);
		CircleObservation circleObservation = null;
		if (circles.total() > 0) {
			CvPoint3D32f circle = new CvPoint3D32f(cvGetSeqElem(circles, 0));
			circleObservation = new CircleObservation(circle.x(), circle.y(), circle.z());
		}
		cvClearSeq(circles);
		cvClearMemStorage(memHough);
		cvReleaseMemStorage(memHough);
		return circleObservation;
	}

	/**
	 * Finds the best circle in an image based on an earlier observation
	 * 
	 * @param img Image object
	 * @param location Previous location
	 * @param radius Previous radius
	 * @param hsb Previous color
	 * @return The best circle
	 */
	private CircleObservation findSphero(Image img, CvPoint location, float radius, CvScalar hsb) {
		// If sphero is not seen for a long time the earlier observation is
		// useless
		if (sphero.notSeenFor() > Constant.NOT_SEEN_FOR || !sphero.isObserved() || location == null || radius < 1.0
				|| hsb == null) {
			main.SpheroExperiment.Log.log(Level.WARNING, "--- Didn't see Sphero for a long time ---");
			sphero.reset();
			return findSphero(img);
		}
		// Search for sphero with valid and usefull information
		else {
			// Threshold image for colours: hue, saturnation, brightness
			double h = hsb.getVal(0), s = hsb.getVal(1), b = hsb.getVal(2);
			CvScalar low = new CvScalar(Math.max(Constant.COLOR_MIN, h - Constant.HUE_SEARCH_RANGE), Math.max(
					Constant.COLOR_MIN, s - Constant.SATURNATION_SEARCH_RANGE), Math.max(Constant.COLOR_MIN, b
					- Constant.BRIGHTNESS_SEARCH_RANGE), 0.0);
			CvScalar high = new CvScalar(Math.min(Constant.COLOR_MAX, h + Constant.HUE_SEARCH_RANGE), Math.min(
					Constant.COLOR_MAX, s + Constant.SATURNATION_SEARCH_RANGE), Math.min(Constant.COLOR_MAX, b
					+ Constant.BRIGHTNESS_SEARCH_RANGE), 0.0);
			int blury = (int) Math.round(radius) / 2;
			IplImage temp = img.getThreshold(low, high, blury);

			// Look for circles of size 'size'
			CvMemStorage mem = cvCreateMemStorage(0);
			int small = (int) Math.round(Math.max(0, radius / Constant.CIRCLE_SEARCH_RANGE));
			int big = (int) Math.round(Math.max(0, radius * Constant.CIRCLE_SEARCH_RANGE));
			IplImage edges = cvCreateImage(img.getSize(), IPL_DEPTH_8U, 1);
			cvCanny(temp, edges, Constant.CANNY_LOWER_BOUND, Constant.CANNY_UPPER_BOUND, Constant.CANNY_KERNEL_SIZE);
			CvSeq circles = cvHoughCircles(edges, mem, CV_HOUGH_GRADIENT, 1, (int) radius,
					Constant.CIRCLE_CANNY_PARAMETER, 10, small, big);
			// cvSaveImage("img//temp" + new Date().getTime() + ".png", temp);

			// Use the circle that is closest to the earlier observation
			CircleObservation circleObservation = null;
			double maxDist = Double.MAX_VALUE;
			for (int i = 0; i < circles.total(); i++) {
				CvPoint3D32f result = new CvPoint3D32f(cvGetSeqElem(circles, i));
				CvPoint center = cvPointFrom32f(new CvPoint2D32f(result.x(), result.y()));
				double dist = StaticMethods.distance(center, location);
				if (dist < maxDist && dist < Constant.CIRCLE_NEAR * radius) {
					circleObservation = new CircleObservation(result.x(), result.y(), result.z());
					maxDist = dist;
				}
			}
			cvReleaseImage(edges);
			cvClearMemStorage(mem);
			cvReleaseMemStorage(mem);
			return circleObservation;
		}
	}

	/**
	 * Finds sphero in the image and automatically reports this observation to
	 * the sphero class
	 * 
	 * @param img
	 */
	public void location(Image img, double mmPerPx) {
		spheroCenter = sphero.getCenter();
		spheroNewCenter = null;
		float radius = sphero.getRadius();
		CvScalar color = sphero.getColor();
		CircleObservation circleObservation = findSphero(img, spheroCenter, radius, color);
		if (circleObservation != null) {
			spheroNewCenter = new CvPoint(Math.round(circleObservation.x), Math.round(circleObservation.y));
			float newRadius = circleObservation.radius;
			CvScalar newHsbColor = img.getColor(spheroNewCenter, newRadius);
			sphero.observe(spheroNewCenter, newRadius, newHsbColor, mmPerPx);
		} else {
			System.out.print(".");
		}
		spheroCenter = null;
		spheroNewCenter = null;
	}
}
