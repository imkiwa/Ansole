package com.romide.terminal.util;

import android.os.Handler;
import android.os.Looper;

/**
 * @author kiva
 */

public class UiKit extends Handler {
    private static UiKit sInstance;

    public static UiKit get() {
        if (sInstance == null) {
            synchronized (UiKit.class) {
                if (sInstance == null) {
                    sInstance = new UiKit();
                }
            }
        }
        return sInstance;
    }

    private UiKit() {
        super(Looper.getMainLooper());
    }
}
