package com.romide.terminal;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

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
		initTerminalEnvironment();
	}

	private void initTerminalEnvironment() {
		PackageManager pm = getPackageManager();

		try {
			PackageInfo info = pm.getPackageInfo(getPackageName(),
					PackageManager.GET_ACTIVITIES);

			KivaTerminal.setenv(ENV_VERSION, info.versionName,
					KivaTerminal.REPLACE_YES);
			KivaTerminal.setenv(ENV_VERSION_CODE,
					String.valueOf(info.versionCode),
					KivaTerminal.REPLACE_YES);
		} catch (Exception neverHappen) {
			neverHappen.printStackTrace();
		}

		KivaTerminal.setenv(ENV_HOME, getFilesDir().getAbsolutePath(),
				KivaTerminal.REPLACE_YES);
		KivaTerminal.setenv(ENV_APK, getPackageCodePath(),
				KivaTerminal.REPLACE_YES);
	}

	public static App get() {
		return app;
	}

}
