package main;

import java.util.Random;

/**
 * @author Pieter Marsman
 * @Implementation of several states where the expiriment can be in. 
 */
public enum ExperimentState {
	HUMAN_FOURT, HUMAN_HALF, HUMAN_THREE, HUMAN_FULL;
	
	public static ExperimentState randomEnum() {
		ExperimentState[] states = ExperimentState.values();
		int rand = new Random().nextInt(states.length);
		return states[rand];
	}
}
