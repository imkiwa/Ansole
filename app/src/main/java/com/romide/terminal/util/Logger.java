package com.romide.terminal.util;

import android.util.Log;

public class Logger {
	
	public static final String TAG = "Ansole";
	
	public static void i(Object msg) {
		Log.i(TAG, msg.toString());
	}
	
	public static void d(Object msg) {
		Log.d(TAG, msg.toString());
	}
	
	public static void e(Object msg) {
		Log.e(TAG, msg.toString());
	}
	
	public static void w(Object msg) {
		Log.w(TAG, msg.toString());
		
	}
	
	public static void v(Object msg) {
		Log.v(TAG, msg.toString());
	}
	
}
