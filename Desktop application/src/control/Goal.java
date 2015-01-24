package control;

import com.googlecode.javacv.cpp.opencv_core.CvPoint3D32f;

import main.Constant;

/**
 * @author Pieter Marsman
 */
public class Goal {

	private int x, y, radius;
	private String name;
	
	/**
	 * Initializes a goal with values of
	 * @param x
	 * @param y
	 * @param number of goal
	 * @param radius
	 */
	public Goal(int x, int y, int number, int radius) {
		this.x = x;
		this.y = y;
		this.radius = radius;
		name = String.valueOf((char) ('A' + number));
	}
	
	/**
	 * Initializes a goal with values of
	 * @param x
	 * @param y
	 * @param number
	 */
	public Goal(int x, int y, int number) {
		this(x, y, number, Constant.GOAL_RADIUS);
	}
	
	/**
	 * @return the center and radius of the goal
	 */
	public CvPoint3D32f getPoint() {
		return new CvPoint3D32f(x, y, radius);
	}

	/**
	 * @return x-coordinate of the goal
	 */
	public int getX() {
		return x;
	}

	/**
	 * @return y-coordinate of the goal
	 */
	public int getY() {
		return y;
	}

	/**
	 * @return radius of the goal
	 */
	public int getRadius() {
		return radius;
	}

	/**
	 * @return name of the goal
	 */
	public String getName() {
		return name;
	}
}
