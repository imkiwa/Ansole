/*
 * Copyright (C) 2011 Steven Luo
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

package com.romide.terminal.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.romide.terminal.R;
import com.romide.terminal.emulatorview.TermSession;
import com.romide.terminal.emulatorview.UpdateCallback;
import com.romide.terminal.session.SessionList;
import com.romide.terminal.session.ShellTermSession;


public class WindowListAdapter extends BaseAdapter implements UpdateCallback {
    private SessionList mSessions;
    private Context ctx;

    public WindowListAdapter(Context ctx, SessionList sessions) {
        this.ctx = ctx;
        setSessions(sessions);
    }

    public void setSessions(SessionList sessions) {
        mSessions = sessions;
        if (sessions != null) {
            onUpdate();
        }
    }

    @Override
	public int getCount() {
        return mSessions.size();
    }

    @Override
	public Object getItem(int position) {
        return mSessions.get(position);
    }

    @Override
	public long getItemId(int position) {
        return position;
    }

    protected String getSessionTitle(int position, String defaultTitle) {
        TermSession session = mSessions.get(position);
        if (session != null && session instanceof ShellTermSession) {
            return ((ShellTermSession) session).getTitle(defaultTitle);
        } else {
            return defaultTitle;
        }
    }

    @Override
	@SuppressLint("ViewHolder")
	public View getView(int position, View convertView, ViewGroup parent) {
        View child = LayoutInflater.from(ctx).inflate(R.layout.window_list_item, parent, false);
        View close = child.findViewById(R.id.window_list_close);

        TextView label = (TextView) child.findViewById(R.id.window_list_label);
        String defaultTitle = ctx.getString(R.string.window_title, position+1);
        label.setText(getSessionTitle(position, defaultTitle));

        final SessionList sessions = mSessions;
        final int closePosition = position;
        close.setOnClickListener(new View.OnClickListener() {
            @Override
			public void onClick(View v) {
                TermSession session = sessions.remove(closePosition);
                if (session != null) {
                    session.finish();
                    notifyDataSetChanged();
                }
            }
        });

        return child;
    }

    @Override
	public void onUpdate() {
        notifyDataSetChanged();
    }
}
