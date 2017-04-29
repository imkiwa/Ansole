package com.romide.terminal;

import android.app.Application;

import com.kiva.termit.TermUtil;
import com.romide.terminal.jni.KivaTerminal;
import com.romide.terminal.util.CrashHandler;

public class App extends Application {
	private static App app;
	public static final String ENV_HOME = "ANSOLE_HOME";
	public static final String ENV_VERSION_CODE = "ANSOLE_VERSION_CODE";
	public static final String ENV_VERSION = "ANSOLE_VERSION";
	public static final String ENV_APK = "ANSOLE_BINARY";
	
	@Override
	public void onCreate() {
		super.onCreate();
		app = this;

		CrashHandler.getInstance().init();
		initTermEnv();
	}

	private void initTermEnv() {
		try {
			KivaTerminal.setEnv(ENV_VERSION, TermUtil.getTermVersionName(this));
            KivaTerminal.setEnv(ENV_VERSION_CODE, TermUtil.getTermVersionCodeString(this));

		} catch (TermUtil.TermNotInstalledException never) {
            never.printStackTrace();
		}

		KivaTerminal.setEnv(ENV_HOME, getFilesDir().getAbsolutePath());
        KivaTerminal.setEnv(ENV_APK, getPackageCodePath());
    }

	public static App get() {
		return app;
	}

}
