package main;

import java.io.IOException;
import java.util.Date;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import output.DataLogger;
import output.Log;
import output.ScreenCanvasFrame;
import output.ScreenListener;
import output.VideoWriter;

import com.googlecode.javacv.cpp.opencv_core.IplImage;
import control.BrooksControl;
import control.Command;
import control.Goals;
import control.HumanControl;
import control.SpheroConnection;
import webcam.Ground;
import webcam.Image;
import webcam.InputStream;
import webcam.Interpreter;
import webcam.WebcamLaptop;
import webcam.WebcamVideoFile;

/**
 * @author Pieter Marsman
 */
public class SpheroExperiment extends Thread {

	public static Logger Log = Logger.getLogger("global");//getGlobal() doesn't work

	private InputStream is;
	private Sphero sphero;
	private Ground ground;
	private SpheroConnection spheroConn;
	private HumanControl human;
	private ScreenCanvasFrame scf;
	private ScreenListener sl;
	private Goals goals;
	private BrooksControl brooks;
	private Urdiales urdiales;
	private Interpreter interpreter;
	private Image image;
	private Log logger;
	private DataLogger dataLogger;
	private VideoWriter videoWriter;
	private boolean run, paused, humanStart;
	private int pathCounter;
	private long startTime;
	private ExperimentState state;

	public static void main(String[] args) {
		SpheroExperiment test = new SpheroExperiment();
	}

	public SpheroExperiment() {
		run = true;
		paused = false;
		humanStart = false;
		// Create logger for debugging
		logger = new Log(Log);
		// Create logger for extracting data
		dataLogger = new DataLogger();
		// Starts a thread that initialize the webcam
		is = new WebcamVideoFile();
		// Initialize all variables that are used to follow sphero
		sphero = new Sphero();
		// Initialize all variables that are used to detect the ground
		ground = new Ground();
		// Connects to the bluetooth device sphero
		spheroConn = new SpheroConnection();
		// // Set up a bluetooth connection to a device for steering sphero and
		// waits for signals to be send (thread)
		human = new HumanControl(this);
		// Set a new goal to reach
		goals = new Goals();
		// Initialize a implementation of the theory of urdiales
		urdiales = new Urdiales(sphero, goals, ground);
		// Initialize a implementation of Brooks subsumtion layer
		brooks = new BrooksControl(sphero, ground, goals, urdiales);
		// Initialize the interpreter for images
		interpreter = new Interpreter(sphero);
		// Load a new image
		image = new Image(is.nextImage());
		// Create a video recorder
		videoWriter = new VideoWriter(image, goals);
		// Initialize the screen listener by loading all the references
		sl = new ScreenListener(is, spheroConn, human, interpreter, sphero, this, goals, image, ground, dataLogger,
				videoWriter);
		// Opens a screen to display the webcam feed
		scf = new ScreenCanvasFrame(sphero, ground, sl, brooks, goals, image, this);
		// Also add the View to the listener
		sl.setView(scf);
		// Human should be in control in the first path
		state = ExperimentState.HUMAN_FULL;
		// Refresh the shown image in the View
		scf.refreshImage();
	}

	@Override
	public void run() {
		pathCounter = 0;
		startTime = new Date().getTime();
		// Sphero is controlled into eternity
		while (run) {
			Log.finer("New loop");
			// Set commands to default null
			Command scCommand = null, hcCommand = null, sharedCommand = null;
			// Get new image from the webcam stream
			IplImage img = is.nextImage();
			if (img != null) {
				// Apply the wrapper around IplImage and process the image
				// into various other images
				image.newImage(img, !paused);
				// Look for sphero in the image
				interpreter.location(image, ground.getMmPerPx());
				// Look for the border of the obstacles near sphero
				ground.observe(image, sphero);
				// Only send command if the process is not paused.
				if (!paused) {
					// Get commands from different actors and send it to Sphero
					hcCommand = human.getCommand();
					humanStart = humanStart || (hcCommand.getVelocity() > Constant.HUMAN_START);
					System.out.println(hcCommand.getVelocity());
					if (humanStart) {
					scCommand = brooks.getCommand();
					sharedCommand = urdiales.getCommand(scCommand, hcCommand, state);
					// If the goal is reached certain actions should follow
					boolean goalReached = goals.updateGoal(sphero);
					if (goalReached)
						goalReached();
					sendCommand(hcCommand, scCommand, sharedCommand);
					// Save the image to a video file for later validation
					videoWriter.record(image.getImg());
					// Log all information into a csv file
					dataLogger.log(urdiales, hcCommand, scCommand, sharedCommand, goalReached, goals, state, sphero,
							startTime);
					} else {
						sendCommand(null, null, null);
					}
				} else {
					sendCommand(null, null, null);
				}
				// Refresh the image that is displayed to the expiriment
				// leader
				scf.refreshImage(hcCommand, scCommand, sharedCommand);
				// Release the image
				image.release();
			}
		}
	}

	/**
	 * @param start time to compare to current time
	 * @param name thing that happens between now and the last time timer() was
	 *            called
	 * @return the current time
	 */
	private long timer(long start, String name) {
		long now = new Date().getTime();
		long time = now - start;
		System.out.println(name + ": " + time);
		return now;
	}

	/**
	 * Alerts the human device, datalogger, videowriter that a goal has been
	 * reached. And changes the state to a random new one.
	 */
	private void goalReached() {
		dataLogger.goalReached();
		pathCounter++;
		human.goalReachedMessage();
		sphero.restartTraveledDistance();
		videoWriter.newPath();
		if (pathCounter >= Constant.PATHS_CHANGE_STATE) {
			pause();
			human.ratingNeededMessage();
			state = ExperimentState.randomEnum();
			pathCounter = 0;
			goals.resetPrevious();
			humanStart = false;
		}
	}

	private void sendCommand(Command hcCommand, Command scCommand, Command sharedCommand) {
		if (spheroConn.ready()) {
			if (state != ExperimentState.HUMAN_FULL ) // && state != ExperimentState.HUMAN_NONE)
				spheroConn.sendCommand(sharedCommand);
			else if (state == ExperimentState.HUMAN_FULL)
				spheroConn.sendCommand(hcCommand);
//			else if (state == ExperimentState.HUMAN_NONE)
//				spheroConn.sendCommand(scCommand);
		}
	}

	/**
	 * Stops this thread by exiting the while statement
	 */
	public void exit() {
		run = false;
	}

	/**
	 * Pause this thread
	 */
	public void pause() {
		paused = true;
		try {
			// To make sure image is ready with processing
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (spheroConn.ready())
			spheroConn.stopMoving();
	}

	/**
	 * Unpause this thread
	 */
	public void unPause(boolean resetBackground) {
		is.flush();
		if (resetBackground)
			image.resetBackground();
		paused = false;
	}

	public void setExperimentState(ExperimentState state) {
		this.state = state;
	}

	public ExperimentState getExperimentState() {
		return this.state;
	}

	public void ratingGiven(int rating) {
		dataLogger.logRating(rating);
		unPause(false);
	}

	public long getStartTime() {
		return startTime;
	}
}
