package com.termux.tasker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.termux.shared.packages.PackageUtils;
import com.termux.shared.termux.TermuxConstants;

/**
 * Class for managing the {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE} for this plug-in.
 */
public class PluginBundleManager {

    /** The {@code String} extra for the path to the executable to execute. */
    public static final String EXTRA_EXECUTABLE = TermuxConstants.TERMUX_TASKER_PACKAGE_NAME + ".extra.EXECUTABLE"; // Default: "com.termux.tasker.extra.EXECUTABLE"

    /** The {@code String} extra for the arguments to pass to the executable. */
    public static final String EXTRA_ARGUMENTS = TermuxConstants.TERMUX_PACKAGE_NAME + ".execute.arguments"; // Default: "com.termux.execute.arguments"

    /** The {@code String} extra for path to current working directory for execution. */
    public static final String EXTRA_WORKDIR = TermuxConstants.TERMUX_TASKER_PACKAGE_NAME + ".extra.WORKDIR"; // Default: "com.termux.tasker.extra.WORKDIR"

    /** The {@code boolean} extra for whether the executable should be run inside a terminal. */
    public static final String EXTRA_TERMINAL = TermuxConstants.TERMUX_TASKER_PACKAGE_NAME + ".extra.TERMINAL"; // Default: "com.termux.tasker.extra.TERMINAL"

    /** The {@code boolean} extra for whether plugin action should wait for result of commands or not. */
    public static final String EXTRA_WAIT_FOR_RESULT = TermuxConstants.TERMUX_TASKER_PACKAGE_NAME + ".extra.WAIT_FOR_RESULT"; // Default: "com.termux.tasker.extra.WAIT_FOR_RESULT"

    /**
     * The {@code int} extra for the versionCode of the plugin app that saved the Bundle.
     *
     * This extra is not strictly required, however it makes backward and forward compatibility significantly
     * easier. For example, suppose a bug is found in how some version of the plug-in stored its Bundle. By
     * having the version, the plug-in can better detect when such bugs occur.
     */
    public static final String BUNDLE_EXTRA_INT_VERSION_CODE = TermuxConstants.TERMUX_TASKER_PACKAGE_NAME + ".extra.VERSION_CODE"; // Default: "com.termux.tasker.extra.VERSION_CODE"

    /**
     * Method to verify the content of the bundle are correct.
     * <p>
     * This method will not mutate {@code bundle}.
     *
     * @param context The {@link Context} to get error string.
     * @param bundle The {@link Bundle} to verify. May be {@code null}, which will always return {@code false}.
     * @return Returns the {@code errmsg} if Bundle is not valid, otherwise {@code null}.
     */
    @SuppressLint("DefaultLocale")
    public static String parseBundle(final Context context, final Bundle bundle) {
        if (bundle == null) return context.getString(R.string.error_null_or_empty_executable);

        /*
         * Make sure the correct number of extras exist.
         * The bundle must contain:
         * - EXTRA_EXECUTABLE
         * - EXTRA_ARGUMENTS
         * - BUNDLE_EXTRA_INT_VERSION_CODE
         * The bundle may optionally contain:
         * - EXTRA_WORKDIR
         * - EXTRA_TERMINAL
         * - EXTRA_WAIT_FOR_RESULT
         * - VARIABLE_REPLACE_KEYS
         */

        if (!bundle.containsKey(EXTRA_EXECUTABLE)) {
            return String.format("The bundle must contain extra %s.", EXTRA_EXECUTABLE);
        }

        if (!bundle.containsKey(EXTRA_ARGUMENTS)) {
            return String.format("The bundle must contain extra %s.", EXTRA_ARGUMENTS);
        }

        if (!bundle.containsKey(BUNDLE_EXTRA_INT_VERSION_CODE)) {
            return String.format("The bundle must contain extra %s.", BUNDLE_EXTRA_INT_VERSION_CODE);
        }

        /*
         * Check if bundle contains at least 3 keys but no more than 7.
         * Run this test after checking for required Bundle extras above so that the error message
         * is more useful. (E.g. the caller will see what extras are missing, rather than just a
         * message that there is the wrong number).
         */
        if (bundle.keySet().size() < 3 || bundle.keySet().size() > 7) {
            return String.format("The bundle must contain 3-7 keys, but currently contains %d keys.", bundle.keySet().size());
        }

        if (TextUtils.isEmpty(bundle.getString(EXTRA_EXECUTABLE))) {
            return String.format("The bundle extra %s appears to be null or empty. It must be a non-empty string.", EXTRA_EXECUTABLE);
        }

        if (bundle.getInt(BUNDLE_EXTRA_INT_VERSION_CODE, 0) != bundle.getInt(BUNDLE_EXTRA_INT_VERSION_CODE, 1)) {
            return String.format("The bundle extra %s appears to be the wrong type. It must be an int.", BUNDLE_EXTRA_INT_VERSION_CODE);
        }

        if (!bundle.containsKey(EXTRA_WAIT_FOR_RESULT)) {
            // Termux:Tasker <= v0.5 did not have the EXTRA_WAIT_FOR_RESULT key so we only wait
            // for results for background commands
            if (bundle.containsKey(EXTRA_TERMINAL))
                bundle.putBoolean(EXTRA_WAIT_FOR_RESULT, !bundle.getBoolean(EXTRA_TERMINAL));
            else
                bundle.putBoolean(EXTRA_WAIT_FOR_RESULT, true);
        }

        return null;
    }

    public static Bundle generateBundle(final Context context, final String executable,
                                        final String arguments, final String workingDirectory,
                                        final boolean inTerminal, final boolean waitForResult) {
        final Bundle result = new Bundle();
        result.putString(EXTRA_EXECUTABLE, executable);
        result.putString(EXTRA_ARGUMENTS,arguments);
        result.putString(EXTRA_WORKDIR, workingDirectory);
        result.putBoolean(EXTRA_TERMINAL, inTerminal);
        result.putBoolean(EXTRA_WAIT_FOR_RESULT, waitForResult);
        result.putInt(BUNDLE_EXTRA_INT_VERSION_CODE, PackageUtils.getVersionCodeForPackage(context));
        return result;
    }
}
