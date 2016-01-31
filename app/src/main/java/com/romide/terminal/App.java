package com.romide.terminal;

import android.app.Application;

import com.kiva.termit.TermUtil;
import com.romide.terminal.jni.KivaTerminal;

public class App extends Application {
	private static App app;
	public static final String ENV_HOME = "KIVA_TERMINAL_HOME";
	public static final String ENV_VERSION_CODE = "KIVA_TERMINAL_VERCODE";
	public static final String ENV_VERSION = "KIVA_TERMINAL_VERSION";
	public static final String ENV_APK = "KIVA_TERMINAL_APK";
	
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
