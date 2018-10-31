package com.indrisoftware.getitallconnected.app.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

class AlertsContract {
    public static final String CONTENT_AUTHORITY = "com.indrisoftware.getitallconnected.app";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_ALERTS = "alerts";

    public static final class AlertsEntry implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_ALERTS).build();

        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ALERTS;

        public static final String TABLE_NAME = "alerts";

        private static final String COLUMN_TRAP_STATUS = "trapStatus";

        private static final String COLUMN_TEAM = "team";

        //TODO add columns

        public static Uri buildAlertsByTeam(String team) {
            return CONTENT_URI.buildUpon().appendPath(team).build();
        }

        public static String getTeamFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }


}
