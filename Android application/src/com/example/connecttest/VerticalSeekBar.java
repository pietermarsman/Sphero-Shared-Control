package com.example.connecttest;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class VerticalSeekBar extends SeekBar {
	
	public static final String TAG = "VerticalSeekbar";

	private OnSeekBarChangeListener onChangeListener;
	private int lastProgress = -1;

	public VerticalSeekBar(Context context) {
		super(context);
	}

	public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public VerticalSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(h, w, oldh, oldw);
	}

	@Override
	protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(heightMeasureSpec, widthMeasureSpec);
		setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
	}

	protected void onDraw(Canvas c) {
		c.rotate(-90);
		c.translate(-getHeight(), 0);
		super.onDraw(c);

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled()) {
			return false;
		}
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			onChangeListener.onStartTrackingTouch(this);
			getProgress(event);
			setPressed(true);
			setSelected(true);
			break;
		case MotionEvent.ACTION_MOVE:
			super.onTouchEvent(event);
			getProgress(event);
			onSizeChanged(getWidth(), getHeight(), 0, 0);
			setPressed(true);
			setSelected(true);
			break;
		case MotionEvent.ACTION_UP:
			onChangeListener.onStopTrackingTouch(this);
			lastProgress = -1;
			setPressed(false);
			setSelected(false);
			setProgress(getMax() - (int) (getMax() * event.getY() / getHeight()));
			onSizeChanged(getWidth(), getHeight(), 0, 0);
			break;

		case MotionEvent.ACTION_CANCEL:
			break;
		}
		return true;
	}
	
	public int getProgress(MotionEvent event) {
		int progress = getMax() - (int) (getMax() * event.getY() / getHeight());
		// Ensure progress stays within boundaries
		progress = Math.max(0, Math.min(getMax(), progress));
		setProgress(progress); // Draw progress
		if (progress != lastProgress) {
			// Only enact listener if the progress has actually changed
			lastProgress = progress;
			onChangeListener.onProgressChanged(this, progress, true);
		}
		return progress;
	}

	@Override
	public void setOnSeekBarChangeListener(OnSeekBarChangeListener onChangeListener) {
		this.onChangeListener = onChangeListener;
	}

}