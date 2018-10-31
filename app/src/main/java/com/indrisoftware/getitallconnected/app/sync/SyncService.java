package com.indrisoftware.getitallconnected.app.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

public class SyncService extends Service {
    public final String LOG_TAG = SyncService.class.getSimpleName();
    private static final Object sSyncAdapterLock = new Object();
    private static SyncAdapter sSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "onCreate SyncAdapter");
        synchronized (sSyncAdapterLock){
            if (sSyncAdapter == null){
                sSyncAdapter = new SyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return sSyncAdapter.getSyncAdapterBinder();
    }
}
