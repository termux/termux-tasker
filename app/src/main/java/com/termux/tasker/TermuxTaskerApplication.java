package com.termux.tasker;

import android.app.Application;
import android.content.Context;

import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.crash.TermuxCrashUtils;
import com.termux.shared.termux.settings.preferences.TermuxTaskerAppSharedPreferences;

public class TermuxTaskerApplication extends Application {

    public void onCreate() {
        super.onCreate();

        Context context = getApplicationContext();

        // Set crash handler for the app
        TermuxCrashUtils.setCrashHandler(this);

        // Set log config for the app
        setLogConfig(context, true);

        Logger.logDebug("Starting Application");
    }

    public static void setLogConfig(Context context, boolean commitToFile) {
        Logger.setDefaultLogTag(TermuxConstants.TERMUX_TASKER_APP_NAME.replaceAll(":", ""));

        // Load the log level from shared preferences and set it to the Logger.CURRENT_LOG_LEVEL
        TermuxTaskerAppSharedPreferences preferences = TermuxTaskerAppSharedPreferences.build(context);
        if (preferences == null) return;
        preferences.setLogLevel(null, preferences.getLogLevel(true), commitToFile);
    }

}
