package main;

import java.util.logging.Level;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;

import webcam.Ground;
import control.Command;
import control.Goals;

/**
 * @author Pieter Marsman
 * @Implementation of the theory of Urdiales et al. that is described in their
 *                 paper "A new multi-criteria optimization strategy for shared
 *                 control in wheelchair assisted navigation" (21 october 2010)
 */
public class Urdiales {

	private Sphero sphero;
	private Goals goals;
	private Ground ground;
	private double velocityFilter;

	/**
	 * Initialize the class with a reference to:
	 * 
	 * @param sphero
	 *            ,
	 * @param goals
	 *            and the
	 * @param ground
	 */
	public Urdiales(Sphero sphero, Goals goals, Ground ground) {
		this.sphero = sphero;
		this.goals = goals;
		this.ground = ground;
		this.velocityFilter = 0.0;
	}

	/**
	 * Get a weighted command between the commands of the:
	 * 
	 * @param human
	 *            and the
	 * @param computer
	 * @return efficiency weighted command
	 */
	public Command getCommand(Command human, Command computer,
			ExperimentState state) {
		double humanInfluence = 1.0;
//		if (state == ExperimentState.HUMAN_NONE)
//			humanInfluence = 0.0;
		if (state == ExperimentState.HUMAN_FOURT)
			humanInfluence = 0.25;
		else if (state == ExperimentState.HUMAN_THREE)
			humanInfluence = 0.75;
		else if (state == ExperimentState.HUMAN_FULL)
			humanInfluence = 1.0;
		else
			humanInfluence = 0.5;
		if (human != null && computer != null) {
			double weightHuman = humanInfluence
					* (smoothness(human) + direction(human) + safety(human))
					/ 3;
			double weightComputer = (1.0 - humanInfluence)
					* (smoothness(computer) + direction(computer) + safety(computer))
					/ 3;
			Command c1 = new Command(human.getVelocity() * weightHuman,
					human.getDirection());
			Command c2 = new Command(computer.getVelocity() * weightComputer,
					computer.getDirection());
			Command result = Command.sum(c1, c2);
			velocityFilter = StaticMethods.filter(velocityFilter, result.getVelocity(), Constant.FILTER_COMMAND_VELOCITY);
			return result;
		} else {
			velocityFilter = StaticMethods.filter(velocityFilter, 0.0, Constant.FILTER_COMMAND_VELOCITY);
			return new Command(0, 0, "null");
		}
	}

	/**
	 * @param c
	 *            Command
	 * @return smoothness of command compared to the current direction of sphero
	 */
	public double smoothness(Command c) {
		if (sphero != null) {
			double difference = StaticMethods.angleDifference(
					sphero.getDirection(), c.getDirection());
			double smoothness = Math.pow(Math.E,
					-Constant.URDIALES_CONSTANT_SMOOTHNESS * difference);
			return smoothness;
		} else {
			return 0.0;
		}
	}

	/**
	 * @param c
	 *            Command
	 * @return direction of command compared to the goal location
	 */
	public double direction(Command c) {
		if (sphero != null && goals.isSet()) {
			int angleSpheroGoal = StaticMethods.angle(sphero.getCenter(),
					goals.getCenter());
			double difference = (double) StaticMethods.angleDifference(
					c.getDirection(), angleSpheroGoal);
			double direction = Math.pow(Math.E,
					-Constant.URDIALES_CONSTANT_DIRECTION * difference);
			return direction;
		} else {
			return 0.0;
		}
	}

	// TODO why is minDist so often null??
	/**
	 * @param c
	 *            Command
	 * @return safety of command compared to the nearest obstacle
	 */
	public double safety(Command c) {
		Tuple<CvPoint, Double> minDist = ground.getClosestPoint();
		if (sphero != null && minDist != null && sphero.getCenter() != null
				&& minDist.x != null) {
			double angleSpheroBorder = StaticMethods.angle(sphero.getCenter(),
					minDist.x);
			double difference = Math.abs(c.getDirection() - angleSpheroBorder);
			double safety = 1 - Math.pow(Math.E,
					-Constant.URDIALES_CONSTANT_SAFETY * difference);
			return safety;
		} else {
			main.SpheroExperiment.Log.log(Level.WARNING, "minDist is null");
			return 0.0;
		}
	}
	
	public double getVelocityCommandFilter(){
		return velocityFilter;
	}
}
