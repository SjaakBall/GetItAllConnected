package com.indrisoftware.getitallconnected.app;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.indrisoftware.getitallconnected.app.sync.SyncAdapter;
import com.indrisoftware.getitallconnected.app.ui.main.MainFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance())
                    .commitNow();
        }

        SyncAdapter.initializeSyncAdapter(this.getApplicationContext());
    }
}
