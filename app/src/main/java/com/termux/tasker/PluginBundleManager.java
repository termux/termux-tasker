package com.termux.tasker;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
/**
 * Class for managing the {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE} for this plug-in.
 */
final class PluginBundleManager {

    /**
     * Type: {@code sting}.
     *
     * The arguments to pass to the script.
     */
    public static final String EXTRA_ARGUMENTS = "com.termux.execute.arguments";

    /**
     * Type: {@code String}.
     *
     * The path to the executable to execute.
     */
    public static final String EXTRA_EXECUTABLE = "com.termux.tasker.extra.EXECUTABLE";

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
     * @param bundle bundle to verify. May be null, which will always return false.
     * @return true if the Bundle is valid, false if the bundle is invalid.
     */
    public static boolean isBundleValid(final Bundle bundle) {
        if (null == bundle) return false;

        if (!bundle.containsKey(EXTRA_EXECUTABLE)) {
            Log.e(Constants.LOG_TAG, String.format("bundle must contain extra %s", EXTRA_EXECUTABLE));
            return false;
        }

        if (!bundle.containsKey(EXTRA_ARGUMENTS)) {
            Log.e(Constants.LOG_TAG, String.format("bundle must contain extra %s", EXTRA_ARGUMENTS));
            return false;
        }

        if (!bundle.containsKey(BUNDLE_EXTRA_INT_VERSION_CODE)) {
            Log.e(Constants.LOG_TAG, String.format("bundle must contain extra %s", BUNDLE_EXTRA_INT_VERSION_CODE));
            return false;
        }

        /*
         * Make sure the correct number of extras exist. Run this test after checking for specific Bundle
         * extras above so that the error message is more useful. (E.g. the caller will see what extras are
         * missing, rather than just a message that there is the wrong number).
         */
        if (4 != bundle.keySet().size()) {
            if (bundle.containsKey("net.dinglisch.android.tasker.extras.VARIABLE_REPLACE_KEYS")){
                return true;
            }
            Log.e(Constants.LOG_TAG, String.format("bundle must contain 4 keys, but currently contains %d keys", bundle.keySet().size()));
            return false;
        }

        if (TextUtils.isEmpty(bundle.getString(EXTRA_EXECUTABLE))) {
            Log.e(Constants.LOG_TAG, String.format("bundle extra %s appears to be null or empty.  It must be a non-empty string", EXTRA_EXECUTABLE));
            return false;
        }

        if (bundle.getInt(BUNDLE_EXTRA_INT_VERSION_CODE, 0) != bundle.getInt(BUNDLE_EXTRA_INT_VERSION_CODE, 1)) {
            Log.e(Constants.LOG_TAG, String.format("bundle extra %s appears to be the wrong type.  It must be an int", BUNDLE_EXTRA_INT_VERSION_CODE));
            return false;
        }

        return true;
    }

    public static Bundle generateBundle(final Context context, final String executable, final String arguments, final boolean inTerminal) {
        final Bundle result = new Bundle();
        result.putInt(BUNDLE_EXTRA_INT_VERSION_CODE, Constants.getVersionCode(context));
        result.putString(EXTRA_ARGUMENTS,arguments);
        result.putString(EXTRA_EXECUTABLE, executable);
        result.putBoolean(EXTRA_TERMINAL, inTerminal);
        return result;
    }
}