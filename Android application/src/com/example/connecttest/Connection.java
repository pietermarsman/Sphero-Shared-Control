package com.example.connecttest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class Connection extends Thread {
	private static final byte COMMAND_MESSAGE = 1, RATING_MESSAGE = 2;
	private static final byte GOAL_REACHED_MESSAGE = 11, START_TEST = 12, START_EXPIRIMENT = 13, PAUSE_EXPIRIMENT = 14,
			RATING_NEEDED = 15, END_AREA = 21;
	public static final String TAG = "Connection";

	private static final int REQUEST_ENABLE_BT = 1;
	private BluetoothAdapter btAdapter;
	private BluetoothSocket btSocket;
	private OutputStream outStream;
	private InputStream inStream;
	private ConnectTest app;
	private Handler mHandler;
	private boolean executed;

	// Well known SPP UUID
	static int sdk = Build.VERSION.SDK_INT;
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	// Laptop
//	 private static String address = "00:26:5E:98:6D:5F";
	// Experiment computer
	private static String address = "00:0C:78:33:AF:99";

	public Connection(ConnectTest app) {
		Log.i(TAG, "App started");
		this.app = app;
		executed = false;
		Log.v(TAG, "UUID of device is: " + MY_UUID.toString());
	}

	public void write(byte[] bytes) {
		Message m = Message.obtain();
		Bundle data = new Bundle();
		data.putByteArray("bytes", bytes);
		m.setData(data);
		if (mHandler != null)
			mHandler.handleMessage(m);
		else if (app != null)
			Log.e(TAG, "mHandler does not exist in write()");
	}

	public void writeCommand(float xCommand, float yCommand) {
		ByteBuffer bb = ByteBuffer.allocate(1 * Byte.SIZE / 8 + 2 * Float.SIZE / 8);
		bb.put((byte) 1);
		bb.putFloat(xCommand);
		bb.putFloat(yCommand);
		Message m = Message.obtain();
		Bundle data = new Bundle();
		data.putByteArray("bytes", bb.array());
		m.setData(data);
		if (mHandler != null)
			mHandler.handleMessage(m);
		else if (app != null)
			Log.e(TAG, "mHandler does not exist in writeCommand()");
	}

	public void writeRating(int rating) {
		ByteBuffer bb = ByteBuffer.allocate(1 * Byte.SIZE / 8 + 1 * Integer.SIZE / 8);
		bb.put((byte) 2);
		bb.putInt(rating);
		Message m = Message.obtain();
		Bundle data = new Bundle();
		data.putByteArray("bytes", bb.array());
		m.setData(data);
		if (mHandler != null)
			mHandler.handleMessage(m);
		else if (app != null)
			Log.e(TAG, "mHandler does not exist in writeRating()");
	}

	public void checkBTState() {
		// Check for Bluetooth support and then check to make sure it is turned
		// on

		// Emulator doesn't support Bluetooth and will return null
		if (btAdapter == null) {
			Log.e(TAG, "Bluetooth Not supported. Aborting.");
		} else {
			if (btAdapter.isEnabled()) {
				Log.d(TAG, "Bluetooth is enabled");
			} else {
				// Prompt user to turn on Bluetooth
				Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
				app.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}
	}

	public void onCreate() {
		Log.i(TAG, "begin onCreate()");
		if (!executed) {

			btAdapter = BluetoothAdapter.getDefaultAdapter();

			// Set up a pointer to the remote node using it's address.
			BluetoothDevice device = btAdapter.getRemoteDevice(address);
			Log.d(TAG, "device name: " + device.getName());

			mHandler = new Handler() {
				public void handleMessage(Message message) {
					try {
						outStream.write(message.getData().getByteArray("bytes"));
						outStream.flush();
					} catch (IOException e) {
						String msg = "In onResume() and an exception occurred during write: " + e.getMessage()
								+ ".\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";
						Log.e(TAG, msg);
					}
				}
			};

			// Discovery is resource intensive. Make sure it isn't going on
			// when you attempt to connect and pass your message.
			btAdapter.cancelDiscovery();

			// Two things are needed to make a connection:
			// A MAC address, which we got above.
			// A Service ID or UUID. In this case we are using the
			// UUID for SPP.
			try {
				btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "In onCreate() and socket create failed: " + e.getMessage() + ".");
			}
		}
		Log.d(TAG, "end onCreate()");
	}

	public void onStart() {
		Log.i(TAG, "begin onStart()");
		while (!btSocket.isConnected()) {
			try {
				// Establish the connection. This will block until it
				// connects.
				btSocket.connect();
			} catch (IOException e) {
				Log.e(TAG, "Unable to connect to bluetooth socket during: " + e.getMessage());
			}
		}
		Log.d(TAG, "Connection established and data link opened...");
		try {
			outStream = btSocket.getOutputStream();
			inStream = btSocket.getInputStream();
		} catch (IOException e) {
			Log.e(TAG, "In onCreate() and output stream creation failed:" + e.getMessage() + ".");
		}
		executed = true;
		this.start();
		Log.d(TAG, "end onStart()");
	}

	public void onDestroy() {
		Log.i(TAG, "begin onDestroy()");
		this.interrupt();
		mHandler = null;

		if (outStream != null) {
			try {
				outStream.flush();
			} catch (IOException e) {
				Log.e(TAG, "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
			}
		}

		try {
			btSocket.close();
		} catch (IOException e2) {
			Log.e(TAG, "In onPause() and failed to close socket." + e2.getMessage() + ".");
		}
		Log.d(TAG, "end onDestroy()");
	}

	public void run() {
		while (true) {
			try {
				int messageSize = 1 * Byte.SIZE / 8;
				byte[] buffer = new byte[messageSize];
				if (inStream.read(buffer) == messageSize) {
					ByteBuffer bb = ByteBuffer.allocate(messageSize);
					bb.put(buffer);
					bb.position(0);
					byte message = bb.get();
					String messageText = "";
					switch (message) {
					case GOAL_REACHED_MESSAGE:
						messageText = "goal reached";
						break;
					case START_TEST:
						messageText = "start test";
						break;
					case START_EXPIRIMENT:
						messageText = "start expiriment";
						break;
					case END_AREA:
						messageText = "end";
						break;
					case PAUSE_EXPIRIMENT:
						messageText = "pause";
						break;
					case RATING_NEEDED:
						messageText = "rating";
						break;
					}
					Message msg = new Message();
					msg.obj = messageText;
					app.seekBarHandler.sendMessage(msg);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
