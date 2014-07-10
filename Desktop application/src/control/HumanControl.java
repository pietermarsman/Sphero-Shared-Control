package control;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import main.Constant;
import main.StaticMethods;
import main.SpheroExperiment;

/**
 * @author Pieter Marsman Class that implements an SPP Server which accepts
 *         single line of message from an SPP client and sends a single line of
 *         response to the client.
 */

// System exits from 40 to 49
public class HumanControl extends Thread implements SpheroController {
	private static final byte COMMAND_MESSAGE = 1, RATING_MESSAGE = 2;
	private static final byte GOAL_REACHED_MESSAGE = 11, START_TEST = 12, START_EXPIRIMENT = 13, PAUSE_EXPIRIMENT = 14, RATING_NEEDED = 15,
			END_AREA = 21;

	private UUID uuid = new UUID("1101", true);
	private StreamConnectionNotifier streamConnNotifier;
	private InputStream inStream = null;
	private OutputStream outStream = null;
	private Command command;
	private StreamConnection connection;
	private RemoteDevice dev;
	private SpheroExperiment app;
	private boolean run = true, paused = false;

	/**
	 * Initializes this class. Gets a reference to the local bluetooth host.
	 */
	public HumanControl(SpheroExperiment app) {
		this.app = app;
		// display local device address and name
		LocalDevice localDevice = null;
		try {
			localDevice = LocalDevice.getLocalDevice();
		} catch (BluetoothStateException e) {
			main.SpheroExperiment.Log.log(Level.SEVERE, "Error while getting local bluetooth device");
			e.printStackTrace();
			System.exit(40);
		}
		main.SpheroExperiment.Log.log(Level.INFO, "HumanControl - Laptopdevice: " + localDevice.getBluetoothAddress());
		main.SpheroExperiment.Log.log(Level.FINE, "HumanControl - Laptopname: " + localDevice.getFriendlyName());
	}

	/**
	 * Starts the bluetooth server that is opened for connections.
	 */
	public void startServer() {
		SpheroExperiment.Log.log(Level.INFO, "Started connection to human");
		command = null;

		// Create the servicve url
		String connectionString = "btspp://localhost:" + uuid + ";name=Sample SPP Server";

		// open server url
		try {
			streamConnNotifier = (StreamConnectionNotifier) Connector.open(connectionString);
			main.SpheroExperiment.Log.log(Level.INFO, "HumanControl - Connector.open");

			// Wait for client connection
			connection = streamConnNotifier.acceptAndOpen();
			main.SpheroExperiment.Log.log(Level.INFO, "HumanControl - acceptAndOpen");

			dev = RemoteDevice.getRemoteDevice(connection);
			main.SpheroExperiment.Log.log(Level.INFO, "HumanControl - Remote address: " + dev.getBluetoothAddress());
			main.SpheroExperiment.Log.log(Level.INFO, "HumanControl - Remote name: " + dev.getFriendlyName(true));

			inStream = connection.openInputStream();
			outStream = connection.openOutputStream();
		} catch (IOException e) {
			main.SpheroExperiment.Log.log(Level.SEVERE, "Error while starting bluetooth server");
			e.printStackTrace();
		}
		this.start();
		SpheroExperiment.Log.log(Level.INFO, "Ready with connecting");
	}

	/**
	 * Stops the bluetoothserver and closes all connections.
	 */
	public void stopServer() {
		try {
			this.interrupt();
			if (inStream != null)
				inStream.close();
			if (outStream != null)
				outStream.close();
			inStream = null;
			dev = null;
			if (connection != null)
				connection.close();
			connection = null;
			if (streamConnNotifier != null)
				streamConnNotifier.close();
			streamConnNotifier = null;
			command = null;
		} catch (IOException e) {
			main.SpheroExperiment.Log.log(Level.SEVERE, "Could not properly close bluetooth server");
			e.printStackTrace();
		}
		main.SpheroExperiment.Log.log(Level.INFO, "HumanControl - Connection to human stopped");
	}

	private void restartServer() {
		stopServer();
		startServer();
	}

	public void run() {
		while (run) {
			// read string from spp client
			if (!paused) {
				try {
					int switchMessageSize = 1 * Byte.SIZE / 8;
					int gravityMessageSize = 2 * Float.SIZE / 8;
					int seekbarMessageSize = 1 * Integer.SIZE / 8;
					byte[] buffer = new byte[switchMessageSize];
					if (inStream.read(buffer) == switchMessageSize) {
						streamConnNotifier.close();
						ByteBuffer bb = ByteBuffer.allocate(switchMessageSize);
						bb.put(buffer);
						bb.position(0);
						byte switchMessage = bb.get();
						if (switchMessage == COMMAND_MESSAGE) {
							buffer = new byte[gravityMessageSize];
							inStream.read(buffer);
							bb = ByteBuffer.allocate(gravityMessageSize);
							bb.put(buffer);
							bb.position(0);
							double x = bb.getFloat();
							double y = bb.getFloat();
							double velocity = Constant.HUMAN_MAX_SPEED * Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
							int direction = StaticMethods.angle(0.0, 0.0, -x, y, 0.0001);
							command = new Command(velocity, direction);
						} else if (switchMessage == RATING_MESSAGE) {
							buffer = new byte[seekbarMessageSize];
							inStream.read(buffer);
							bb = ByteBuffer.allocate(seekbarMessageSize);
							bb.put(buffer);
							bb.position(0);
							app.ratingGiven(bb.getInt());
						}
					}
				} catch (IOException e) {
					main.SpheroExperiment.Log.log(Level.SEVERE, "Something wrong with bluetooth recieving data");
					e.printStackTrace();
					System.exit(43);
				}
			}
		}
	}

	/**
	 * Exits the current bluetooth connection.
	 */
	public void exit() {
		run = false;
		stopServer();
		this.interrupt();
	}

	@Override
	public Command getCommand() {
		return command;
	}

	/**
	 * @return if the stream is ready to process data.
	 */
	public boolean ready() {
		return inStream != null;
	}

	/**
	 * send a byte message that the goal is reached to the client
	 */
	public void goalReachedMessage() {
		try {
			if (outStream != null) {
				outStream.write(GOAL_REACHED_MESSAGE);
				outStream.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send a message to the client that the experiment is continued.
	 */
	public void unPause() {
		paused = false;
		try {
			if (outStream != null) {
				outStream.write(START_EXPIRIMENT);
				outStream.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send a message to the client that the experiment is ended.
	 */
	public void endExpirimentMessage() {
		try {
			if (outStream != null) {
				outStream.write(END_AREA);
				outStream.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send a message to the client that the experiment is paused.
	 */
	public void pause() {
		paused = true;
		try {
			if (outStream != null) {
				outStream.write(PAUSE_EXPIRIMENT);
				outStream.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void ratingNeededMessage() {
		try {
			if (outStream != null) {
				outStream.write(RATING_NEEDED);
				outStream.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}