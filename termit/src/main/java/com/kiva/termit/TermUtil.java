package com.kiva.termit;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * @author Kiva
 * @date 2016/1/31
 */
public final class TermUtil {
    private static PackageInfo termAppInfo;

    public static int getTermVersionCode(Context context) throws TermNotInstalledException {
        initTermAppInfoIfNeed(context);
        return termAppInfo.versionCode;
    }

    public static String getTermVersionCodeString(Context context) throws TermNotInstalledException {
        return String.valueOf(getTermVersionCode(context));
    }

    public static String getTermVersionName(Context context) throws TermNotInstalledException {
        initTermAppInfoIfNeed(context);
        return termAppInfo.versionName;
    }

    public static TermCaller.Builder newTermRequest() {
        return new TermCaller.Builder();
    }

    public static String getIntentResult(Intent resultData) {
        if (resultData == null) {
            return null;
        }

        return resultData.getStringExtra(TermConstant.EXTRA_WINDOW_HANDLE);
    }

    private static void initTermAppInfoIfNeed(Context context) throws TermNotInstalledException {
        if (termAppInfo != null) {
            return;
        }

        PackageManager mgr = context.getPackageManager();
        try {
            termAppInfo = mgr.getPackageInfo(TermConstant.TERM_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            throw new TermNotInstalledException();
        }
    }

    public static class TermNotInstalledException extends Exception {
        public TermNotInstalledException() {
            super("package [" + TermConstant.TERM_PACKAGE_NAME + "] not installed!");
        }
    }
}
