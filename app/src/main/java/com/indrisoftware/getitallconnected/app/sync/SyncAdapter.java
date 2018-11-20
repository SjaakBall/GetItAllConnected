package com.indrisoftware.getitallconnected.app.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.indrisoftware.getitallconnected.app.R;
import com.indrisoftware.getitallconnected.app.Utility;
import com.indrisoftware.getitallconnected.app.data.AlertsContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Vector;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private final String LOG_TAG = SyncAdapter.class.getSimpleName();

    // Interval at which to sync with the GIAC restservice, in seconds.
    // 60 seconds (1 minute) * 180 = 3 hours
    private static final int SYNC_INTERVAL = 60 * 180;
    private static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;
    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;

    private static final String[] NOTIFY_ALERTS_PROJECTION = new String[] {
            AlertsContract.AlertsEntry.COLUMN_TEAM,
            AlertsContract.AlertsEntry.COLUMN_TRAP_STATUS
    };

    // these indices must match the projection
//    private static final int INDEX_ALERTS_ID = 0;
    private static final int INDEX_TEAM = 0;
    private static final int INDEX_TRAP_STATUS = 1;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    private static Account getSyncAccount(Context context) {
        final AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        final Account newAccount = new Account(context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        if (accountManager != null && null == accountManager.getPassword(newAccount)) {
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }

            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        SyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        syncImmediately(context);
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(final Context context, final int syncInterval, final int flexTime) {
        final Account account = getSyncAccount(context);
        final String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder()
                    .syncPeriodic(syncInterval, flexTime)
                    .setSyncAdapter(account, authority)
                    .setExtras(new Bundle())
                    .build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account, authority, new Bundle(), syncInterval);
        }
    }

    /**
     * Helper method to have the sync adapter sync immediately
     *
     * @param context The context used to access the account service
     */
    public static void syncImmediately(final Context context) {
        final Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context), context.getString(R.string.content_authority), bundle);
    }

    @Override
    public void onPerformSync(
            Account account,
            Bundle extras,
            String authority,
            ContentProviderClient provider,
            SyncResult syncResult) {
        Log.d(LOG_TAG, "Starting sync");
        String teamQuery = Utility.getPreferredTeam(getContext());

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String jsonStr = null;


        try {
            final String CIAC_BASE_URL =
                    "http://localhost:8080/Getitallservice/alerts/TeamD";
            final String QUERY_PARAM = "q";

            Uri builtUri = Uri
                    .parse(CIAC_BASE_URL)
                    .buildUpon()
                    .appendQueryParameter("", teamQuery)
                    .build();

            URL url = new URL(builtUri.toString());
            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return;
            }
            jsonStr = buffer.toString();
            getDataFromJson(jsonStr);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return;
    }

    private void getDataFromJson(String jsonStr) {

        final String GIACA_LIST = "list";
        final String GIACA_TRAPSTATUS = "trapStatus";
        final String GIACA_TEAM = "team";

        //get data from json
        try {
            JSONObject alertsJson = new JSONObject(jsonStr);
            JSONArray alertsArray = alertsJson.getJSONArray(jsonStr);

            Vector<ContentValues> cVVector = new Vector<ContentValues>(alertsArray.length());

            for (int i = 0; i < alertsArray.length(); i++) {
                JSONObject alertJsonObject = alertsArray.getJSONObject(i);
                int trapStatus = alertJsonObject.getInt(GIACA_TRAPSTATUS);
                String team = alertJsonObject.getString(GIACA_TEAM);

                ContentValues alertsValues = new ContentValues();
                alertsValues.put(AlertsContract.AlertsEntry.COLUMN_TRAP_STATUS, trapStatus);
                alertsValues.put(AlertsContract.AlertsEntry.COLUMN_TEAM, team);

                cVVector.add(alertsValues);
            }

            int inserted = 0;
            // add to database
            if (cVVector.size() > 0) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                getContext().getContentResolver().bulkInsert(AlertsContract.AlertsEntry.CONTENT_URI, cvArray);

//                // delete old data so we don't build up an endless history
//                getContext().getContentResolver().delete(AlertsContract.AlertsEntry.CONTENT_URI,
//                        AlertsContract.AlertsEntry.COLUMN_DATE + " <= ?",
//                        new String[] {Long.toString(dayTime.setJulianDay(julianStartDay-1))});

                notifyAlerts();
            }

            Log.d(LOG_TAG, "Sync Complete. " + cVVector.size() + " Inserted");

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
        //put data in local database
    }

    private void notifyAlerts() {
        Context context = getContext();
        //checking the last update and notify if it' the first of the day
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
        boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));

        if (displayNotifications) {
            String lastNotificationKey = context.getString(R.string.pref_last_notification);
            long lastSync = prefs.getLong(lastNotificationKey, 0);

            if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {
                // Last sync was more than 1 day ago, let's send a notification with the alerts.
                String teamQuery = Utility.getPreferredTeam(context);

                Uri alertsUri = AlertsContract.AlertsEntry.buildAlertsByTeam(teamQuery);

                Cursor cursor = context.getContentResolver().query(alertsUri, NOTIFY_ALERTS_PROJECTION, null, null, null);

                if (cursor.moveToFirst()){
                    String team = cursor.getString(INDEX_TEAM);
                    String trapStatus = cursor.getString(INDEX_TRAP_STATUS);
                }
            }

        }

    }

    /*
    * [{
	"id": 1,
	"productid": 1111,
	"prodgroupid": 30,
	"remoteSSID": "geen",
	"msgcount": 2,
	"inPlanning": null,
	"planOrder": 1,
	"eventMessage": null,
	"frombundel": null,
	"team": "TeamD",
	"teamId": 4,
	"customerChain": "Puy de dôme",
	"customerChainId": 1,
	"counter": null,
	"trapStatus": false,
	"macAddress": "005FB7173963E0F8",
	"severity": "ERROR",
	"temperature": 22,
	"humidity": 49,
	"battery": 99,
	"rssi": -25,
	"location": null,
	"latitude": 52.2345,
	"longitude": 6.2345,
	"altitude": 2.0,
	"customer": "JP Goedee",
	"customerId": 14,
	"city": "Isserteaux",
	"trapFoodExp": null,
	"trapLost": true,
	"trap_sensor": false,
	"temp_sensor": true,
	"humid_sensor": true,
	"updated": 1539160915000,
	"dateIn": 1538998080000,
	"alertMessages": [{
		"id": 1,
		"messageId": null,
		"macAddress": "005FB7173963E0F8",
		"filterId": 5,
		"filter": "Kies de veilige marge Temp .   ",
		"icon": "/resources/images/temp.png",
		"fromBundel": null,
		"severityId": 5,
		"severity": "FATAL",
		"color": "#000000",
		"message": "Let op!!! Temperatuur niet goed",
		"value1": 22,
		"value_ext1": "C",
		"value_image1": null,
		"closed": false,
		"dateUpdate": 1539160915000,
		"dateIn": 1538998080000,
		"boolean1": null
	}, {
		"id": 2,
		"messageId": null,
		"macAddress": "005FB7173963E0F8",
		"filterId": 6,
		"filter": "Kies de veilige marge Humid.  ",
		"icon": "/resources/images/vocht.png",
		"fromBundel": null,
		"severityId": 3,
		"severity": "ERROR",
		"color": "#e51212",
		"message": "Let op!!! Vocht niet goed",
		"value1": 49,
		"value_ext1": "%",
		"value_image1": null,
		"closed": false,
		"dateUpdate": 1539160915000,
		"dateIn": 1538998080000,
		"boolean1": null
	}]
}]
    * */
}
