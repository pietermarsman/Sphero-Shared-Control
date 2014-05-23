package control;

import java.awt.Point;

/**
 * @author Pieter Marsman
 */
public class Command {

	private double velocity;
	private int direction;
	private String message;

	/**
	 * Initializes command for Sphero with values of 
	 * @param velocity
	 * @param direction
	 */
	public Command(double velocity, int direction) {
		this.velocity = velocity;
		this.direction = direction;
		this.message = "";
	}

	/**
	 * Initializes command for Sphero with values of 
	 * @param velocity
	 * @param direction
	 * @param message
	 */
	public Command(double velocity, int direction, String message) {
		this(velocity, direction);
		this.message = message;
	}

	/**
	 * @return velocity of command
	 */
	public double getVelocity() {
		return velocity;
	}

	/**
	 * @return direction of command
	 */
	public int getDirection() {
		return direction;
	}

	public String toString() {
		return message + ": " + velocity + ", " + direction + "�";
	}

	/**
	 * @return x value of the command vector
	 */
	public double getX() {
		if (direction < 90)
			return Math.sin(Math.toRadians(direction)) * velocity;
		else if (direction < 180)
			return Math.cos(Math.toRadians(direction - 90)) * velocity;
		else if (direction < 270)
			return -Math.sin(Math.toRadians(direction - 180)) * velocity;
		else
			return -Math.cos(Math.toRadians(direction - 270)) * velocity;
	}

	/**
	 * @return y value of the command vector
	 */
	public double getY() {
		if (direction < 90)
			return -Math.cos(Math.toRadians(direction)) * velocity;
		else if (direction < 180)
			return Math.sin(Math.toRadians(direction - 90)) * velocity;
		else if (direction < 270)
			return Math.cos(Math.toRadians(direction - 180)) * velocity;
		else
			return -Math.sin(Math.toRadians(direction - 270)) * velocity;
	}

	/**
	 * @param x
	 * @param y
	 * @return Command generated from the x and y coordinate for vectors
	 */
	public static Command fromCartesian(double x, double y) {
		double velocity = Math.sqrt(x * x + y * y);
		int direction = (int) (-Math.toDegrees(Math.atan2(y, x)) + 450) % 360;
		return new Command(velocity, direction);
	}

	/**
	 * @param c1
	 * @param c2
	 * @return Average between two commands
	 */
	public static Command sum(Command c1, Command c2) {
		double x = c1.getX() + c2.getX();
		double y = c1.getY() + c2.getY();
		return Command.fromCartesian(x, y);
	}
}
