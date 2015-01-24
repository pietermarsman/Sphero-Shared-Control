package com.example.connecttest;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SeekBarChangeListener implements OnSeekBarChangeListener {
	
	public static final String TAG = "SeekBarChangeListener";

	VerticalSeekBar seekBarLeft, seekBarRight;
	Connection conn;
	int rating = -1;
	Handler seekBarHandler;
	ConnectTest app;

	public SeekBarChangeListener(VerticalSeekBar verticalSeekBarLeft, VerticalSeekBar verticalSeekBarRight,
			Connection conn, Handler seekBarHandler, ConnectTest app) {
		this.seekBarLeft = verticalSeekBarLeft;
		this.seekBarRight = verticalSeekBarRight;
		this.conn = conn;
		this.seekBarHandler = seekBarHandler;
		this.app = app;
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		Log.v(TAG, "Stoped tracking");
		if (rating != -1) {
			conn.writeRating(rating);
			// Change feedback text to the final value
			Message msg = new Message();
			String textTochange = "feedback given";
			msg.obj = textTochange;
			msg.arg1 = rating;
			seekBarHandler.sendMessage(msg);
		}
		rating = -1;
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		Log.v(TAG, "progress: " + progress);
		if (seekBarLeft == seekBar)
			seekBarRight.setProgress(progress);
		else if (seekBarRight == seekBar)
			seekBarLeft.setProgress(progress);
		rating = progress;
		// Change feedback text to the current selected value
		Message msg = new Message();
		String textToChange = "feedback change";
		msg.obj = textToChange;
		msg.arg1 = rating;
		seekBarHandler.sendMessage(msg);
	}
}
