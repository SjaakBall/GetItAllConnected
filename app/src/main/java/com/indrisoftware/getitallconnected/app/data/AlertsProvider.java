package com.indrisoftware.getitallconnected.app.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class AlertsProvider extends ContentProvider {

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();

    static final int ALERTS = 100;
    static final int ALERTS_BY_TEAM = 101;

    private static final SQLiteQueryBuilder sAlertsByTeamQueryBuilder;

    static {
        sAlertsByTeamQueryBuilder = new SQLiteQueryBuilder();
    }

    private Cursor getAlertsByTeam(Uri uri, String[] projection, String sortOrder){
        String team = AlertsContract.AlertsEntry.getTeamFromUri(uri);

        String[] selectionArgs;
        String selection;

        return null;
    }

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = AlertsContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, AlertsContract.PATH_ALERTS, ALERTS);
        matcher.addURI(authority, AlertsContract.PATH_ALERTS + "/*", ALERTS_BY_TEAM);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case ALERTS:
                return AlertsContract.AlertsEntry.CONTENT_TYPE;
            case ALERTS_BY_TEAM:
                return null;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

}
