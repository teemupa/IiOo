package com.aware.plugin.iioo;

/**
 * Created by Teemu on 23.3.2015.
 */

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.Intent;
import android.util.Log;

import java.util.Calendar;
import java.lang.Math;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;
import android.database.Cursor;
import android.hardware.SensorManager;
import com.aware.providers.Locations_Provider;
import com.aware.providers.Telephony_Provider;
import com.aware.providers.Magnetometer_Provider;

public class Plugin extends Aware_Plugin {

    public static final String ACTION_AWARE_PLUGIN_IO_DETECTOR = "ACTION_AWARE_PLUGIN_IO_DETECTOR";
    public static final String GPS_OP = "gps_opinion";
    public static final String TELE_OP = "tele_opinion";
    public static final String MAG_OP = "mag_opinion";
    public static final String UPDATED = "updated_timestamp";
    public static String SHARED_CONTEXT = "IO_status";

    final static long gps_time_delta = 120000L; //2min = 2*60*1000 = 120000ms
    final static long tele_time_delta = 120000L;
    final static long mag_time_delta = 60000L;

    private final double gps_avg_thres = 8.0; //Estimated value to cross in OUTDOORS
    private final double tele_avg_thres = 20.0; //Estimated value to cross in OUTDOORS
    private final double mag_avg_thres = 60.0;
    private final double mag_diff_thres = 3.0;

    private String gps_value = "n/a";
    private String tele_value = "n/a";
    private String mag_avg_value = "n/a";
    private String mag_var_value = "n/a";

    public static String GPS_OPINION = null;
    private String TELEPHONY_OPINION = "";
    private String MAGNETO_OPINION = "";
    private String UPDATE_TIME = "";
    private String io_status = "";

    private static ContextProducer contextProd;

    public int test = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        TAG = "Template";
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_MAGNETOMETER, true);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_MAGNETOMETER, SensorManager.SENSOR_DELAY_NORMAL);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_TELEPHONY, true);

        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_LOCATION_GPS, true);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_LOCATION_GPS, 0);

        Intent applySettings = new Intent(Aware.ACTION_AWARE_REFRESH);
        sendBroadcast(applySettings);


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //TODO: Get values from the sensor providers
        //TODO: Evaluate sensor readings
        //TODO: Share context based on evaluation
        //TODO: Update stuff to the DB (make own provider)

        Log.d(TAG, "onStartCommand");

        if (DEBUG) Log.d(TAG, "Template plugin running");

        int gps = checkGPS();
        int tele = checkTelephony();
        int mag = checkMagneto();

        if(gps != -1) {
            if (gps + tele + mag > 1) {
                io_status = "INDOORS";
                Log.d("IO:", "IO STATUS IS INDOORS");
            } else {
                io_status = "OUTDOORS";
                Log.d("IO:", "IO STATUS IS OUTDOORS");
            }
        }else{
            if(mag == 0 && tele > 0){
                io_status = "INDOORS";
            }else if(mag + tele < 1){
                io_status = "OUTDOORS";
            }else if(mag + tele >= 1){
                io_status = "INDOORS";
            }
        }


        contextProd = new ContextProducer() {
            @Override
            public void onContext() {
                Intent context_io_detector = new Intent();
                context_io_detector.setAction(ACTION_AWARE_PLUGIN_IO_DETECTOR);
                context_io_detector.putExtra(GPS_OP, GPS_OPINION);
                context_io_detector.putExtra(TELE_OP, TELEPHONY_OPINION);
                context_io_detector.putExtra(MAG_OP, MAGNETO_OPINION);
                context_io_detector.putExtra(UPDATED, UPDATE_TIME);
                //context_io_detector.putExtra(SHARED_CONTEXT, io_status);
                sendBroadcast(context_io_detector);
            }
        };

        provideData();

        contextProd.onContext();

        return START_STICKY;
    }

    private int checkMagneto(){
        //Lol, Magneto

        double mag_avg = 0.0; //Average of the magnetic field strength (sqrt(x^2 + y^2 + z^2))
        double mag_sum = 0.0;
        double avg_diff = 0.0;
        List<Double> values = new ArrayList<Double>();


        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(System.currentTimeMillis());


        String where = Magnetometer_Provider.Magnetometer_Data.TIMESTAMP + ">" + (date.getTimeInMillis() - mag_time_delta);
        Cursor cur = getApplicationContext().getContentResolver().query(Magnetometer_Provider.Magnetometer_Data.CONTENT_URI, null, where, null, null, null);

        cur.moveToFirst();

        if(cur.getCount() > 0) {
            double i = 0.0;
            while (cur.isAfterLast() == false) {
                double value0 = cur.getDouble(cur.getColumnIndex(Magnetometer_Provider.Magnetometer_Data.VALUES_0));
                double value1 = cur.getDouble(cur.getColumnIndex(Magnetometer_Provider.Magnetometer_Data.VALUES_1));
                double value2 = cur.getDouble(cur.getColumnIndex(Magnetometer_Provider.Magnetometer_Data.VALUES_2));
                //Log.d("IO", "MAGNETOMETER value0, value1, value2: " + String.valueOf(value0) + " " + String.valueOf(value1) + " " + String.valueOf(value2));
                double mag_total = Math.sqrt(value0*value0 + value1*value1 + value2*value2);
                mag_sum = mag_sum + mag_total;
                i = i + 1.0;
                values.add(mag_total);

                cur.moveToNext();
            }

            mag_avg = mag_sum / i;
            Log.d("IO", "MAGNETOMETER avg: " + String.valueOf(mag_avg));
        }
        cur.close();

        double diffsum = 0.0;
        double diff;
        for(double i : values){
            diff = mag_avg - i;
            diffsum = diffsum + Math.abs(diff);
        }

        avg_diff = diffsum / (double) values.size();

        mag_avg_value = Double.toString(mag_avg);
        mag_var_value = Double.toString(avg_diff);

        //TODO: CALCULATE THE VARIANCE, AVG MEANS NOTHING HERE
        //TODO: Check whether user is moving or not
        if(mag_avg > mag_avg_thres && avg_diff > mag_diff_thres){
            Log.d("IO", "MAGNETOMETER says you are INDOORS with avg strength of: " + String.valueOf(mag_avg) + " " + mag_var_value);
            MAGNETO_OPINION = "IN";
            return 1;
        }else if(mag_avg < mag_avg_thres && avg_diff < mag_diff_thres){
            Log.d("IO", "MAGNETOMETER says you are OUTDOORS with avg strength of: " + String.valueOf(mag_avg) + " " + mag_var_value);
            MAGNETO_OPINION = "OUT";
            return 0;
        }else if(mag_avg < mag_avg_thres && avg_diff > mag_diff_thres){
            MAGNETO_OPINION = "IN";
            Log.d("IO", "MAGNETOMETER says you are INDOORS with avg strength of: " + String.valueOf(mag_avg) + " " + mag_var_value);
            return 1;

        }else if(mag_avg > mag_avg_thres && avg_diff < mag_diff_thres){
            MAGNETO_OPINION = "IN";
            Log.d("IO", "MAGNETOMETER says you are INDOORS with avg strength of: " + String.valueOf(mag_avg) + " " + mag_var_value);
            return 1;
        }

        return 0;
    }

    private int checkTelephony(){

        //TODO: Use neighbouring tower data?

        double tele_avg = 0.0;
        double tele_sum = 0.0;

        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(System.currentTimeMillis());

        String where = Telephony_Provider.Telephony_Data.TIMESTAMP + ">" + (date.getTimeInMillis() - tele_time_delta);
        Cursor cur = getApplicationContext().getContentResolver().query(Telephony_Provider.GSM_Data.CONTENT_URI, null, where, null, null, null);
        cur.moveToFirst();
        if(cur.getCount() > 0) {
            double i = 0.0;
            while (cur.isAfterLast() == false) {
                double rss = cur.getDouble(cur.getColumnIndex(Telephony_Provider.GSM_Data.SIGNAL_STRENGTH));
                double tele_time = cur.getDouble(cur.getColumnIndex(Telephony_Provider.GSM_Data.TIMESTAMP));
                String cid = cur.getString(cur.getColumnIndex(Telephony_Provider.GSM_Data.CID));
                Log.d("IO", "TELEPHONY RSS: " + String.valueOf(rss));
                tele_sum = tele_sum + rss;
                i = i + 1.0;
                cur.moveToNext();
            }

            tele_avg = tele_sum / i;
        }

        tele_value = Double.toString(tele_avg);
        if (tele_avg < tele_avg_thres && tele_avg > 0.0) {
            Log.d("IO:", "TELEPHONY says INDOORS with AVG " + String.valueOf(tele_avg));
            TELEPHONY_OPINION = "IN";
            cur.close();
            return 2;
        }else if(tele_avg > tele_avg_thres && tele_avg > 0.0){
            Log.d("IO:", "TELEPHONY says OUTDOORS with AVG " + String.valueOf(tele_avg));
            TELEPHONY_OPINION = "OUT";
            cur.close();
            return 0;
        }else {
            cur = getApplicationContext().getContentResolver().query(Telephony_Provider.GSM_Data.CONTENT_URI, null, null, null, Telephony_Provider.GSM_Data.TIMESTAMP + " DESC LIMIT 1");

            if (cur.getCount() > 0) {
                cur.moveToLast();
                double rss2 = cur.getDouble(cur.getColumnIndex(Telephony_Provider.GSM_Data.SIGNAL_STRENGTH));
                Log.d("IO:", "Not enough data, latest TELEPHONY value is " + String.valueOf(rss2));
                if (rss2 < tele_avg_thres) {
                    Log.d("IO:", "TELEPHONY says INDOORS with latest value of " + String.valueOf(rss2));
                    TELEPHONY_OPINION = "IN";
                    cur.close();
                    return 1;
                }else if(rss2 > tele_avg_thres){
                    //TODO: What if the latest value wayyy too old? From another day for example??
                    Log.d("IO:", "TELEPHONY says OUTDOORS with latest value of " + String.valueOf(rss2));
                    TELEPHONY_OPINION = "OUT";
                    cur.close();
                    return 0;
                }

            }
        }
        cur.close();
        return 0;
    }
    private int checkGPS(){
        /*Return 1 if indoors,
        Returns 0 if outdoors,
        Returns -1 if not available
         */
        double gps_sum = 0.0;
        double gps_avg = 0.0;

        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(System.currentTimeMillis());

        String where = Locations_Provider.Locations_Data.PROVIDER + " = ? AND " + Locations_Provider.Locations_Data.TIMESTAMP + ">" + (date.getTimeInMillis() - gps_time_delta);
        String[] args = {"gps"};
        Cursor cur = getApplicationContext().getContentResolver().query(Locations_Provider.Locations_Data.CONTENT_URI, null, where, args, null, null);
        cur.moveToFirst();
        double i = 0;
        if(cur.getCount() > 0) {
            while (cur.isAfterLast() == false) {
                double gps_accuracy = cur.getDouble(cur.getColumnIndex(Locations_Provider.Locations_Data.ACCURACY));
                Log.d("IO", "GPS ACCURACY: " + String.valueOf(gps_accuracy));
                gps_sum = gps_sum + gps_accuracy;
                i = i + 1.0;
                cur.moveToNext();
            }
        }
        cur.close();
        gps_avg = gps_sum / i;
        gps_value = Double.toString(gps_avg);

        if (gps_avg < gps_avg_thres) {
            Log.d("IO:", "GPS says INDOORS with AVG " + String.valueOf(gps_avg));
            GPS_OPINION = "IN";
            return 1;
        }else if(gps_avg > gps_avg_thres){
            Log.d("IO:", "GPS says OUTDOORS with AVG " + String.valueOf(gps_avg));
            GPS_OPINION = "OUT";
            return 0;
        }else{
            //TODO: Use the latest value?
            Log.d("IO:", "GPS does have not enough data -> indoors?");
            GPS_OPINION = "-";
            return -1;
        }

    }

    private void provideData(){
        ContentValues new_data = new ContentValues();
        new_data.put(Provider.IODetector_Data.TIMESTAMP, System.currentTimeMillis());
        new_data.put(Provider.IODetector_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
        new_data.put(Provider.IODetector_Data.IO_STATUS, io_status);
        new_data.put(Provider.IODetector_Data.GPS_THRESH, gps_avg_thres);
        new_data.put(Provider.IODetector_Data.TELE_THRESH, tele_avg_thres);
        new_data.put(Provider.IODetector_Data.MAG_THRESH, mag_avg_thres);
        new_data.put(Provider.IODetector_Data.GPS_VALUE, gps_value);
        new_data.put(Provider.IODetector_Data.TELE_VALUE, tele_value);
        new_data.put(Provider.IODetector_Data.MAG_AVG_VALUE, mag_avg_value);
        new_data.put(Provider.IODetector_Data.MAG_VAR_VALUE, mag_var_value);
        new_data.put(Provider.IODetector_Data.GPS_OP, GPS_OPINION);
        new_data.put(Provider.IODetector_Data.TELE_OP, TELEPHONY_OPINION);
        new_data.put(Provider.IODetector_Data.MAG_OP, MAGNETO_OPINION);
        getContentResolver().insert(Provider.IODetector_Data.CONTENT_URI, new_data);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        if (DEBUG) Log.d(TAG, "Template plugin terminated");
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_MAGNETOMETER, false);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_TELEPHONY, false);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_LOCATION_GPS, false);
        Intent applySettings = new Intent(Aware.ACTION_AWARE_REFRESH);
        sendBroadcast(applySettings);
    }


}
