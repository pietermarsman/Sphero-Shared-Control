package control;

import java.util.Collection;
import java.util.logging.Level;

import output.Log;
import output.ScreenCanvasFrame;

import main.Constant;
import main.StaticMethods;
import main.Sphero;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;

import se.nicklasgavelin.bluetooth.Bluetooth;
import se.nicklasgavelin.bluetooth.Bluetooth.EVENT;
import se.nicklasgavelin.bluetooth.BluetoothDevice;
import se.nicklasgavelin.bluetooth.BluetoothDiscoveryListener;
import se.nicklasgavelin.sphero.Robot;
import se.nicklasgavelin.sphero.command.FrontLEDCommand;
import se.nicklasgavelin.sphero.command.RGBLEDCommand;
import se.nicklasgavelin.sphero.command.RollCommand;
import se.nicklasgavelin.sphero.command.SetDataStreamingCommand;
import se.nicklasgavelin.sphero.exception.InvalidRobotAddressException;
import se.nicklasgavelin.sphero.exception.RobotBluetoothException;
import webcam.Image;
import webcam.InputStream;
import webcam.Interpreter;

/**
 * @author Pieter Marsman
 */
public class SpheroConnection implements BluetoothDiscoveryListener {
	
	// rgr
//	private static String id = "0006664b9a49";
	// wbw
	private static String id = "0006664ba8a8";

	BluetoothDevice btd;
	Robot r;
	int calibrationAngle;

	/**
	 * Initializes this class.
	 */
	public SpheroConnection() {
		calibrationAngle = 0;
		btd = null;
	}

	/**
	 * Has a blocking call so this method can take a while before finishing.
	 * @return if connected to Sphero
	 */
	public boolean connect() {
		main.SpheroExperiment.Log.log(Level.FINE, "SpheroConnection - Started connection to Sphero");
		Bluetooth bt = new Bluetooth(this, Bluetooth.SERIAL_COM);
		btd = new BluetoothDevice(bt, "btspp://" + id + ":1;authenticate=true;encrypt=false;master=false");
		try {
			this.r = new Robot(btd);
		} catch (Exception e) {
			main.SpheroExperiment.Log.log(Level.WARNING, "Could not connect with robot " + id + " because of invalid properties");
			e.printStackTrace();
			return false;
		}
		if (r.connect()) {
			r.sendCommand(new RGBLEDCommand(0, 0, 0));
//			SetDataStreamingCommand sdsc = new SetDataStreamingCommand(400, 1, 50);
//			sdsc.addMask(SetDataStreamingCommand.DATA_STREAMING_MASKS.GYRO.X.FILTERED);
//			r.sendCommand(sdsc);
		} else {
			main.SpheroExperiment.Log.log(Level.WARNING, "SpheroConnection - Failed to connect to Sphero");
			return false;
		}
		main.SpheroExperiment.Log.log(Level.FINE, "SpheroConnection - Finished connection to Sphero");
		return true;
	}

	/**
	 * Calibrate Sphero such that the forward command for Sphero lets Sphero go
	 * up on the image
	 * 
	 * @param sphero
	 * @param is
	 * @param scf
	 */
	public boolean calibrate(Sphero sphero, InputStream is, ScreenCanvasFrame scf, Interpreter interpreter, Image img) {
		try {
			CvPoint center = null;
			// Search for sphero in the image until it is found
			do {
				is.flush();
				img.newImage(is.nextImage(), false);
				interpreter.location(img, -1.0);
				center = sphero.getCenter();
				scf.refreshImage();
			} while (center == null);
			// Let sphero drive for a while
			sendCommand(new Command(0.3f, 0));
			Thread.sleep(Constant.CALIBRATION_SLEEP);
			r.sendCommand(new RollCommand(0, 0, true));
			// Search for sphero's new location in the image until it is found
			CvPoint center2 = null;
			do {
				is.flush();
				img.newImage(is.nextImage(), false);
				interpreter.location(img, -1.0);
				center2 = sphero.getCenter();
				img.drawObservation(sphero, null, null, null, null, null, null);
				scf.refreshImage();
			} while (center2 == null);
			// Draw the difference in the location of Sphero
			img.drawLine(center, center2);
			scf.refreshImage();
			// Set the angle in order to calibrate Sphero
			calibrationAngle = StaticMethods.angle(center, center2);
			main.SpheroExperiment.Log.log(Level.INFO, "Calibrated with angle: " + calibrationAngle);
			return true;
		} catch (InterruptedException e) {
			main.SpheroExperiment.Log.log(Level.WARNING, "Error while calibrating sphero. Could not get thread to sleep");
			return false;
		}
	}

	/**
	 * Exits the connection with Sphero
	 */
	public void exit() {
		if (ready())
			r.disconnect();
	}

	/**
	 * @param command that should be send to Sphero
	 */
	public void sendCommand(Command command) {
		if (command != null) {
			float dir = (command.getDirection() + (360 - calibrationAngle)) % 360;
			float vel = (float) command.getVelocity();
			r.roll(dir, vel);
		} else {
			r.stopMotors();
		}
	}

	/**
	 * @return if the connection is ready to go
	 */
	public boolean ready() {
		if (r != null)
			return r.isConnected();
		else
			return false;
	}

	@Override
	public void deviceDiscovered(BluetoothDevice arg0) {
	}

	@Override
	public void deviceSearchCompleted(Collection<BluetoothDevice> arg0) {
	}

	@Override
	public void deviceSearchFailed(EVENT arg0) {
	}

	@Override
	public void deviceSearchStarted() {
	}

	public void stopMoving() {
		sendCommand(new Command(0,0,"Pause"));
	}
}
