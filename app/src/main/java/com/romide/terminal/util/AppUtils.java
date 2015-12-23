package com.romide.terminal.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.TextUtils;

public class AppUtils {
	private static String[] components;

	public static void restartApp(Context ctx) {
		Intent i = ctx.getPackageManager().getLaunchIntentForPackage(
				ctx.getPackageName());
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		ctx.startActivity(i);
	}

	public static void registerComponents(String[] ca) {
		components = ca;
	}

	public static void setLauncherActivity(Context ctx, String enable) {
		if (components == null) {
			return;
		}

		for (String n : components) {
			disableComponent(ctx, n);
		}

		if (!TextUtils.isEmpty(enable)) {
			enableComponent(ctx, enable);
		}
	}

	private static void enableComponent(Context ctx, String name) {
		ComponentName cn = new ComponentName(ctx.getPackageName(), name);
		toggleComponent(ctx, cn, PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
	}

	private static void disableComponent(Context ctx, String name) {
		ComponentName cn = new ComponentName(ctx.getPackageName(), name);
		toggleComponent(ctx, cn,
				PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
	}

	private static void toggleComponent(Context ctx, ComponentName cn, int flag) {
		ctx.getPackageManager().setComponentEnabledSetting(cn, flag,
				PackageManager.DONT_KILL_APP);
	}
}
