package output;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.logging.Level;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import main.ExperimentState;
import main.StaticMethods;
import main.Sphero;
import main.SpheroExperiment;

import control.Command;
import control.Goals;
import control.HumanControl;
import control.SpheroConnection;

import webcam.Ground;
import webcam.Image;
import webcam.InputStream;
import webcam.Interpreter;

/**
 * @author Pieter Marsman
 */
public class ScreenListener implements ChangeListener, ActionListener, MouseListener, ItemListener {

	private final String goalState = "Click on goals";
	private InputStream is;
	private SpheroConnection spheroConn;
	private HumanControl human;
	private ScreenCanvasFrame scf;
	private Interpreter interpreter;
	private Sphero sphero;
	private SpheroExperiment mainClass;
	private Goals goals;
	private Image image;
	private Ground ground;
	private DataLogger dataLogger;
	private VideoWriter vw;

	/**
	 * Initializes the listener with a lot of references
	 * 
	 * @param is
	 * @param spheroConn
	 * @param human
	 * @param interpreter
	 * @param sphero
	 * @param main
	 * @param goals
	 * @param image
	 * @param ground
	 * @param dataLogger
	 * @param vw
	 */
	public ScreenListener(InputStream is, SpheroConnection spheroConn, HumanControl human, Interpreter interpreter,
			Sphero sphero, SpheroExperiment main, Goals goals, Image image, Ground ground, DataLogger dataLogger,
			VideoWriter vw) {
		this.is = is;
		this.spheroConn = spheroConn;
		this.human = human;
		this.interpreter = interpreter;
		this.sphero = sphero;
		this.mainClass = main;
		this.scf = null;
		this.goals = goals;
		this.image = image;
		this.ground = ground;
		this.dataLogger = dataLogger;
		this.vw = vw;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// Connect to Sphero and callibrate
		if (e.getActionCommand() == "Connect") {
			boolean connected = false;
			while (!connected) {
				connected = spheroConn.connect();
				if (connected) {
					scf.removeState("Connection failed");
					scf.addState("Connected");
				} else
					scf.addState("Connection failed");
			}
			boolean calibrated = spheroConn.calibrate(sphero, is, scf, interpreter, image);
			if (false)
				scf.addState("Calibrated");
			SpheroExperiment.Log.log(Level.FINE, "Connect to Sphero button clicked");
		}

		// Connect to human device (tablet)
		if (e.getActionCommand() == "Human") {
			human.startServer();
			scf.addState("Connected to human");
			SpheroExperiment.Log.log(Level.FINE, "Connect to human button clicked");
		}

		// Select goals with mouse cursor
		if (e.getActionCommand() == "Goals") {
			if (!scf.isState(goalState)) {
				scf.addState(goalState);
			} else {
				scf.removeState(goalState);
				scf.addState("Goals detected");
			}
			SpheroExperiment.Log.log(Level.FINE, "Detect goals button clicked");
		}

		// Start the expiriment
		if (e.getActionCommand() == "Start") {
			mainClass.setExperimentState(ExperimentState.HUMAN_FULL);
			human.unPause();
			if (!mainClass.isAlive())
				mainClass.start();
			mainClass.unPause(false);
			scf.removeState("Paused");
			scf.addState("Started");
			SpheroExperiment.Log.log(Level.FINE, "Start button clicked");
		}

		// Pause the expiriment
		if (e.getActionCommand() == "Pause") {
			human.pause();
			mainClass.pause();
			scf.removeState("Started");
			scf.addState("Paused");
			// spheroConn.sendCommand(new Command(0, 0));
			dataLogger.pauseExpiriment();
			SpheroExperiment.Log.log(Level.FINE, "Pause button clicked");
		}

		// Exit
		if (e.getActionCommand() == "Exit") {
			human.endExpirimentMessage();
			exit();
			SpheroExperiment.Log.log(Level.FINE, "Exit button clicked");
		}
		scf.changeText(null, null, null);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		main.SpheroExperiment.Log.log(Level.INFO, "Screen - State changed: " + e);
	}

	/**
	 * method that is called to exit the expiriment.
	 */
	private void exit() {
		main.SpheroExperiment.Log.info("-----------------Shutting down system---------------------------");
		mainClass.exit();
		if (human != null)
			human.exit();
//		if (spheroConn != null)
//			spheroConn.exit();
		is.exit();
		dataLogger.close();
		vw.exit();
		System.exit(-1);
	}

	/**
	 * @param scf canvas view that should be added to this listener in order to
	 *            have reference to eachother from both.
	 */
	public void setView(ScreenCanvasFrame scf) {
		this.scf = scf;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (scf.isState(goalState)) {
			goals.addGoal(e.getX(), e.getY());
			scf.refreshImage();
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

	@Override
	public void mousePressed(MouseEvent e) {

	}

	@Override
	public void mouseReleased(MouseEvent e) {

	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.DESELECTED) {
			dataLogger.setDataLogging(false);
		} else {
			dataLogger.setDataLogging(true);
		}
	}

}
