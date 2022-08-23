package com.termux.tasker.utils;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.termux.shared.data.DataUtils;
import com.termux.shared.errors.Errno;
import com.termux.shared.file.FileUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.shell.command.result.ResultConfig;
import com.termux.shared.shell.command.result.ResultData;
import com.termux.shared.shell.command.result.ResultSender;
import com.termux.shared.shell.command.runner.app.AppShell;
import com.termux.shared.termux.settings.preferences.TermuxPreferenceConstants.TERMUX_TASKER_APP;
import com.termux.shared.termux.settings.preferences.TermuxTaskerAppSharedPreferences;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.tasker.FireReceiver;
import com.termux.tasker.PluginResultsService;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE;
import com.termux.shared.settings.properties.SharedProperties;
import com.termux.tasker.R;

import java.util.regex.Pattern;

/**
 * A util class to handle sending of plugin commands to the execution service,
 * processing of the result of plugin commands received back from the execution service and
 * sending immediate or pending results back to the plugin host.
 *
 * This is currently designed and tested with Tasker as the plugin host app and the
 * {@link TermuxConstants.TERMUX_APP.TERMUX_SERVICE} as the execution service of plugin
 * commands but should work with other plugin host apps.
 * The {@link TermuxConstants.TERMUX_APP.TERMUX_SERVICE} will run the commands with the
 * {@link AppShell} class if background mode is enabled and
 * {@link TermuxSession} if foreground terminal mode is enabled. The
 * result of commands is only returned if the plugin action bundle has
 * {@link com.termux.tasker.PluginBundleManager#EXTRA_WAIT_FOR_RESULT} set to true.
 *
 * Flow to be used for the commands received by {@link FireReceiver}:
 * 1. Call {@link #sendExecuteIntentToExecuteService} from {@link FireReceiver}. This expects the
 * original intent received by {@link FireReceiver}, the execution intent that should be sent to
 * call the execution service containing its {@link ComponentName} and any extras required to run the
 * plugin commands, and a boolean for whether commands are to be run in background mode or not.
 * If the plugin action has a timeout greater than 0 and result is to be returned, then the
 * function will automatically create a {@link android.app.PendingIntent} that can be used to
 * return the results back to {@link PluginResultsService} and add the original
 * {@link android.content.Intent} received by {@link FireReceiver} as {@link #EXTRA_ORIGINAL_INTENT}
 * to it and then add the created {@link android.app.PendingIntent} to the execution intent as
 * {@link TERMUX_SERVICE#EXTRA_PENDING_INTENT} extra.
 * Otherwise, the function immediately returns  {@link TaskerPlugin.Setting#RESULT_CODE_OK}
 * back to the plugin host app using the {@link #sendImmediateResultToPluginHostApp} function and
 * the flow ends here for the usage of {@link FireReceiver}.
 *
 * 2. The execution service should receive the execution intent and run the required plugin commands.
 * If background mode is enabled, then results should be returned back via the
 * {@link android.app.PendingIntent} received, which in this case will be meant for the
 * {@link PluginResultsService}. The send the result back, the execution service can send the result
 * intent with {@link ResultSender#sendCommandResultDataWithPendingIntent(Context, String, String, ResultConfig, ResultData, boolean)},
 * like TermuxService does via com.termux.app.utils.PluginUtils.processPluginExecutionCommandResult().
 * The pending intent should already contain the original {@link android.content.Intent} received by the
 * {@link FireReceiver}. A result {@link Bundle} object with the {@link TERMUX_SERVICE#EXTRA_PLUGIN_RESULT_BUNDLE}
 * key should also be sent back in an {@link android.content.Intent} using the
 * {@link android.app.PendingIntent#send(Context, int, Intent)} function. The bundle can contain
 * the keys whose values will be sent back to the plugin host app:
 * {@link TERMUX_SERVICE#EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT} (String),
 * {@link TERMUX_SERVICE#EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT_ORIGINAL_LENGTH} (String)
 * {@link TERMUX_SERVICE#EXTRA_PLUGIN_RESULT_BUNDLE_STDERR} (String),
 * {@link TERMUX_SERVICE#EXTRA_PLUGIN_RESULT_BUNDLE_STDERR_ORIGINAL_LENGTH} (String),
 * {@link TERMUX_SERVICE#EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE} (Integer),
 * {@link TERMUX_SERVICE#EXTRA_PLUGIN_RESULT_BUNDLE_ERR} (Integer) and
 * {@link TERMUX_SERVICE#EXTRA_PLUGIN_RESULT_BUNDLE_ERRMSG} (String)
 *
 * 3. The {@link android.app.PendingIntent} sent is received by the {@link PluginResultsService}
 * with the onHandleIntent function which calls the
 * {@link #sendPendingResultToPluginHostApp} function with the intent received.
 *
 * 4. The {@link #sendPendingResultToPluginHostApp} function extracts the original intent and the
 * result bundle from the intent received and calls the {@link #createVariablesBundle} function
 * to create the variables bundle to be sent back to plugin host and then sends it with
 * {@link TaskerPlugin.Setting#signalFinish} function. If any of the Integer keys
 * do not exist in the result bundle or if the values of String keys are null or empty in the result
 * bundle, then they are not sent back to the plugin host. The flow ends here.
 *
 * The {@link #sendImmediateResultToPluginHostApp} function can be used to send result back to the
 * plugin host immediately like in case there is an error processing the plugin command request or
 * if the result should not be expected to be sent back by the execution service.
 *
 * The {@link #createVariablesBundle} function creates a variables bundle that can be sent back to
 * the plugin host. The bundle will contain the keys:
 * {@link #PLUGIN_VARIABLE_STDOUT} (String),
 * {@link #PLUGIN_VARIABLE_STDOUT_ORIGINAL_LENGTH} (String),
 * {@link #PLUGIN_VARIABLE_STDERR} (String),
 * {@link #PLUGIN_VARIABLE_STDERR_ORIGINAL_LENGTH} (String),
 * {@link #PLUGIN_VARIABLE_EXIT_CODE} (String)
 * and {@link #PLUGIN_VARIABLE_ERRMSG} (String).
 *
 * The {@link #PLUGIN_VARIABLE_ERRMSG} key will only be added if the {@link #PLUGIN_VARIABLE_ERR} value
 * to be sent back to the plugin host is greater than {@link TaskerPlugin.Setting#RESULT_CODE_OK}.
 * Any null or empty values are not added to the variables bundle.
 *
 * The value for {@link #PLUGIN_VARIABLE_ERR} is first sanitized by the {@link #sanitizeErrCode}
 * function before it is sent back to the plugin host.
 */
public class PluginUtils {

    /** Plugin variable for stdout value of termux command */
    public static final String PLUGIN_VARIABLE_STDOUT = "%stdout"; // Default: "%stdout"
    /** Plugin variable for original length of stdout value of termux command */
    public static final String PLUGIN_VARIABLE_STDOUT_ORIGINAL_LENGTH = "%stdout_original_length"; // Default: "%stdout_original_length"
    /** Plugin variable for stderr value of termux command */
    public static final String PLUGIN_VARIABLE_STDERR = "%stderr"; // Default: "%stderr"
    /** Plugin variable for original length of stderr value of termux command */
    public static final String PLUGIN_VARIABLE_STDERR_ORIGINAL_LENGTH = "%stderr_original_length"; // Default: "%stderr_original_length"
    /** Plugin variable for exit code value of termux command */
    public static final String PLUGIN_VARIABLE_EXIT_CODE = "%result"; // Default: "%result"
    /** Plugin variable for err value of termux command */
    public static final String PLUGIN_VARIABLE_ERR = "%err"; // Default: "%err"
    /** Plugin variable for errmsg value of termux command */
    public static final String PLUGIN_VARIABLE_ERRMSG = "%errmsg"; // Default: "%errmsg"

    /** Intent {@code Parcelable} extra containing original intent received from plugin host app by FireReceiver */
    public static final String EXTRA_ORIGINAL_INTENT = "originalIntent"; // Default: "originalIntent"

    /**
     * A regex to validate if a string matches a valid plugin host variable name with the percent sign "%" prefix.
     * Valid values: A string containing a percent sign character "%", followed by 1 alphanumeric character,
     * followed by 2 or more alphanumeric or underscore "_" characters but does not end with an underscore "_"
     */
    public static final String PLUGIN_HOST_VARIABLE_NAME_MATCH_EXPRESSION =  "%[a-zA-Z0-9][a-zA-Z0-9_]{2,}(?<!_)";

    private static final String LOG_TAG = "PluginUtils";

    /**
     * Send execution intent to execution service containing command information and original intent
     * received by {@link FireReceiver}.
     *
     * @param context The {@link Context} that will be used to send execution intent to the execution service.
     * @param receiver The {@link BroadcastReceiver} of the originalIntent.
     * @param originalIntent The {@link Intent} received by {@link FireReceiver}.
     * @param executionIntent The {@link Intent} to be sent to execution service containing command information.
     * @param waitForResult This must be set to {@link true} if plugin action should wait for result
     *                      from the execution service that should be sent back to plugin host synchronously.
     */
    public static void sendExecuteIntentToExecuteService(final Context context, final BroadcastReceiver receiver,
                                                         final Intent originalIntent, final Intent executionIntent,
                                                         boolean waitForResult) {
        if (context == null) return;

        if (executionIntent == null) {
            Logger.logError(LOG_TAG, "The executionIntent passed to sendExecuteIntentToExecuteService() cannot be null.");
            return;
        }

        if (executionIntent.getComponent() == null) {
            Logger.logError(LOG_TAG, "The Component for the executionIntent passed to sendExecuteIntentToExecuteService() cannot be null.");
            return;
        }

        Logger.logVerbose(LOG_TAG, "Ordered Broadcast (timeout > 0): `" + (receiver != null && receiver.isOrderedBroadcast()) + "`");

        // If timeout for plugin action is greater than 0 and plugin action should wait for results
        waitForResult = (receiver != null && receiver.isOrderedBroadcast() && waitForResult);
        Logger.logDebug(LOG_TAG, "Sending execution intent to " + executionIntent.getComponent().toString() + (waitForResult ? " and " : " without ") + "waiting for result");

        if (waitForResult) {
            // Notify plugin host app that result will be sent later
            // Result should be sent to PluginResultsService via a PendingIntent by execution service
            // after commands have finished executing
            receiver.setResultCode(TaskerPlugin.Setting.RESULT_CODE_PENDING);

            // Create intent for PluginResultsService class and add original intent received by
            // FireReceiver to it
            Intent pluginResultsServiceIntent = new Intent(context, PluginResultsService.class);
            pluginResultsServiceIntent.putExtra(EXTRA_ORIGINAL_INTENT, originalIntent);

            // Create PendingIntent that can be used by execution service to send result of commands
            // back to PluginResultsService
            PendingIntent pendingIntent = PendingIntent.getService(context, getLastPendingIntentRequestCode(context), pluginResultsServiceIntent, PendingIntent.FLAG_ONE_SHOT);
            executionIntent.putExtra(TERMUX_SERVICE.EXTRA_PENDING_INTENT, pendingIntent);
        } else {
            // If execution result is not to be returned, do not expect results back from the
            // execution service and return result now so that plugin action does not timeout
            sendImmediateResultToPluginHostApp(receiver, originalIntent, TaskerPlugin.Setting.RESULT_CODE_OK, null);
        }

        try {
            // Send execution intent to execution service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // https://developer.android.com/about/versions/oreo/background.html
                context.startForegroundService(executionIntent);
            } else {
                context.startService(executionIntent);
            }
        } catch (Exception e) {
            String errmsg = Logger.getMessageAndStackTraceString("Failed to send execution intent to " + executionIntent.getComponent().toString(), e);
            Logger.logErrorAndShowToast(context, LOG_TAG, errmsg);
            PluginUtils.sendImmediateResultToPluginHostApp(receiver, originalIntent, TaskerPlugin.Setting.RESULT_CODE_FAILED, errmsg);
        }
    }

    /**
     * Send immediate result to plugin host app in a variables bundle.
     *
     * @param receiver The {@link BroadcastReceiver} of the originalIntent.
     * @param originalIntent The original {@link Intent} received by {@link FireReceiver}.
     * @param errCode The value for {@link #PLUGIN_VARIABLE_ERR} variable of plugin action.
     * @param errmsg The value for {@link #PLUGIN_VARIABLE_ERRMSG} variable of plugin action.
     */
    public static void sendImmediateResultToPluginHostApp(final BroadcastReceiver receiver, final Intent originalIntent,
                                                          final int errCode, final String errmsg) {
        sendImmediateResultToPluginHostApp(receiver, originalIntent, null, null,
                null, null, null, errCode, errmsg);
    }

    /**
     * Send immediate result to plugin host app in a variables bundle.
     *
     * @param receiver The {@link BroadcastReceiver} of the originalIntent.
     * @param originalIntent The original {@link Intent} received by {@link FireReceiver}.
     * @param stdout The value for {@link #PLUGIN_VARIABLE_STDOUT} variable of plugin action.
     * @param stdoutOriginalLength The value for {@link #PLUGIN_VARIABLE_STDOUT_ORIGINAL_LENGTH}
     *                             variable of plugin action.
     * @param stderr The value for {@link #PLUGIN_VARIABLE_STDERR} variable of plugin action.
     * @param stderrOriginalLength The value for {@link #PLUGIN_VARIABLE_STDERR_ORIGINAL_LENGTH}
     *                             variable of plugin action.
     * @param exitCode The value for {@link #PLUGIN_VARIABLE_EXIT_CODE} variable of plugin action.
     * @param errCode The value for {@link #PLUGIN_VARIABLE_ERR} variable of plugin action.
     * @param errmsg The value for {@link #PLUGIN_VARIABLE_ERRMSG} variable of plugin action.
     */
    public static void sendImmediateResultToPluginHostApp(final BroadcastReceiver receiver, final Intent originalIntent,
                                                          final String stdout, String stdoutOriginalLength,
                                                          final String stderr, String stderrOriginalLength,
                                                          final String exitCode, final int errCode, final String errmsg) {
        if (receiver == null) return;

        // If timeout for plugin action is 0, then don't send anything
        if (!receiver.isOrderedBroadcast()) return;

        int err = sanitizeErrCode(errCode);

        Logger.logInfo(LOG_TAG, "Sending immediate result to plugin host app. " + PLUGIN_VARIABLE_ERR + ": " + ((err == TaskerPlugin.Setting.RESULT_CODE_OK) ? "success" : "failed") + " (" + err +  ")");

        if (TaskerPlugin.Setting.hostSupportsVariableReturn(originalIntent.getExtras())) {
            final Bundle varsBundle = createVariablesBundle(stdout, stdoutOriginalLength,
                    stderr, stderrOriginalLength, exitCode, err, errmsg);
            TaskerPlugin.addVariableBundle(receiver.getResultExtras(true), varsBundle);
        }

        receiver.setResultCode(err);
    }

    /**
     * Send pending result to plugin host app in a variables bundle.
     *
     * @param context The {@link Context} that will be used to send variables bundle to plugin host app and logging.
     * @param intent The {@link Intent} containing result and original intent received by {@link FireReceiver}.
     */
    public static void sendPendingResultToPluginHostApp(final Context context, final Intent intent) {
        if (intent == null){
            Logger.logWarn(LOG_TAG, "Ignoring null intent passed to sendPendingResultToPluginHostApp().");
            return;
        }

        final Intent originalIntent = intent.getParcelableExtra(EXTRA_ORIGINAL_INTENT);
        if (originalIntent == null) {
            Logger.logError(LOG_TAG, "The intent passed to sendPendingResultToPluginHostApp() must contain the original intent received by the FireReceiver at the " + EXTRA_ORIGINAL_INTENT + " key.");
            return;
        }

        final Bundle resultBundle = intent.getBundleExtra(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE);
        if (resultBundle == null) {
            Logger.logError(LOG_TAG, "The intent passed to sendPendingResultToPluginHostApp() must contain the result bundle at the " + TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE + " key.");
            return;
        }

        int err = TaskerPlugin.Setting.RESULT_CODE_OK;
        // This check is necessary, otherwise default value will be 0 if extra does not exist,
        // and so plugin host app like Tasker will consider the action as failed for the value 0,
        // since it equals Activity.RESULT_CANCELED (0) instead of TaskerPlugin.Setting.RESULT_CODE_OK/Activity.RESULT_OK (-1)
        if (resultBundle.containsKey(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_ERR))
            err = sanitizeErrCode(resultBundle.getInt(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_ERR));

        Logger.logInfo(LOG_TAG, "Sending pending result to plugin host app. " + PLUGIN_VARIABLE_ERR + ": " + ((err == TaskerPlugin.Setting.RESULT_CODE_OK) ? "success" : "failed") + " (" + err +  ")");

        String exitCode = null;
        if (resultBundle.containsKey(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE))
            exitCode = Integer.toString(resultBundle.getInt(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE));

        final Bundle varsBundle = createVariablesBundle(
                resultBundle.getString(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT, ""),
                resultBundle.getString(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT_ORIGINAL_LENGTH, ""),
                resultBundle.getString(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_STDERR, ""),
                resultBundle.getString(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_STDERR_ORIGINAL_LENGTH, ""),
                exitCode, err, resultBundle.getString(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_ERRMSG, ""));

        if(context != null)
            TaskerPlugin.Setting.signalFinish(context, originalIntent, err, varsBundle);
    }

    /**
     * Process {@link ExecutionCommand} error.
     *
     * The ExecutionCommand currentState must be equal to {@link ExecutionCommand.ExecutionState#FAILED}.
     * The {@link ResultData#getErrCode()} must have been set to a value greater than
     * {@link Errno#ERRNO_SUCCESS}.
     * The {@link ResultData#errorsList} must also be set with appropriate error info.
     *
     * @param context The {@link Context} for operations.
     * @param receiver The {@link BroadcastReceiver} of the originalIntent.
     * @param originalIntent The {@link Intent} received by {@link FireReceiver}.
     * @param logTag The log tag to use for logging.
     * @param executionCommand The {@link ExecutionCommand} that failed.
     * @param errCode The value for {@link #PLUGIN_VARIABLE_ERR} variable of plugin action.
     */
    public static void processPluginExecutionCommandError(final Context context, final BroadcastReceiver receiver, final Intent originalIntent, String logTag, final ExecutionCommand executionCommand, final int errCode) {
        if (context == null || executionCommand == null) return;

        logTag = DataUtils.getDefaultIfNull(logTag, LOG_TAG);

        if (!executionCommand.isStateFailed()) {
            Logger.logWarn(logTag, executionCommand.getCommandIdAndLabelLogString() + ": Ignoring call to processPluginExecutionCommandError() since the execution command is not in ExecutionState.FAILED");
            return;
        }

        boolean isExecutionCommandLoggingEnabled = Logger.shouldEnableLoggingForCustomLogLevel(executionCommand.backgroundCustomLogLevel);

        // Log the error and any exception
        Logger.logErrorExtended(logTag, ExecutionCommand.getExecutionOutputLogString(executionCommand, true,
                true, isExecutionCommandLoggingEnabled));

        PluginUtils.sendImmediateResultToPluginHostApp(receiver, originalIntent,
                errCode, ResultData.getErrorsListMinimalString(executionCommand.resultData));
    }



    /**
     * Create variables bundle to send back to plugin host app.
     *
     * @param stdout The value for {@link #PLUGIN_VARIABLE_STDOUT} variable of plugin action.
     * @param stdoutOriginalLength The value for {@link #PLUGIN_VARIABLE_STDOUT_ORIGINAL_LENGTH}
     *                             variable of plugin action.
     * @param stderr The value for {@link #PLUGIN_VARIABLE_STDERR} variable of plugin action.
     * @param stderrOriginalLength The value for {@link #PLUGIN_VARIABLE_STDERR_ORIGINAL_LENGTH}
     *                             variable of plugin action.
     * @param exitCode The value for {@link #PLUGIN_VARIABLE_EXIT_CODE} variable of plugin action.
     * @param errCode The value for {@link #PLUGIN_VARIABLE_ERR} variable of plugin action.
     * @param errmsg The value for {@link #PLUGIN_VARIABLE_ERRMSG} variable of plugin action.
     * @return Returns the variables {@code Bundle}.
     */
    public static Bundle createVariablesBundle(String stdout, String stdoutOriginalLength,
                                               String stderr, String stderrOriginalLength,
                                               String exitCode, int errCode, String errmsg) {

        Logger.logDebugExtended(LOG_TAG, "Variables bundle for plugin host app:\n" +
                PLUGIN_VARIABLE_STDOUT + ": `" + stdout + "`\n" +
                PLUGIN_VARIABLE_STDOUT_ORIGINAL_LENGTH + ": `" + stdoutOriginalLength + "`\n" +
                PLUGIN_VARIABLE_STDERR + ": `" + stderr + "`\n" +
                PLUGIN_VARIABLE_STDERR_ORIGINAL_LENGTH + ": `" + stderrOriginalLength + "`\n" +
                PLUGIN_VARIABLE_EXIT_CODE + ": `" + exitCode + "`\n" +
                PLUGIN_VARIABLE_ERR + ": `" + errCode + "`\n" +
                PLUGIN_VARIABLE_ERRMSG + ": `" + errmsg + "`");

        if (errCode == TaskerPlugin.Setting.RESULT_CODE_OK && errmsg != null && !errmsg.isEmpty()) {
            Logger.logWarn(LOG_TAG, "Ignoring setting " + PLUGIN_VARIABLE_ERRMSG + " variable since " + PLUGIN_VARIABLE_ERR + " is set to RESULT_CODE_OK \"" + TaskerPlugin.Setting.RESULT_CODE_OK + "\", " + PLUGIN_VARIABLE_ERRMSG + ": \"" + errmsg + "\"");
            errmsg = "";
        }

        // Send back empty values for variables not to be returned.
        // This will/should unset their respective variables in the plugin host app,
        // since if multiple actions are run in the same task, some variables from previous actions
        // may still be set and get mixed in with current ones.
        if (stdout == null) stdout = "";
        if (stdoutOriginalLength == null) stdoutOriginalLength = "";
        if (stderr == null) stderr = "";
        if (stderrOriginalLength == null) stderrOriginalLength = "";
        if (exitCode == null) exitCode = "";
        if (errmsg == null) errmsg = "";

        final Bundle variablesBundle = new Bundle();

        if (isPluginHostAppVariableNameValid(PLUGIN_VARIABLE_STDOUT))
            variablesBundle.putString(PLUGIN_VARIABLE_STDOUT, stdout);
        if (isPluginHostAppVariableNameValid(PLUGIN_VARIABLE_STDOUT_ORIGINAL_LENGTH))
            variablesBundle.putString(PLUGIN_VARIABLE_STDOUT_ORIGINAL_LENGTH, stdoutOriginalLength);
        if (isPluginHostAppVariableNameValid(PLUGIN_VARIABLE_STDERR))
            variablesBundle.putString(PLUGIN_VARIABLE_STDERR, stderr);
        if (isPluginHostAppVariableNameValid(PLUGIN_VARIABLE_STDERR_ORIGINAL_LENGTH))
            variablesBundle.putString(PLUGIN_VARIABLE_STDERR_ORIGINAL_LENGTH, stderrOriginalLength);
        if (isPluginHostAppVariableNameValid(PLUGIN_VARIABLE_EXIT_CODE))
            variablesBundle.putString(PLUGIN_VARIABLE_EXIT_CODE, exitCode);
        if (isPluginHostAppVariableNameValid(PLUGIN_VARIABLE_ERRMSG))
            variablesBundle.putString(PLUGIN_VARIABLE_ERRMSG, errmsg);

        return variablesBundle;
    }

    /**
     * Sanitize errCode value so that it can be sent back to plugin host app as %err value.
     * For custom result codes for the plugin, start numbering from
     * {@link TaskerPlugin.Setting#RESULT_CODE_FAILED_PLUGIN_FIRST},
     * otherwise plugin host app like Tasker will consider them as unknown result codes.
     *
     * @param errCode The value to sanitize.
     * @return Returns {@code errCode} value as is if valid, otherwise returns
     *         {@link TaskerPlugin.Setting#RESULT_CODE_OK}.
     */
    public static int sanitizeErrCode(final int errCode) {
        int err;
        if (errCode >= TaskerPlugin.Setting.RESULT_CODE_OK) {
            err = errCode;
        } else {
            Logger.logWarn(LOG_TAG, "Ignoring invalid "  + PLUGIN_VARIABLE_ERR + " value \"" + errCode + "\" for plugin action and force setting it to RESULT_CODE_OK \"" + TaskerPlugin.Setting.RESULT_CODE_OK + "\"");
            err = TaskerPlugin.Setting.RESULT_CODE_OK;
        }

        return err;
    }



    /**
     * Checks if plugin host variable name is valid and can be sent back to plugin host app.
     *
     * @param name The {@code name} of the plugin variable.
     * @return Returns {@code true} if valid, otherwise {@code false}.
     */
    public static boolean isPluginHostAppVariableNameValid(final String name) {
        if (name == null || name.isEmpty() || !TaskerPlugin.variableNameValid(name)) {
            Logger.logWarn(LOG_TAG, "Ignoring invalid plugin variable name: \"" + name + "\"");
            return false;
        }

        return true;
    }

    /**
     * Determines whether string exactly matches a valid plugin host app variable.
     *
     * @param string The {@link String} to check.
     * @return Returns {@code true} if string exactly matches a plugin host app variable, otherwise {@code false}.
     */
    public static boolean isPluginHostAppVariableString(String string) {
        if (string == null || string.isEmpty()) return false;
        return Pattern.compile("^" + PLUGIN_HOST_VARIABLE_NAME_MATCH_EXPRESSION + "$", 0).matcher(string).matches();
    }

    /**
     * Determines whether string contains a plugin host app variable.
     *
     * @param string The {@link String} to check.
     * @return Returns {@code true} if string contains a plugin host app variable, otherwise {@code false}.
     */
    public static boolean isPluginHostAppVariableContainingString(String string) {
        if (string == null || string.isEmpty()) return false;
        return Pattern.compile(".*" + PLUGIN_HOST_VARIABLE_NAME_MATCH_EXPRESSION + ".*", 0).matcher(string).matches();
    }



    /**
     * Check if package has {@link TermuxConstants#PERMISSION_RUN_COMMAND}.
     *
     * @param context The {@link Context} to get error string.
     * @param packageName The package name to check.
     * @return Returns the {@code errmsg} if package has not been granted
     * {@link TermuxConstants#PERMISSION_RUN_COMMAND}, otherwise {@code null}.
     */
    public static String checkIfPackageHasPermissionRunCommand(final Context context, final String packageName) {

        String errmsg = null;

        // Check if packageName has been granted PERMISSION_RUN_COMMAND
        PackageManager packageManager = context.getPackageManager();
        // If permission not granted
        if (packageManager.checkPermission(TermuxConstants.PERMISSION_RUN_COMMAND, packageName) != PackageManager.PERMISSION_GRANTED) {
            ApplicationInfo applicationInfo;
            try {
                applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            } catch (final PackageManager.NameNotFoundException e) {
                applicationInfo = null;
            }
            final String appName = (String) (applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo) : context.getString(R.string.error_unknown_app));
            errmsg = context.getString(R.string.error_plugin_permission_ungranted_warning, appName, packageName, TermuxConstants.PERMISSION_RUN_COMMAND);
        }

        return errmsg;
    }

    /**
     * Check if executable is not under {@link TermuxConstants#TERMUX_TASKER_SCRIPTS_DIR_PATH} and
     * {@link TermuxConstants#PROP_ALLOW_EXTERNAL_APPS} property is not set to "true".
     *
     * @param context The {@link Context} to get error string.
     * @param executable The {@code path} to check.
     * @return Returns the {@code errmsg} if policy is violated, otherwise {@code null}.
     */
    public static String checkIfTermuxTaskerAllowExternalAppsPolicyIsViolated(final Context context, final String executable) {
        if (executable == null || executable.isEmpty()) return context.getString(R.string.error_null_or_empty_executable);

        String errmsg = null;

        if (!FileUtils.isPathInDirPath(executable, TermuxConstants.TERMUX_TASKER_SCRIPTS_DIR_PATH, true) &&
                !SharedProperties.isPropertyValueTrue(context,
                        SharedProperties.getPropertiesFileFromList(TermuxConstants.TERMUX_PROPERTIES_FILE_PATHS_LIST, LOG_TAG),
                        TermuxConstants.PROP_ALLOW_EXTERNAL_APPS, true)) {
            errmsg = context.getString(R.string.error_allow_external_apps_ungranted_warning);
        }

        return errmsg;
    }

    /**
     * Try to get the next unique {@link PendingIntent} request code that isn't already being used by
     * the app and which would create a unique {@link PendingIntent} that doesn't conflict with that
     * of any other execution commands.
     *
     * @param context The {@link Context} for operations.
     * @return Returns the request code that should be safe to use.
     */
    public synchronized static int getLastPendingIntentRequestCode(final Context context) {
        if (context == null) return TERMUX_TASKER_APP.DEFAULT_VALUE_KEY_LAST_PENDING_INTENT_REQUEST_CODE;

        TermuxTaskerAppSharedPreferences preferences = TermuxTaskerAppSharedPreferences.build(context);
        if (preferences == null) return TERMUX_TASKER_APP.DEFAULT_VALUE_KEY_LAST_PENDING_INTENT_REQUEST_CODE;

        int lastPendingIntentRequestCode = preferences.getLastPendingIntentRequestCode();

        int nextPendingIntentRequestCode = lastPendingIntentRequestCode + 1;

        if (nextPendingIntentRequestCode == Integer.MAX_VALUE || nextPendingIntentRequestCode < 0)
            nextPendingIntentRequestCode = TERMUX_TASKER_APP.DEFAULT_VALUE_KEY_LAST_PENDING_INTENT_REQUEST_CODE;

        preferences.setLastPendingIntentRequestCode(nextPendingIntentRequestCode);
        return nextPendingIntentRequestCode;
    }

}
