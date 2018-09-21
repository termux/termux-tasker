package com.termux.tasker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is the "fire" BroadcastReceiver for a Locale Plug-in.
 */
public final class FireReceiver extends BroadcastReceiver {

    public static final String TERMUX_SERVICE = "com.termux.app.TermuxService";
    public static final String ACTION_EXECUTE = "com.termux.service_execute";

    public void onReceive(final Context context, final Intent intent) {
        if (!com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING.equals(intent.getAction())) {
            Log.e(Constants.LOG_TAG, "Unexpected intent action: " + intent.getAction());
            return;
        }

        BundleScrubber.scrub(intent);
        final Bundle bundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
        BundleScrubber.scrub(bundle);

        if (!PluginBundleManager.isBundleValid(bundle)) return;

        final String executable = bundle.getString(PluginBundleManager.EXTRA_EXECUTABLE);
        final String arguments = bundle.getString(PluginBundleManager.EXTRA_ARGUMENTS);
        final boolean inTerminal = bundle.getBoolean(PluginBundleManager.EXTRA_TERMINAL);
        Matcher matcher = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(arguments);
        List<String> list = new ArrayList<>();
        while (matcher.find()){
            list.add(matcher.group(1).replace("\"",""));
        }

        File executableFile = new File(EditConfigurationActivity.TASKER_DIR, executable);
        if (!executableFile.isFile()) {
            String message = "Termux:Tasker - no such executable:\n" + executable;
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            return;
        }

        ensureFileReadableAndExecutable(executableFile);
        Uri scriptUri = new Uri.Builder().scheme("com.termux.file").path(executableFile.getAbsolutePath()).build();

        // Note: Must match TermuxService#ACTION_EXECUTE constant:
        Intent executeIntent = new Intent(ACTION_EXECUTE, scriptUri);
        executeIntent.setClassName("com.termux", TERMUX_SERVICE);
        if (!inTerminal) executeIntent.putExtra("com.termux.execute.background", true);
        executeIntent.putExtra(PluginBundleManager.EXTRA_ARGUMENTS, list.toArray(new String[list.size()]));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // https://developer.android.com/about/versions/oreo/background.html
            context.startForegroundService(executeIntent);
        } else {
            context.startService(executeIntent);
        }
    }

    /** Ensure readable and executable file if user forgot to do so. */
    static void ensureFileReadableAndExecutable(File file) {
        if (!file.canRead()) file.setReadable(true);
        if (!file.canExecute()) file.setExecutable(true);
    }

}