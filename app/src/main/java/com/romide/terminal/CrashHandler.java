package com.romide.terminal;

import static com.romide.terminal.util.Constant.CRASH_REPORTER_EXTENSION;
import static com.romide.terminal.util.Constant.STACK_TRACE;
import static com.romide.terminal.util.Constant.VERSION_CODE;
import static com.romide.terminal.util.Constant.VERSION_NAME;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Toast;

import com.romide.terminal.util.Logger;

public class CrashHandler implements UncaughtExceptionHandler {
    private Thread.UncaughtExceptionHandler defaultHandler;
    private static CrashHandler sInstance;
    private Properties deviceInfo = new Properties();
    private boolean handled = false;

    private CrashHandler() {
    }

    public static CrashHandler getInstance() {
        if (sInstance == null) {
            sInstance = new CrashHandler();
        }
        return sInstance;
    }

    public void init() {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && defaultHandler != null && !handled) {
            defaultHandler.uncaughtException(thread, ex);
        } else {
            handled = true;
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Logger.w("Thread has been interrupted: " + e.toString());
            }

            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    private boolean handleException(final Throwable ex) {
        if (ex == null) {
            return true;
        }

        final String msg = ex.getLocalizedMessage();
        if (msg == null) {
            return false;
        }

        final Context appContext = App.get().getApplicationContext();
        collectCrashDeviceInfo(appContext);
        final String dumpFile = saveCrashInfoToFile(appContext, ex);

        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                Toast t = Toast.makeText(appContext,
                        appContext.getString(R.string.see_dump, dumpFile),
                        Toast.LENGTH_LONG);
                t.setGravity(Gravity.CENTER, 0, 0);
                t.show();
                Looper.loop();
            }
        }.start();

        return true;
    }

    @SuppressLint("SimpleDateFormat")
    private String saveCrashInfoToFile(Context ctx, Throwable ex) {
        Writer info = new StringWriter();
        PrintWriter printWriter = new PrintWriter(info);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();

        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }

        String result = info.toString();
        printWriter.close();
        deviceInfo.put("ExceptionMessage", ex.getLocalizedMessage());
        deviceInfo.put(STACK_TRACE, result);

        FileOutputStream trace = null;
        File dumpFile = null;
        try {
            Date date = new Date();
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String fileName = String.format("TerminalCrash_%s%s", fmt.format(date),
                    CRASH_REPORTER_EXTENSION);

            if (Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED)) {
                dumpFile = new File(Environment.getExternalStorageDirectory(),
                        fileName);
            } else {
                dumpFile = new File(ctx.getFilesDir(), fileName);
            }

            trace = new FileOutputStream(dumpFile);
            trace.write(createDumpInfoFromDeviceInfo().getBytes());
            trace.flush();
        } catch (Exception e) {
            Logger.w("Error while writing crush dump: " + e.toString());
        } finally {
            if (trace != null) {
                try {
                    trace.close();
                } catch (IOException e) {
                    Logger.w("Error while closing crush dump file: " + e.toString());
                }
            }
        }

        return dumpFile == null ? null : dumpFile.getAbsolutePath();
    }

    public void collectCrashDeviceInfo(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(),
                    PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                deviceInfo.put(VERSION_NAME, pi.versionName == null ? "not set"
                        : pi.versionName);
                deviceInfo.put(VERSION_CODE, "" + pi.versionCode);
            }
        } catch (NameNotFoundException e) {
            Logger.w("error while collect crash device info: " + e.toString());
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                deviceInfo.put(field.getName(), "" + field.get(null));
            } catch (Exception e) {
                Logger.w("error while reflecting field info: " + e.toString());
            }
        }
    }

    public String createDumpInfoFromDeviceInfo() {
        StringBuilder builder = new StringBuilder();

        for (Object key : deviceInfo.keySet()) {
            Object val = deviceInfo.get(key);

            String l = String.format(
                    "%s = %s\n",
                    key.toString(), val.toString()
            );
            builder.append(l);
        }

        return builder.toString();
    }

}
