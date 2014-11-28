package com.saidalattrach.sudokusolver;

import android.app.Activity;
import android.os.Bundle;

public class Main extends Activity
{
	private InputInterface inputInterface;
	
	protected void onCreate(Bundle bundle)
	{
		super.onCreate(bundle);
		inputInterface = new InputInterface(this);
		setContentView(inputInterface);
	}		
}