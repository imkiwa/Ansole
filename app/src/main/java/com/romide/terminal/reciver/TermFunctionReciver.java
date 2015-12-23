package com.romide.terminal.reciver;

import java.io.File;
import java.io.IOException;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Toast;

import com.romide.terminal.emulatorview.TermSession;

public class TermFunctionReciver extends BroadcastReceiver {

	private TermSession terminal;

	public TermFunctionReciver(TermSession ts) {
		this.setTerminal(ts);
	}

	public static final String ACTION = "com.romide.terminal.function";
	public static final String KEY_FLAG = "flag";
	public static final String KEY_ENDFILE = "end_file";
	public static final int FLAG_TOAST = 0;
	public static final int FLAG_DIALOG = 1;
	public static final int FLAG_SDL2 = 2;

	public static final String KEY_FILE = "file";
	public static final String KEY_MSG = "msg";
	public static final String KEY_TITLE = "title";

	@Override
	public void onReceive(Context context, Intent intent) {
		int flag = intent.getIntExtra(KEY_FLAG, -1);

		if (flag < 0) {
			return;
		}
		String endfile = intent.getStringExtra(KEY_ENDFILE);
		execAction(context, intent, flag, endfile);
	}

	private void execAction(Context context, Intent intent, int flag,
			final String endfile) {
		File file = new File(endfile);
		if (file.exists()) {
			file.delete();
		}

		switch (flag) {
		case FLAG_SDL2: {
//			Log.d(TermDebug.LOG_TAG, "SDL2 ===> entrance");
//			String sdllib = intent.getStringExtra(KEY_FILE);
//			if (TextUtils.isEmpty(sdllib)) {
//				sendActionEnd(endfile);
//				return;
//			}
//			
//			Log.d(TermDebug.LOG_TAG, "SDL2 ===> check");
//
//			File sdl = new File(sdllib);
//			if (!sdl.exists()) {
//				sendActionEnd(endfile);
//				return;
//			}
//			
//			Log.d(TermDebug.LOG_TAG, "SDL2 ===> load");
//			try {
//				System.load(sdl.getAbsolutePath());
//
//				Log.d(TermDebug.LOG_TAG, "SDL2 ===> start");
//				Intent i = new Intent(context, SDLActivity.class);
//				context.startActivity(i);
//			} catch (Throwable e) {
//				Log.d(TermDebug.LOG_TAG, "SDL2 ===> fail");
//				e.printStackTrace();
//			} finally {
//				Log.d(TermDebug.LOG_TAG, "SDL2 ===> end");
//				sendActionEnd(endfile);
//			}

			break;
		}
		case FLAG_TOAST: {
			String msg = intent.getStringExtra(KEY_MSG);
			if (TextUtils.isEmpty(msg)) {
				sendActionEnd(endfile);
				return;
			}

			Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
			sendActionEnd(endfile);
			break;
		}
		case FLAG_DIALOG: {
			String title = intent.getStringExtra(KEY_TITLE);
			String msg = intent.getStringExtra(KEY_MSG);
			if (TextUtils.isEmpty(title) || TextUtils.isEmpty(msg)) {
				sendActionEnd(endfile);
				return;
			}

			AlertDialog.Builder build = new AlertDialog.Builder(context);
			build.setTitle(title);
			build.setMessage(msg);
			build.setPositiveButton(android.R.string.yes,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							sendActionEnd(endfile);
						}
					});
			build.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					sendActionEnd(endfile);
				}
			});
			build.show();
			break;
		}
		}
	}

	private void sendActionEnd(String path) {
		File file = new File(path);
		if (file.exists()) {
			file.delete();
		}
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public TermSession getTerminal() {
		return terminal;
	}

	public void setTerminal(TermSession terminal) {
		this.terminal = terminal;
	}

}