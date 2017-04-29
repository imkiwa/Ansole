package com.romide.terminal.session;

import java.io.File;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.view.KeyEvent;

import com.romide.terminal.R;

/**
 * Terminal emulator settings
 */
public class TermSettings {
    private SharedPreferences mPrefs;

    private int mStatusBar;
    private int mActionBarMode;
    private int mOrientation;
    private int mCursorStyle;
    private int mCursorBlink;
    private int mCursorBlinkPeriod;
    private int mFontSize;
    private int mColorId;
    private boolean mUTF8ByDefault;
    private int mBackKeyAction;
    private int mControlKeyId;
    private int mFnKeyId;
    private int mUseCookedIME;
    private String mShell;
    private String mFailsafeShell;
    private String mInitialCommand;
    private String mTermType;
    private boolean mCloseOnExit;
    private boolean mVerifyPath;
    private boolean mDoPathExtensions;
    private boolean mAllowPathPrepend;
    private String mHomePath;

    private String mPrependPath = null;
    private String mAppendPath = null;

    private boolean mAltSendsEsc;

    private boolean mMouseTracking;

    private boolean mTerminalBeep;

    private boolean mUseKeyboardShortcuts;

    private boolean mUseExtShell;
    private boolean mUseExtProfile;
    private boolean mStartOnBoot;
    private boolean mAutoSu;
    private static final String USE_EXT_SHELL_LOGIN_KEY = "external_use_login";
    private static final String USE_EXT_PROFILE_LOGIN_KEY = "external_use_profile";
    private static final String EXT_SHELL_KEY = "external_shell_path";
    private static final String EXT_PROFILE_KEY = "external_profile_path";
    private static final String START_ON_BOOT = "start_on_boot";
    private static final String AUTO_SU = "auto_su";

    private static final String STATUSBAR_KEY = "statusbar";
    private static final String ACTIONBAR_KEY = "actionbar";
    private static final String ORIENTATION_KEY = "orientation";
    private static final String FONTSIZE_KEY = "fontsize";
    private static final String COLOR_KEY = "color";
    private static final String UTF8_KEY = "utf8_by_default";
    private static final String BACKACTION_KEY = "backaction";
    private static final String CONTROLKEY_KEY = "controlkey";
    private static final String FNKEY_KEY = "fnkey";
    private static final String IME_KEY = "ime";
    private static final String SHELL_KEY = "shell";
    private static final String INITIALCOMMAND_KEY = "initialcommand";
    private static final String TERMTYPE_KEY = "termtype";
    private static final String CLOSEONEXIT_KEY = "close_window_on_process_exit";
    private static final String VERIFYPATH_KEY = "verify_path";
    private static final String PATHEXTENSIONS_KEY = "do_path_extensions";
    private static final String PATHPREPEND_KEY = "allow_prepend_path";
    private static final String HOMEPATH_KEY = "home_path";
    private static final String ALT_SENDS_ESC = "alt_sends_esc";
    private static final String MOUSE_TRACKING = "mouse_tracking";
    private static final String TERMINAL_BEEP = "terminal_beep";
    private static final String USE_KEYBOARD_SHORTCUTS = "use_keyboard_shortcuts";
    private static final String CURSOR_BLINK_KEY = "cursorBlink";
    private static final String CURSOR_BLINK_PERIOD_KEY = "cursorBlinkPeriod";

    private static final String[] SU_LOCATIONS = {"/system/bin/su", "/system/xbin/su"};

    public static final int WHITE = 0xffffffff;
    public static final int BLACK = 0xff000000;
    public static final int BLUE = 0xff1976d2;
    public static final int GREEN = 0xff388e3c;
    public static final int AMBER = 0xffffa000;
    public static final int RED = 0xffd32f2f;
    public static final int HOLO_BLUE = 0xff33b5e5;
    public static final int SOLARIZED_FG = 0xff657b83;
    public static final int SOLARIZED_BG = 0xfffdf6e3;
    public static final int SOLARIZED_DARK_FG = 0xff839496;
    public static final int SOLARIZED_DARK_BG = 0xff002b36;
    public static final int MONOKAI_BG = 0xff262626;
    public static final int LINUX_CONSOLE_WHITE = 0xffaaaaaa;

    // foreground color, background color
    public static final int[][] COLOR_SCHEMES = {{BLACK, WHITE},
            {WHITE, BLACK}, {WHITE, BLUE}, {GREEN, BLACK},
            {AMBER, BLACK}, {RED, BLACK}, {HOLO_BLUE, BLACK},
            {SOLARIZED_FG, SOLARIZED_BG},
            {SOLARIZED_DARK_FG, SOLARIZED_DARK_BG},
            {LINUX_CONSOLE_WHITE, BLACK}, {WHITE, MONOKAI_BG}};

    public static final int ACTION_BAR_MODE_NONE = 0;
    public static final int ACTION_BAR_MODE_ALWAYS_VISIBLE = 1;
    public static final int ACTION_BAR_MODE_HIDES = 2;
    private static final int ACTION_BAR_MODE_MAX = 2;

//    public static final int ORIENTATION_UNSPECIFIED = 0;
//    public static final int ORIENTATION_LANDSCAPE = 1;
//    public static final int ORIENTATION_PORTRAIT = 2;

    /**
     * An integer not in the range of real key codes.
     */
    public static final int KEYCODE_NONE = -1;

    public static final int CONTROL_KEY_ID_NONE = 7;
    public static final int[] CONTROL_KEY_SCHEMES = {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_AT,
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT,
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_CAMERA, KEYCODE_NONE};

    public static final int FN_KEY_ID_NONE = 7;
    public static final int[] FN_KEY_SCHEMES = {KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_AT, KeyEvent.KEYCODE_ALT_LEFT,
            KeyEvent.KEYCODE_ALT_RIGHT, KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_CAMERA, KEYCODE_NONE};

    public static final int BACK_KEY_STOPS_SERVICE = 0;
    public static final int BACK_KEY_CLOSES_WINDOW = 1;
    public static final int BACK_KEY_CLOSES_ACTIVITY = 2;
    public static final int BACK_KEY_SENDS_ESC = 3;
    public static final int BACK_KEY_SENDS_TAB = 4;
    private static final int BACK_KEY_MAX = 4;

    public TermSettings(Resources res, SharedPreferences prefs) {
        readDefaultPrefs(res);
        readPrefs(prefs);
    }

    private void readDefaultPrefs(Resources res) {

        mUseExtShell = res.getBoolean(R.bool.pref_external_use_login);
        mUseExtProfile = res.getBoolean(R.bool.pref_external_use_profile);

        mStatusBar = Integer.parseInt(res
                .getString(R.string.pref_statusbar_default));
        mActionBarMode = res.getInteger(R.integer.pref_actionbar_default);
        mOrientation = res.getInteger(R.integer.pref_orientation_default);
        mCursorStyle = Integer.parseInt(res
                .getString(R.string.pref_cursorstyle_default));
        mCursorBlink = Integer.parseInt(res
                .getString(R.string.pref_cursorblink_default));
        mCursorBlinkPeriod = Integer.parseInt(res.getString(R.string.pref_cursorblink_period_default));
        mFontSize = Integer.parseInt(res
                .getString(R.string.pref_fontsize_default));
        mColorId = Integer.parseInt(res.getString(R.string.pref_color_default));
        mUTF8ByDefault = res.getBoolean(R.bool.pref_utf8_by_default_default);
        mBackKeyAction = Integer.parseInt(res
                .getString(R.string.pref_backaction_default));
        mControlKeyId = Integer.parseInt(res
                .getString(R.string.pref_controlkey_default));
        mFnKeyId = Integer.parseInt(res.getString(R.string.pref_fnkey_default));
        mUseCookedIME = Integer.parseInt(res
                .getString(R.string.pref_ime_default));
        mFailsafeShell = res.getString(R.string.pref_shell_default);
        mShell = mFailsafeShell;
        mInitialCommand = res.getString(R.string.pref_initialcommand_default);
        mTermType = res.getString(R.string.pref_termtype_default);
        mCloseOnExit = res
                .getBoolean(R.bool.pref_close_window_on_process_exit_default);
        mVerifyPath = res.getBoolean(R.bool.pref_verify_path_default);
        mDoPathExtensions = res
                .getBoolean(R.bool.pref_do_path_extensions_default);
        mAllowPathPrepend = res
                .getBoolean(R.bool.pref_allow_prepend_path_default);
        // the mHomePath default is set dynamically in readPrefs()
        mAltSendsEsc = res.getBoolean(R.bool.pref_alt_sends_esc_default);
        mMouseTracking = res.getBoolean(R.bool.pref_mouse_tracking_default);
        mTerminalBeep = res.getBoolean(R.bool.pref_terminal_beep_default);
        mUseKeyboardShortcuts = res
                .getBoolean(R.bool.pref_use_keyboard_shortcuts_default);

        mStartOnBoot = res.getBoolean(R.bool.pref_start_on_boot);
        mAutoSu = res.getBoolean(R.bool.pref_auto_su);
    }

    public void readPrefs(SharedPreferences prefs) {
        mPrefs = prefs;

        mUseExtShell = readBooleanPref(USE_EXT_SHELL_LOGIN_KEY, mUseExtShell);
        mUseExtProfile = readBooleanPref(USE_EXT_PROFILE_LOGIN_KEY,
                mUseExtProfile);
        mCursorBlink = readIntPref(CURSOR_BLINK_KEY, mCursorBlink, 1);
        mCursorBlinkPeriod = readIntPref(CURSOR_BLINK_PERIOD_KEY, mCursorBlinkPeriod, 1000);
        mStatusBar = readIntPref(STATUSBAR_KEY, mStatusBar, 1);
        mActionBarMode = readIntPref(ACTIONBAR_KEY, mActionBarMode,
                ACTION_BAR_MODE_MAX);
        mOrientation = readIntPref(ORIENTATION_KEY, mOrientation, 2);
        mFontSize = readIntPref(FONTSIZE_KEY, mFontSize, 288);
        mColorId = readIntPref(COLOR_KEY, mColorId, COLOR_SCHEMES.length - 1);
        mUTF8ByDefault = readBooleanPref(UTF8_KEY, mUTF8ByDefault);
        mBackKeyAction = readIntPref(BACKACTION_KEY, mBackKeyAction,
                BACK_KEY_MAX);
        mControlKeyId = readIntPref(CONTROLKEY_KEY, mControlKeyId,
                CONTROL_KEY_SCHEMES.length - 1);
        mFnKeyId = readIntPref(FNKEY_KEY, mFnKeyId, FN_KEY_SCHEMES.length - 1);
        mUseCookedIME = readIntPref(IME_KEY, mUseCookedIME, 1);
        mShell = readStringPref(SHELL_KEY, mShell);
        mAutoSu = readBooleanPref(AUTO_SU, mAutoSu);
        mInitialCommand = createInitialCommand(mAutoSu, mUseExtShell,
                mUseExtProfile);
        mTermType = readStringPref(TERMTYPE_KEY, mTermType);
        mCloseOnExit = readBooleanPref(CLOSEONEXIT_KEY, mCloseOnExit);
        mVerifyPath = readBooleanPref(VERIFYPATH_KEY, mVerifyPath);
        mDoPathExtensions = readBooleanPref(PATHEXTENSIONS_KEY,
                mDoPathExtensions);
        mAllowPathPrepend = readBooleanPref(PATHPREPEND_KEY, mAllowPathPrepend);
        mHomePath = readStringPref(HOMEPATH_KEY, mHomePath);
        mAltSendsEsc = readBooleanPref(ALT_SENDS_ESC, mAltSendsEsc);
        mMouseTracking = readBooleanPref(MOUSE_TRACKING, mMouseTracking);
        mTerminalBeep = readBooleanPref(TERMINAL_BEEP, mTerminalBeep);
        mUseKeyboardShortcuts = readBooleanPref(USE_KEYBOARD_SHORTCUTS,
                mUseKeyboardShortcuts);

        mStartOnBoot = readBooleanPref(START_ON_BOOT, mStartOnBoot);

        mPrefs = null; // we leak a Context if we hold on to this
    }

    private String createInitialCommand(boolean autoSu, boolean extShell,
                                        boolean extProfile) {
        StringBuilder builder = new StringBuilder();
        if (autoSu) {
            String su = findSu();
            if (su != null) {
                builder.append("exec ");
                builder.append(su);
            }
        }
        if (extShell) {
            builder.append("\nexec ");
            builder.append(readStringPref(EXT_SHELL_KEY, mShell));
        }
        if (extProfile) {
            builder.append("\n. ");
            builder.append(readStringPref(EXT_PROFILE_KEY, "/system/etc/bash.bashrc"));
        }

        builder.append("\n");
        builder.append(readStringPref(INITIALCOMMAND_KEY, mInitialCommand));
        return builder.toString().trim();
    }

    private String findSu() {
        for (String su : SU_LOCATIONS) {
            if (new File(su).canExecute()) {
                return su;
            }
        }
        return null;
    }

    private int readIntPref(String key, int defaultValue, int maxValue) {
        int val;
        try {
            val = Integer.parseInt(mPrefs.getString(key,
                    Integer.toString(defaultValue)));
        } catch (NumberFormatException e) {
            val = defaultValue;
        }
        val = Math.max(0, Math.min(val, maxValue));
        return val;
    }

    private String readStringPref(String key, String defaultValue) {
        return mPrefs.getString(key, defaultValue);
    }

    private boolean readBooleanPref(String key, boolean defaultValue) {
        return mPrefs.getBoolean(key, defaultValue);
    }

    public boolean showStatusBar() {
        return (mStatusBar != 0);
    }

    public int actionBarMode() {
        return mActionBarMode;
    }

    public int getScreenOrientation() {
        return mOrientation;
    }

    public int getCursorStyle() {
        return mCursorStyle;
    }

    public int getCursorBlink() {
        return mCursorBlink;
    }

    public int getCursorBlinkPeriod() {
        return mCursorBlinkPeriod;
    }

    public int getFontSize() {
        return mFontSize;
    }

    public int[] getColorScheme() {
        return COLOR_SCHEMES[mColorId];
    }

    public boolean defaultToUTF8Mode() {
        return mUTF8ByDefault;
    }

    public int getBackKeyAction() {
        return mBackKeyAction;
    }

    public boolean backKeySendsCharacter() {
        return mBackKeyAction >= BACK_KEY_SENDS_ESC;
    }

    public boolean getAltSendsEscFlag() {
        return mAltSendsEsc;
    }

    public boolean getMouseTrackingFlag() {
        return mMouseTracking;
    }

    public boolean getTerminalBeepFlag() {
        return mTerminalBeep;
    }

    public boolean getUseKeyboardShortcutsFlag() {
        return mUseKeyboardShortcuts;
    }

    public int getBackKeyCharacter() {
        switch (mBackKeyAction) {
            case BACK_KEY_SENDS_ESC:
                return 27;
            case BACK_KEY_SENDS_TAB:
                return 9;
            default:
                return 0;
        }
    }

    public int getControlKeyId() {
        return mControlKeyId;
    }

    public int getFnKeyId() {
        return mFnKeyId;
    }

    public int getControlKeyCode() {
        return CONTROL_KEY_SCHEMES[mControlKeyId];
    }

    public int getFnKeyCode() {
        return FN_KEY_SCHEMES[mFnKeyId];
    }

    public boolean useCookedIME() {
        return (mUseCookedIME != 0);
    }

    public String getShell() {
        return mShell;
    }

    public String getFailsafeShell() {
        return mFailsafeShell;
    }

    public String getInitialCommand() {
        return mInitialCommand;
    }

    public String getTermType() {
        return mTermType;
    }

    public boolean closeWindowOnProcessExit() {
        return mCloseOnExit;
    }

    public boolean verifyPath() {
        return mVerifyPath;
    }

    public boolean doPathExtensions() {
        return mDoPathExtensions;
    }

    public boolean allowPathPrepend() {
        return mAllowPathPrepend;
    }

    public void setPrependPath(String prependPath) {
        mPrependPath = prependPath;
    }

    public String getPrependPath() {
        return mPrependPath;
    }

    public void setAppendPath(String appendPath) {
        mAppendPath = appendPath;
    }

    public String getAppendPath() {
        return mAppendPath;
    }

    public void setHomePath(String homePath) {
        mHomePath = homePath;
    }

    public String getHomePath() {
        return mHomePath;
    }

    public boolean startOnBoot() {
        return mStartOnBoot;
    }
}
