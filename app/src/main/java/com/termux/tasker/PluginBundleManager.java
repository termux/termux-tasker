package com.termux.tasker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.android.PackageUtils;
import com.termux.shared.data.DataUtils;
import com.termux.shared.logger.Logger;
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

    /** The {@code String} extra for stdin for background commands. */
    public static final String EXTRA_STDIN = TermuxConstants.TERMUX_TASKER_PACKAGE_NAME + ".extra.STDIN"; // Default: "com.termux.tasker.extra.STDIN"

    /** The {@code String} extra for terminal session action defined by
     * {@link com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE}
     * `VALUE_EXTRA_SESSION_ACTION_*` values.
     */
    public static final String EXTRA_SESSION_ACTION = TermuxConstants.TERMUX_TASKER_PACKAGE_NAME + ".extra.SESSION_ACTION"; // Default: "com.termux.tasker.extra.SESSION_ACTION"

    /** The {@code String} extra for custom log levels for background commands between
     * {@link Logger#LOG_LEVEL_OFF} and {@link Logger#MAX_LOG_LEVEL} as per
     * https://github.com/termux/termux-app/commit/60f37bde.
     */
    public static final String EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL = TermuxConstants.TERMUX_TASKER_PACKAGE_NAME + ".extra.BACKGROUND_CUSTOM_LOG_LEVEL"; // Default: "com.termux.tasker.extra.BACKGROUND_CUSTOM_LOG_LEVEL"

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

    public static final String UNICODE_CHECK = "\u2713";
    public static final String UNICODE_UNCHECK = "\u2715";

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
    public static String parseBundle(@NonNull final Context context, final Bundle bundle) {
        if (bundle == null) return context.getString(R.string.error_null_bundle);

        /*
         * Make sure the correct number of extras exist.
         * The bundle must contain:
         * - EXTRA_EXECUTABLE
         * - EXTRA_ARGUMENTS
         * - BUNDLE_EXTRA_INT_VERSION_CODE
         * The bundle may optionally contain:
         * - EXTRA_WORKDIR
         * - EXTRA_STDIN
         * - EXTRA_SESSION_ACTION
         * - EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL
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
         * Check if bundle contains at least 3 keys but no more than 10.
         * Run this test after checking for required Bundle extras above so that the error message
         * is more useful. (E.g. the caller will see what extras are missing, rather than just a
         * message that there is the wrong number).
         */
        if (bundle.keySet().size() < 3 || bundle.keySet().size() > 10) {
            return String.format("The bundle must contain 3-10 keys, but currently contains %d keys.", bundle.keySet().size());
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

    @Nullable
    public static Bundle generateBundle(@NonNull final Context context, final String executable,
                                        final String arguments, final String workingDirectory,
                                        final String stdin, final String sessionAction,
                                        final String backgroundCustomLogLevel,
                                        final boolean inTerminal, final boolean waitForResult) {
        final Bundle result = new Bundle();
        result.putString(EXTRA_EXECUTABLE, executable);
        result.putString(EXTRA_ARGUMENTS,arguments);
        result.putString(EXTRA_WORKDIR, workingDirectory);
        result.putBoolean(EXTRA_TERMINAL, inTerminal);
        result.putBoolean(EXTRA_WAIT_FOR_RESULT, waitForResult);

        result.putString(EXTRA_STDIN, stdin);
        result.putString(EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL, backgroundCustomLogLevel);
        result.putString(EXTRA_SESSION_ACTION, sessionAction);

        Integer versionCode = PackageUtils.getVersionCodeForPackage(context);
        if (versionCode == null) {
            Logger.showToast(context, context.getString(R.string.error_get_version_code_failed, context.getPackageName()), true);
            return null;
        }

        result.putInt(BUNDLE_EXTRA_INT_VERSION_CODE, versionCode);
        return result;
    }

    /**
     * The message that will be displayed by the plugin host app for the action configuration.
     * Blurb length can be a maximum of 60 characters as defined by locale lib.
     * @return A blurb for the plug-in.
     */
    public static String generateBlurb(@NonNull final Context context, final String executable,
                                       final String arguments, final String workingDirectory,
                                       final String stdin, final String sessionAction,
                                       final String backgroundCustomLogLevel,
                                       final boolean inTerminal, final boolean waitForResult) {
        StringBuilder builder = new StringBuilder();
        builder.append(context.getString(R.string.blurb_executable_and_arguments, executable,
                arguments == null  ? "" : " " + (arguments.length() > 20 ? arguments.substring(0, 20) : arguments)));
        builder.append("\n\n").append(context.getString(R.string.blurb_working_directory, (!DataUtils.isNullOrEmpty(workingDirectory) ? UNICODE_CHECK : UNICODE_UNCHECK)));

        if (!inTerminal) {
            builder.append("\n").append(context.getString(R.string.blurb_stdin, (!DataUtils.isNullOrEmpty(stdin) ? UNICODE_CHECK : UNICODE_UNCHECK)));
            builder.append("\n").append(context.getString(R.string.blurb_custom_log_level, backgroundCustomLogLevel));
        } else {
            if (!DataUtils.isNullOrEmpty(sessionAction))
                builder.append("\n").append(context.getString(R.string.blurb_session_action, sessionAction));
        }

        builder.append("\n").append(context.getString(R.string.blurb_in_terminal, (inTerminal ? UNICODE_CHECK : UNICODE_UNCHECK)));
        builder.append("\n").append(context.getString(R.string.blurb_wait_for_result, (waitForResult ? UNICODE_CHECK : UNICODE_UNCHECK)));

        String blurb = builder.toString();
        final int maxBlurbLength = 120; // R.integer.twofortyfouram_locale_maximum_blurb_length is set to 60 but we are ignoring that since Tasker doesn't have that limit.
        return (blurb.length() > maxBlurbLength) ? blurb.substring(0, maxBlurbLength) : blurb;
    }

    /** Get size of {@link Bundle} when stored as a {@link Parcel}. */
    public static int getBundleSize(@NonNull Bundle bundle) {
        Parcel parcel = Parcel.obtain();
        bundle.writeToParcel(parcel, 0);
        int size = parcel.dataSize();
        parcel.recycle();
        return size;
    }

}
