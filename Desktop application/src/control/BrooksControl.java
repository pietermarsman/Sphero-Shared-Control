package control;

import java.util.Date;

import main.Constant;
import main.StaticMethods;
import main.Sphero;
import main.Tuple;
import main.Urdiales;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import webcam.Ground;

/**
 * @author Pieter Marsman
 */
public class BrooksControl implements SpheroController {

	private Urdiales urdiales;
	private Sphero sphero;
	private Ground ground;
	private Goals goals;
	private CvPoint closestObstacle;
	private float filteredVelocity;
	private long latestAvoidCommand, latestStopCommand, latestStuckCommand;
	private double speedFilter;

	/**
	 * Initialize this class with references to. This is an implementation of
	 * Brooks paradigm stated in
	 * "A Robust Layered Control System For A Mobile Robot" (1986).
	 * 
	 * @param sphero
	 * @param ground
	 * @param goals
	 */
	public BrooksControl(Sphero sphero, Ground ground, Goals goals, Urdiales urdiales) {
		this.sphero = sphero;
		this.ground = ground;
		this.goals = goals;
		this.closestObstacle = null;
		this.filteredVelocity = 0.0f;
		this.urdiales = urdiales;
		latestAvoidCommand = 0;
		latestStopCommand = 0;
		latestStuckCommand = 0;
		speedFilter = 0.0;
	}

	@Override
	public Command getCommand() {
		speedFilter = StaticMethods.filter(speedFilter, sphero.getTraveledSpeed(ground.getMmPerPx()), Constant.FILTER_SPEED_DEVIATION);
		if (sphero.getCenter() != null) {
			Command stuck = stuck();
			Command avoid = avoidObstacles();
			Command wander = wanderTo();
			if (stuck != null) {
				return stuck;
			}
			if (avoid != null) {
				return avoid;
			}
			else if (wander != null) {
				return wander;
			}
		}
		// more layers can be added
		return new Command(0, 0, "stop");
	}

	// layer -1
	private Command stuck() {
		// If the real speed is low for a while and the commands are high 
		Tuple<CvPoint, Double> tuple = ground.getClosestPoint();
		boolean nearWall = (tuple.y - sphero.getRadius()) * ground.getMmPerPx() < Constant.NEAR_WALL;
		boolean realSpeedLow = speedFilter < Constant.REAL_SPEED_LOW;
		boolean commandSpeedHigh = urdiales.getVelocityCommandFilter() > 0.1;
		int angle = StaticMethods.angle(sphero.getCenter(), tuple.x);
		int avoidAngle = (angle + 180) % 360;
		if (nearWall && realSpeedLow && commandSpeedHigh) {
			latestStuckCommand = new Date().getTime();
			return new Command(Constant.MAX_SPEED / 6.0, avoidAngle, "Stuck");
		}
		if (new Date().getTime() - latestStopCommand < Constant.CONTROL_AVOID_DURATION) {
			return new Command(Constant.MAX_SPEED / 6.0, avoidAngle, "Stuck");
		}
		return null;
	}
	
	// layer 0
	/**
	 * Uses the speed and radius of sphero to forsee collisions with the closest
	 * obstacle. If the obstacle can be reached within a second with the current
	 * speed and the driving angle is heading to the obstacle an avoid command
	 * is triggered.
	 * 
	 * @return a command that avoids obstacles if nessacary
	 */
	private Command avoidObstacles() {
		CvPoint spheroCenter = sphero.getCenter();
		if (spheroCenter != null) {
			Tuple<CvPoint, Double> tuple = ground.getClosestPoint();
			if (tuple != null && tuple.x != null && tuple.y != null) {
				CvPoint minDistPoint = tuple.x;
				// Compute distance in centimeters
				double minDist = Math.max(0.0, (tuple.y - sphero.getRadius()) * ground.getMmPerPx() / 10.0);
				// Compute time to collision
				double secToCollision = minDist / sphero.getVelocity();
				closestObstacle = new CvPoint(minDistPoint);
				int angle = StaticMethods.angle(spheroCenter, minDistPoint);
				int avoidAngle = (angle + 180) % 360;
				// If Sphero is going in less than constant seconds
				if (secToCollision < Constant.CONTROL_AVOID_TIME
						&& StaticMethods.angleDifference(avoidAngle, sphero.getDirection()) < Constant.CONTROL_AVOID_ANGLE) {
					if (sphero.getVelocity() > Constant.CONTROL_AVOID_STOP_SPEED) {
						latestStopCommand = new Date().getTime();
						return new Command(0, 0, "Stop");
					}
					else {
						latestAvoidCommand = new Date().getTime();
						return new Command(Constant.MAX_SPEED / 4.0, avoidAngle, "Avoid");
					}
				}
				if (new Date().getTime() - latestStopCommand < Constant.CONTROL_AVOID_DURATION) {
					return new Command(0, 0, "Stop");
				}
				if (new Date().getTime() - latestAvoidCommand < Constant.CONTROL_AVOID_DURATION) {
					return new Command(Constant.MAX_SPEED / 4.0, avoidAngle, "Avoid");
				}
			}
		}
		return null;
	}

	// layer 1
	private Command wanderTo() {
		// Only if the next goal is known
		CvPoint goalCenter = goals.getCenter();
		if (goalCenter != null) {
			int angle = StaticMethods.angle(sphero.getCenter(), goalCenter);
			double dist = StaticMethods.distance(sphero.getCenter(), goalCenter);
			// Compute distance in centimeters
			double relativeDist = dist * ground.getMmPerPx() / 10.0;
			// Compute time to arive at destination
			double secToDestination = relativeDist / sphero.getVelocity();
			// Difference between ideal speed and current speed
			double speedDifference = sphero.getVelocity() - Constant.CONTROL_IDEAL_SPEED;
			double angleDifference = StaticMethods.angleDifference(angle, sphero.getDirection());
			float speedDistance = (float) (Constant.MAX_SPEED * (1 - Math.pow(Math.E, -Constant.CONTROL_SPEED_SLOPE
					* (Constant.CONTROL_SPEED_START + secToDestination))));
			float speedIdeal = (float) (Constant.MAX_SPEED * Math.min(1.0f,
					Math.pow(Math.E, -1 / Constant.CONTROL_IDEAL_SPEED * (speedDifference + Constant.CONTROL_IDEAL_SPEED))));
			float speedTurn = (float) Math.max((Constant.MAX_SPEED * (180.0f - angleDifference) / 180.0f), 0.1);
			float speed = Math.min(Math.min(speedDistance, speedIdeal), speedTurn);
			filteredVelocity = (float) Math.max(Math.min(StaticMethods.filter(filteredVelocity, speed, 0.75), 1.0), 0.0);
			return new Command(filteredVelocity, angle, "Wander");
		} else
			return null;
	}

	/**
	 * @return closest obstacle that brooks is aware of
	 */
	public CvPoint getClosestObstacle() {
		return closestObstacle;
	}
}
