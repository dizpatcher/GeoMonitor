package com.example.geomonitor;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HistoryProvider extends ContentProvider {

    private DBHelper dbHelper = null;
    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(HistoryProviderContract.AUTHORITY, "ActivityHistory", 1);
    }

    @Override
    public boolean onCreate() {
        Log.d("g53mdp", "HistoryProvider onCreate");
        this.dbHelper = new DBHelper(this.getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Log.d("g53mdp", uri.toString() + " " + uriMatcher.match(uri));
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return  db.query("ActivityHistory", projection, selection, selectionArgs, null, null, sortOrder);
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        if (uri.getLastPathSegment() == null) {
            return HistoryProviderContract.CONTENT_TYPE_MULTIPLE;
        }
        else {
            return HistoryProviderContract.CONTENT_TYPE_SINGLE;
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insert("ActivityHistory", null, contentValues);
        db.close();
        Uri newUri = ContentUris.withAppendedId(uri, id); //new Uri after inserting
        Log.d("g53mdp", newUri.toString());
        getContext().getContentResolver().notifyChange(newUri, null);
        return newUri;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = db.delete("ActivityHistory", selection, selectionArgs);
        db.close();
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsAffected = db.update("ActivityHistory", contentValues, selection, selectionArgs);
        db.close();
        return rowsAffected;
    }
}
