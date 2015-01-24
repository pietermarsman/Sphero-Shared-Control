package main;

/**
 * @author Pieter Marsman
 * @Particular observation of a circle
 */
public class CircleObservation {

	public float x, y, radius;

	public CircleObservation(float x, float y, float radius) {
		this.x = x;
		this.y = y;
		this.radius = radius;
	}
	
	public String toString() {
		return x + ", " + y + ", " + radius;
	}
}
