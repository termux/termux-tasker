package com.termux.tasker;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.textfield.TextInputEditText;
import com.termux.tasker.utils.FileUtils;
import com.termux.tasker.utils.Logger;
import com.termux.tasker.utils.PluginUtils;
import com.termux.tasker.utils.TermuxAppUtils;

import androidx.appcompat.app.ActionBar;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This is the "Edit" activity for a Locale Plug-in.
 * <p>
 * This Activity can be started in one of two states:
 * <ul>
 * <li>New plug-in: The Activity's Intent will not contain "com.twofortyfouram.locale.Intent#EXTRA_BUNDLE".</li>
 * <li>Old plug-in: The Activity's Intent will contain "com.twofortyfouram.locale.Intent#EXTRA_BUNDLE" from
 * a previously saved plug-in instance that the user is editing.</li>
 * </ul>
 */
public final class EditConfigurationActivity extends AbstractPluginActivity {

    private AutoCompleteTextView mExecutablePathText;
    private TextInputEditText mArgumentsText;
    private AutoCompleteTextView mWorkingDirectoryPathText;
    private CheckBox mInTerminalCheckbox;
    private TextView mExecutableAbsolutePathText;
    private TextView mWorkingDirectoryAbsolutePathText;
    private TextView mTermuxAppFilesPathInaccessibleWarning;
    private TextView mPluginPermissionUngrantedWarning;
    private TextView mAllowExternalAppsUngrantedWarning;

    private String[] executableFileNamesList = new String[0];
    ArrayAdapter<String> executableFileNamesAdaptor;
    private String[] workingDirectoriesNamesList = new String[0];
    ArrayAdapter<String> workingDirectoriesNamesAdaptor;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.app_name);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setContentView(R.layout.edit_activity);

        final Intent intent = getIntent();
        BundleScrubber.scrub(intent);
        final Bundle localeBundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
        BundleScrubber.scrub(localeBundle);


        mExecutablePathText = findViewById(R.id.executable_path);
        mArgumentsText = findViewById(R.id.arguments);
        mWorkingDirectoryPathText = findViewById(R.id.working_directory_path);
        mInTerminalCheckbox = findViewById(R.id.in_terminal);
        mExecutableAbsolutePathText = findViewById(R.id.executable_absolute_path);
        mWorkingDirectoryAbsolutePathText = findViewById(R.id.working_directory_absolute_path);
        mTermuxAppFilesPathInaccessibleWarning = findViewById(R.id.termux_app_files_path_inaccessible_warning);
        mPluginPermissionUngrantedWarning = findViewById(R.id.plugin_permission_ungranted_warning);
        mAllowExternalAppsUngrantedWarning = findViewById(R.id.allow_external_apps_ungranted_warning);


        mExecutablePathText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                processExecutablePath(editable == null ? null : editable.toString());
            }
        });

        executableFileNamesAdaptor = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(Arrays.asList(executableFileNamesList)));
        mExecutablePathText.setAdapter(executableFileNamesAdaptor);


        mWorkingDirectoryPathText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                processWorkingDirectoryPath(editable == null ? null : editable.toString());
            }
        });

        workingDirectoriesNamesAdaptor = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(Arrays.asList(workingDirectoriesNamesList)));
        mWorkingDirectoryPathText.setAdapter(workingDirectoriesNamesAdaptor);


        if (savedInstanceState == null) {
            if (localeBundle != null) {
                String errmsg;
                // If bundle is valid, then load values from bundle
                errmsg = PluginBundleManager.isBundleValid(this, localeBundle);
                if (errmsg == null) {
                    final String selectedExecutable = localeBundle.getString(PluginBundleManager.EXTRA_EXECUTABLE);
                    mExecutablePathText.setText(selectedExecutable);
                    final String selectedArguments = localeBundle.getString(PluginBundleManager.EXTRA_ARGUMENTS);
                    mArgumentsText.setText(selectedArguments);
                    final String selectedWorkingDirectory = localeBundle.getString(PluginBundleManager.EXTRA_WORKDIR);
                    mWorkingDirectoryPathText.setText(selectedWorkingDirectory);
                    final boolean inTerminal = localeBundle.getBoolean(PluginBundleManager.EXTRA_TERMINAL);
                    mInTerminalCheckbox.setChecked(inTerminal);
                } else {
                    Logger.logError(this, errmsg);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.edit_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_log_level) {
            Logger.showSetLogLevelDialog(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        if (!isCanceled()) {
            final String executable = mExecutablePathText.getText() == null ? null : mExecutablePathText.getText().toString();
            final String arguments =  mArgumentsText.getText() == null ? null : mArgumentsText.getText().toString();
            final String workingDirectory = mWorkingDirectoryPathText.getText() == null ? null : mWorkingDirectoryPathText.getText().toString();
            final boolean inTerminal = mInTerminalCheckbox.isChecked();

            if (executable != null && executable.length() > 0) {
                final Intent resultIntent = new Intent();

                /*
                 * This extra is the data to ourselves: either for the Activity or the BroadcastReceiver. Note
                 * that anything placed in this Bundle must be available to Locale's class loader. So storing
                 * String, int, and other standard objects will work just fine. Parcelable objects are not
                 * acceptable, unless they also implement Serializable. Serializable objects must be standard
                 * Android platform objects (A Serializable class private to this plug-in's APK cannot be
                 * stored in the Bundle, as Locale's classloader will not recognize it).
                 */
                final Bundle resultBundle = PluginBundleManager.generateBundle(getApplicationContext(), executable, arguments, workingDirectory, inTerminal);

                // The blurb is a concise status text to be displayed in the host's UI.
                final String blurb = generateBlurb(executable, arguments, inTerminal);

                // If host supports variable replacement when running plugin action, then
                // request it to replace variables in following fields
                if (TaskerPlugin.Setting.hostSupportsOnFireVariableReplacement(this)){
                    TaskerPlugin.Setting.setVariableReplaceKeys(resultBundle,new String[] {
                            PluginBundleManager.EXTRA_EXECUTABLE,
                            PluginBundleManager.EXTRA_ARGUMENTS,
                            PluginBundleManager.EXTRA_WORKDIR
                    });
                }

                resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, resultBundle);
                resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, blurb);

                // Configuration information for Tasker variables returned from the executed task
                //
                // Do not run if we are opening a terminal, because the user might not care about this
                // if they are running something that will literally pop up in front of them (Plus
                // getting that information requires additional work for now)
                if(!inTerminal) {
                    if (TaskerPlugin.hostSupportsRelevantVariables(getIntent().getExtras())) {
                        TaskerPlugin.addRelevantVariableList(resultIntent, new String[]{
                                PluginResultsService.PLUGIN_VARIABLE_STDOUT + "\nStandard Output\nThe <B>stdout</B> of the command.",
                                PluginResultsService.PLUGIN_VARIABLE_STDERR + "\nStandard Error\nThe <B>stderr</B> of the command.",
                                PluginResultsService.PLUGIN_VARIABLE_EXIT_CODE + "\nExit Code\nThe <B>exit code</B> of the command. " +
                                        "0 often means success and anything else is usually a failure of some sort."
                        });
                    }
                }

                // To use variables, we can't have a timeout of 0, but if someone doesn't pay
                // attention to this and runs a task that never ends, 10 seconds seems like a
                // reasonable timeout. If they need more time, or want this to run entirely
                // asynchronously, that can be set
                if (TaskerPlugin.Setting.hostSupportsSynchronousExecution(getIntent().getExtras())) {
                    TaskerPlugin.Setting.requestTimeoutMS(resultIntent, 10000);
                }

                setResult(RESULT_OK, resultIntent);
            }
        }

        super.finish();
    }

    /**
     * The message that will be displayed by the plugin host app for the action configuration.
     * Blurb length can be a maximum of 60 characters as defined by locale lib.
     * @param executable value set for the action.
     * @param arguments value set for the action.
     * @param inTerminal value set for the action.
     * @return A blurb for the plug-in.
     */
    String generateBlurb(final String executable, final String arguments, boolean inTerminal) {
        final int stringResource = inTerminal ? R.string.blurb_in_terminal : R.string.blurb_in_background;
        final String message = getString(stringResource, executable, arguments);
        final int maxBlurbLength = 60; // R.integer.twofortyfouram_locale_maximum_blurb_length.
        return (message.length() > maxBlurbLength) ? message.substring(0, maxBlurbLength) : message;
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkIfPluginCanAccessTermuxApp();
        checkIfPluginHostHasPermissionRunCommand();
        processExecutablePath(mExecutablePathText == null ? null : mExecutablePathText.getText().toString());
        processWorkingDirectoryPath(mWorkingDirectoryPathText == null ? null : mWorkingDirectoryPathText.getText().toString());
    }

    private void checkIfPluginCanAccessTermuxApp() {
        if (mTermuxAppFilesPathInaccessibleWarning == null) return;

        String errmsg;

        // If Termux app is not installed or PREFIX_PATH is not accessible, then show warning
        errmsg = TermuxAppUtils.checkIfTermuxAppIsInstalledAndAccessible(this);
        if (errmsg != null) {
            mTermuxAppFilesPathInaccessibleWarning.setText(errmsg);
            mTermuxAppFilesPathInaccessibleWarning.setVisibility(View.VISIBLE);
        } else {
            mTermuxAppFilesPathInaccessibleWarning.setVisibility(View.GONE);
            mTermuxAppFilesPathInaccessibleWarning.setText(null);
        }
    }

    private void checkIfPluginHostHasPermissionRunCommand() {
        if (mPluginPermissionUngrantedWarning == null) return;

        String errmsg;
        String pluginHostPackage = this.getCallingPackage();

        // Check if pluginHostPackage has been granted PERMISSION_RUN_COMMAND
        errmsg = PluginUtils.checkIfPackageHasPermissionRunCommand(this, pluginHostPackage);
        if (errmsg != null) {
            mPluginPermissionUngrantedWarning.setText(errmsg);
            mPluginPermissionUngrantedWarning.setVisibility(View.VISIBLE);
        } else {
            mPluginPermissionUngrantedWarning.setVisibility(View.GONE);
            mPluginPermissionUngrantedWarning.setText(null);
        }
    }

    private void processExecutablePath(String executable) {
        if (mExecutablePathText == null) return;

        boolean validate = true;
        boolean executableDefined = true;

        mExecutablePathText.setError(null);
        mExecutableAbsolutePathText.setText(null);
        mAllowExternalAppsUngrantedWarning.setVisibility(View.GONE);
        mAllowExternalAppsUngrantedWarning.setText(null);

        if (executable == null || executable.isEmpty()) {
            mExecutablePathText.setError(this.getString(R.string.executable_required));
            validate = false;
            executableDefined = false;
        }

        executable = FileUtils.getAbsolutePathForExecutable(executable);

        // If executable text contains a variable, then no need to set absolute path or validate the path
        if (PluginResultsService.isPluginHostAppVariableContainingString(executable)) {
            mExecutableAbsolutePathText.setText(this.getString(R.string.executable_absolute_path, this.getString(R.string.variable_in_path)));
            executable = null;
            validate = false;
        } else if (executableDefined) {
            mExecutableAbsolutePathText.setText(this.getString(R.string.executable_absolute_path, executable));
        }

        if (validate) {
            File executableFile = new File(executable);

            String errmsg;

            // If executable is not a file, cannot be read or be executed, then return RESULT_CODE_FAILED to plugin host app
            // Readable and executable checks are to be ignored if path is in TASKER_PATH since FireReceiver will automatically
            // set read and execute permissions on execution
            errmsg = FileUtils.checkIfExecutableFileIsReadableAndExecutable(this, executable, false, true);
            if (errmsg != null) {
                mExecutablePathText.setError(errmsg);
            }

            // If executable is not in TASKER_PATH and allow-external-apps property to not set to "true", then show warning
            errmsg = PluginUtils.checkIfAllowExternalAppsPolicyIsViolated(this, executable);
            if (errmsg != null) {
                mAllowExternalAppsUngrantedWarning.setText(errmsg);
                mAllowExternalAppsUngrantedWarning.setVisibility(View.VISIBLE);
            }
        }

        setExecutablePathTextDropdownList(executable);
    }

    private void setExecutablePathTextDropdownList(String executable) {
        if (mExecutablePathText == null) return;

        File executableFile = null;
        File executableParentFile = null;
        File[] files;

        if (executable != null && !executable.isEmpty()) {
            executableFile = new File(executable);
            executableParentFile = executableFile.getParentFile();
        }

        String executablePathText = mExecutablePathText.getText().toString();

        // If executable is null, empty or executable parent is not a directory, then show files in TASKER_DIR
        if (executable == null || executable.isEmpty() || executableParentFile == null || !executableParentFile.isDirectory()) {
            files = Constants.TASKER_DIR.listFiles();
        }
        // If executable is a substring of FILES_PATH, then show files in FILES_PATH
        else if (Constants.FILES_PATH.contains(executable)) {
            executableParentFile = new File(Constants.FILES_PATH);
            files = executableParentFile.listFiles();
        }
        // If executable path in text field ends with "/", then show files in current directory instead of parent directory
        else if (executablePathText.endsWith("/")) {
            executableParentFile = executableFile;
            files = executableParentFile.listFiles();
        }
        // Else show files in parent directory
        else {
            files = executableParentFile.listFiles();
        }

        //Logger.logVerbose(this, "executable: " + executable);
        //Logger.logVerbose(this, "executablePathText: " + executablePathText);

        if (files != null && files.length > 0) {
            Arrays.sort(files);
            String executableFileNamesPrefix = "";
            // If executable is not null, empty or executable parent is not null
            if (executable != null && !executable.isEmpty() && executableParentFile != null) {
                String executableParentPath = executableParentFile.getAbsolutePath();
                //Logger.logVerbose(this, "executableParentPath: " + executableParentPath);

                // If executable path in text field starts with "/", then prefix file names with the parent directory in the drop down list
                if (executablePathText.startsWith("/"))
                    executableFileNamesPrefix = executableParentPath + "/";
                // If executable path in text field starts with "$PREFIX/" or "~/", then prefix file names with the unexpanded path in the drop down list
                else if (executablePathText.startsWith("$PREFIX/") || executablePathText.startsWith("~/")) {
                    executableFileNamesPrefix = FileUtils.getUnExpandedTermuxPath(executableParentPath + "/");
                }
            }

            // Create a string array of filenames with the optional prefix for the drop down list
            executableFileNamesList = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                executableFileNamesList[i] = executableFileNamesPrefix + files[i].getName();
            }
        } else {
            executableFileNamesList = new String[0];
        }

        //Logger.logVerbose(this, Arrays.toString(executableFileNamesList));

        // Update drop down list and show it
        executableFileNamesAdaptor.clear();
        executableFileNamesAdaptor.addAll(new ArrayList<>(Arrays.asList(executableFileNamesList)));
        executableFileNamesAdaptor.notifyDataSetChanged();
        if (mExecutablePathText.isFocused())
            mExecutablePathText.showDropDown();
    }

    private void processWorkingDirectoryPath(String workingDirectory) {
        if (mWorkingDirectoryPathText == null) return;

        boolean validate = true;
        boolean workingDirectoryDefined = true;

        mWorkingDirectoryPathText.setError(null);
        mWorkingDirectoryAbsolutePathText.setVisibility(View.GONE);
        mWorkingDirectoryAbsolutePathText.setText(null);

        if (workingDirectory == null || workingDirectory.isEmpty()) {
            validate = false;
            workingDirectoryDefined = false;
        }

        workingDirectory = FileUtils.getAbsolutePathForExecutable(workingDirectory);

        // If workingDirectory text contains a variable, then no need to set absolute path or validate the path
        if (PluginResultsService.isPluginHostAppVariableContainingString(workingDirectory)) {
            mWorkingDirectoryAbsolutePathText.setText(this.getString(R.string.working_directory_absolute_path, this.getString(R.string.variable_in_path)));
            mWorkingDirectoryAbsolutePathText.setVisibility(View.VISIBLE);
            workingDirectory = null;
            validate = false;
        } else if (workingDirectoryDefined) {
            mWorkingDirectoryAbsolutePathText.setText(this.getString(R.string.working_directory_absolute_path, workingDirectory));
            mWorkingDirectoryAbsolutePathText.setVisibility(View.VISIBLE);
        }

        if (validate) {
            File executableFile = new File(workingDirectory);

            String errmsg;

            // If workingDirectory is not a directory or cannot be read, then return RESULT_CODE_FAILED to plugin host app
            // Existence and readable checks are to be ignored if path is in HOME_PATH since FireReceiver will automatically
            // create and set read permissions on execution
            errmsg = FileUtils.checkIfDirectoryIsReadable(this, workingDirectory, false, false, true);
            if (errmsg != null) {
                mWorkingDirectoryPathText.setError(errmsg);
            }
        }

        setWorkingDirectoryPathTextDropdownList(workingDirectory);
    }

    private void setWorkingDirectoryPathTextDropdownList(String workingDirectory) {
        if (mWorkingDirectoryPathText == null) return;

        File workingDirectoryFile = null;
        File workingDirectoryParentFile = null;
        File[] files;

        if (workingDirectory != null && !workingDirectory.isEmpty()) {
            workingDirectoryFile = new File(workingDirectory);
            workingDirectoryParentFile = workingDirectoryFile.getParentFile();
        }

        String workingDirectoryPathText = mWorkingDirectoryPathText.getText().toString();

        // If workingDirectory is null, empty or workingDirectory parent is not a directory, then show nothing
        if (workingDirectory == null || workingDirectory.isEmpty() || workingDirectoryParentFile == null || !workingDirectoryParentFile.isDirectory()) {
            files = new File[0];
        }
        // If workingDirectory is a substring of FILES_PATH, then show files in FILES_PATH
        else if (Constants.FILES_PATH.contains(workingDirectory)) {
            workingDirectoryParentFile = new File(Constants.FILES_PATH);
            files = workingDirectoryParentFile.listFiles();
        }
        // If workingDirectory path in text field ends with "/", then show files in current directory instead of parent directory
        else if (workingDirectoryPathText.endsWith("/")) {
            workingDirectoryParentFile = workingDirectoryFile;
            files = workingDirectoryParentFile.listFiles();
        }
        // Else show files in parent directory
        else {
            files = workingDirectoryParentFile.listFiles();
        }

        //Logger.logVerbose(this, "workingDirectory: " + workingDirectory);
        //Logger.logVerbose(this, "workingDirectoryPathText: " + workingDirectoryPathText);

        if (files != null && files.length > 0) {
            Arrays.sort(files);
            String workingDirectoryFileNamesPrefix = "";

            // If workingDirectory is not null, empty or workingDirectory parent is not null
            String workingDirectoryParentPath = workingDirectoryParentFile.getAbsolutePath();
            //Logger.logVerbose(this, "workingDirectoryParentPath: " + workingDirectoryParentPath);

            // If workingDirectory path in text field starts with "/", then prefix file names with the parent directory in the drop down list
            if (workingDirectoryPathText.startsWith("/"))
                workingDirectoryFileNamesPrefix = workingDirectoryParentPath + "/";
                // If workingDirectory path in text field starts with "$PREFIX/" or "~/", then prefix file names with the unexpanded path in the drop down list
            else if (workingDirectoryPathText.startsWith("$PREFIX/") || workingDirectoryPathText.startsWith("~/")) {
                workingDirectoryFileNamesPrefix = FileUtils.getUnExpandedTermuxPath(workingDirectoryParentPath + "/");
            }

            // Create a string array of filenames with the optional prefix for the drop down list
            workingDirectoriesNamesList = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                workingDirectoriesNamesList[i] = workingDirectoryFileNamesPrefix + files[i].getName();
            }
        } else {
            workingDirectoriesNamesList = new String[0];
        }

        //Logger.logVerbose(this, Arrays.toString(workingDirectoriesNamesList));

        // Update drop down list and show it
        workingDirectoriesNamesAdaptor.clear();
        workingDirectoriesNamesAdaptor.addAll(new ArrayList<>(Arrays.asList(workingDirectoriesNamesList)));
        workingDirectoriesNamesAdaptor.notifyDataSetChanged();
        if (mWorkingDirectoryPathText.isFocused())
            mWorkingDirectoryPathText.showDropDown();
    }

}
