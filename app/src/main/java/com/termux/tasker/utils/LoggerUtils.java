package com.termux.tasker.utils;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.termux.shared.termux.settings.preferences.TermuxTaskerAppSharedPreferences;
import com.termux.tasker.R;
import com.termux.shared.logger.Logger;

import static com.termux.shared.logger.Logger.getLogLevelLabel;

public class LoggerUtils {

    public static void showSetLogLevelDialog(final Context context) {
        if (context == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.log_level_title));

        String[] logLevels = {
                getLogLevelLabel(context, Logger.LOG_LEVEL_OFF, true),
                getLogLevelLabel(context, Logger.LOG_LEVEL_NORMAL, true),
                getLogLevelLabel(context, Logger.LOG_LEVEL_DEBUG, true),
                getLogLevelLabel(context, Logger.LOG_LEVEL_VERBOSE, true)
        };

        TermuxTaskerAppSharedPreferences preferences =  TermuxTaskerAppSharedPreferences.build(context);
        if (preferences == null) return;

        int currentLogLevel = preferences.getLogLevel(true);

        builder.setSingleChoiceItems(logLevels, currentLogLevel, (dialog, logLevelIndex) -> {
            int logLevel;
            switch (logLevelIndex) {
                case 0:
                    logLevel = Logger.LOG_LEVEL_OFF;
                    break;
                case 1:
                    logLevel = Logger.LOG_LEVEL_NORMAL;
                    break;
                case 2:
                    logLevel = Logger.LOG_LEVEL_DEBUG;
                    break;
                case 3:
                    logLevel = Logger.LOG_LEVEL_VERBOSE;
                    break;
                default:
                    logLevel = Logger.DEFAULT_LOG_LEVEL;
                    break;
            }

            preferences.setLogLevel(context, logLevel, true);

            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
