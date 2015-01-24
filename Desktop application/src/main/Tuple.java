package main;

/**
 * @author Pieter Marsman 
 * @Implementation bind two objects to each other
 * @param <X> type of first object
 * @param <Y> type of second object
 */
public class Tuple<X, Y> {
	public final X x;
	public final Y y;

	public Tuple(X x, Y y) {
		this.x = x;
		this.y = y;
	}
}
