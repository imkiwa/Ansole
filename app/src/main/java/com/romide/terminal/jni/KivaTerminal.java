package com.romide.terminal.jni;

public final class KivaTerminal {

	public static final int REPLACE_YES = 1;
	public static final int REPLACE_NO = 0;
	
	public static native void setenv(String name, String value, int replace);
	
	static {
		System.loadLibrary("kiva");
	}
}
