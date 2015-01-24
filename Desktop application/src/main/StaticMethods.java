package main;

import static com.googlecode.javacv.cpp.opencv_core.CV_AA;
import static com.googlecode.javacv.cpp.opencv_core.cvCircle;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSeqElem;
import static com.googlecode.javacv.cpp.opencv_core.cvPointFrom32f;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import webcam.Image;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;

/**
 * @author Pieter Marsman 
 * @implementation All kinds of static methods
 */
public class StaticMethods {

	/**
	 * @param c1 point a
	 * @param c2 point b
	 * @return Euclidean distance between a and b
	 */
	public static double distance(CvPoint c1, CvPoint c2) {
		double d1 = Math.abs(c1.x() - c2.x());
		double d2 = Math.abs(c1.y() - c2.y());
		double distance = Math.sqrt(d1 * d1 + d2 * d2);
		return distance;
	}

	/**
	 * @param x1 x-coordinate of point a
	 * @param y1 y-coordinate of point a
	 * @param x2 x-coordinate of point b
	 * @param y2 y-coordinate of point b
	 * @return Euclidean distance between a and b
	 */
	public static double dist(double x1, double y1, double x2, double y2) {
		double d1 = Math.abs(x1 - x2);
		double d2 = Math.abs(y1 - y2);
		double distance = Math.sqrt(d1 * d1 + d2 * d2);
		return distance;
	}

	/**
	 * @param sx1 x-coordinate of ending a of a line segment
	 * @param sy1 y-coordinate of ending a of a line segment
	 * @param sx2 x-coordinate of ending b of a line segment
	 * @param sy2 y-coordinate of ending b of a line segment
	 * @param px x-coordinate of point p
	 * @param py y-coordinate of point p
	 * @return The closest point to point p on line segment a to b
	 */
	public static CvPoint getClosestPointOnSegment(int sx1, int sy1, int sx2, int sy2, int px, int py) {
		double xDelta = sx2 - sx1;
		double yDelta = sy2 - sy1;
		if ((xDelta == 0) && (yDelta == 0))
			throw new IllegalArgumentException("Segment start equals segment end");
		double u = ((px - sx1) * xDelta + (py - sy1) * yDelta) / (xDelta * xDelta + yDelta * yDelta);
		final Point closestPoint;
		if (u < 0)
			closestPoint = new Point(sx1, sy1);
		else if (u > 1)
			closestPoint = new Point(sx2, sy2);
		else
			closestPoint = new Point((int) Math.round(sx1 + u * xDelta), (int) Math.round(sy1 + u * yDelta));
		return new CvPoint(closestPoint.x, closestPoint.y);
	}

	/**
	 * @param c1 point a
	 * @param c2 point b
	 * @return the angle between point a and point b. An angle of zero is to the
	 *         north.
	 */
	public static int angle(CvPoint c1, CvPoint c2) {
		double dx = c1.x() - c2.x();
		double dy = c1.y() - c2.y();
		return ((int) Math.toDegrees(Math.atan2(dy, dx)) + 270) % 360;
	}

	/**
	 * @param p1x x-coordinate of point a
	 * @param p1y y-coordinate of point a
	 * @param p2x x-coordinate of point b
	 * @param p2y y-coordinate of point b
	 * @param precision to convert point to int and back. Should be between 0
	 *            and 1.
	 * @return the angle between point a and point b that are both doubles. An
	 *         angle of zero is to the north.
	 */
	public static int angle(double p1x, double p1y, double p2x, double p2y, double precision) {
		double multiplier = 1 / precision;
		return angle(new CvPoint((int) (p1x * multiplier), (int) (p1y * multiplier)), new CvPoint(
				(int) (p2x * multiplier), (int) (p2y * multiplier)));
	}

	/**
	 * Draws the first circle of the cvSeq onto the image
	 * 
	 * @param img that is drawn on
	 * @param circles Sequence of circles. Only the first one is used.
	 */
	public static void drawCircles(IplImage img, CvSeq circles) {
		for (int i = 0; i < circles.total(); i++) {
			CvPoint3D32f circle = new CvPoint3D32f(cvGetSeqElem(circles, i));
			CvPoint center = cvPointFrom32f(new CvPoint2D32f(circle.x(), circle.y()));
			int radius = Math.round(circle.z());
			cvCircle(img, center, radius, CvScalar.GREEN, 6, CV_AA, 0);
		}
	}

	/**
	 * Determines the median color by mediating over the three seperate values
	 * of the color.
	 * 
	 * @param colors Array of colors
	 * @return new color with mediated values of the values of the colors from
	 *         the array.
	 */
	public static CvScalar medianColor(CvScalar[] colors) {
		List<Double> h = new LinkedList<Double>(), s = new LinkedList<Double>(), b = new LinkedList<Double>();
		for (CvScalar color : colors) {
			if (color != null) {
				h.add(color.getVal(0));
				s.add(color.getVal(1));
				b.add(color.getVal(2));
			}
		}
		Collections.sort(h);
		Collections.sort(s);
		Collections.sort(b);
		int index = h.size() / 2;
		if (index > 0)
			return new CvScalar(h.get(index), s.get(index), b.get(index), 0.0);
		return null;
	}

	/**
	 * @param m Array of doubles
	 * @return median value of the array
	 */
	public static double median(double[] m) {
		Arrays.sort(m);
		int middle = m.length / 2;
		if (m.length % 2 == 1) {
			return m[middle];
		} else {
			return (m[middle - 1] + m[middle]) / 2.0;
		}
	}

	/**
	 * Determines the average surrounding of point center in image img, where
	 * the surrounding is further away than the radius.
	 * 
	 * @param img Image
	 * @param center positions where the surroundings should be calculated of.
	 * @param radius the method looks at least further than the radius
	 * @return the average hsb color around point center
	 */
	public static CvScalar averageSurrounding(Image img, CvPoint center, float radius) {
		if (center != null && radius != 0) {
			int samples = (int) 360 / Constant.SURROUNDING_DEGREES_PER_SAMPLE, index = 0;
			double[] h = new double[samples], s = new double[samples], b = new double[samples];
			for (double i = 0.0; i < 360.0; i += Constant.SURROUNDING_DEGREES_PER_SAMPLE) {
				double rad = Math.toRadians(i);
				double dy = Math.cos(rad) * Constant.SURROUNDING_SAMPLING_SCALE * radius;
				double dx = Math.sin(rad) * Constant.SURROUNDING_SAMPLING_SCALE * radius;
				int x = Math.max(Math.min(center.x() + (int) dx, img.getSize().width() - 1), 0);
				int y = Math.max(Math.min(center.y() + (int) dy, img.getSize().height() - 1), 0);
				CvPoint point = new CvPoint(x, y);
				CvScalar newColor = img.getColor(point, 0, true);
				if (newColor != null) {
					h[index] = newColor.getVal(0);
					s[index] = newColor.getVal(1);
					b[index] = newColor.getVal(2);
					index++;
				}
			}
			return new CvScalar(StaticMethods.median(h), StaticMethods.median(s), StaticMethods.median(b), 0.0);
		} else {
			return null;
		}
	}

	/**
	 * @param v1 first value
	 * @param v2 second value
	 * @param filter value that is applied to the first value. Should be between
	 *            0 and 1.
	 * @return v1 * filter + v2 * (1 - filter)
	 */
	public static double filter(double v1, double v2, double filter) {
		return v1 * filter + v2 * (1 - filter);
	}

	/**
	 * @param angle1
	 * @param angle2
	 * @return difference between angles
	 */
	public static int angleDifference(int angle1, int angle2) {
		int maxAngle = Math.max(angle1, angle2);
		int minAngle = Math.min(angle1, angle2);
		int diff = maxAngle - minAngle;
		if (diff > 180)
			return 360 - diff;
		else
			return diff;
	}

	/**
	 * @param s String value
	 * @param t Should be a natural number
	 * @return t Times concatenated string s
	 */
	public static String concatenationRepeat(String s, int t) {
		String r = "";
		for (int i = 0; i < t; i++)
			r += s;
		return r;
	}

}
