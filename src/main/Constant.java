package main;

import java.util.Locale;

/**
 * @author Pieter Marsman
 * @All kinds of constants that can be used during
 *         expiriments with Sphero, the detection of Sphero and interaction
 *         through bluetooth.
 */
public class Constant {

	public static final int // Bluring
			SMALL_BLUR = 5,

			// Averaging
			SPHERO_AVERAGE_OVER = 3, GROUND_AVERAGE_OVER = 200,

			// Color threshold
			HUE_SEARCH_RANGE = 30, GROUND_HUE_SEARCH_RANGE = 80,
			SATURNATION_SEARCH_RANGE = 75,
			BRIGHTNESS_SEARCH_RANGE = 75,

			// Image
			CANNY_LOWER_BOUND = 50, CANNY_UPPER_BOUND = 150, CANNY_KERNEL_SIZE = 3,

			// Contour approximation
			GROUND_NUMBER_OF_POINTS = 100,

			// Detection
			NOT_SEEN_FOR = 500,

			// Samling surrounding
			SURROUNDING_DEGREES_PER_SAMPLE = 72,

			// Goals
			GOAL_RADIUS = 20,

			// Calibration
			CALIBRATION_SLEEP = 1500,

			// Control
			CONTROL_AVOID_ANGLE = 45, CONTROL_AVOID_DURATION = 300,
			
			// Experiment
			PATHS_CHANGE_STATE = 4;

	public static final double
	// Color threshold
			COLOR_MAX = 255,
			COLOR_MIN = 0,
			
			// Environment specs
			ENVIRONMENT_HEIGHT = 1168.0, // in mm
			FILTER_MM_PER_PX = 0.95,
			
			// Sphero specs
			SPHERO_RADIUS_MM = 7.4 * 10,

			// Control Brooks implementation
			CONTROL_AVOID_TIME = 0.3,
			CONTROL_AVOID_STOP_SPEED = 20.0,
			CONTROL_SPEED_SLOPE = 0.1,
			CONTROL_SPEED_START = 1.0,
			CONTROL_IDEAL_SPEED = 40.0,
			FILTER_SPEED_DEVIATION = 0.95,
			REAL_SPEED_LOW = 10.0,
			NEAR_WALL = 5.0,

			// Averaging
			SPHERO_RADIUS_FILTER = 0.95,
			SPHERO_COLOR_FILTER = 0.1,
			SPHERO_VELOCITY_FILTER = 0.75,
			SURROUNDING_SAMPLING_SCALE = 1.8,

			// Circle detection
			CIRCLE_SEARCH_RANGE = 1.1, CIRCLE_CANNY_PARAMETER = 100.0,
			CIRCLE_DETECTION_THRESHOLD = 20.0,
			CIRCLE_NEAR = 1.0,

			// Urdiales
			URDIALES_CONSTANT_SMOOTHNESS = 1.0 / 180.0,
			URDIALES_CONSTANT_DIRECTION = 1.0 / 180.0, URDIALES_CONSTANT_SAFETY = 1.0 / 180.0,
			FILTER_COMMAND_VELOCITY = 0.95,

			// Image
			IMAGE_BACKGROUND_SUBTRACTION_FILTER = 0.005;

	public static final float MAX_SPEED = 1.0f, HUMAN_MAX_SPEED = 0.2f, CALIBRATION_SPEED = 0.3f, HUMAN_START = 0.15f;

	public static final String CSV_SEPARATOR = ";";

	public static final Locale LOCATION = Locale.GERMAN;
}
