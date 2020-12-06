package com.termux.tasker.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.termux.tasker.R;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Logger {

    public static final String DEFAULT_LOG_TAG = "termux-tasker";

    public static final int LOG_LEVEL_OFF = 0; // log nothing
    public static final int LOG_LEVEL_NORMAL = 1; // start logging error, warn and info messages and stacktraces
    public static final int LOG_LEVEL_DEBUG = 2; // start logging debug messages
    public static final int LOG_LEVEL_VERBOSE = 3; // start logging verbose messages

    public static final int DEFAULT_LOG_LEVEL = LOG_LEVEL_NORMAL;

    static public void logMesssage(Context context, int logLevel, String tag, String message) {
        if (context == null) return;

        int currentLogLevel = getLogLevel(context);

        if(logLevel == Log.ERROR && currentLogLevel >= LOG_LEVEL_NORMAL)
            Log.e(tag, message);
        else if(logLevel == Log.WARN && currentLogLevel >= LOG_LEVEL_NORMAL)
            Log.w(tag, message);
        else if(logLevel == Log.INFO && currentLogLevel >= LOG_LEVEL_NORMAL)
            Log.i(tag, message);
        else if(logLevel == Log.DEBUG && currentLogLevel >= LOG_LEVEL_DEBUG)
            Log.d(tag, message);
        else if(logLevel == Log.VERBOSE && currentLogLevel >= LOG_LEVEL_VERBOSE)
            Log.v(tag, message);
    }

    static public void logError(Context context, String tag, String message) {
        logMesssage(context, Log.ERROR, tag, message);
    }

    static public void logError(Context context, String message) {
        logMesssage(context, Log.ERROR, DEFAULT_LOG_TAG, message);
    }

    static public void logWarn(Context context, String tag, String message) {
        logMesssage(context, Log.WARN, tag, message);
    }

    static public void logWarn(Context context, String message) {
        logMesssage(context, Log.WARN, DEFAULT_LOG_TAG, message);
    }

    static public void logInfo(Context context, String tag, String message) {
        logMesssage(context, Log.INFO, tag, message);
    }

    static public void logInfo(Context context, String message) {
        logMesssage(context, Log.INFO, DEFAULT_LOG_TAG, message);
    }

    static public void logDebug(Context context, String tag, String message) {
        logMesssage(context, Log.DEBUG, tag, message);
    }

    static public void logDebug(Context context, String message) {
        logMesssage(context, Log.DEBUG, DEFAULT_LOG_TAG, message);
    }

    static public void logVerbose(Context context, String tag, String message) {
        logMesssage(context, Log.VERBOSE, tag, message);
    }

    static public void logVerbose(Context context, String message) {
        logMesssage(context, Log.VERBOSE, DEFAULT_LOG_TAG, message);
    }

    static public void logErrorAndShowToast(Context context, String tag, String message) {
        if (context == null) return;

        if(getLogLevel(context) >= LOG_LEVEL_NORMAL) {
            logError(context, tag, message);
            showToast(context, message);
        }
    }

    static public void logErrorAndShowToast(Context context, String message) {
        logErrorAndShowToast(context, DEFAULT_LOG_TAG, message);
    }

    static public void logDebugAndShowToast(Context context, String tag, String message) {
        if (context == null) return;

        if(getLogLevel(context) >= LOG_LEVEL_DEBUG) {
            logDebug(context, tag, message);
            showToast(context, message);
        }
    }

    static public void logDebugAndShowToast(Context context, String message) {
        logDebugAndShowToast(context, DEFAULT_LOG_TAG, message);
    }

    static public void logStackTrace(Context context, String tag, String message, Exception e) {
        if (context == null) return;

        if(getLogLevel(context) >= LOG_LEVEL_NORMAL)
        {
            try {
                StringWriter errors = new StringWriter();
                PrintWriter pw = new PrintWriter(errors);
                e.printStackTrace(pw);
                pw.close();
                if(message != null)
                    Log.e(tag, message + ":\n" + errors.toString());
                else
                    Log.e(tag, errors.toString());
                errors.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    static public void logStackTrace(Context context, String tag, Exception e) {
        logStackTrace(context, tag, null, e);
    }

    static public void logStackTrace(Context context, Exception e) {
        logStackTrace(context, DEFAULT_LOG_TAG, null, e);
    }

    static public void showToast(final Context context, final String toastText) {
        if (context == null) return;

        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, toastText, Toast.LENGTH_LONG).show());
    }

    public static void showSetLogLevelDialog(final Context context) {
        if (context == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.log_level_title));

        String[] logLevels = {
                getLogLevelLabel(context, LOG_LEVEL_OFF, true),
                getLogLevelLabel(context, LOG_LEVEL_NORMAL, true),
                getLogLevelLabel(context, LOG_LEVEL_DEBUG, true),
                getLogLevelLabel(context, LOG_LEVEL_VERBOSE, true)
        };

        int currentLogLevel = getLogLevel(context);
        builder.setSingleChoiceItems(logLevels, currentLogLevel, (dialog, logLevel) -> {
            switch (logLevel) {
                case 0: setLogLevel(context, LOG_LEVEL_OFF); break;
                case 1: setLogLevel(context, LOG_LEVEL_NORMAL); break;
                case 2: setLogLevel(context, LOG_LEVEL_DEBUG); break;
                case 3: setLogLevel(context, LOG_LEVEL_VERBOSE); break;
                default: break;
            }
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    static public int getLogLevel(Context context) {
        return QueryPreferences.getLogLevel(context);
    }

    static public int getLogLevelFromFile(Context context) {
        return QueryPreferences.getLogLevelFromFile(context);
    }

    public static String getLogLevelLabel(final Context context, final int logLevel, final boolean addDefaultTag) {
        String logLabel;
        switch (logLevel) {
            case LOG_LEVEL_OFF: logLabel = context.getString(R.string.log_level_off); break;
            case LOG_LEVEL_NORMAL: logLabel = context.getString(R.string.log_level_normal); break;
            case LOG_LEVEL_DEBUG: logLabel = context.getString(R.string.log_level_debug); break;
            case LOG_LEVEL_VERBOSE: logLabel = context.getString(R.string.log_level_verbose); break;
            default: logLabel = context.getString(R.string.log_level_unknown); break;
        }

        if (addDefaultTag && logLevel == DEFAULT_LOG_LEVEL)
            return logLabel + " (default)";
        else
            return logLabel;
    }

    static public void setLogLevel(Context context, int logLevel) {
        QueryPreferences.setLogLevel(context, logLevel);
        showToast(context, context.getString(R.string.log_level_value, getLogLevelLabel(context, getLogLevel(context), false)));
    }

}
