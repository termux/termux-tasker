package com.termux.tasker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.termux.tasker.utils.FileUtils;
import com.termux.tasker.utils.Logger;
import com.termux.tasker.utils.PluginUtils;
import com.termux.tasker.utils.TermuxAppUtils;

import java.util.List;

/**
 * This is the "fire" BroadcastReceiver for a Locale Plug-in.
 */
public final class FireReceiver extends BroadcastReceiver {

    public static final String ACTION_EXECUTE = "com.termux.service_execute";
    public static final String EXTRA_CURRENT_WORKING_DIRECTORY = "com.termux.execute.cwd";
    public static final String EXTRA_EXECUTE_IN_BACKGROUND = "com.termux.execute.background";

    public void onReceive(final Context context, final Intent intent) {
        // Load the log level value from shared preferences file into the SharedPreferencesImpl
        // memory cache if file was updated by the main app process
        // Till the onReceive() function is called again, any changes to log level will not be considered
        // since log level will be loaded from memory cache by Logger.getLogLevel() for checking
        // current log level by all the logging functions
        // Log levels can also be read from the file if updated, but that will slow down execution,
        // hence that is not done
        Logger.getLogLevelFromFile(context);

        // If wrong action passed, then just return
        if (!com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING.equals(intent.getAction())) {
            Logger.logError(context, "Unexpected intent action: " + intent.getAction());
            return;
        }

        Logger.logInfo(context, "FireReceiver received execution intent");

        String errmsg;

        BundleScrubber.scrub(intent);
        final Bundle bundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
        BundleScrubber.scrub(bundle);

        // If bundle is not valid, then return RESULT_CODE_FAILED to plugin host app
        errmsg = PluginBundleManager.isBundleValid(context, bundle);
        if (errmsg != null) {
            Logger.logError(context, errmsg);
            PluginResultsService.sendImmediateResultToPluginHostApp(context, this, intent, null, null, null, TaskerPlugin.Setting.RESULT_CODE_FAILED, errmsg);
            return;
        }

        String executable = bundle.getString(PluginBundleManager.EXTRA_EXECUTABLE);
        final String arguments_string = bundle.getString(PluginBundleManager.EXTRA_ARGUMENTS);
        String workingDirectory = bundle.getString(PluginBundleManager.EXTRA_WORKDIR);
        final boolean inTerminal = bundle.getBoolean(PluginBundleManager.EXTRA_TERMINAL);

        // If Termux app is not installed or PREFIX_PATH is not accessible, then return RESULT_CODE_FAILED to plugin host app
        errmsg = TermuxAppUtils.checkIfTermuxAppIsInstalledAndAccessible(context);
        if (errmsg != null) {
            Logger.logError(context, errmsg);
            PluginResultsService.sendImmediateResultToPluginHostApp(context, this, intent, null, null, null, TaskerPlugin.Setting.RESULT_CODE_FAILED, errmsg);
            return;
        }

        // Get absolute canonical path of executable
        executable = FileUtils.getAbsolutePathForExecutable(executable);

        // If executable is not in TASKER_PATH and allow-external-apps property to not set to "true", then return RESULT_CODE_FAILED to plugin host app
        errmsg = PluginUtils.checkIfAllowExternalAppsPolicyIsViolated(context, executable);
        if (errmsg != null) {
            errmsg  += "\n" + context.getString(R.string.executable_absolute_path, executable) +
                       "\n" + context.getString(R.string.help);
            Logger.logError(context, errmsg);
            PluginResultsService.sendImmediateResultToPluginHostApp(context, this, intent, null, null, null, TaskerPlugin.Setting.RESULT_CODE_FAILED, errmsg);
            return;
        }

        // If executable is not a file, cannot be read or be executed, then return RESULT_CODE_FAILED to plugin host app
        errmsg = FileUtils.checkIfExecutableFileIsReadableAndExecutable(context, executable, true, false);
        if (errmsg != null) {
            errmsg  += "\n" + context.getString(R.string.executable_absolute_path, executable);
            Logger.logError(context, errmsg);
            PluginResultsService.sendImmediateResultToPluginHostApp(context, this, intent, null, null, null, TaskerPlugin.Setting.RESULT_CODE_FAILED, errmsg);
            return;
        }

        // If workingDirectory is not null or empty
        if (workingDirectory != null && !workingDirectory.isEmpty()) {
            // Get absolute canonical path of executable
            workingDirectory = FileUtils.getAbsolutePathForExecutable(workingDirectory);

            // If workingDirectory is not a directory or cannot be read, then return RESULT_CODE_FAILED to plugin host app
            errmsg = FileUtils.checkIfDirectoryIsReadable(context, workingDirectory, true, true, false);
            if (errmsg != null) {
                errmsg  += "\n" + context.getString(R.string.working_directory_absolute_path, workingDirectory);
                Logger.logError(context, errmsg);
                PluginResultsService.sendImmediateResultToPluginHostApp(context, this, intent, null, null, null, TaskerPlugin.Setting.RESULT_CODE_FAILED, errmsg);
                return;
            }
        }

        Uri scriptUri = new Uri.Builder().scheme("com.termux.file").path(executable).build();

        // Parse arguments_string into a list of arguments like normally done on shells like bourne shell
        // Arguemnts are split on whitespaces unless quoted with single or double quotes
        // Double quotes and backslashes can be escaped with backslashes in arguments surrounded with double quotes
        List<String> arguments_list = ArgumentTokenizer.tokenize(arguments_string);

        Logger.logDebug(context,  "execution intent:\n" +
                 "Executable: `" + executable + "`\n" +
                 "Arguments:" + getArgumentsString(arguments_list) + "\n" +
                 "Working Directory: `" + workingDirectory + "`\n" +
                 "inTerminal: `" + inTerminal + "`");

        // Create execution intent with the action TermuxService#ACTION_EXECUTE to be sent to the TERMUX_SERVICE
        Intent executionIntent = new Intent(ACTION_EXECUTE, scriptUri);
        executionIntent.setClassName(Constants.TERMUX_PACKAGE, Constants.TERMUX_SERVICE);
        executionIntent.putExtra(PluginBundleManager.EXTRA_ARGUMENTS, arguments_list.toArray(new String[arguments_list.size()]));
        if (workingDirectory != null && !workingDirectory.isEmpty()) executionIntent.putExtra(EXTRA_CURRENT_WORKING_DIRECTORY, workingDirectory);
        if (!inTerminal) executionIntent.putExtra(EXTRA_EXECUTE_IN_BACKGROUND, true);

        // Send execution intent to TERMUX_SERVICE
        PluginResultsService.sendExecuteIntentToExecuteService(context, this, intent, executionIntent, !inTerminal);
    }

    private String getArgumentsString(List<String> arguments_list) {
        if (arguments_list==null || arguments_list.size() == 0) return "";

        StringBuilder arguments_list_string = new StringBuilder("\n```\n");
        for(int i = 0; i != arguments_list.size(); i++) {
            arguments_list_string.append("Arg ").append(i).append(": `").append(arguments_list.get(i)).append("`\n");
        }
        arguments_list_string.append("```");

        return arguments_list_string.toString();
    }

}
