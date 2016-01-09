/*
 * Copyright (C) 2012 Steven Luo
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

package com.romide.terminal.view;

import android.content.Context;
import android.util.DisplayMetrics;

import com.romide.terminal.emulatorview.ColorScheme;
import com.romide.terminal.emulatorview.EmulatorView;
import com.romide.terminal.emulatorview.TermSession;
import com.romide.terminal.util.TermSettings;

public class TermView extends EmulatorView {

	private onImeStatusChangedListener imeStatusChangedListener;
	private int oldbottom;
	
	public TermView(Context context, TermSession session, DisplayMetrics metrics) {
		this(context, session, metrics, null);
	}


	public TermView(Context context, TermSession session,
			DisplayMetrics metrics, TermView.onImeStatusChangedListener lis) {
		super(context, session, metrics);
		this.imeStatusChangedListener = lis;
		oldbottom = getBottom();
	}

	

	public void updatePrefs(TermSettings settings, ColorScheme scheme) {
		if (scheme == null) {
			scheme = new ColorScheme(settings.getColorScheme());
		}

		setTextSize(settings.getFontSize());
		setUseCookedIME(settings.useCookedIME());
		setColorScheme(scheme);
		setBackKeyCharacter(settings.getBackKeyCharacter());
		setAltSendsEsc(settings.getAltSendsEscFlag());
		setControlKeyCode(settings.getControlKeyCode());
		setFnKeyCode(settings.getFnKeyCode());
		setTermType(settings.getTermType());
		setMouseTracking(settings.getMouseTrackingFlag());
		setCursorBlink(settings.getCursorBlink());
        setCursorBlinkPeriod(settings.getCursorBlinkPeriod());
	}

	public void updatePrefs(TermSettings settings) {
		updatePrefs(settings, null);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (oldbottom == bottom) {
			return;
		}
		if (oldbottom < bottom) {
			sendImeStatusChanged(false);
		} else {
			sendImeStatusChanged(true);
		}
		oldbottom = bottom;
	}

	private void sendImeStatusChanged(boolean open) {
		if (this.imeStatusChangedListener != null) {
			imeStatusChangedListener.onImeStatusChange(open);
		}
	}

	public void setImeStatusChangedListener(
			onImeStatusChangedListener imeStatusChangedListener) {
		this.imeStatusChangedListener = imeStatusChangedListener;
	}

	public interface onImeStatusChangedListener {
		void onImeStatusChange(boolean open);
	}

}
