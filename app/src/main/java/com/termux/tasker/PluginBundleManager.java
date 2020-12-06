package com.termux.tasker;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.termux.tasker.utils.PluginUtils;

/**
 * Class for managing the {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE} for this plug-in.
 */
final class PluginBundleManager {

    /**
     * Type: {@code String}.
     *
     * The path to the executable to execute.
     */
    public static final String EXTRA_EXECUTABLE = "com.termux.tasker.extra.EXECUTABLE";

    /**
     * Type: {@code sting}.
     *
     * The arguments to pass to the script.
     */
    public static final String EXTRA_ARGUMENTS = "com.termux.execute.arguments";

    /**
     * Type: {@code String}.
     *
     * The path to current working directory for execution.
     */
    public static final String EXTRA_WORKDIR = "com.termux.tasker.extra.WORKDIR";

    /**
     * Type: {@code boolean}.
     *
     * If the executable should be run inside a terminal.
     */
    public static final String EXTRA_TERMINAL = "com.termux.tasker.extra.TERMINAL";

    /**
     * Type: {@code int}.
     * <p>
     * versionCode of the plug-in that saved the Bundle.
     *
     * This extra is not strictly required, however it makes backward and forward compatibility significantly
     * easier. For example, suppose a bug is found in how some version of the plug-in stored its Bundle. By
     * having the version, the plug-in can better detect when such bugs occur.
     */
    public static final String BUNDLE_EXTRA_INT_VERSION_CODE = "com.termux.tasker.extra.VERSION_CODE";

    /**
     * Method to verify the content of the bundle are correct.
     * <p>
     * This method will not mutate {@code bundle}.
     *
     * @param context to get error string.
     * @param bundle bundle to verify. May be null, which will always return false.
     * @return errmsg if Bundle is not valid, otherwise null.
     */
    public static String isBundleValid(final Context context, final Bundle bundle) {
        if (bundle == null) return context.getString(R.string.null_or_empty_executable);

        /*
         * Make sure the correct number of extras exist.
         * The bundle must contain:
         * - EXTRA_EXECUTABLE
         * - EXTRA_ARGUMENTS
         * - BUNDLE_EXTRA_INT_VERSION_CODE
         * The bundle may optionally contain:
         * - EXTRA_WORKDIR
         * - EXTRA_TERMINAL
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
         * Check if bundle contains at least 3 keys but no more than 6.
         * Run this test after checking for required Bundle extras above so that the error message
         * is more useful. (E.g. the caller will see what extras are missing, rather than just a
         * message that there is the wrong number).
         */
        if (bundle.keySet().size() < 3 || bundle.keySet().size() > 6) {
            return String.format("The bundle must contain 3-6 keys, but currently contains %d keys.", bundle.keySet().size());
        }

        if (TextUtils.isEmpty(bundle.getString(EXTRA_EXECUTABLE))) {
            return String.format("The bundle extra %s appears to be null or empty. It must be a non-empty string.", EXTRA_EXECUTABLE);
        }

        if (bundle.getInt(BUNDLE_EXTRA_INT_VERSION_CODE, 0) != bundle.getInt(BUNDLE_EXTRA_INT_VERSION_CODE, 1)) {
            return String.format("The bundle extra %s appears to be the wrong type. It must be an int.", BUNDLE_EXTRA_INT_VERSION_CODE);
        }

        return null;
    }

    public static Bundle generateBundle(final Context context, final String executable, final String arguments, final String workingDirectory, final boolean inTerminal) {
        final Bundle result = new Bundle();
        result.putString(EXTRA_EXECUTABLE, executable);
        result.putString(EXTRA_ARGUMENTS,arguments);
        result.putString(EXTRA_WORKDIR, workingDirectory);
        result.putBoolean(EXTRA_TERMINAL, inTerminal);
        result.putInt(BUNDLE_EXTRA_INT_VERSION_CODE, PluginUtils.getVersionCode(context));
        return result;
    }
}
