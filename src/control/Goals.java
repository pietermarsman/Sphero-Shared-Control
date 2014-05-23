package control;

import static com.googlecode.javacv.cpp.opencv_core.cvGetSeqElem;
import static com.googlecode.javacv.cpp.opencv_core.cvPointFrom32f;
import static com.googlecode.javacv.cpp.opencv_core.cvReleaseMemStorage;
import static com.googlecode.javacv.cpp.opencv_highgui.cvSaveImage;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_HOUGH_GRADIENT;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvHoughCircles;

import java.awt.Point;
import java.util.LinkedList;
import java.util.List;

import output.ScreenCanvasFrame;

import main.Constant;
import main.StaticMethods;
import main.Sphero;

import webcam.Image;
import webcam.InputStream;

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
public class Goals {

	private List<Goal> locations;
	private int last;
	private Goal current, previous;

	/**
	 * Initializes the list of goals with references to the
	 * @param is InputStream and
	 * @param scf canvas frame
	 */
	public Goals(InputStream is, ScreenCanvasFrame scf) {
		this();
	}

	/**
	 * Initializes the list of goals as an empty list
	 */
	public Goals() {
		locations = new LinkedList<Goal>();
		last = -1;
		current = null;
		previous = null;
	}

	/**
	 * Add a goal with x- and y-coordinate. The last goal that is added is the current goal.
	 * @param x
	 * @param y
	 */
	public void addGoal(int x, int y) {
		locations.add(new Goal(x, y, locations.size()));
		last = locations.size() - 1;
	}

	/**
	 * @return get the current goal
	 */
	public Goal getGoal() {
		if (last >= 0) {
			int next = (last + 1) % locations.size();
			if (locations.get(next) != current) {
				previous = current;
				current = locations.get(next);
			}
			return current;
		} else
			return null;
	}

	/**
	 * @return get center of current goal.
	 */
	public CvPoint getCenter() {
		if (getGoal() != null)
			return new CvPoint((int) getGoal().getPoint().x(), (int) getGoal().getPoint().y());
		else
			return null;
	}

	/**
	 * @return get a list of all goals.
	 */
	public List<Goal> getAllGoals() {
		return locations;
	}

	public boolean checkGoalLocation(CvPoint loc) {
		boolean result = false;
		if (!locations.isEmpty()) {
			CvPoint3D32f circle = getGoal().getPoint();
			CvPoint center = cvPointFrom32f(new CvPoint2D32f(circle.x(), circle.y()));
			float radius = circle.z();
			if (StaticMethods.distance(loc, center) < radius) {
				result = true;
				last++;
			}
			return result;
		} else
			return false;
	}

	/**
	 * @param sphero
	 * @return if sphero is in the radius of the current goal.
	 */
	public boolean updateGoal(Sphero sphero) {
		if (sphero.getCenter() != null)
			return checkGoalLocation(sphero.getCenter());
		return false;
	}

	/**
	 * @return if there are already goals set
	 */
	public boolean isSet() {
		return last != -1;
	}

	/**
	 * @return get the last goal. Handy for looking on which path you are.
	 */
	public Goal getPrevious() {
		return previous;
	}

	public void resetPrevious() {
		previous = null;
	}
}
