package webcam;

import static com.googlecode.javacv.cpp.opencv_core.cvGetSeqElem;
import static com.googlecode.javacv.cpp.opencv_core.cvPoint;
import static com.googlecode.javacv.cpp.opencv_core.cvClearSeq;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvBoundingRect;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvFindContours;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvApproxPoly;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_RETR_TREE;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_POLY_APPROX_DP;
import main.Constant;
import main.StaticMethods;
import main.Sphero;
import main.Tuple;

import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.cpp.opencv_core.CvContour;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

/**
 * @author Pieter Marsman
 */
public class Ground {

	private CvMemStorage storage = CvMemStorage.create();
	private CvSeq ground = null;
	private CvRect boundingBox = null;
	private CvScalar[] colors;
	private Tuple<CvPoint, Double> closestPoint;
	private double mmPerPix;

	/**
	 * Initializes value for this class
	 */
	public Ground() {
		mmPerPix = -1.0;
		colors = new CvScalar[Constant.GROUND_AVERAGE_OVER];
		for (int i = 0; i < Constant.GROUND_AVERAGE_OVER; i++) {
			colors[i] = null;
		}
	}

	/**
	 * Observation of sphero in image
	 * @param img
	 * @param s
	 */
	public void observe(Image img, Sphero s) {
		for (int i = Constant.GROUND_AVERAGE_OVER - 1; i > 0; i--) {
			this.colors[i] = this.colors[i - 1];
		}
		CvScalar newColor = StaticMethods.averageSurrounding(img, s.getCenter(), s.getRadius());
		this.colors[0] = newColor;
		findGround(img, getColor(), s);
	}

	/**
	 * Finds the borders of the obstacles around sphero
	 * @param img
	 * @param hsb color value for the ground
	 * @param sphero
	 */
	public void findGround(Image img, CvScalar hsb, Sphero sphero) {
		if (img != null && hsb != null) {
			// Get a threshold image for colors: hue, saturation, brightness
			double h = hsb.getVal(0), s = hsb.getVal(1), b = hsb.getVal(2);
			CvScalar low = new CvScalar(Math.max(Constant.COLOR_MIN, h - Constant.GROUND_HUE_SEARCH_RANGE), Math.max(
					Constant.COLOR_MIN, s - Constant.SATURNATION_SEARCH_RANGE), Math.max(Constant.COLOR_MIN, b
					- Constant.BRIGHTNESS_SEARCH_RANGE), 0.0);
			CvScalar high = new CvScalar(Math.min(Constant.COLOR_MAX, h + Constant.GROUND_HUE_SEARCH_RANGE), Math.min(
					Constant.COLOR_MAX, s + Constant.SATURNATION_SEARCH_RANGE), Math.min(Constant.COLOR_MAX, b
					+ Constant.BRIGHTNESS_SEARCH_RANGE), 0.0);
			IplImage threshold = img.getBackground(low, high);
//			cvSaveImage("thres.png", threshold);

			// Find contours in the thresholded image
			ground = new CvContour(null);
			cvFindContours(threshold, storage, ground, Loader.sizeof(CvContour.class), CV_RETR_TREE,
					CV_CHAIN_APPROX_SIMPLE, cvPoint(0, 0));

			// The contour that sphero is in defines the borders of the ground
			surroundingContour(sphero.getCenter());

			// Approximate the border
			if (isSet()) {
				double scale = ground.total() / Constant.GROUND_NUMBER_OF_POINTS;
				ground = cvApproxPoly(ground, Loader.sizeof(CvContour.class), null, CV_POLY_APPROX_DP, scale, 1);
			}
			
			// Create a bounding box to calculate the size of the environment
			if (isSet())
				boundingBox = cvBoundingRect(ground, 0);
			mmPerPx();

			// Define the closest point on the border
			closestPoint = recursiveClosestPoint(sphero.getCenter());

			// Draw the ground observation
			img.drawGroundObservation(ground);

			if (isSet())
				cvClearSeq(ground);
		}
	}

	private void surroundingContour(CvPoint p) {
		// Find the contour that is surrounding Sphero
		while (isSet() && p != null) {
			boolean result = false;
			CvPoint pointI = null, pointJ = null;
			for (int i = 0, j = ground.total() - 1; i < ground.total(); j = i++) {
				pointI = new CvPoint(cvGetSeqElem(ground, i));
				pointJ = new CvPoint(cvGetSeqElem(ground, j));
				if ((pointI.y() > p.y()) != (pointJ.y() > p.y())
						&& (p.x() < (pointJ.x() - pointI.x()) * (p.y() - pointI.y()) / (pointJ.y() - pointI.y())
								+ pointI.x())) {
					result = !result;
				}
			}
			if (result)
				break;
			ground = ground.h_next();
		}
	}
	
	private Tuple<CvPoint, Double> recursiveClosestPoint(CvPoint point) {
		if (isSet()) {
			if (ground.h_next() == null && ground.v_next() == null){
				return closestPointOnPolygon(point);
			}
			else if (ground.h_next() != null && ground.v_next() == null){
				Tuple<CvPoint, Double> best = closestPointOnPolygon(point);
				ground = ground.h_next();
				Tuple<CvPoint, Double> bestRecursive = recursiveClosestPoint(point);
				ground = ground.h_prev();
				if (bestRecursive.y > best.y)
					return best;
				else
					return bestRecursive;
			}
			else if (ground.h_next() == null && ground.v_next() != null) {
				Tuple<CvPoint, Double> best = closestPointOnPolygon(point);
				ground = ground.v_next();
				Tuple<CvPoint, Double> bestRecursive = recursiveClosestPoint(point);
				ground = ground.v_prev();
				if (bestRecursive.y > best.y)
					return best;
				else
					return bestRecursive;
			}
			else if (ground.h_next() != null && ground.v_next() != null) {
				Tuple<CvPoint, Double> best = closestPointOnPolygon(point);
				ground = ground.v_next();
				Tuple<CvPoint, Double> bestRecursiveVert = recursiveClosestPoint(point);
				ground = ground.v_prev();
				ground = ground.h_next();
				Tuple<CvPoint, Double> bestRecursiveHorz = recursiveClosestPoint(point);
				ground = ground.h_prev();
				if (best.y < bestRecursiveVert.y && best.y < bestRecursiveHorz.y)
					return best;
				else if (bestRecursiveVert.y < bestRecursiveHorz.y)
					return bestRecursiveVert;
				else
					return bestRecursiveHorz;
			}
		}
		return null;
	}
	
	/**
	 * Determines the closest point on the ground polygon
	 * @param point
	 * @return
	 */
	private Tuple<CvPoint, Double> closestPointOnPolygon(CvPoint point) {
		double minDist = Double.MAX_VALUE;
		CvPoint minDistPoint = null;
		CvPoint previous = new CvPoint(cvGetSeqElem(ground, ground.total()-1));
		CvPoint p = null;
		for (int i = 0; i < ground.total(); i++) {
			p = new CvPoint(cvGetSeqElem(ground, i));
			if (p.x() != previous.x() || p.y() != previous.y()) {
				CvPoint onLine = StaticMethods.getClosestPointOnSegment(p.x(), p.y(), previous.x(),
						previous.y(), point.x(), point.y());
				double d = StaticMethods.distance(point, onLine);
				if (d < minDist) {
					minDist = d;
					minDistPoint = new CvPoint(onLine);
				}
			}
			previous = p;
		}
		return new Tuple<CvPoint, Double>(minDistPoint, minDist);
	}

	public String toString() {
		return "Obstacle - color: " + colors;
	}

	/**
	 * @return median color of the ground
	 */
	public CvScalar getColor() {
		return StaticMethods.medianColor(colors);
	}

	/**
	 * @return if ground has a proper value
	 */
	private boolean isSet() {
		return ground != null && !ground.isNull();
	}

	/**
	 * @return closest point on the border
	 */
	public Tuple<CvPoint, Double> getClosestPoint() {
		return closestPoint;
	}
	
	/**
	 * @return boundingbox around the surrounding countour of Sphero
	 */
	public CvRect getBoundingBox() {
		return boundingBox;
	}
	
	private void mmPerPx() {
		if (boundingBox != null) {
			double heightInPx = boundingBox.height();
			if (mmPerPix == -1.0)
				mmPerPix = Constant.ENVIRONMENT_HEIGHT / heightInPx;
			else
				mmPerPix = StaticMethods.filter(mmPerPix, Constant.ENVIRONMENT_HEIGHT / heightInPx, Constant.FILTER_MM_PER_PX);
		} else {
			mmPerPix = -1.0;
		}
	}
	
	public double getMmPerPx() {
		return mmPerPix;
	}
}
