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

package com.romide.terminal.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.ListViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.romide.terminal.R;
import com.romide.terminal.adapter.WindowListAdapter;
import com.romide.terminal.compat.ActionBarCompat;
import com.romide.terminal.compat.ActivityCompat;
import com.romide.terminal.compat.AndroidCompat;
import com.romide.terminal.service.TermService;
import com.romide.terminal.util.SessionList;
import com.romide.terminal.util.TermDebug;

public class WindowList extends ActivityBase {
    private SessionList sessions;
    private WindowListAdapter mWindowListAdapter;
    private TermService mTermService;

    private ListView listView;

    private ServiceConnection mTSConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            TermService.TSBinder binder = (TermService.TSBinder) service;
            mTermService = binder.getService();
            populateList();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mTermService = null;
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.window_list_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listView = (ListView) findViewById(R.id.window_list_view);

        View newWindow = getLayoutInflater().inflate(
                R.layout.window_list_new_window, listView, false);
        listView.addHeaderView(newWindow, null, true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onListItemClick(position);
            }
        });

        setResult(RESULT_CANCELED);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent TSIntent = new Intent(this, TermService.class);
        if (!bindService(TSIntent, mTSConnection, BIND_AUTO_CREATE)) {
            Log.w(TermDebug.LOG_TAG, "bind to service failed!");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        WindowListAdapter adapter = mWindowListAdapter;
        if (sessions != null) {
            sessions.removeCallback(adapter);
            sessions.removeTitleChangedListener(adapter);
        }
        if (adapter != null) {
            adapter.setSessions(null);
        }
        unbindService(mTSConnection);
    }

    private void populateList() {
        sessions = mTermService.getSessions();
        WindowListAdapter adapter = mWindowListAdapter;

        if (adapter == null) {
            adapter = new WindowListAdapter(this, sessions);
            listView.setAdapter(adapter);
            mWindowListAdapter = adapter;
        } else {
            adapter.setSessions(sessions);
        }
        sessions.addCallback(adapter);
        sessions.addTitleChangedListener(adapter);
    }

    protected void onListItemClick(int position) {
        Intent data = new Intent();
        data.putExtra(Term.EXTRA_WINDOW_ID, position - 1);
        setResult(RESULT_OK, data);
        finish();
    }
}
