package com.termux.tasker.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class QueryPreferences {

    private static final String QUERY_PREFERENCES_FILENAME = "com.termux.tasker_preferences";

    private static final String LOGGING_LEVEL = "log_level";

    //get default SharedPreferences with MODE_PRIVATE
    public static SharedPreferences getDefaultSharedPreferences(Context context) throws Exception {
        return context.getSharedPreferences(QUERY_PREFERENCES_FILENAME, Context.MODE_PRIVATE);
    }

    //get default SharedPreferences with MODE_PRIVATE | MODE_MULTI_PROCESS
    public static SharedPreferences getDefaultSharedPreferencesMultiProcess(Context context) throws Exception  {
        return context.getSharedPreferences(QUERY_PREFERENCES_FILENAME, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
    }

    //gets value from SharedPreferencesImpl memory cache
    public static int getLogLevel(Context context) {
        try {
            return getDefaultSharedPreferences(context)
                    .getInt(LOGGING_LEVEL, Logger.DEFAULT_LOG_LEVEL);
        }
        catch (Exception e) {
            Logger.logStackTrace(context, "Error getting \"" + LOGGING_LEVEL + "\" from shared preferences", e);
            return Logger.DEFAULT_LOG_LEVEL;
        }
    }

    //gets value from shared preferences file if updated, otherwise from SharedPreferencesImpl memory cache
    public static int getLogLevelFromFile(Context context) {
        try {
            return getDefaultSharedPreferencesMultiProcess(context)
                    .getInt(LOGGING_LEVEL, Logger.DEFAULT_LOG_LEVEL);
        }
        catch (Exception e) {
            Logger.logStackTrace(context, "Error getting \"" + LOGGING_LEVEL + "\" from shared preferences", e);
            return Logger.DEFAULT_LOG_LEVEL;
        }
    }

    //  sets value to shared preferences memory cache and file synchronously
    @SuppressLint("ApplySharedPref")
    public static void setLogLevel(Context context, int logLevel) {
        if (context == null) return;

        try {
            getDefaultSharedPreferences(context)
                    .edit()
                    .putInt(LOGGING_LEVEL, logLevel)
                    .commit(); //using commit() instead of apply() since app is multi-process, FireReceiver has tag android:process=":background"
        } catch (Exception e) {
            Logger.logStackTrace(context, "Error setting \"" + LOGGING_LEVEL + "\" to shared preferences", e);
        }
    }
}
