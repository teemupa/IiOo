package com.aware.plugin.iioo;

/**
 * Created by Teemu on 23.3.2015.
 */

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.HashMap;

import com.aware.Aware;
import com.aware.utils.DatabaseHelper;

public class Provider extends ContentProvider {

    public static final int DATABASE_VERSION = 1;

    public static String AUTHORITY = "com.aware.plugin.iioo.provider.iioo";

    private static final int IO_DETECTOR = 1;
    private static final int IO_DETECTOR_ID = 2;

    public static final String DATABASE_NAME = Environment.getExternalStorageDirectory() + "/AWARE/plugin_iioo.db";

    public static final String[] DATABASE_TABLES = {
            "plugin_iioo"
    };

    public static final String[] TABLES_FIELDS = {
            IODetector_Data._ID + " integer primary key autoincrement," +
                    IODetector_Data.TIMESTAMP + " real default 0," +
                    IODetector_Data.DEVICE_ID + " text default ''," +
                    IODetector_Data.IO_STATUS + " text default ''," +
                    IODetector_Data.GPS_THRESH + " real default 0," +
                    IODetector_Data.TELE_THRESH + " real default 0," +
                    IODetector_Data.MAG_THRESH + " real default 0," +
                    IODetector_Data.GPS_VALUE + " real default 0," +
                    IODetector_Data.TELE_VALUE + " real default 0," +
                    IODetector_Data.MAG_AVG_VALUE + " real default 0," +
                    IODetector_Data.MAG_VAR_VALUE + " real default 0," +
                    IODetector_Data.GPS_OP + " text default ''," +
                    IODetector_Data.TELE_OP + " text default ''," +
                    IODetector_Data.MAG_OP + " text default ''," +
                    "UNIQUE("+IODetector_Data.TIMESTAMP+","+IODetector_Data.DEVICE_ID+")"
    };

    public static final class IODetector_Data implements BaseColumns {
        private IODetector_Data(){};

        public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/plugin_iioo");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.plugin.iioo";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.plugin.iioo";

        public static final String _ID = "_id";
        public static final String IO_STATUS = "io";
        public static final String TIMESTAMP = "timestamp";
        public static final String DEVICE_ID = "device_id";
        public static final String GPS_VALUE = "gps_value";
        public static final String TELE_VALUE = "tele_value";
        public static final String MAG_AVG_VALUE = "mag_avg_value";
        public static final String MAG_VAR_VALUE = "mag_var_value";
        public static final String MAG_THRESH = "mag_thresh";
        public static final String TELE_THRESH = "tele_thresh";
        public static final String GPS_THRESH = "gps_thresh";
        public static final String MAG_OP = "mag_op";
        public static final String GPS_OP = "gps_op";
        public static final String TELE_OP = "tele_op";
    }

    private static UriMatcher URIMatcher;
    private static HashMap<String, String> databaseMap;
    private static DatabaseHelper databaseHelper;
    private static SQLiteDatabase database;

    @Override
    public boolean onCreate() {

        AUTHORITY = getContext().getPackageName() + ".provider.iioo";

        URIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        URIMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], IO_DETECTOR);
        URIMatcher.addURI(AUTHORITY, DATABASE_TABLES[0]+"/#", IO_DETECTOR_ID);

        databaseMap = new HashMap<String, String>();
        databaseMap.put(IODetector_Data._ID, IODetector_Data._ID);
        databaseMap.put(IODetector_Data.TIMESTAMP, IODetector_Data.TIMESTAMP);
        databaseMap.put(IODetector_Data.DEVICE_ID, IODetector_Data.DEVICE_ID);
        databaseMap.put(IODetector_Data.IO_STATUS, IODetector_Data.IO_STATUS);
        databaseMap.put(IODetector_Data.GPS_THRESH, IODetector_Data.GPS_THRESH);
        databaseMap.put(IODetector_Data.TELE_THRESH, IODetector_Data.TELE_VALUE);
        databaseMap.put(IODetector_Data.MAG_THRESH, IODetector_Data.MAG_THRESH);
        databaseMap.put(IODetector_Data.GPS_VALUE, IODetector_Data.GPS_VALUE);
        databaseMap.put(IODetector_Data.TELE_VALUE, IODetector_Data.TELE_VALUE);
        databaseMap.put(IODetector_Data.MAG_AVG_VALUE, IODetector_Data.MAG_AVG_VALUE);
        databaseMap.put(IODetector_Data.MAG_VAR_VALUE, IODetector_Data.MAG_VAR_VALUE);
        databaseMap.put(IODetector_Data.GPS_OP, IODetector_Data.GPS_OP);
        databaseMap.put(IODetector_Data.TELE_OP, IODetector_Data.TELE_OP);
        databaseMap.put(IODetector_Data.MAG_OP, IODetector_Data.MAG_OP);
        //TODO: Put values

        return true;
    }

    private boolean initializeDB() {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper( getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS );
        }
        if( databaseHelper != null && ( database == null || ! database.isOpen() )) {
            database = databaseHelper.getWritableDatabase();
        }
        return( database != null && databaseHelper != null);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY, "Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (URIMatcher.match(uri)) {
            case IO_DETECTOR:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (URIMatcher.match(uri)) {
            case IO_DETECTOR:
                return IODetector_Data.CONTENT_TYPE;
            case IO_DETECTOR_ID:
                return IODetector_Data.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }

        ContentValues values = (initialValues != null) ? new ContentValues(
                initialValues) : new ContentValues();

        switch (URIMatcher.match(uri)) {
            case IO_DETECTOR:
                long weather_id = database.insert(DATABASE_TABLES[0], IODetector_Data.DEVICE_ID, values);

                if (weather_id > 0) {
                    Uri new_uri = ContentUris.withAppendedId(
                            IODetector_Data.CONTENT_URI,
                            weather_id);
                    getContext().getContentResolver().notifyChange(new_uri,
                            null);
                    return new_uri;
                }
                throw new SQLException("Failed to insert row into " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (URIMatcher.match(uri)) {
            case IO_DETECTOR:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(databaseMap);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG)
                Log.e(Aware.TAG, e.getMessage());

            return null;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (URIMatcher.match(uri)) {
            case IO_DETECTOR:
                count = database.update(DATABASE_TABLES[0], values, selection,
                        selectionArgs);
                break;
            default:

                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
