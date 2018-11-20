package com.indrisoftware.getitallconnected.app.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

public class AlertsContract {
    static final String CONTENT_AUTHORITY = "com.indrisoftware.getitallconnected.app";
    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    static final String PATH_ALERTS = "alerts";

    public static final class AlertsEntry implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_ALERTS).build();

        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ALERTS;

        public static final String TABLE_NAME = "alerts";

        public static final String COLUMN_TRAP_STATUS = "trapStatus";

        public static final String COLUMN_TEAM = "team";

        public static Uri buildAlertsUri(long id){
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildAlertsByTeam(String team){
            return CONTENT_URI.buildUpon().appendPath(team).build();
        }

        public static String getTeamFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }


}
