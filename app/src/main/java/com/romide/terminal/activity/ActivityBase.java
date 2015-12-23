package com.romide.terminal.activity;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

/**
 * @author Kiva
 * @date 2015/12/22
 */
public class ActivityBase extends AppCompatActivity {
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final ActionBar ab = getSupportActionBar();
        if (item.getItemId() == android.R.id.home && ab != null &&
                (ab.getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) != 0) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
