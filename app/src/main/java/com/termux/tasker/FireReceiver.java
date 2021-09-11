package com.termux.tasker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.termux.shared.crash.TermuxCrashUtils;
import com.termux.shared.data.IntentUtils;
import com.termux.shared.file.TermuxFileUtils;
import com.termux.shared.file.filesystem.FileType;
import com.termux.shared.logger.Logger;
import com.termux.shared.models.ExecutionCommand;
import com.termux.shared.models.errors.Errno;
import com.termux.shared.models.errors.Error;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE;
import com.termux.shared.file.FileUtils;
import com.termux.shared.termux.TermuxUtils;
import com.termux.tasker.utils.PluginUtils;
import com.termux.tasker.utils.TaskerPlugin;

import java.util.List;

/**
 * This is the "fire" BroadcastReceiver for a Locale Plug-in.
 */
public final class FireReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "FireReceiver";

    public void onReceive(final Context context, final Intent intent) {
        // Set crash handler for the receiver
        TermuxCrashUtils.setCrashHandler(context);

        // Load the log level from shared preferences and set it to the Loggger.CURRENT_LOG_LEVEL
        // Till the onReceive() function is called again, any changes to log level will not be considered
        // since log level will be stored in the Loggger.CURRENT_LOG_LEVEL static variable.
        // The FireReceiver is started in a separate process than the main app process since
        // it has the tag android:process=":background" and so it maintains separate Logger and
        // shared preference instances.
        TermuxTaskerApplication.setLogLevel(context, false);

        // If wrong action passed, then just return
        if (!com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING.equals(intent.getAction())) {
            Logger.logError(LOG_TAG, "Unexpected intent action: " + intent.getAction());
            return;
        }

        Logger.logInfo(LOG_TAG, "Received execution intent");

        String errmsg;
        Error error;

        BundleScrubber.scrub(intent);
        final Bundle bundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
        BundleScrubber.scrub(bundle);

        // If bundle is not valid, then return RESULT_CODE_FAILED to plugin host app
        errmsg = PluginBundleManager.parseBundle(context, bundle);
        if (errmsg != null) {
            Logger.logError(LOG_TAG, errmsg);
            PluginUtils.sendImmediateResultToPluginHostApp(this, intent, TaskerPlugin.Setting.RESULT_CODE_FAILED, errmsg);
            return;
        }

        ExecutionCommand executionCommand = new ExecutionCommand();

        String executableExtra = executionCommand.executable = IntentUtils.getStringExtraIfSet(intent, PluginBundleManager.EXTRA_EXECUTABLE, null);
        final String arguments_string = bundle.getString(PluginBundleManager.EXTRA_ARGUMENTS);
        executionCommand.workingDirectory = IntentUtils.getStringExtraIfSet(intent, PluginBundleManager.EXTRA_WORKDIR, null);
        executionCommand.inBackground = !(intent.getBooleanExtra(PluginBundleManager.EXTRA_TERMINAL, false));
        final boolean waitForResult = bundle.getBoolean(PluginBundleManager.EXTRA_WAIT_FOR_RESULT, true);

        if (executionCommand.inBackground)
            executionCommand.stdin = IntentUtils.getStringExtraIfSet(intent, PluginBundleManager.EXTRA_STDIN, null);


        // If Termux app is not installed, enabled or accessible with current context or if
        // TermuxConstants.TERMUX_PREFIX_DIR_PATH does not exist or has required permissions, then
        // return RESULT_CODE_FAILED to plugin host app.
        errmsg = TermuxUtils.isTermuxAppAccessible(context);
        if (errmsg != null) {
            Logger.logError(LOG_TAG, errmsg);
            PluginUtils.sendImmediateResultToPluginHostApp(this, intent, TaskerPlugin.Setting.RESULT_CODE_FAILED, errmsg);
            return;
        }


        // If executable is null or empty, then exit here instead of getting canonical path which would expand to "/"
        if (executionCommand.executable == null || executionCommand.executable.isEmpty()) {
            errmsg  = context.getString(R.string.error_null_or_empty_executable);
            Logger.logError(LOG_TAG, errmsg);
            PluginUtils.sendImmediateResultToPluginHostApp(this, intent, TaskerPlugin.Setting.RESULT_CODE_FAILED, errmsg);
            return;
        }

        // Get canonical path of executable
        executionCommand.executable = TermuxFileUtils.getCanonicalPath(executionCommand.executable, TermuxConstants.TERMUX_TASKER_SCRIPTS_DIR_PATH, true);


        // If executable is not in TermuxConstants#TERMUX_TASKER_SCRIPTS_DIR_PATH and
        // "allow-external-apps" property to not set to "true", then return RESULT_CODE_FAILED to plugin host app
        errmsg = PluginUtils.checkIfTermuxTaskerAllowExternalAppsPolicyIsViolated(context, executionCommand.executable);
        if (errmsg != null) {
            errmsg  += "\n" + context.getString(R.string.msg_executable_absolute_path, executionCommand.executable) +
                       "\n" + context.getString(R.string.help, TermuxConstants.TERMUX_TASKER_GITHUB_REPO_URL);
            executionCommand.setStateFailed(Errno.ERRNO_FAILED.getCode(), errmsg);
            PluginUtils.processPluginExecutionCommandError(context, this, intent, LOG_TAG, executionCommand, TaskerPlugin.Setting.RESULT_CODE_FAILED);
            return;
        }


        // If executable is not a regular file, or is not readable or executable, then return
        // RESULT_CODE_FAILED to plugin host app
        // Setting of read and execute permissions are only done if executable is under TermuxConstants#TERMUX_TASKER_SCRIPTS_DIR_PATH
        error = FileUtils.validateRegularFileExistenceAndPermissions("executable", executionCommand.executable,
                TermuxConstants.TERMUX_TASKER_SCRIPTS_DIR_PATH,
                FileUtils.APP_EXECUTABLE_FILE_PERMISSIONS,
                true, true,
                false);
        if (error != null) {
            executionCommand.setStateFailed(error);
            PluginUtils.processPluginExecutionCommandError(context, this, intent, LOG_TAG, executionCommand, TaskerPlugin.Setting.RESULT_CODE_FAILED);
            return;
        }


        // If workingDirectory is not null or empty
        if (executionCommand.workingDirectory != null && !executionCommand.workingDirectory.isEmpty()) {
            // Get canonical path of workingDirectory
            executionCommand.workingDirectory = TermuxFileUtils.getCanonicalPath(executionCommand.workingDirectory, null, true);

            // If workingDirectory is not a directory, or is not readable or writable, then just return
            // Creation of missing directory and setting of read, write and execute permissions are
            // only done if workingDirectory is under allowed termux working directory paths.
            // We try to set execute permissions, but ignore if they are missing, since only read and
            // write permissions are required for working directories.
            error = TermuxFileUtils.validateDirectoryFileExistenceAndPermissions("working", executionCommand.workingDirectory,
                    true, true, true,
                    false, true);
            if (error != null) {
                executionCommand.setStateFailed(error);
                PluginUtils.processPluginExecutionCommandError(context, this, intent, LOG_TAG, executionCommand, TaskerPlugin.Setting.RESULT_CODE_FAILED);
                return;
            }
        }


        // If the executable passed as the extra was an applet for coreutils/busybox, then we must
        // use it instead of the canonical path above since otherwise arguments would be passed to
        // coreutils/busybox instead and command would fail. Broken symlinks would already have been
        // validated so it should be fine to use it.
        executableExtra = TermuxFileUtils.getExpandedTermuxPath(executableExtra);
        if (FileUtils.getFileType(executableExtra, false) == FileType.SYMLINK) {
            Logger.logVerbose(LOG_TAG, "The executableExtra path \"" + executableExtra + "\" is a symlink so using it instead of the canonical path \"" + executionCommand.executable + "\"");
            executionCommand.executable = executableExtra;
        }

        executionCommand.executableUri = new Uri.Builder().scheme(TERMUX_SERVICE.URI_SCHEME_SERVICE_EXECUTE).path(executionCommand.executable).build();


        // Parse arguments_string into a list of arguments like normally done on shells like bourne shell
        // Arguments are split on whitespaces unless quoted with single or double quotes
        // Double quotes and backslashes can be escaped with backslashes in arguments surrounded
        // with double quotes
        List<String> arguments_list = ArgumentTokenizer.tokenize(arguments_string);
        executionCommand.arguments = arguments_list.toArray(new String[arguments_list.size()]);


        Logger.logVerboseExtended(LOG_TAG, executionCommand.toString());
        Logger.logVerbose(LOG_TAG, "Wait For Result: `" + waitForResult + "`");

        // Create execution intent with the action TERMUX_SERVICE#ACTION_SERVICE_EXECUTE to be sentto the TERMUX_SERVICE
        Intent executionIntent = new Intent(TERMUX_SERVICE.ACTION_SERVICE_EXECUTE, executionCommand.executableUri);
        executionIntent.setClassName(TermuxConstants.TERMUX_PACKAGE_NAME, TermuxConstants.TERMUX_APP.TERMUX_SERVICE_NAME);

        executionIntent.putExtra(TERMUX_SERVICE.EXTRA_ARGUMENTS, executionCommand.arguments);
        if (executionCommand.workingDirectory != null && !executionCommand.workingDirectory.isEmpty())
            executionIntent.putExtra(TERMUX_SERVICE.EXTRA_WORKDIR, executionCommand.workingDirectory);
        executionIntent.putExtra(TERMUX_SERVICE.EXTRA_STDIN, executionCommand.stdin);
        executionIntent.putExtra(TERMUX_SERVICE.EXTRA_BACKGROUND, executionCommand.inBackground);

        // Send execution intent to TERMUX_SERVICE
        PluginUtils.sendExecuteIntentToExecuteService(context, this, intent, executionIntent, waitForResult);
    }

}
