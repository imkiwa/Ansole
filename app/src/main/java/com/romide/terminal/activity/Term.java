/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.romide.terminal.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Toast;

import com.romide.terminal.R;
import com.romide.terminal.adapter.WindowListAdapter;
import com.romide.terminal.compat.ActivityCompat;
import com.romide.terminal.compat.AndroidCompat;
import com.romide.terminal.compat.MenuItemCompat;
import com.romide.terminal.emulatorview.EmulatorView;
import com.romide.terminal.emulatorview.TermSession;
import com.romide.terminal.emulatorview.TerminalEmulator.BellListener;
import com.romide.terminal.emulatorview.UpdateCallback;
import com.romide.terminal.emulatorview.compat.ClipboardManagerCompat;
import com.romide.terminal.emulatorview.compat.ClipboardManagerCompatFactory;
import com.romide.terminal.emulatorview.compat.KeycodeConstants;
import com.romide.terminal.service.TermService;
import com.romide.terminal.session.GenericTermSession;
import com.romide.terminal.session.SessionList;
import com.romide.terminal.session.ShellTermSession;
import com.romide.terminal.session.TermSettings;
import com.romide.terminal.util.TermDebug;
import com.romide.terminal.util.UiKit;
import com.romide.terminal.view.TermView;
import com.romide.terminal.view.TermViewFlipper;

import java.io.IOException;
import java.text.Collator;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * A terminal emulator activity.
 */

public class Term extends ActivityBase implements UpdateCallback {
    /**
     * The ViewFlipper which holds the collection of EmulatorView widgets.
     */
    private TermViewFlipper mViewFlipper;

    /**
     * The name of the ViewFlipper in the resources.
     */
    private static final int VIEW_FLIPPER = R.id.view_flipper;

    private static final int TERM_LIST = R.id.term_list;

    private static final int TERM_MENU_COUNT = 5;
    private TermMenuItem[] mTermMenuItems = new TermMenuItem[TERM_MENU_COUNT];

    private SessionList mTermSessions;
    private AppCompatSpinner mTermList;

    private SharedPreferences mPrefs;
    private TermSettings mSettings;

    private final static int SELECT_TEXT_ID = 0;
    private final static int COPY_ALL_ID = 1;
    private final static int PASTE_ID = 2;
    private final static int SEND_CONTROL_KEY_ID = 3;
    private final static int SEND_FN_KEY_ID = 4;

    private boolean mAlreadyStarted = false;
    private boolean mStopServiceOnFinish = false;

    private Intent mTermServiceIntent;

    public static final int REQUEST_CHOOSE_WINDOW = 1;
    public static final String EXTRA_WINDOW_ID = "terminal.window_id";
    private int onResumeSelectWindow = -1;

    private PowerManager.WakeLock mWakeLock;

    private static final String ACTION_PATH_BROADCAST = "terminal.broadcast.APPEND_TO_PATH";
    private static final String ACTION_PATH_PREPEND_BROADCAST = "terminal.broadcast.PREPEND_TO_PATH";
    private static final String PERMISSION_PATH_BROADCAST = "terminal.permission.APPEND_TO_PATH";
    private static final String PERMISSION_PATH_PREPEND_BROADCAST = "terminal.permission.PREPEND_TO_PATH";
    private int mPendingPathBroadcasts = 0;
    private BroadcastReceiver mPathReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String path = makePathFromBundle(getResultExtras(false));
            if (intent.getAction().equals(ACTION_PATH_PREPEND_BROADCAST)) {
                mSettings.setPrependPath(path);
            } else {
                mSettings.setAppendPath(path);
            }
            mPendingPathBroadcasts--;

            if (mPendingPathBroadcasts <= 0 && mTermService != null) {
                populateViewFlipper();
                populateWindowList();
            }
        }
    };
    // Available on API 12 and later
    private static final int FLAG_INCLUDE_STOPPED_PACKAGES = 0x20;

    private TermService mTermService;
    private ServiceConnection mTermServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TermDebug.LOG_TAG, "Connected to service");
            TermService.TSBinder binder = (TermService.TSBinder) service;
            mTermService = binder.getService();
            if (mPendingPathBroadcasts <= 0) {
                populateViewFlipper();
                populateWindowList();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mTermService = null;
        }
    };

    private SoundPool mSoundPoll;
    private int mBellSoundId;
    private long lastBellTime = System.currentTimeMillis();
    private Runnable mBellRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSoundPoll != null) {
                mSoundPoll.play(mBellSoundId, 1, 1, 0, 0, 1);
            }
        }
    };
    private BellListener mBellListener = new BellListener() {
        @Override
        public void onBell() {
            long now = System.currentTimeMillis();
            if (now - lastBellTime > 100) {
                lastBellTime = now;
                UiKit.get().post(mBellRunnable);
            }
        }
    };

    private ActionBar mActionBar;
    private int mActionBarMode = TermSettings.ACTION_BAR_MODE_NONE;

    private WindowListAdapter mWinListAdapter;

    private class WindowListActionBarAdapter extends WindowListAdapter
            implements UpdateCallback {

        public WindowListActionBarAdapter(Context ctx, SessionList sessions) {
            super(ctx, sessions);
        }

        @SuppressWarnings("ResourceType")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AppCompatTextView label = (AppCompatTextView) LayoutInflater.from(Term.this).inflate(R.layout.window_list_item_actionbar, null, false);

            String title = getSessionTitle(position,
                    getString(R.string.window_title, position + 1));
            label.setText(title);
            return label;
        }

        @Override
        public View getDropDownView(int position, View convertView,
                                    ViewGroup parent) {
            return super.getView(position, convertView, parent);
        }

        @Override
        public void onUpdate() {
            notifyDataSetChanged();
            if (mTermList != null) {
                mTermList.setSelection(mViewFlipper
                        .getDisplayedChild());
            }
        }
    }

    private boolean mHaveFullHwKeyboard = false;

    private class EmulatorViewGestureListener extends SimpleOnGestureListener {
        private EmulatorView view;

        public EmulatorViewGestureListener(EmulatorView view) {
            this.view = view;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            // Let the EmulatorView handle taps if mouse tracking is active
            if (view.isMouseTrackingActive())
                return false;

            // Check for link at tap location
            String link = view.getURLat(e.getX(), e.getY());
            if (link != null)
                execURL(link);
            else
                doUIToggle((int) e.getX(), (int) e.getY(),
                        view.getVisibleWidth(), view.getVisibleHeight());
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            float absVelocityX = Math.abs(velocityX);
            float absVelocityY = Math.abs(velocityY);
            if (absVelocityX > Math.max(1000.0f, 2.0 * absVelocityY)) {
                // Assume user wanted side to side movement
                if (velocityX > 0) {
                    // Left to right swipe -- previous window
                    mViewFlipper.showPrevious();
                } else {
                    // Right to left swipe -- next window
                    mViewFlipper.showNext();
                }
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Should we use keyboard shortcuts?
     */
    private boolean mUseKeyboardShortcuts;

    /**
     * Intercepts keys before the view/terminal gets it.
     */
    private View.OnKeyListener mKeyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            return backkeyInterceptor(keyCode, event)
                    || keyboardShortcuts(keyCode, event);
        }

        /**
         * Keyboard shortcuts (tab management, paste)
         */
        private boolean keyboardShortcuts(int keyCode, KeyEvent event) {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }
            if (!mUseKeyboardShortcuts) {
                return false;
            }
            boolean isCtrlPressed = (event.getMetaState() & KeycodeConstants.META_CTRL_ON) != 0;
            boolean isShiftPressed = (event.getMetaState() & KeycodeConstants.META_SHIFT_ON) != 0;

            if (keyCode == KeycodeConstants.KEYCODE_TAB && isCtrlPressed) {
                if (isShiftPressed) {
                    mViewFlipper.showPrevious();
                } else {
                    mViewFlipper.showNext();
                }

                return true;
            } else if (keyCode == KeycodeConstants.KEYCODE_N && isCtrlPressed
                    && isShiftPressed) {
                doCreateNewWindow();

                return true;
            } else if (keyCode == KeycodeConstants.KEYCODE_V && isCtrlPressed
                    && isShiftPressed) {
                doPaste();

                return true;
            } else {
                return false;
            }
        }

        /**
         * Make sure the back button always leaves the application.
         */
        private boolean backkeyInterceptor(int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK
                    && mActionBarMode == TermSettings.ACTION_BAR_MODE_HIDES
                    && mActionBar.isShowing()) {
                /*
                 * We need to intercept the key event before the view sees it,
				 * otherwise the view will handle it before we get it
				 */
                onKeyUp(keyCode, event);
                return true;
            } else {
                return false;
            }
        }
    };

    @SuppressWarnings("deprecation")
    @SuppressLint("InlinedApi")
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.d(TermDebug.LOG_TAG, "Terminal started");

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSettings = new TermSettings(getResources(), mPrefs);

        Intent broadcast = new Intent(ACTION_PATH_BROADCAST);
        if (AndroidCompat.SDK >= 12) {
            broadcast.addFlags(FLAG_INCLUDE_STOPPED_PACKAGES);
        }
        mPendingPathBroadcasts++;
        sendOrderedBroadcast(broadcast, PERMISSION_PATH_BROADCAST,
                mPathReceiver, null, RESULT_OK, null, null);

        broadcast = new Intent(broadcast);
        broadcast.setAction(ACTION_PATH_PREPEND_BROADCAST);
        mPendingPathBroadcasts++;
        sendOrderedBroadcast(broadcast, PERMISSION_PATH_PREPEND_BROADCAST,
                mPathReceiver, null, RESULT_OK, null, null);

        mActionBarMode = mSettings.actionBarMode();

        setContentView(R.layout.term_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTermMenuItems[0] = new TermMenuItem(SELECT_TEXT_ID, getString(R.string.select_text));
        mTermMenuItems[1] = new TermMenuItem(COPY_ALL_ID, getString(R.string.copy_all));
        mTermMenuItems[2] = new TermMenuItem(PASTE_ID, getString(R.string.paste));
        mTermMenuItems[3] = new TermMenuItem(SEND_CONTROL_KEY_ID, getString(R.string.send_control_key));
        mTermMenuItems[4] = new TermMenuItem(SEND_FN_KEY_ID, getString(R.string.send_fn_key));

        mViewFlipper = (TermViewFlipper) findViewById(VIEW_FLIPPER);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                TermDebug.LOG_TAG);

        ActionBar actionBar = getSupportActionBar(); /*ActivityCompat.getActionBar(this);*/
        if (actionBar != null) {
            mActionBar = actionBar;
            actionBar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
            if (mActionBarMode == TermSettings.ACTION_BAR_MODE_HIDES) {
                actionBar.hide();
            }
        }

        mHaveFullHwKeyboard = checkHaveFullHwKeyboard(getResources()
                .getConfiguration());

        // ///////////////////////////////////////
        mSoundPoll = new SoundPool(1, AudioManager.STREAM_SYSTEM, 0);
        mBellSoundId = mSoundPoll.load(this, R.raw.bell, 1);
        mTermServiceIntent = new Intent(this, TermService.class);
        startService(mTermServiceIntent);

        if (!bindService(mTermServiceIntent, mTermServiceConnection, BIND_AUTO_CREATE)) {
            Log.e(TermDebug.LOG_TAG, "could not start service");
        }
        // ///////////////////////////////////////

        updatePrefs();
        mAlreadyStarted = true;
    }

    private String makePathFromBundle(Bundle extras) {
        if (extras == null || extras.size() == 0) {
            return "";
        }

        String[] keys = new String[extras.size()];
        keys = extras.keySet().toArray(keys);
        Collator collator = Collator.getInstance(Locale.US);
        Arrays.sort(keys, collator);

        StringBuilder path = new StringBuilder();
        for (String key : keys) {
            String dir = extras.getString(key);
            if (dir != null && !dir.equals("")) {
                path.append(dir);
                path.append(":");
            }
        }

        return path.substring(0, path.length() - 1);
    }

    private void populateViewFlipper() {
        if (mTermService != null) {
            mTermSessions = mTermService.getSessions();
            mTermSessions.addCallback(this);

            if (mTermSessions.size() == 0) {
                try {
                    mTermSessions.add(createTermSession());
                } catch (IOException e) {
                    Toast.makeText(this, "Failed to start terminal session", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }

            for (TermSession session : mTermSessions) {
                EmulatorView view = createEmulatorView(session);
                mViewFlipper.addView(view);
            }

            updatePrefs();

            Intent intent = getIntent();
            int flags = intent.getFlags();
            String action = intent.getAction();
            if ((flags & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0
                    && action != null) {
                if (action.equals(RemoteInterface.PRIVATE_OPEN_NEW_WINDOW)) {
                    mViewFlipper.setDisplayedChild(mTermSessions.size() - 1);
                } else if (action.equals(RemoteInterface.PRIVATE_SWITCH_WINDOW)) {
                    int target = intent.getIntExtra(
                            RemoteInterface.PRIVATE_EXTRA_TARGET_WINDOW, -1);
                    if (target >= 0) {
                        mViewFlipper.setDisplayedChild(target);
                    }
                }
            }

            mViewFlipper.resumeCurrentView();
        }
    }

    private void populateWindowList() {
        if (mActionBar == null) {
            // Not needed
            return;
        }

        if (mTermSessions != null) {
            int position = mViewFlipper.getDisplayedChild();
            WindowListAdapter adapter = mWinListAdapter;
            if (adapter == null) {
                adapter = new WindowListActionBarAdapter(this, mTermSessions);
                mWinListAdapter = adapter;

                if (mTermList == null) {
                    mTermList = (AppCompatSpinner) findViewById(TERM_LIST);
                    mTermList.setAdapter(mWinListAdapter);
                    mTermList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            int oldPosition = mViewFlipper.getDisplayedChild();
                            if (position != oldPosition) {
                                mViewFlipper.setDisplayedChild(position);

                                if (mActionBarMode == TermSettings.ACTION_BAR_MODE_HIDES) {
                                    mActionBar.hide();
                                }
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });
                }

                SessionList sessions = mTermSessions;
                sessions.addCallback(adapter);
                sessions.addTitleChangedListener(adapter);
                mViewFlipper.addCallback(adapter);
            } else {
                adapter.setSessions(mTermSessions);
            }
            mTermList.setSelection(position);
        }
    }

    @SuppressLint("Wakelock")
    @Override
    public void onDestroy() {
        super.onDestroy();
        mViewFlipper.removeAllViews();
        unbindService(mTermServiceConnection);
        if (mStopServiceOnFinish) {
            stopService(mTermServiceIntent);
        }
        mTermService = null;
        mTermServiceConnection = null;
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    public static Intent createTermIntent(Context context) {
        Intent intent = new Intent(context, Term.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }

    protected static TermSession createTermSession(Context context,
                                                   TermSettings settings, String initialCommand) throws IOException {
        GenericTermSession session = new ShellTermSession(settings,
                initialCommand);
        session.setProcessExitMessage(context
                .getString(R.string.process_exit_message));

        return session;
    }

    private TermSession createTermSession() throws IOException {
        TermSettings settings = mSettings;
        TermSession session = createTermSession(this, settings,
                settings.getInitialCommand());
        session.setFinishCallback(mTermService);
        if (mSettings.getTerminalBeepFlag()) {
            session.setBellListener(mBellListener);
        }
        return session;
    }

    private TermView createEmulatorView(TermSession session) {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        final TermView emulatorView = new TermView(this, session, metrics, null);

        emulatorView.setExtGestureListener(new EmulatorViewGestureListener(
                emulatorView));
        emulatorView.setOnKeyListener(mKeyListener);
        registerForContextMenu(emulatorView);

        return emulatorView;
    }

    public TermSession getCurrentTermSession() {
        SessionList sessions = mTermSessions;
        if (sessions == null) {
            return null;
        } else {
            return sessions.get(mViewFlipper.getDisplayedChild());
        }
    }

    private EmulatorView getCurrentEmulatorView() {
        return (EmulatorView) mViewFlipper.getCurrentView();
    }

    @SuppressWarnings("ResourceType")
    private void updatePrefs() {
        mUseKeyboardShortcuts = mSettings.getUseKeyboardShortcutsFlag();

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        mViewFlipper.updatePrefs(mSettings);

        for (View v : mViewFlipper) {
            ((EmulatorView) v).setDensity(metrics);
            ((TermView) v).updatePrefs(mSettings);
        }

        if (mTermSessions != null) {
            for (TermSession session : mTermSessions) {
                ((GenericTermSession) session).updatePrefs(mSettings);
            }
        }

        {
            Window win = getWindow();
            WindowManager.LayoutParams params = win.getAttributes();
            final int FULLSCREEN = WindowManager.LayoutParams.FLAG_FULLSCREEN;
            int desiredFlag = mSettings.showStatusBar() ? 0 : FULLSCREEN;
            if (desiredFlag != (params.flags & FULLSCREEN)
                    || (AndroidCompat.SDK >= 11 && mActionBarMode != mSettings
                    .actionBarMode())) {
                if (mAlreadyStarted) {
                    // Can't switch to/from fullscreen after
                    // starting the activity.
                    // comment next line if we use AppCompatActivity
                    // restart();
                } else {
                    win.setFlags(desiredFlag, FULLSCREEN);
                    if (mActionBarMode == TermSettings.ACTION_BAR_MODE_HIDES) {
                        mActionBar.hide();
                    }
                }
            }
        }

        switch (mSettings.getScreenOrientation()) {
            case 0:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                break;
            case 1:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case 2:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        SessionList sessions = mTermSessions;
        TermViewFlipper viewFlipper = mViewFlipper;
        if (sessions != null) {
            sessions.addCallback(this);
            WindowListAdapter adapter = mWinListAdapter;
            if (adapter != null) {
                sessions.addCallback(adapter);
                sessions.addTitleChangedListener(adapter);
                viewFlipper.addCallback(adapter);
            }
        }
        if (sessions != null && sessions.size() < viewFlipper.getChildCount()) {
            for (int i = 0; i < viewFlipper.getChildCount(); ++i) {
                EmulatorView v = (EmulatorView) viewFlipper.getChildAt(i);
                if (!sessions.contains(v.getTermSession())) {
                    v.onPause();
                    viewFlipper.removeView(v);
                    --i;
                }
            }
        }

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        // the HOME dir needs to be set here since it comes from Context
        SharedPreferences.Editor editor = mPrefs.edit();
        String defValue = getDir("HOME", MODE_PRIVATE).getAbsolutePath();
        String homePath = mPrefs.getString("home_path", defValue);
        editor.putString("home_path", homePath);
        editor.apply();

        mSettings.readPrefs(mPrefs);
        updatePrefs();

        if (onResumeSelectWindow >= 0) {
            viewFlipper.setDisplayedChild(onResumeSelectWindow);
            onResumeSelectWindow = -1;
        }
        viewFlipper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        SessionList sessions = mTermSessions;
        TermViewFlipper viewFlipper = mViewFlipper;

        viewFlipper.onPause();
        if (sessions != null) {
            sessions.removeCallback(this);
            WindowListAdapter adapter = mWinListAdapter;
            if (adapter != null) {
                sessions.removeCallback(adapter);
                sessions.removeTitleChangedListener(adapter);
                viewFlipper.removeCallback(adapter);
            }
        }

		/*
         * Explicitly close the input method Otherwise, the soft keyboard could
		 * cover up whatever activity takes our place
		 */
        final IBinder token = viewFlipper.getWindowToken();
        new Thread() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(token, 0);
            }
        }.start();
    }


    private boolean checkHaveFullHwKeyboard(Configuration c) {
        return (c.keyboard == Configuration.KEYBOARD_QWERTY)
                && (c.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mHaveFullHwKeyboard = checkHaveFullHwKeyboard(newConfig);

        EmulatorView v = (EmulatorView) mViewFlipper.getCurrentView();
        if (v != null) {
            v.updateSize(false);
        }

        if (mWinListAdapter != null) {
            // Force Android to redraw the label in the navigation dropdown
            mWinListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItemCompat.setShowAsAction(menu.findItem(R.id.menu_new_window),
                MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        MenuItemCompat.setShowAsAction(menu.findItem(R.id.menu_close_window),
                MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_preferences) {
            doPreferences();
        } else if (id == R.id.menu_new_window) {
            doCreateNewWindow();
        } else if (id == R.id.menu_close_window) {
            confirmCloseWindow();
        } else if (id == R.id.menu_reset) {
            doResetTerm();
        } else if (id == R.id.menu_toggle_wakelock) {
            doToggleWakeLock();
        } else if (id == R.id.menu_title) {
            doRenameWindow();
        } else if (id == R.id.menu_window_list) {
            startActivityForResult(new Intent(this, WindowListActivity.class),
                    REQUEST_CHOOSE_WINDOW);
        }
        // Hide the action bar if appropriate
        if (mActionBarMode == TermSettings.ACTION_BAR_MODE_HIDES) {
            mActionBar.hide();
        }
        return super.onOptionsItemSelected(item);
    }

    private void doCreateNewWindow() {
        if (mTermSessions == null) {
            Log.w(TermDebug.LOG_TAG, "Couldn't create new window because mTermSessions == null");
            return;
        }

        try {
            TermSession session = createTermSession();

            mTermSessions.add(session);

            TermView view = createEmulatorView(session);
            view.updatePrefs(mSettings);

            mViewFlipper.addView(view);
            mViewFlipper.setDisplayedChild(mViewFlipper.getChildCount() - 1);
        } catch (IOException e) {
            Toast.makeText(this, "Failed to create a session", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmCloseWindow() {
        TermSession termSession = getCurrentTermSession();

        if (termSession instanceof GenericTermSession) {
            GenericTermSession session = ((GenericTermSession) termSession);
            if (session.isShellClosed()) {
                doCloseWindow();
                return;
            }
        }

        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.confirm_window_close_message)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                UiKit.get().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        doCloseWindow();
                                    }
                                });
                            }
                        })
                .setNegativeButton(android.R.string.no, null)
                .show();

    }

    public void doResetTerm() {
        TermSession ts = getCurrentTermSession();
        if (ts == null) {
            return;
        }

        ts.reset();
        Toast.makeText(this, R.string.reset_toast_notification,
                Toast.LENGTH_SHORT).show();
    }

    private void doRenameWindow() {
        final AppCompatEditText editText = new AppCompatEditText(this);
        editText.setText(getCurrentTermSession().getTitle());
        final AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(R.string.input_window_title);
        b.setView(editText);
        b.setPositiveButton(android.R.string.yes,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        doChangeWindowTitle(editText.getText().toString());
                    }
                });
        b.setNegativeButton(android.R.string.no, null);
        b.show();
    }

    private void doChangeWindowTitle(final String title) {
        final Runnable typeName = new Runnable() {
            @Override
            public void run() {
                TermSession ts = getCurrentTermSession();
                if (ts == null) {
                    return;
                }
                ts.setTitle(title);
            }
        };
        UiKit.get().post(typeName);
    }

    private void doCloseWindow() {
        if (mTermSessions == null) {
            return;
        }

        EmulatorView view = getCurrentEmulatorView();
        if (view == null) {
            return;
        }
        TermSession session = mTermSessions.remove(mViewFlipper
                .getDisplayedChild());
        view.onPause();
        session.finish();
        mViewFlipper.removeView(view);
        if (mTermSessions.size() == 0) {
            mStopServiceOnFinish = true;
            finish();
        } else {
            mViewFlipper.showNext();
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        switch (request) {
            case REQUEST_CHOOSE_WINDOW:
                if (result == RESULT_OK && data != null) {
                    int position = data.getIntExtra(EXTRA_WINDOW_ID, -2);
                    if (position >= 0) {
                        // Switch windows after session list is in sync, not here
                        onResumeSelectWindow = position;
                    } else if (position == -1) {
                        doCreateNewWindow();
                        onResumeSelectWindow = mTermSessions.size() - 1;
                    }
                } else {
                    // Close the activity if user closed all sessions
                    if (mTermSessions == null || mTermSessions.size() == 0) {
                        mStopServiceOnFinish = true;
                        finish();
                    }
                }
                break;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            // Don't repeat action if intent comes from history
            return;
        }

        String action = intent.getAction();
        if (action == null) {
            return;
        }

        if (action.equals(RemoteInterface.PRIVATE_OPEN_NEW_WINDOW)) {
            // New session was created, add an EmulatorView to match
            SessionList sessions = mTermSessions;
            if (sessions == null) {
                // Presumably populateViewFlipper() will do this later ...
                return;
            }
            int position = sessions.size() - 1;

            TermSession session = sessions.get(position);
            EmulatorView view = createEmulatorView(session);

            mViewFlipper.addView(view);
            onResumeSelectWindow = position;
        } else if (action.equals(RemoteInterface.PRIVATE_SWITCH_WINDOW)) {
            int target = intent.getIntExtra(
                    RemoteInterface.PRIVATE_EXTRA_TARGET_WINDOW, -1);
            if (target >= 0) {
                onResumeSelectWindow = target;
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.edit_text)
                .setItems(mTermMenuItems, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        int id = mTermMenuItems[which].getId();
                        switch (id) {
                            case SELECT_TEXT_ID:
                                getCurrentEmulatorView().toggleSelectingText();
                                break;
                            case COPY_ALL_ID:
                                doCopyAll();
                                break;
                            case PASTE_ID:
                                doPaste();
                                break;
                            case SEND_CONTROL_KEY_ID:
                                doSendControlKey();
                                break;
                            case SEND_FN_KEY_ID:
                                doSendFnKey();
                                break;
                        }
                    }
                })
                .setPositiveButton(android.R.string.no, null)
                .show();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem wakeLockItem = menu.findItem(R.id.menu_toggle_wakelock);
        if (mWakeLock.isHeld()) {
            wakeLockItem.setTitle(R.string.disable_wakelock);
        } else {
            wakeLockItem.setTitle(R.string.enable_wakelock);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (mActionBarMode == TermSettings.ACTION_BAR_MODE_HIDES
                        && mActionBar.isShowing()) {
                    mActionBar.hide();
                    return true;
                }

                switch (mSettings.getBackKeyAction()) {
                    case TermSettings.BACK_KEY_STOPS_SERVICE:
                        mStopServiceOnFinish = true;
                    case TermSettings.BACK_KEY_CLOSES_ACTIVITY:
                        finish();
                        return true;
                    case TermSettings.BACK_KEY_CLOSES_WINDOW:
                        doCloseWindow();
                        return true;
                    default:
                        return true;
                }
            case KeyEvent.KEYCODE_MENU:
                if (mActionBar != null && !mActionBar.isShowing()) {
                    mActionBar.show();
                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    // Called when the list of sessions changes
    @Override
    public void onUpdate() {
        SessionList sessions = mTermSessions;
        if (sessions == null) {
            return;
        }

        if (sessions.size() == 0) {
            mStopServiceOnFinish = true;
            finish();
        } else if (sessions.size() < mViewFlipper.getChildCount()) {
            for (int i = 0; i < mViewFlipper.getChildCount(); ++i) {
                EmulatorView v = (EmulatorView) mViewFlipper.getChildAt(i);
                if (!sessions.contains(v.getTermSession())) {
                    v.onPause();
                    mViewFlipper.removeView(v);
                    --i;
                }
            }
        }
    }

    private boolean canPaste() {
        ClipboardManagerCompat clip = ClipboardManagerCompatFactory
                .getManager(getApplicationContext());
        return clip.hasText();
    }

    private void doPreferences() {
        startActivity(new Intent(this, TermPreference.class));
    }

    private void doCopyAll() {
        ClipboardManagerCompat clip = ClipboardManagerCompatFactory
                .getManager(getApplicationContext());
        clip.setText(getCurrentTermSession().getTranscriptText().trim());
    }

    private void doPaste() {
        if (!canPaste()) {
            return;
        }
        ClipboardManagerCompat clip = ClipboardManagerCompatFactory
                .getManager(getApplicationContext());
        CharSequence paste = clip.getText();
        getCurrentTermSession().write(paste.toString());
    }

    private void doSendControlKey() {
        getCurrentEmulatorView().sendControlKey();
    }

    private void doSendFnKey() {
        getCurrentEmulatorView().sendFnKey();
    }

    private void doToggleSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    @SuppressLint("Wakelock")
    private void doToggleWakeLock() {
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        } else {
            mWakeLock.acquire();
        }
        ActivityCompat.invalidateOptionsMenu(this);
    }

    private void doToggleActionBar() {
        ActionBar bar = mActionBar;
        if (bar == null) {
            return;
        }
        if (bar.isShowing()) {
            bar.hide();
        } else {
            bar.show();
        }
    }

    private void doUIToggle(int x, int y, int width, int height) {
        switch (mActionBarMode) {
            case TermSettings.ACTION_BAR_MODE_NONE:
                if (AndroidCompat.SDK >= 11
                        && (mHaveFullHwKeyboard || y < height / 2)) {
                    openOptionsMenu();
                    return;
                } else {
                    doToggleSoftKeyboard();
                }
                break;
            case TermSettings.ACTION_BAR_MODE_ALWAYS_VISIBLE:
                if (!mHaveFullHwKeyboard) {
                    doToggleSoftKeyboard();
                }
                break;
            case TermSettings.ACTION_BAR_MODE_HIDES:
                if (mHaveFullHwKeyboard || y < height / 2) {
                    doToggleActionBar();
                    return;
                } else {
                    doToggleSoftKeyboard();
                }
                break;
        }
        getCurrentEmulatorView().requestFocus();
    }

    /**
     * Send a URL up to Android to be handled by a browser.
     *
     * @param link The URL to be opened.
     */
    private void execURL(String link) {
        Uri webLink = Uri.parse(link);
        final Intent openLink = new Intent(Intent.ACTION_VIEW, webLink);
        PackageManager pm = getPackageManager();
        final List<ResolveInfo> handlers = pm
                .queryIntentActivities(openLink, 0);
        final Runnable openLinkRunnable = new Runnable() {
            @Override
            public void run() {
                if (handlers.size() > 0)
                    startActivity(openLink);
            }
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.application_terminal)
                .setMessage(getString(R.string.open_link, link))
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                UiKit.get().post(openLinkRunnable);
                            }
                        }).setNegativeButton(android.R.string.no, null).show();
    }


    private class TermMenuItem implements CharSequence {
        private int id;
        private String title;

        public TermMenuItem(int id, String title) {
            this.id = id;
            this.title = title;
        }

        public int getId() {
            return id;
        }

        @Override
        public int length() {
            return title.length();
        }

        @Override
        public char charAt(int index) {
            return title.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return title.subSequence(start, end);
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
