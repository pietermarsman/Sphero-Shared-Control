package com.example.connecttest;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import com.example.bttest.R;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import android.graphics.Point;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class ConnectTest extends Activity implements SensorEventListener {

	public static final float FILTER = 0.8f, SENSOR_SENSITIVITY = 0.01f, COMMAND_MULTIPLIER = 1.0f;
	public static final String TAG = "ConnectTest";
	private final int goals = 3;

	private View userFeedbackBall;
	private FrameLayout userFeedback;
	private SensorManager mSensorManager;
	private Sensor mSensor;
	private Connection conn;
	private float[] gravity, oldGravity;
	protected PowerManager.WakeLock mWakeLock;
	private VerticalSeekBar seekBarRight, seekBarLeft;
	private Vibrator v;
	private TextView feedbackProgressLeft, feedbackProgressRight;
	private List<TextView> goalTexts;
	private TextView currentGoal;

	public TextView out;
	public Handler seekBarHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			String text = (String) msg.obj;
			if (text == "goal reached")
				goalReached();
			else if (text == "feedback given")
				feedbackGiven(msg.arg1);
			else if (text == "feedback change")
				feedbackChange(msg.arg1);
			else if (text == "end") {
				conn.onDestroy();
				out.setText("The end");
			} else if (text == "start test")
				out.setText("Drive to Goal (test)");
			else if (text == "start expiriment")
				out.setText("Drive to Goal");
			else if (text == "pause")
				out.setText("Paused");
			else if (text == "rating")
				feedbackNeeded();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "begin onCreate()");
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);

		out = (TextView) findViewById(R.id.out);
		out.setMovementMethod(new ScrollingMovementMethod());
		
		goalTexts = new LinkedList<TextView>();
		goalTexts.add((TextView) findViewById(R.id.textGoal1));
		goalTexts.add((TextView) findViewById(R.id.textGoal2));
		goalTexts.add((TextView) findViewById(R.id.textGoal3));
		currentGoal = goalTexts.get(0);
		changeGoal(currentGoal);

		conn = new Connection(this);
		conn.onCreate();

		// Initialize sensors
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
		v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		gravity = new float[3];
		oldGravity = new float[3];

		feedbackProgressLeft = (TextView) findViewById(R.id.feedbackProgressLeft);
		feedbackProgressRight = (TextView) findViewById(R.id.feedbackProgressRight);
		seekBarRight = (VerticalSeekBar) findViewById(R.id.verticalSeekBarRight);
		seekBarLeft = (VerticalSeekBar) findViewById(R.id.verticalSeekBarLeft);
		seekBarRight.setOnSeekBarChangeListener(new SeekBarChangeListener(seekBarLeft, seekBarRight, conn,
				seekBarHandler, this));
		seekBarLeft.setOnSeekBarChangeListener(new SeekBarChangeListener(seekBarLeft, seekBarRight, conn,
				seekBarHandler, this));
		seekBarLeft.setEnabled(true);
		seekBarRight.setEnabled(true);
		seekBarRight.setAlpha(0.4f);
		seekBarLeft.setAlpha(0.4f);

		userFeedbackBall = (View) findViewById(R.id.feedbackCircle);
		userFeedback = (FrameLayout) findViewById(R.id.userFeedback);

		Log.d(TAG, "end onCreate()");
	}

	protected void onStart() {
		Log.i(TAG, "begin onStart()");
		super.onStart();
		conn.onStart();
		conn.checkBTState();
		out.setText("Connected to computer");
		Log.d(TAG, "end onStart()");
	}

	protected void onRestart() {
		Log.i(TAG, "begin onRestart()");
		super.onRestart();
		Log.d(TAG, "end onRestart()");
	}

	protected void onResume() {
		Log.i(TAG, "begin onResume()");
		super.onResume();
		// Don't dim the light
		// final PowerManager pm = (PowerManager)
		// getSystemService(Context.POWER_SERVICE);
		// this.mWakeLock =
		// pm.newWakeLock(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
		// "My Tag");
		// this.mWakeLock.acquire();
		mSensorManager.registerListener(this, mSensor, Sensor.TYPE_GRAVITY);
		Log.d(TAG, "end onResume()");
	}

	protected void onPause() {
		Log.i(TAG, "begin onPause()");
		super.onPause();
		// this.mWakeLock.release();
		mSensorManager.unregisterListener(this);
		Log.d(TAG, "end onPause()");
	}

	protected void onStop() {
		Log.i(TAG, "begin onStop()");
		super.onStop();
		Log.d(TAG, "end onStop()");
	}

	protected void onDestroy() {
		Log.i(TAG, "begin onStop()");
		super.onDestroy();
		conn.onDestroy();
		Log.d(TAG, "end onStop()");
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		Log.i(TAG, "Accuracy changed to: " + accuracy);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// Zero means that is is gravity sensor change
		// bb.putInt(0);
		double diff = 0.0;
		for (int i = 0; i < 3; i++) {
			gravity[i] = gravity[i] * FILTER + event.values[i] * (1 - FILTER);
			diff += Math.abs(gravity[i] - oldGravity[i]) / 3.0;
		}
		if (diff > SENSOR_SENSITIVITY) {
			float total = Math.abs(gravity[0]) + Math.abs(gravity[1]) + Math.abs(gravity[2]);
			changeUserFeedback(gravity[0], gravity[1], total);
			sendCommand(gravity[0], gravity[1], total);
			oldGravity = gravity.clone();
		}
	}

	private void changeUserFeedback(float x, float y, float total) {
		Point middle = new Point((userFeedback.getWidth() - userFeedbackBall.getWidth()) / 2,
				(userFeedback.getHeight() - userFeedbackBall.getHeight()) / 2);
		int range = Math.min(middle.x, middle.y);
		float xpos = Math.max(Math.min(middle.x + range * COMMAND_MULTIPLIER * (-x / total), middle.x + range),
				middle.x - range);
		float ypos = Math.max(Math.min(middle.y + range * COMMAND_MULTIPLIER * (y / total), middle.y + range), middle.y
				- range);
		userFeedbackBall.setX(xpos);
		userFeedbackBall.setY(ypos);
	}

	private void sendCommand(float x, float y, float total) {
		ByteBuffer bb = ByteBuffer.allocateDirect(4 * 3);
		float xCommand = Math.max(Math.min(x / total * COMMAND_MULTIPLIER, 1.0f), -1.0f);
		float yCommand = Math.max(Math.min(y / total * COMMAND_MULTIPLIER, 1.0f), -1.0f);
		conn.writeCommand(xCommand, yCommand);
	}

	public void feedbackNeeded() {
		seekBarRight.setProgress(3);
		seekBarLeft.setProgress(3);
		seekBarRight.setEnabled(true);
		seekBarLeft.setEnabled(true);
		seekBarRight.setAlpha(1.0f);
		seekBarLeft.setAlpha(1.0f);
		out.setText("How much were you in control?");
		// TODO send both values back to the computer
		v.vibrate(1000);
	}
	
	private void goalReached() {
		boolean next = false;
		v.vibrate(200);
		for (TextView tv : goalTexts) {
			if (tv == currentGoal) {
				next = true;
				tv.setTextSize(getResources().getDimension(R.dimen.text_goal_size));
				tv.setTypeface(null, Typeface.NORMAL);
				tv.setTextColor(getResources().getColor(R.color.red));
			}
			else if (next) {
				next = false;
				changeGoal(tv);
			}
		}
		if (next) {
			changeGoal(goalTexts.get(0));
		}
	}
	
	private void changeGoal(TextView tv) {
		currentGoal = tv;
		tv.setTextSize(getResources().getDimension(R.dimen.text_goal_selected_size));
		tv.setTypeface(null, Typeface.BOLD);
		tv.setTextColor(getResources().getColor(R.color.green));
	}

	public void feedbackGiven(int progress) {
		seekBarRight.setEnabled(false);
		seekBarLeft.setEnabled(false);
		seekBarRight.setAlpha(0.4f);
		seekBarLeft.setAlpha(0.4f);
		out.setText("Drive to goal");
		feedbackProgressLeft.setText(String.valueOf(progress));
		feedbackProgressRight.setText(String.valueOf(progress));
	}
	
	public void feedbackChange(int progress) {
		feedbackProgressLeft.setText(String.valueOf(progress));
		feedbackProgressRight.setText(String.valueOf(progress));
	}
}