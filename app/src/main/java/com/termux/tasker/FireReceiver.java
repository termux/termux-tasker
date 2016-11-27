package com.termux.tasker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

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
        final boolean inTerminal = bundle.getBoolean(PluginBundleManager.EXTRA_TERMINAL);

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
        context.startService(executeIntent);
    }

    /** Ensure readable and executable file if user forgot to do so. */
    static void ensureFileReadableAndExecutable(File file) {
        if (!file.canRead()) file.setReadable(true);
        if (!file.canExecute()) file.setExecutable(true);
    }

}