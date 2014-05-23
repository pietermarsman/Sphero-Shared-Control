package main;

import java.util.Date;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;

/**
 * @author Pieter Marsman
 * @Representation of a Sphero ball that is observerd multiple times
 */
public class Sphero {

	private double velocity, startTime;
	private int direction;
	private CvPoint[] center;
	private float radius;
	private long[] time;
	private CvScalar color;
	private double traveledDistance;

	/**
	 * Initiates Sphero with all values set to default
	 */
	public Sphero() {
		reset();
	}

	/**
	 * Initiates Sphero with a
	 * 
	 * @param center
	 *            ,
	 * @param radius
	 *            and
	 * @param colour
	 */
	public Sphero(CvPoint center, float radius, CvScalar colour) {
		this();
		this.center[0] = center;
		this.radius = radius;
		this.color = colour;
		this.time[0] = new Date().getTime();
		this.startTime = this.time[0];
	}

	/**
	 * Sets all values to default
	 */
	public void reset() {
		center = new CvPoint[Constant.SPHERO_AVERAGE_OVER];
		time = new long[Constant.SPHERO_AVERAGE_OVER];
		for (int i = 0; i < Constant.SPHERO_AVERAGE_OVER; i++) {
			center[i] = null;
			time[i] = 0;
		}
		radius = 0;
		color = null;
	}

	/**
	 * Used to update the values with a new observation.
	 * 
	 * @param newCenter
	 *            of Sphero, the last Constant.SPHERO_AVERAGE_OVER values are
	 *            remembered
	 * @param newRadius
	 *            of Sphero, this is filtered with value
	 *            Constant.SPHERO_RADIUS_FILTER
	 * @param newColor
	 *            of Sphero, this is filtered with value
	 *            Constant.SPHERO_COLOR_FILTER
	 */
	public void observe(CvPoint newCenter, float newRadius, CvScalar newColor, double mmPerPx) {
		for (int i = Constant.SPHERO_AVERAGE_OVER - 1; i > 0; i--) {
			this.center[i] = this.center[i - 1];
			this.time[i] = this.time[i - 1];
		}
		this.center[0] = new CvPoint(newCenter.x(), newCenter.y());
		if (radius < 1.0)
			radius = newRadius;
		else
			radius = (float) StaticMethods.filter(radius, newRadius, Constant.SPHERO_RADIUS_FILTER);
		if (color == null)
			this.color = newColor;
		else
			for (int i = 0; i < 3; i++) {
				double value = color.getVal(i) * (1 - Constant.SPHERO_COLOR_FILTER) + newColor.getVal(i) * Constant.SPHERO_COLOR_FILTER;
				color.setVal(i, value);
			}
		this.time[0] = new Date().getTime();
		updateVelocity(mmPerPx);
		updateDirection();
		traveledDistance += traveledDistance(mmPerPx);
	}

	/**
	 * @return distance between current and previous location
	 */
	private double traveledDistance(double mmPerPx) {
		if (Constant.SPHERO_AVERAGE_OVER > 1 && this.center[0] != null && this.center[1] != null && mmPerPx > 0.0) {
			return StaticMethods.distance(this.center[0], this.center[1]) * mmPerPx;
		} else
			return 0.0;
	}

	/**
	 * Updates the direction of Sphero, based on the previous observations
	 */
	private void updateDirection() {
		if (center[Constant.SPHERO_AVERAGE_OVER - 1] != null) {
			direction = StaticMethods.angle(center[Constant.SPHERO_AVERAGE_OVER - 1], center[0]);
		}
	}

	/**
	 * Updates the velocity in cm per sec of Sphero, based on the previous
	 * observations
	 */
	private void updateVelocity(double mmPerPx) {
		if (center[Constant.SPHERO_AVERAGE_OVER - 1] != null) {
			double timeElapsed = (time[0] - time[Constant.SPHERO_AVERAGE_OVER - 1]) / 1000.0;
			double distance = StaticMethods.distance(center[0], center[Constant.SPHERO_AVERAGE_OVER - 1]);
			double distanceCentimeters = distance * mmPerPx / 10.0;
			double currentSpeed = distanceCentimeters / timeElapsed;
			velocity = StaticMethods.filter(velocity, currentSpeed, Constant.SPHERO_VELOCITY_FILTER);
		}
	}

	/**
	 * @return if Sphero is ever observed since last reset.
	 */
	public boolean isObserved() {
		return time[0] != 0;
	}

	/**
	 * @return the time since the last observation.
	 */
	public long notSeenFor() {
		return new Date().getTime() - time[0];
	}

	@Override
	public String toString() {
		return "Sphero - pos: " + getCenter() + ", radius: " + getRadius() + ", color: " + getColor() + ", time: " + getTime();
	}

	/**
	 * @return the time since the last reset
	 */
	public long getTime() {
		return (long) (new Date().getTime() - startTime);
	}

	/**
	 * @return last observed center of Sphero
	 */
	public CvPoint getCenter() {
		return center[0];
	}

	/**
	 * @return filtered radius of Sphero
	 */
	public float getRadius() {
		return radius;
	}

	/**
	 * @return filtered color of Sphero
	 */
	public CvScalar getColor() {
		return color;
	}

	/**
	 * @return direction of Sphero based on the last observations
	 */
	public int getDirection() {
		return direction;
	}

	/**
	 * @return velocity of Sphero in cm / sec
	 */
	public double getVelocity() {
		return velocity;
	}

	/**
	 * @return distance that Sphero has traveled since the last time
	 *         restartTraveledDistance() has been called
	 */
	public double getTraveledDistance() {
		return traveledDistance;
	}
	
	public double getTraveledSpeed(double mmPerPx) {
		if (Constant.SPHERO_AVERAGE_OVER > 1 && this.center[0] != null && this.center[1] != null && mmPerPx > 0.0) {
			return (StaticMethods.distance(this.center[0], this.center[1]) * mmPerPx / 10.0) / ((this.time[0] - this.time[1]) / 1000.0);
		} else
			return 0.0;
	}

	/**
	 * Restart the travel distance counter
	 */
	public void restartTraveledDistance() {
		traveledDistance = 0.0;
	}
}
