package com.romide.terminal.reciver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import com.romide.terminal.activity.Term;
import com.romide.terminal.util.TermSettings;

public class BootReciver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, Intent intent) {
		TermSettings settings = new TermSettings(context.getResources(),
				PreferenceManager.getDefaultSharedPreferences(context));

		if (settings.startOnBoot()) {
			Intent i = new Intent(context, Term.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(i);
		}
	}
}
