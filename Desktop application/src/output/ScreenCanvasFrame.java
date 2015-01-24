package output;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import main.Constant;
import main.Sphero;
import main.SpheroExperiment;

import webcam.Ground;
import webcam.Image;
import webcam.InputStream;
import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

import control.BrooksControl;
import control.Command;
import control.Goals;
import control.HumanControl;
import control.SpheroConnection;

/**
 * @author Pieter Marsman
 */
public class ScreenCanvasFrame {

	ScreenListener sl;
	private CanvasFrame frame;
	private JButton buttonExit, buttonStartExpiriment, buttonPause, buttonGoals, buttonConnect, buttonHuman;
	private JCheckBox csvLogging;
	private JTextArea textInfo;
	private Sphero sphero;
	private Ground ground;
	private double frameRate, last;
	private List<String> states;
	private BrooksControl brooks;
	private Goals goals;
	private Image image;
	private SpheroExperiment app;

	/**
	 * Initializes this class with references to
	 * 
	 * @param sphero
	 *            ,
	 * @param ground
	 *            ,
	 * @param screenListener
	 *            ,
	 * @param brooks
	 *            ,
	 * @param goals
	 *            and the
	 * @param image
	 */
	public ScreenCanvasFrame(Sphero sphero, Ground ground, ScreenListener screenListener, BrooksControl brooks, Goals goals, Image image, SpheroExperiment app) {
		init(sphero, ground, screenListener, brooks, goals, image, app);
		setListeners();
		setProperties();
		addElements();
	}

	/**
	 * Initiales this class with only a reference to
	 * 
	 * @param image
	 */
	public ScreenCanvasFrame(Image image) {
		this(null, null, null, null, null, image, null);
	}

	private void init(Sphero sphero, Ground ground, ScreenListener sl, BrooksControl brooks, Goals goals, Image image, SpheroExperiment app) {
		this.sl = sl;
		frame = new CanvasFrame("Input and Output");
		this.sphero = sphero;
		this.ground = ground;
		this.brooks = brooks;
		this.goals = goals;
		this.image = image;
		this.app = app;
		buttonConnect = new JButton("1. Connect Sphero");
		buttonHuman = new JButton("2. Connect human");
		buttonGoals = new JButton("3. Detect goals");
		buttonStartExpiriment = new JButton("4. Start");
		csvLogging = new JCheckBox("CSV logging");
		buttonPause = new JButton("5. Pause");
		buttonExit = new JButton("6. Exit");
		textInfo = new JTextArea(8, 10);
		last = new Date().getTime();
		frameRate = 0.0;
		states = new LinkedList<String>();
	}

	private void setListeners() {
		frame.getContentPane().getComponent(0).addMouseListener(sl);
		buttonExit.setActionCommand("Exit");
		buttonExit.addActionListener(sl);
		buttonStartExpiriment.setActionCommand("Start");
		buttonStartExpiriment.addActionListener(sl);
		csvLogging.setActionCommand("csv");
		csvLogging.addItemListener(sl);
		buttonPause.setActionCommand("Pause");
		buttonPause.addActionListener(sl);
		buttonGoals.setActionCommand("Goals");
		buttonGoals.addActionListener(sl);
		buttonConnect.setActionCommand("Connect");
		buttonConnect.addActionListener(sl);
		buttonHuman.setActionCommand("Human");
		buttonHuman.addActionListener(sl);
	}

	private void setProperties() {
		frame.getContentPane().setLayout(new BorderLayout(5, 5));
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		textInfo.setEditable(false);
	}

	private void addElements() {
		frame.getContentPane().add(frame.getContentPane().getComponent(0), BorderLayout.CENTER);
		JPanel controlPannel = new JPanel();
		controlPannel.setLayout(new GridLayout(7, 1));
		controlPannel.add(buttonConnect);
		controlPannel.add(buttonHuman);
		controlPannel.add(buttonGoals);
		controlPannel.add(buttonStartExpiriment);
		controlPannel.add(csvLogging);
		controlPannel.add(buttonPause);
		controlPannel.add(buttonExit);
		frame.getContentPane().add(controlPannel, BorderLayout.LINE_END);
		frame.getContentPane().add(textInfo, BorderLayout.PAGE_END);
	}

	/**
	 * @param c
	 *            the command that should appear in the output window
	 */
	public void changeText(Command human, Command computer, Command shared) {
		frameRate = 0.9 * frameRate + 0.1 * (new Date().getTime() - last);
		last = new Date().getTime();
		String text = "";
		if (sphero != null)
			text += "Location: " + sphero.getCenter() + " / " + sphero.getRadius() + "\n" + "Direction: " + sphero.getDirection() + "° / "
					+ Math.round(sphero.getVelocity() * 10.0) / 10.0 + " cm/s" + "\n" + "Color sphero: " + sphero.getColor() + "\n";
		if (ground != null)
			text += "Color ground: " + ground.getColor() + "\n";
		text += "frame rate: " + Math.round(1000 / frameRate) + "f/s" + "\n" + "State: " + states + "\n" + "Experiment state: " + app.getExperimentState()
				+ "\n" + "Human: " + human + "\tComputer: " + computer + "\tShared: " + shared;
		textInfo.setText(text);
	}

	/**
	 * Refreshes the image in the canvas and draws the three commands on it
	 * 
	 * @param c1
	 * @param c2
	 * @param c3
	 */
	public void refreshImage(Command human, Command computer, Command shared) {
		image.drawObservation(sphero, ground, brooks, human, computer, shared, goals);
		frame.showImage(image.getDrawing());
		changeText(human, computer, shared);
	}

	/**
	 * Refresh the image in the canvas
	 */
	public void refreshImage() {
		refreshImage(null, null, null);
	}

	/**
	 * @param state
	 *            that should be added to the list of states that is displayed
	 *            in the output textfield
	 */
	public void addState(String state) {
		states.add(state);
	}

	/**
	 * @param state
	 *            that should be removed from the list of states
	 * @return if the deletion was succesfull
	 */
	public boolean removeState(String state) {
		return states.remove(state);
	}

	/**
	 * @param state
	 * @return if the state is currently in the list of states
	 */
	public boolean isState(String state) {
		return states.contains(state);
	}
}
