package com.kiva.termit;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

/**
 * @author Kiva
 * @date 2015/12/25
 */
public class TermCaller {
    private String action;
    private String extraWindowTitle, extraCommand, extraWindowHandle;
    private Intent myIntent;

    private TermCaller() { /* private for Builder */}

    private void setUpIntent() {
        if (myIntent != null || TextUtils.isEmpty(action)) {
            return;
        }

        myIntent = new Intent(action);

        if (!TextUtils.isEmpty(extraWindowTitle)) {
            myIntent.putExtra(TermConstant.EXTRA_WINDOW_TITLE, extraWindowTitle);
        }

        if (!TextUtils.isEmpty(extraCommand)) {
            myIntent.putExtra(TermConstant.EXTRA_INITIAL_COMMAND, extraCommand);
        }

        if (!TextUtils.isEmpty(extraWindowHandle)) {
            myIntent.putExtra(TermConstant.EXTRA_WINDOW_HANDLE, extraWindowHandle);
        }
    }

    public Intent getIntent() {
        setUpIntent();
        return myIntent;
    }

    public String getExtraWindowTitle() {
        return extraWindowTitle;
    }

    public String getExtraCommand() {
        return extraCommand;
    }

    public String getExtraWindowHandle() {
        return extraWindowHandle;
    }

    public String getAction() {
        return action;
    }

    public static class Builder {
        private final TermCaller caller;

        protected Builder() {
            caller = new TermCaller();
        }

        public Builder runScript() {
            caller.action = TermConstant.ACTION_RUN_SCRIPT;
            return this;
        }

        public Builder windowTitle(String title) {
            caller.extraWindowHandle = null;
            caller.extraWindowTitle = title;
            return this;
        }

        public Builder windowHandle(String handle) {
            caller.extraWindowTitle = null;
            caller.extraWindowHandle = handle;
            return this;
        }

        public Builder executeCommand(String cmd) {
            caller.extraCommand = cmd;
            return this;
        }

        public Builder appendTo(String windowHandle, String execCommand) {
            return runScript().windowHandle(windowHandle).executeCommand(execCommand);
        }

        public Builder newWindow(String windowTitle, String execCommand) {
            return runScript().windowTitle(windowTitle).executeCommand(execCommand);
        }

        public TermCaller build() {
            return caller;
        }
    }
}
