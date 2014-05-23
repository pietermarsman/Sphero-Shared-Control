package webcam;

import static com.googlecode.javacv.cpp.opencv_core.CV_AA;
import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvCircle;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvInRangeS;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2HSV;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_GAUSSIAN;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_MEDIAN;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvSmooth;
import static com.googlecode.javacv.cpp.opencv_core.cvReleaseImage;
import static com.googlecode.javacv.cpp.opencv_core.cvCopy;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSize;
import static com.googlecode.javacv.cpp.opencv_core.CV_RGB;
import static com.googlecode.javacv.cpp.opencv_core.cvLine;
import static com.googlecode.javacv.cpp.opencv_core.cvDrawContours;
import static com.googlecode.javacv.cpp.opencv_core.cvPointFrom32f;
import static com.googlecode.javacv.cpp.opencv_core.cvPoint;
import static com.googlecode.javacv.cpp.opencv_core.cvRectangle;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;

import main.Constant;
import main.Sphero;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvPoint3D32f;
import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_video.BackgroundSubtractorMOG2;

import control.BrooksControl;
import control.Command;
import control.Goal;
import control.Goals;

/**
 * @author Pieter Marsman
 */
public class Image {

	private IplImage img, drawing;
	private IplImage threshold, temp;
	private IplImage hsbImg, hsbSmooth;
	private IplImage gray;
	private IplImage background, foreground;
	private CvSize cs;
	BackgroundSubtractorMOG2 bg;

	/**
	 * Constructor
	 * 
	 * @param image
	 *            is the original of this package
	 */
	public Image(IplImage image) {
		bg = new BackgroundSubtractorMOG2();
		newImage(image, false);
	}

	/**
	 * @param image
	 *            that should be wrapped
	 */
	public void newImage(IplImage image, boolean subtractBackground) {
		threshold = null;
		temp = null;
		cs = cvGetSize(image);
		img = cvCreateImage(cs, image.depth(), image.nChannels());
		drawing = cvCreateImage(cs, image.depth(), image.nChannels());
		background = cvCreateImage(cs, image.depth(), image.nChannels());
		foreground = cvCreateImage(cs, image.depth(), 1);
		gray = cvCreateImage(cs, IPL_DEPTH_8U, 1);
		cvCopy(image, img);
		cvCopy(image, drawing);
		toHsb();
		if (subtractBackground)
			background();
	}

	private void toGray() {
		temp = cvCreateImage(cs, IPL_DEPTH_8U, 1);
		cvCvtColor(img, temp, CV_BGR2GRAY);
		cvSmooth(temp, gray, CV_GAUSSIAN, Constant.SMALL_BLUR);
	}

	private void toHsb() {
		hsbImg = cvCreateImage(cs, IPL_DEPTH_8U, 3);
		hsbSmooth = cvCreateImage(cs, IPL_DEPTH_8U, 3);
		cvCvtColor(img, hsbImg, CV_BGR2HSV);
		cvSmooth(hsbImg, hsbSmooth, CV_GAUSSIAN, Constant.SMALL_BLUR);
	}

	private void background() {
		bg.apply(img, foreground, Constant.IMAGE_BACKGROUND_SUBTRACTION_FILTER);
		bg.getBackgroundImage(background);
		cvCvtColor(background, background, CV_BGR2HSV);
		// cvSaveImage("thres.png", background);
	}

	/**
	 * Releases all the images that are used in this class
	 */
	public void release() {
		cvReleaseImage(img);
		cvReleaseImage(drawing);
		if (threshold != null)
			cvReleaseImage(threshold);
		if (temp != null)
			cvReleaseImage(temp);
		cvReleaseImage(hsbImg);
		cvReleaseImage(hsbSmooth);
		cvReleaseImage(gray);
		cvReleaseImage(background);
		cvReleaseImage(foreground);
	}

	// --- Draw stuff ---
	/**
	 * @param sphero
	 * @param ground
	 * @param brooks
	 * @param c1
	 *            Command 1
	 * @param c2
	 *            Command 2
	 * @param c3
	 *            Command 3
	 * @param goals
	 */
	public void drawObservation(Sphero sphero, Ground ground, BrooksControl brooks, Command c1, Command c2, Command c3, Goals goals) {
		CvPoint spheroCenter = null, closestObstacle = null;

		spheroCenter = drawSpheroObservation(sphero);
		drawGoals(goals);
		drawCommands(sphero, c1, c2, c3);
		// Ground is drawn by the object self to prevent
		// ACCESS_VIOLATION_ERROR's
		if (ground != null && ground.getClosestPoint() != null)
			drawLine(ground.getClosestPoint().x, spheroCenter);
	}

	/**
	 * @param s
	 *            Sphero object
	 * @return Center of sphero
	 */
	private CvPoint drawSpheroObservation(Sphero s) {
		CvPoint spheroCenter = null;
		int radius = 0;
		if (s != null) {
			spheroCenter = s.getCenter();
			radius = (int) s.getRadius();
		}
		if (spheroCenter != null && radius >= 1.0)
			cvCircle(drawing, spheroCenter, radius, CvScalar.GREEN, 2, CV_AA, 0);
		return spheroCenter;
	}

	/**
	 * @param g
	 *            Ground object
	 */
	public void drawGroundObservation(CvSeq ground) {
		if (ground != null) {
			if (!ground.isNull()) {
				cvDrawContours(drawing, ground, CvScalar.RED, CV_RGB(0, 0, 0), 2, 1, CV_AA);
			}
		}
	}

	/**
	 * @param goals
	 *            Goals object
	 */
	private void drawGoals(Goals goals) {
		if (goals != null) {
			for (Goal goal : goals.getAllGoals()) {
				CvPoint3D32f circle = goal.getPoint();
				CvPoint center = cvPointFrom32f(new CvPoint2D32f(circle.x(), circle.y()));
				int goalRadius = (int) circle.z();
				cvCircle(drawing, center, goalRadius, CvScalar.MAGENTA, 2, CV_AA, 0);
			}
		}
	}

	/**
	 * @param spheroCenter
	 *            Center of sphero
	 * @param c1
	 *            Command 1
	 * @param c2
	 *            Command 2
	 * @param c3
	 *            Command 3
	 */
	private void drawCommands(Sphero s, Command c1, Command c2, Command c3) {
		if (s.getCenter() != null) {
			for (Command c : new LinkedList<Command>(Arrays.asList(c1, c2, c3))) {
				if (c != null) {
					CvPoint to = new CvPoint(s.getCenter().x() + (int) (c.getX() * 100.0), s.getCenter().y() + (int) (c.getY() * 100.0));
					cvLine(drawing, s.getCenter(), to, CvScalar.BLUE, 2, 8, 0);
				}
			}
		}
	}

	/**
	 * Draws a line on private IplImage drawing
	 * 
	 * @param c1
	 *            from CvPoint
	 * @param c2
	 *            to CvPoint
	 */
	public void drawLine(CvPoint c1, CvPoint c2) {
		if (c1 != null && c2 != null) {
			cvLine(drawing, c1, c2, CvScalar.GREEN, 2, 8, 0);
		}
	}

	// --- Getters ---
	/**
	 * @return size of the image
	 */
	public CvSize getSize() {
		return cs;
	}

	/**
	 * @param location
	 * @param radius
	 * @param back
	 * @return color of the hsb image on the location, blurred with value
	 *         corresponding to radius and possibly the background image
	 */
	public CvScalar getColor(CvPoint location, float radius, boolean back) {
		ByteBuffer buffer;
		if (back)
			buffer = background.getByteBuffer();
		else
			buffer = hsbSmooth.getByteBuffer();
		int index = location.y() * img.widthStep() + location.x() * img.nChannels();
		CvScalar s = null;
		if (index < buffer.limit())
			s = new CvScalar(buffer.get(index) & 0xFF, buffer.get(index + 1) & 0xFF, buffer.get(index + 2) & 0xFF, 0);
		return s;
	}

	/**
	 * @param location
	 * @param radius
	 * @return color of the hsb image on the location, blurred with value
	 *         corresponding to radius
	 */
	public CvScalar getColor(CvPoint location, float radius) {
		return getColor(location, radius, false);
	}

	/**
	 * @return hsb image
	 */
	public IplImage getHsb() {
		return hsbSmooth;
	}

	/**
	 * @return gray image. Is computed during this call.
	 */
	public IplImage getGray() {
		toGray();
		return gray;
	}

	/**
	 * @param low
	 *            threshold for hsb image.
	 * @param high
	 *            threshold for hsb image.
	 * @param blur
	 *            value before thresholding image.
	 * @return thresholded hsb image. Is computed during this call.
	 */
	public IplImage getThreshold(CvScalar low, CvScalar high, int blur) {
		if (threshold != null)
			cvReleaseImage(threshold);
		threshold = null;
		threshold = cvCreateImage(cs, 8, 1);
		cvInRangeS(hsbImg, low, high, threshold);
		if (blur > 0) {
			blur = blur + (blur + 1) % 2;
			cvSmooth(threshold, threshold, CV_MEDIAN, blur);
		}
		return threshold;
	}

	/**
	 * @return image with drawings on it.
	 */
	public IplImage getDrawing() {
		return drawing;
	}

	/**
	 * @return image
	 */
	public IplImage getImg() {
		return img;
	}

	/**
	 * @param low
	 * @param high
	 * @return background thresholded between low and high hsb values.
	 */
	public IplImage getBackground(CvScalar low, CvScalar high) {
		if (threshold != null)
			cvReleaseImage(threshold);
		threshold = null;
		threshold = cvCreateImage(cs, IPL_DEPTH_8U, 1);
		cvInRangeS(background, low, high, threshold);
		cvSmooth(threshold, threshold, CV_MEDIAN, Constant.SMALL_BLUR);
		return threshold;
	}

	/**
	 * @return background
	 */
	public IplImage getBackground() {
		return background;
	}

	/**
	 * @return foreground
	 */
	public IplImage getForeground() {
		return foreground;
	}

	public void resetBackground() {
		bg = null;
		bg = new BackgroundSubtractorMOG2();
		cvReleaseImage(background);
		background = cvCreateImage(cs, IPL_DEPTH_8U, 1);
	}
}
