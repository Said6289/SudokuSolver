package com.saidalattrach.sudokusolver;

import android.content.Context;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;

public class InputInterface extends SurfaceView implements SurfaceHolder.Callback, OnTouchListener
{
	private Grid grid;
	
	public InputInterface(Context context) {
		super(context);
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		setOnTouchListener(this);
		grid = new Grid(getHolder(), context);
	}
	
	public boolean onTouch(View view, MotionEvent e)
	{
		int action = e.getActionMasked();
				
		if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP)
			grid.input(e.getX(), e.getY(), action);
		
		return true;
	}
	
	public void surfaceCreated(SurfaceHolder holder)
	{
		grid.start();
	}

	public void surfaceDestroyed(SurfaceHolder holder)
	{
		grid.stop();
	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
	
}