package com.indrisoftware.getitallconnected.app.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Objects;

public class AlertsProvider extends ContentProvider {

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private AlertsDbHelper mOpenHelper;

    static final int ALERTS = 100;
    static final int ALERTS_BY_TEAM = 101;

    private static final SQLiteQueryBuilder sAlertsByTeamQueryBuilder;

    static {
        sAlertsByTeamQueryBuilder = new SQLiteQueryBuilder();
    }

    private static final String sTeamSelection = AlertsContract.AlertsEntry.TABLE_NAME + "." + AlertsContract.AlertsEntry.COLUMN_TEAM + " = ? ";

    private Cursor getAlertsByTeam(Uri uri, String[] projection, String sortOrder) {
        String team = AlertsContract.AlertsEntry.getTeamFromUri(uri);

        String[] selectionArgs = new String[]{team};

        return sAlertsByTeamQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sTeamSelection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
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
        mOpenHelper = new AlertsDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            case ALERTS_BY_TEAM: {
                retCursor = getAlertsByTeam(uri, projection, sortOrder);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(Objects.requireNonNull(getContext()).getContentResolver(), uri);
        return retCursor;
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
        final SQLiteDatabase sqLiteDatabase = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case ALERTS: {
                long _id = sqLiteDatabase.insert(AlertsContract.AlertsEntry.TABLE_NAME, null, values);
                if (_id > 0) {
                    returnUri = AlertsContract.AlertsEntry.buildAlertsUri(_id);
                } else {
                    throw new SQLException("Failed to insert row into " + uri);
                }
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        Objects.requireNonNull(getContext()).getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;

        if (null == selection) {
            selection = "1";
        }
        switch (match) {
            case ALERTS:
                rowsDeleted = db.delete(
                        AlertsContract.AlertsEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            Objects.requireNonNull(getContext()).getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case ALERTS:
                rowsUpdated = db.update(AlertsContract.AlertsEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            Objects.requireNonNull(getContext()).getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }
}