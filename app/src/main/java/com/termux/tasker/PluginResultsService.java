package com.termux.tasker;

import android.app.Activity;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;

import com.termux.tasker.utils.Logger;

import java.util.regex.Pattern;

/**
 * A unified class and service to handle sending of plugin commands to the execution service,
 * receiving the results of plugin commands back from the execution service and sending immediate or
 * pending results back to the plugin host.
 *
 * This is currently designed and tested with Tasker as the plugin host app and the
 * com.termux.app/.TermuxService as the execution service of plugin commands but should work with
 * other plugin host apps. The TermuxService will run the commands with the BackgroundJob class if
 * background mode is enabled and results of commands are only be returned in this case. If
 * background mode is not enabled, a foreground terminal session will be opened and results are not
 * returned.
 *
 * Flow for the usage of {@link PluginResultsService}:
 * 1. Call {@link #sendExecuteIntentToExecuteService} from {@link FireReceiver}. This expects the
 * original intent received by {@link FireReceiver}, the execution intent that should be started to
 * call the execution service containing its ComponentName and any extras required to run the
 * plugin commands, and a boolean for whether commands are to be run in background mode or not.
 * If the plugin action has a timeout greater than 0 and background mode is enabled, then the
 * function will automatically create a {@link android.app.PendingIntent} that can be used to
 * return the results back to {@link PluginResultsService} and add the original
 * {@link android.content.Intent} received by {@link FireReceiver} as {@link #EXTRA_ORIGINAL_INTENT}
 * to it and then add the created {@link android.app.PendingIntent} to the execution intent as
 * {@link #EXTRA_PENDING_INTENT} extra.
 * Otherwise, the function immediaterly returns
 * {@link com.termux.tasker.TaskerPlugin.Setting#RESULT_CODE_OK} back to the plugin host app using
 * the {@link #sendImmediateResultToPluginHostApp} function and the flow ends here for the usage
 * of {@link PluginResultsService}.
 *
 * 2. The execution service should receive the execution intent and run the required plugin commands.
 * If background mode is enabled, then results should be returned back to the
 * {@link PluginResultsService}. For this, optionally call
 * {@link #sendExecuteResultToResultsService} or create the result intent manually and send it back
 * using the {@link android.app.PendingIntent} received by the {@link PluginResultsService}. The
 * pending intent should already contain the original {@link android.content.Intent} received by the
 * {@link FireReceiver}. A result {@link Bundle} object with the {@link #EXTRA_RESULT_BUNDLE}
 * key should also be sent back in an {@link android.content.Intent} using the
 * {@link android.app.PendingIntent#send(Context, int, Intent)} function. The bundle can contain
 * the keys {@link #EXTRA_STDOUT} (String), {@link #EXTRA_STDERR} (String),
 * {@link #EXTRA_EXIT_CODE} (Integer), {@link #PLUGIN_VARIABLE_ERR} (Integer) and
 * {@link #PLUGIN_VARIABLE_ERRMSG} (String) who values will be sent back to the plugin host app.
 *
 * 3. The {@link android.app.PendingIntent} sent is received by the {@link PluginResultsService}
 * with the {@link #onHandleIntent} function which calls the
 * {@link #sendPendingResultToPluginHostApp} function with the intent received.
 *
 * 4. The {@link #sendPendingResultToPluginHostApp} function extracts the original intent and the
 * result bundle from the intent received and calls the {@link #createVariablesBundle} function
 * to create the variables bundle to be sent back to plugin host and then sends it with
 * {@link com.termux.tasker.TaskerPlugin.Setting#signalFinish} function. If any of the Integer keys
 * do not exist in the result bundle or if the values of String keys are null or empty in the result
 * bundle, then they are not sent back to the plugin host. The flow ends here.
 *
 * The {@link #sendImmediateResultToPluginHostApp} function can be used to send result back to the
 * plugin host immediately like in case there is an error processing the plugin command request or
 * if the result should not be expected to be sent back by the execution service.
 *
 * The {@link #createVariablesBundle} function creates a variables bundle that can be sent back to
 * the plugin host. The bundle will contain the keys {@link #PLUGIN_VARIABLE_STDOUT} (String),
 * {@link #PLUGIN_VARIABLE_STDERR} (String), {@link #PLUGIN_VARIABLE_EXIT_CODE} (String) and
 * {@link #PLUGIN_VARIABLE_ERRMSG} (String). The {@link #PLUGIN_VARIABLE_ERRMSG} key will only be
 * added if the {@link #PLUGIN_VARIABLE_ERR} value to be sent back to the plugin host is greater
 * than {@link com.termux.tasker.TaskerPlugin.Setting#RESULT_CODE_OK}. Any null or empty values are
 * not added to the variables bundle.
 *
 * The value for {@link #PLUGIN_VARIABLE_ERR} is first sanitized by the {@link #sanitizeErrCode}
 * function before it is sent back to the plugin host.
 */

public class PluginResultsService extends IntentService {

    public static final String PLUGIN_VARIABLE_STDOUT = TaskerPlugin.VARIABLE_PREFIX + "stdout"; //plugin variable for stdout value of termux command
    public static final String PLUGIN_VARIABLE_STDERR = TaskerPlugin.VARIABLE_PREFIX + "stderr"; //plugin variable for stderr value of termux command
    public static final String PLUGIN_VARIABLE_EXIT_CODE = TaskerPlugin.VARIABLE_PREFIX + "result"; //plugin variable for exit code of termux command
    public static final String PLUGIN_VARIABLE_ERR = TaskerPlugin.VARIABLE_PREFIX + "err"; //plugin variable for err value of plugin action
    public static final String PLUGIN_VARIABLE_ERRMSG = TaskerPlugin.Setting.VARNAME_ERROR_MESSAGE; //plugin variable for errmsg value of plugin action

    public static final String EXTRA_STDOUT = "stdout"; //string extra for stdout value of termux command
    public static final String EXTRA_STDERR = "stderr"; //string extra for stderr value of termux command
    public static final String EXTRA_EXIT_CODE = "exitCode"; //int extra for exit code value of termux command
    public static final String EXTRA_ERR = "err"; //int extra for err value of plugin action
    public static final String EXTRA_ERRMSG = "errmsg"; //string extra for errmsg value of plugin action

    public static final String EXTRA_RESULT_BUNDLE = "result";  //bundle extra containing result of commands to send back to plugin host app
    public static final String EXTRA_ORIGINAL_INTENT = "originalIntent"; //parcelable extra containing original intent received from plugin host app by FireReceiver

    public static final String EXTRA_PENDING_INTENT = "pendingIntent"; //parcelable extra for execution intent containing pending intent for the PluginResultsService and the original intent received by FireReceiver

    public static final String PLUGIN_SERVICE_LABEL = "PluginResultsService";
    public static final String EXECUTION_SERVICE_LABEL = "TermuxService";

    public PluginResultsService(){
        super(PLUGIN_SERVICE_LABEL);
    }

    /**
     * Receive intent containing result of commands and send pending result back to plugin host app.
     *
     * @param intent containing result and original intent received by {@link FireReceiver}.
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Logger.logInfo(this,PLUGIN_SERVICE_LABEL + " received execution result from " + EXECUTION_SERVICE_LABEL);
        sendPendingResultToPluginHostApp(this, intent);
    }

    /**
     * Send execution intent to execution service containing command information and original intent
     * received by {@link FireReceiver}.
     *
     * @param context that will be used to send execution intent to the execution service.
     * @param receiver of the originalIntent broadcast.
     * @param originalIntent received by {@link FireReceiver}.
     * @param executionIntent to be sent to execution service containing command information.
     */
    public static void sendExecuteIntentToExecuteService(final Context context, final BroadcastReceiver receiver, final Intent originalIntent, final Intent executionIntent, final boolean executeInBackground) {
        if (context == null) return;

        if (executionIntent == null) {
            Logger.logError(context, "The executionIntent passed to sendExecuteIntentToExecuteService() cannot be null.");
            return;
        }

        if (executionIntent.getComponent() == null) {
            Logger.logError(context, "The Component for the executionIntent passed to sendExecuteIntentToExecuteService() cannot be null.");
            return;
        }

        Logger.logDebug(context,"Sending execution intent to " + EXECUTION_SERVICE_LABEL);

        // If timeout for plugin action is greater than 0 and execute in background is enabled
        if (receiver != null && receiver.isOrderedBroadcast() && executeInBackground) {
            // Notify plugin host app that result will be sent later
            // Result should be sent to PluginResultsService via a PendingIntent by execution service after commands have finished executing
            receiver.setResultCode(TaskerPlugin.Setting.RESULT_CODE_PENDING);

            // Create intent for PluginResultsService class and add original intent received by FireReceiver to it
            Intent pluginResultsServiceIntent = new Intent(context, PluginResultsService.class);
            pluginResultsServiceIntent.putExtra(PluginResultsService.EXTRA_ORIGINAL_INTENT, originalIntent);

            // Create PendingIntent that can be used by execution service to send result of commands back to PluginResultsService
            PendingIntent pendingIntent = PendingIntent.getService(context, 1, pluginResultsServiceIntent, PendingIntent.FLAG_ONE_SHOT);
            executionIntent.putExtra(EXTRA_PENDING_INTENT, pendingIntent);
        } else {
            // If execute in background is not enabled, do not expect results back from execution service and return result now so that plugin action does not timeout
            sendImmediateResultToPluginHostApp(context, receiver, originalIntent, null, null, null, TaskerPlugin.Setting.RESULT_CODE_OK, null);
        }

        // Send execution intent to execution service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // https://developer.android.com/about/versions/oreo/background.html
            context.startForegroundService(executionIntent);
        } else {
            context.startService(executionIntent);
        }
    }

    /**
     * Send execution result of commands to PluginResultsService with the PendingIntent received by
     * execution service from {@link FireReceiver}.
     *
     * @param context that will be used to send result intent to the PluginResultsService.
     * @param pendingIntent sent by {@link FireReceiver} to the execution service.
     * @param stdout value for {@link #EXTRA_STDOUT} extra of {@link #EXTRA_RESULT_BUNDLE} bundle of intent.
     * @param stderr value for {@link #EXTRA_STDERR} extra of {@link #EXTRA_RESULT_BUNDLE} bundle of intent.
     * @param exitCode value for {@link #EXTRA_EXIT_CODE} extra of {@link #EXTRA_RESULT_BUNDLE} bundle of intent.
     * @param errCode value for {@link #EXTRA_ERR} extra of {@link #EXTRA_RESULT_BUNDLE} bundle of intent.
     * @param errmsg value for {@link #EXTRA_ERRMSG} extra of {@link #EXTRA_RESULT_BUNDLE} bundle of intent.
     */
    public static void sendExecuteResultToResultsService(final Context context, final PendingIntent pendingIntent, final String stdout, final String stderr, final String exitCode, final String errCode, final String errmsg) {

        Logger.logDebug(context,  "Sending execution result to " + PLUGIN_SERVICE_LABEL + ":\n" +
                                            EXTRA_STDOUT + ": `" + stdout + "`\n" +
                                            EXTRA_STDERR + ": `" + stderr + "`\n" +
                                            EXTRA_EXIT_CODE + ": `" + exitCode + "`\n" +
                                            EXTRA_ERR + ": `" + errCode + "`\n" +
                                            EXTRA_ERRMSG + ": `" + errmsg + "`");

        final Bundle resultBundle = new Bundle();

        resultBundle.putString(EXTRA_STDOUT, stdout);
        resultBundle.putString(EXTRA_STDERR, stderr);
        if (exitCode != null && !exitCode.isEmpty()) resultBundle.putInt(EXTRA_EXIT_CODE, Integer.parseInt(exitCode));
        if (errCode != null && !errCode.isEmpty()) resultBundle.putInt(EXTRA_ERR, Integer.parseInt(errCode));
        resultBundle.putString(EXTRA_ERRMSG, errmsg);

        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_RESULT_BUNDLE, resultBundle);

        if(pendingIntent != null && context != null) {
            try {
                pendingIntent.send(context, Activity.RESULT_OK, resultIntent);
            } catch (PendingIntent.CanceledException e) {
                // The caller doesn't want the result? That's fine, just ignore
            }
        }
    }

    /**
     * Send immediate result to plugin host app in a variables bundle.
     *
     * @param context for logging.
     * @param receiver of the originalIntent broadcast.
     * @param originalIntent received by {@link FireReceiver}.
     * @param stdout value for {@link #PLUGIN_VARIABLE_STDOUT} variable of plugin action.
     * @param stderr value for {@link #PLUGIN_VARIABLE_STDERR} variable of plugin action.
     * @param exitCode value for {@link #PLUGIN_VARIABLE_EXIT_CODE} variable of plugin action.
     * @param errCode value for {@link #PLUGIN_VARIABLE_ERR} variable of plugin action.
     * @param errmsg value for {@link #PLUGIN_VARIABLE_ERRMSG} variable of plugin action.
     */
    public static void sendImmediateResultToPluginHostApp(final Context context, final BroadcastReceiver receiver, final Intent originalIntent, final String stdout, final String stderr, final String exitCode, final int errCode, final String errmsg) {
        if (receiver == null) return;

        // If timeout for plugin action is 0, then don't send anything
        if (!receiver.isOrderedBroadcast()) return;

        int err = sanitizeErrCode(context, errCode);

        Logger.logInfo(context,"Sending immediate result to plugin host app. " + PLUGIN_VARIABLE_ERR + ": " + ((err == TaskerPlugin.Setting.RESULT_CODE_OK) ? "success" : "failed") + " (" + err +  ")");

        if (TaskerPlugin.Setting.hostSupportsVariableReturn(originalIntent.getExtras())) {
            final Bundle varsBundle = createVariablesBundle(context, stdout, stderr, exitCode, err, errmsg);
            TaskerPlugin.addVariableBundle(receiver.getResultExtras(true), varsBundle);
        }

        receiver.setResultCode(err);
    }

    /**
     * Send pending result to plugin host app in a variables bundle.
     *
     * @param context that will be used to send variables bundle to plugin host app and logging.
     * @param intent containing result and original intent received by {@link FireReceiver}.
     */
    public static void sendPendingResultToPluginHostApp(final Context context, final Intent intent) {
        if (intent == null){
            Logger.logWarn(context, "Ignoring null intent passed to sendPendingResultToPluginHostApp().");
            return;
        }

        final Intent originalIntent = intent.getParcelableExtra(EXTRA_ORIGINAL_INTENT);
        if (originalIntent == null) {
            Logger.logError(context, "The intent passed to sendPendingResultToPluginHostApp() must contain the original intent received by the FireReceiver at the " + EXTRA_ORIGINAL_INTENT + " key.");
            return;
        }

        final Bundle resultBundle = intent.getBundleExtra(EXTRA_RESULT_BUNDLE);
        if (resultBundle == null) {
            Logger.logError(context, "The intent passed to sendPendingResultToPluginHostApp() must contain the result bundle at the " + EXTRA_RESULT_BUNDLE + " key.");
            return;
        }

        int err = TaskerPlugin.Setting.RESULT_CODE_OK;
        // This check is necessary, otherwise default value will be 0 if extra does not exist,
        // and so plugin host app like Tasker will consider the action as failed for the value 0,
        // since it equals Activity.RESULT_CANCELED (0) instead of TaskerPlugin.Setting.RESULT_CODE_OK/Activity.RESULT_OK (-1)
        if (resultBundle.containsKey(EXTRA_ERR))
            err = sanitizeErrCode(context, resultBundle.getInt(EXTRA_ERR));

        Logger.logInfo(context, "Sending pending result to plugin host app. " + PLUGIN_VARIABLE_ERR + ": " + ((err == TaskerPlugin.Setting.RESULT_CODE_OK) ? "success" : "failed") + " (" + err +  ")");

        String exitCode = null;
        if (resultBundle.containsKey(EXTRA_EXIT_CODE))
            exitCode = Integer.toString(resultBundle.getInt(EXTRA_EXIT_CODE));

        final Bundle varsBundle = createVariablesBundle(context, resultBundle.getString(EXTRA_STDOUT, ""), resultBundle.getString(EXTRA_STDERR, ""), exitCode, err, resultBundle.getString(EXTRA_ERRMSG, ""));

        if(context != null)
            TaskerPlugin.Setting.signalFinish(context, originalIntent, err, varsBundle);
    }

    /**
     * Create variables bundle to send back to plugin host app.
     *
     * @param context for logging.
     * @param stdout value for {@link #PLUGIN_VARIABLE_STDOUT} variable of plugin action.
     * @param stderr value for {@link #PLUGIN_VARIABLE_STDERR} variable of plugin action.
     * @param exitCode value for {@link #PLUGIN_VARIABLE_EXIT_CODE} variable of plugin action.
     * @param errCode value for {@link #PLUGIN_VARIABLE_ERR} variable of plugin action.
     * @param errmsg value for {@link #PLUGIN_VARIABLE_ERRMSG} variable of plugin action.
     * @return variables bundle.
     */
    public static Bundle createVariablesBundle(final Context context, String stdout, String stderr, String exitCode, int errCode, String errmsg) {

        Logger.logDebug(context, "Variables bundle for plugin host app:\n" +
                              PLUGIN_VARIABLE_STDOUT + ": `" + stdout + "`\n" +
                              PLUGIN_VARIABLE_STDERR + ": `" + stderr + "`\n" +
                              PLUGIN_VARIABLE_EXIT_CODE + ": `" + exitCode + "`\n" +
                              PLUGIN_VARIABLE_ERR + ": `" + errCode + "`\n" +
                              PLUGIN_VARIABLE_ERRMSG + ": `" + errmsg + "`");

        if (errCode == TaskerPlugin.Setting.RESULT_CODE_OK && errmsg != null && !errmsg.isEmpty()) {
            Logger.logWarn(context, "Ignoring setting " + PLUGIN_VARIABLE_ERRMSG + " variable since " + PLUGIN_VARIABLE_ERR + " is set to RESULT_CODE_OK \"" + TaskerPlugin.Setting.RESULT_CODE_OK + "\", " + PLUGIN_VARIABLE_ERRMSG + ": \"" + errmsg + "\"");
            errmsg = "";
        }

        // Send back empty values for variables not to be returned.
        // This will/should unset their respective variables in the plugin host app,
        // since if multiple actions are run in the same task, some variables from previous actions
        // may still be set and get mixed in with current ones.
        if (stdout == null) stdout = "";
        if (stderr == null) stderr = "";
        if (exitCode == null) exitCode = "";
        if (errmsg == null) errmsg = "";

        final Bundle variablesBundle = new Bundle();

        if (isPluginHostAppVariableNameValid(context, PLUGIN_VARIABLE_STDOUT))
            variablesBundle.putString(PLUGIN_VARIABLE_STDOUT, stdout);
        if (isPluginHostAppVariableNameValid(context, PLUGIN_VARIABLE_STDERR))
            variablesBundle.putString(PLUGIN_VARIABLE_STDERR, stderr);
        if (isPluginHostAppVariableNameValid(context, PLUGIN_VARIABLE_EXIT_CODE))
            variablesBundle.putString(PLUGIN_VARIABLE_EXIT_CODE, exitCode);
        if (isPluginHostAppVariableNameValid(context, PLUGIN_VARIABLE_ERRMSG))
            variablesBundle.putString(PLUGIN_VARIABLE_ERRMSG, errmsg);

        return variablesBundle;
    }

    /**
     * Sanitize errCode value so that it can be sent back to plugin host app as %err value.
     * For custom result codes for the plugin, start numbering from
     * {@link com.termux.tasker.TaskerPlugin.Setting#RESULT_CODE_FAILED_PLUGIN_FIRST},
     * otherwise plugin host app like Tasker will consider them as unknown result codes.
     *
     * @param context for logging.
     * @param errCode value to sanitize.
     * @return errCode value as is if valid, otherwise returns
     *         {@link com.termux.tasker.TaskerPlugin.Setting#RESULT_CODE_OK}.
     */
    public static int sanitizeErrCode(final Context context, final int errCode) {
        int err;
        if (errCode >= TaskerPlugin.Setting.RESULT_CODE_OK) {
            err = errCode;
        } else {
            Logger.logWarn(context, "Ignoring invalid "  + PLUGIN_VARIABLE_ERR + " value \"" + errCode + "\" for plugin action and force setting it to RESULT_CODE_OK \"" + TaskerPlugin.Setting.RESULT_CODE_OK + "\"");
            err = TaskerPlugin.Setting.RESULT_CODE_OK;
        }

        return err;
    }

    /**
     * Checks if plugin host variable name is valid and can be sent back to plugin host app.
     *
     * @param context for logging.
     * @param name of plugin variable.
     * @return true if valid, otherwise false.
     */
    public static boolean isPluginHostAppVariableNameValid(final Context context, final String name) {
        if (!TaskerPlugin.variableNameValid(name)) {
            Logger.logWarn(context, "Ignoring invalid plugin variable name: \"" + name + "\"");
            return false;
        }

        return true;
    }

    /**
     * Determines whether string matches a plugin host app variable.
     *
     * @return true if string matches a plugin host app variable, otherwise false.
     */
    public static boolean isPluginHostAppVariableString(String string) {
        String VARIABLE_NAME_CONTAINING_EXPRESSION = "^" + TaskerPlugin.VARIABLE_NAME_MATCH_EXPRESSION + "$";
        Pattern VARIABLE_NAME_CONTAINING_PATTERN = Pattern.compile(VARIABLE_NAME_CONTAINING_EXPRESSION, 0);

        return VARIABLE_NAME_CONTAINING_PATTERN.matcher(string).matches();
    }

    /**
     * Determines whether string contains a plugin host app variable.
     *
     * @return true if string contains a plugin host app variable, otherwise false.
     */
    public static boolean isPluginHostAppVariableContainingString(String string) {
        String VARIABLE_NAME_CONTAINING_EXPRESSION = ".*" + TaskerPlugin.VARIABLE_NAME_MATCH_EXPRESSION + ".*";
        Pattern VARIABLE_NAME_CONTAINING_PATTERN = Pattern.compile(VARIABLE_NAME_CONTAINING_EXPRESSION, 0);

        return VARIABLE_NAME_CONTAINING_PATTERN.matcher(string).matches();
    }

}
