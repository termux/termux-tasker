package com.termux.tasker;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.termux.shared.activities.TextIOActivity;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.data.DataUtils;
import com.termux.shared.data.IntentUtils;
import com.termux.shared.errors.Error;
import com.termux.shared.logger.Logger;
import com.termux.shared.models.TextIOInfo;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE;
import com.termux.shared.file.FileUtils;
import com.termux.shared.termux.TermuxUtils;
import com.termux.shared.termux.file.TermuxFileUtils;
import com.termux.shared.termux.theme.TermuxThemeUtils;
import com.termux.shared.theme.NightMode;
import com.termux.tasker.utils.LoggerUtils;
import com.termux.tasker.utils.PluginUtils;
import com.termux.tasker.utils.TaskerPlugin;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.termux.tasker.utils.TaskerPlugin.Setting.RESULT_CODE_FAILED;

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

    private TextInputLayout mExecutablePathTextLayout;
    private AutoCompleteTextView mExecutablePathText;
    private TextInputEditText mArgumentsText;
    private TextInputLayout mWorkingDirectoryPathTextLayout;
    private AutoCompleteTextView mWorkingDirectoryPathText;
    private TextView mStdinView;
    private TextInputLayout mSessionActionLayout;
    private TextInputEditText mSessionAction;
    private TextInputLayout mBackgroundCustomLogLevelLayout;
    private TextInputEditText mBackgroundCustomLogLevel;
    private CheckBox mInTerminalCheckbox;
    private CheckBox mWaitForResult;
    private TextView mExecutableAbsolutePathText;
    private TextView mWorkingDirectoryAbsolutePathText;
    private TextView mTermuxAppFilesPathInaccessibleWarning;
    private TextView mPluginPermissionUngrantedWarning;
    private TextView mAllowExternalAppsUngrantedWarning;

    private ActivityResultLauncher<Intent> mStartTextIOActivityForResult;

    private String mStdin;

    private String[] executableFileNamesList = new String[0];
    ArrayAdapter<String> executableFileNamesAdaptor;
    private String[] workingDirectoriesNamesList = new String[0];
    ArrayAdapter<String> workingDirectoriesNamesAdaptor;

    public static final String ACTION_GET_STDIN = "ACTION_GET_STDIN";

    private static final String LOG_TAG = "EditConfigurationActivity";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set NightMode.APP_NIGHT_MODE
        TermuxThemeUtils.setAppNightMode(this);
        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);

        setContentView(R.layout.activity_edit_configuration);

        AppCompatActivityUtils.setToolbar(this, R.id.toolbar);
        AppCompatActivityUtils.setToolbarTitle(this, R.id.toolbar, TermuxConstants.TERMUX_TASKER_APP_NAME, 0);
        AppCompatActivityUtils.setShowBackButtonInActionBar(this, true);

        setStartTextIOActivityForResult();

        final Intent intent = getIntent();
        BundleScrubber.scrub(intent);
        final Bundle localeBundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
        BundleScrubber.scrub(localeBundle);

        Logger.logInfo(LOG_TAG, "Bundle Received: " + IntentUtils.getBundleString(localeBundle));

        TextView mHelp = findViewById(R.id.textview_help);
        mHelp.setText(this.getString(R.string.plugin_api_help, TermuxConstants.TERMUX_TASKER_GITHUB_REPO_URL));

        mExecutablePathTextLayout = findViewById(R.id.layout_executable_path);
        mExecutablePathText = findViewById(R.id.executable_path);
        mArgumentsText = findViewById(R.id.arguments);
        mWorkingDirectoryPathTextLayout = findViewById(R.id.layout_working_directory_path);
        mWorkingDirectoryPathText = findViewById(R.id.working_directory_path);
        mStdinView = findViewById(R.id.view_stdin);
        mSessionActionLayout = findViewById(R.id.layout_session_action);
        mSessionAction = findViewById(R.id.session_action);
        mBackgroundCustomLogLevelLayout = findViewById(R.id.layout_background_custom_log_level);
        mBackgroundCustomLogLevel = findViewById(R.id.background_custom_log_level);
        mInTerminalCheckbox = findViewById(R.id.in_terminal);
        mWaitForResult = findViewById(R.id.wait_for_result);
        mExecutableAbsolutePathText = findViewById(R.id.executable_absolute_path);
        mWorkingDirectoryAbsolutePathText = findViewById(R.id.working_directory_absolute_path);
        mTermuxAppFilesPathInaccessibleWarning = findViewById(R.id.termux_app_files_path_inaccessible_warning);
        mPluginPermissionUngrantedWarning = findViewById(R.id.plugin_permission_ungranted_warning);
        mAllowExternalAppsUngrantedWarning = findViewById(R.id.allow_external_apps_ungranted_warning);


        setExecutionPathViews();
        setWorkingDirectoryPathViews();
        setStdinView();
        setSessionActionViews();
        setBackgroundCustomLogLevelViews();
        setInTerminalView();

        // Currently savedInstanceState bundle is not supported
        if (savedInstanceState != null || localeBundle == null) {
            Logger.logInfo(LOG_TAG, "Not loading values from null bundle");
            // Enable by default
            mInTerminalCheckbox.setChecked(false);
            mWaitForResult.setChecked(true);
            updateStdinViewVisibility(false);
            updateSessionActionViewVisibility(false);
            updateBackgroundCustomLogLevelViewVisibility(false);
            return;
        }

        String errmsg;
        // If bundle is valid, then load values from bundle
        errmsg = PluginBundleManager.parseBundle(this, localeBundle);
        if (errmsg != null) {
            Logger.logError(LOG_TAG, errmsg);
            return;
        }

        final String selectedExecutable = localeBundle.getString(PluginBundleManager.EXTRA_EXECUTABLE);
        mExecutablePathText.setText(selectedExecutable);
        processExecutablePath(selectedExecutable);

        final String selectedArguments = localeBundle.getString(PluginBundleManager.EXTRA_ARGUMENTS);
        mArgumentsText.setText(selectedArguments);

        final String selectedWorkingDirectory = localeBundle.getString(PluginBundleManager.EXTRA_WORKDIR);
        mWorkingDirectoryPathText.setText(selectedWorkingDirectory);
        processWorkingDirectoryPath(selectedWorkingDirectory);

        final boolean inTerminal = localeBundle.getBoolean(PluginBundleManager.EXTRA_TERMINAL);
        mInTerminalCheckbox.setChecked(inTerminal);

        mStdin = DataUtils.getTruncatedCommandOutput(localeBundle.getString(PluginBundleManager.EXTRA_STDIN),
                DataUtils.TRANSACTION_SIZE_LIMIT_IN_BYTES, true, false, false);
        updateStdinViewText();
        updateStdinViewVisibility(inTerminal);

        final String sessionAction = localeBundle.getString(PluginBundleManager.EXTRA_SESSION_ACTION);
        mSessionAction.setText(sessionAction);
        processSessionAction(sessionAction);
        updateSessionActionViewVisibility(inTerminal);

        final String backgroundCustomLogLevel = localeBundle.getString(PluginBundleManager.EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL);
        mBackgroundCustomLogLevel.setText(backgroundCustomLogLevel);
        processBackgroundCustomLogLevel(backgroundCustomLogLevel);
        updateBackgroundCustomLogLevelViewVisibility(inTerminal);

        final boolean waitForResult = localeBundle.getBoolean(PluginBundleManager.EXTRA_WAIT_FOR_RESULT, true);
        mWaitForResult.setChecked(waitForResult);
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkIfPluginCanAccessTermuxApp();
        checkIfPluginHostHasPermissionRunCommand();
        processExecutablePath(mExecutablePathText == null ? null : mExecutablePathText.getText().toString());
        processWorkingDirectoryPath(mWorkingDirectoryPathText == null ? null : mWorkingDirectoryPathText.getText().toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_edit_configuration, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_log_level) {
            LoggerUtils.showSetLogLevelDialog(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }





    private void setExecutionPathViews() {
        mExecutablePathText.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                processExecutablePath(editable == null ? null : editable.toString());
            }
        });

        executableFileNamesAdaptor = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(Arrays.asList(executableFileNamesList)));
        mExecutablePathText.setAdapter(executableFileNamesAdaptor);
    }


    private void setWorkingDirectoryPathViews() {
        mWorkingDirectoryPathText.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                processWorkingDirectoryPath(editable == null ? null : editable.toString());
            }
        });

        workingDirectoriesNamesAdaptor = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(Arrays.asList(workingDirectoriesNamesList)));
        mWorkingDirectoryPathText.setAdapter(workingDirectoriesNamesAdaptor);
    }


    private void setStdinView() {
        mStdinView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStartTextIOActivityForResult == null) return;

                // Tasker has a Bundle size limit check:
                // Parcel obtain = Parcel.obtain(); bundle.writeToParcel(obtain, 0); if (obtain.dataSize() < 100000);
                // And will throw `plugin data too large` if it exceeds.
                // Note that on Android 7-11, String characters are stored in Parcel as UTF-16, i.e 2 bytes
                // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r40:frameworks/base/core/java/android/os/Parcel.java;l=773
                TextIOInfo textIOInfo = new TextIOInfo(ACTION_GET_STDIN, EditConfigurationActivity.this.getClass().getCanonicalName());
                textIOInfo.setTitle(getString(R.string.title_stdin));
                textIOInfo.setText(mStdin);
                textIOInfo.setTextSize(12);
                textIOInfo.setTextLengthLimit(90000/2); // Leave some for other data in result bundle. Limit is 45K characters.
                textIOInfo.setTextTypeFaceFamily("monospace");
                textIOInfo.setTextTypeFaceStyle(Typeface.NORMAL);
                textIOInfo.setTextHorizontallyScrolling(true);
                textIOInfo.setShowTextCharacterUsage(true);
                textIOInfo.setShowBackButtonInActionBar(true);

                mStartTextIOActivityForResult.launch(TextIOActivity.newInstance(EditConfigurationActivity.this, textIOInfo));
            }
        });
    }

    private void updateStdinViewText() {
        if (mStdinView == null) return;
        mStdinView.setText(DataUtils.getTruncatedCommandOutput(mStdin, 200, true, false, false));
    }

    private void updateStdinViewVisibility(boolean inTerminal) {
        if (mStdinView == null) return;
        mStdinView.setVisibility(inTerminal ? View.GONE : View.VISIBLE);
    }


    private void setSessionActionViews() {
        mSessionAction.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                processSessionAction(editable == null ? null : editable.toString());
            }
        });
    }

    private void updateSessionActionViewVisibility(boolean inTerminal) {
        if (mSessionAction == null) return;
        mSessionAction.setVisibility(inTerminal ? View.VISIBLE : View.GONE);
    }


    private void setBackgroundCustomLogLevelViews() {
        mBackgroundCustomLogLevel.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                processBackgroundCustomLogLevel(editable == null ? null : editable.toString());
            }
        });
    }

    private void updateBackgroundCustomLogLevelViewVisibility(boolean inTerminal) {
        if (mBackgroundCustomLogLevel == null) return;
        mBackgroundCustomLogLevel.setVisibility(inTerminal ? View.GONE : View.VISIBLE);
    }


    private void setInTerminalView() {
        mInTerminalCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateStdinViewVisibility(isChecked);
                updateSessionActionViewVisibility(isChecked);
                updateBackgroundCustomLogLevelViewVisibility(isChecked);
            }
        });
    }



    private void checkIfPluginCanAccessTermuxApp() {
        if (mTermuxAppFilesPathInaccessibleWarning == null) return;

        String errmsg;

        // If Termux app is not installed, enabled or accessible with current context or if
        // TermuxConstants.TERMUX_PREFIX_DIR_PATH does not exist or has required permissions,
        // then show warning.
        errmsg = TermuxUtils.isTermuxAppAccessible(this);
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

        mExecutablePathTextLayout.setError(null);
        mExecutableAbsolutePathText.setText(null);
        mAllowExternalAppsUngrantedWarning.setVisibility(View.GONE);
        mAllowExternalAppsUngrantedWarning.setText(null);

        if (executable == null || executable.isEmpty()) {
            mExecutablePathTextLayout.setError(this.getString(R.string.error_executable_required));
            validate = false;
            executableDefined = false;
        }

        executable = TermuxFileUtils.getCanonicalPath(executable, TermuxConstants.TERMUX_TASKER_SCRIPTS_DIR_PATH, true);

        // If executable text contains a variable, then no need to set absolute path or validate the path
        if (PluginUtils.isPluginHostAppVariableContainingString(executable)) {
            mExecutableAbsolutePathText.setText(this.getString(R.string.msg_absolute_path, this.getString(R.string.msg_variable_in_string)));
            executable = null;
            validate = false;
        } else if (executableDefined) {
            mExecutableAbsolutePathText.setText(this.getString(R.string.msg_absolute_path, executable));
        }

        if (validate) {
            // If executable is not a file, cannot be read or be executed, then show warning
            // Missing permissions (but not existence) checks are to be ignored if path is in
            // TermuxConstants#TERMUX_TASKER_SCRIPTS_DIR_PATH since FireReceiver
            // will automatically set permissions on execution.
            Error error = FileUtils.validateRegularFileExistenceAndPermissions("executable", executable,
                    TermuxConstants.TERMUX_TASKER_SCRIPTS_DIR_PATH,
                    FileUtils.APP_EXECUTABLE_FILE_PERMISSIONS,false, false,
                    true);
            if (error != null) {
                Error shortError = FileUtils.getShortFileUtilsError(error);
                mExecutablePathTextLayout.setError(shortError.getMessage());
            }

            // If executable is not in TermuxConstants#TERMUX_TASKER_SCRIPTS_DIR_PATH and
            // "allow-external-apps" property to not set to "true", then show warning
            String errmsg = PluginUtils.checkIfTermuxTaskerAllowExternalAppsPolicyIsViolated(this, executable);
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

        // If executable is null, empty or executable parent is not a directory, then show files
        // in TermuxConstants.TERMUX_TASKER_SCRIPTS_DIR
        if (executable == null || executable.isEmpty() || executableParentFile == null || !executableParentFile.isDirectory()) {
            files = TermuxConstants.TERMUX_TASKER_SCRIPTS_DIR.listFiles();
        }
        // If executable is a substring of TermuxConstants.TERMUX_FILES_DIR_PATH, then show
        // files in TermuxConstants.TERMUX_FILES_DIR_PATH
        else if (TermuxConstants.TERMUX_FILES_DIR_PATH.contains(executable)) {
            executableParentFile = new File(TermuxConstants.TERMUX_FILES_DIR_PATH);
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

        //Logger.logVerbose(LOG_TAG, "executable: " + executable);
        //Logger.logVerbose(LOG_TAG, "executablePathText: " + executablePathText);

        if (files != null && files.length > 0) {
            Arrays.sort(files);
            String executableFileNamesPrefix = "";
            // If executable is not null, empty or executable parent is not null
            if (executable != null && !executable.isEmpty() && executableParentFile != null) {
                String executableParentPath = executableParentFile.getAbsolutePath();
                //Logger.logVerbose(LOG_TAG, "executableParentPath: " + executableParentPath);

                // If executable path in text field starts with "/", then prefix file names with the
                // parent directory in the drop down list
                if (executablePathText.startsWith("/"))
                    executableFileNamesPrefix = executableParentPath + "/";
                    // If executable path in text field starts with "$PREFIX/" or "~/", then prefix file names
                    // with the unexpanded path in the drop down list
                else if (executablePathText.startsWith("$PREFIX/") || executablePathText.startsWith("~/")) {
                    executableFileNamesPrefix = TermuxFileUtils.getUnExpandedTermuxPath(executableParentPath + "/");
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

        //Logger.logVerbose(LOG_TAG, Arrays.toString(executableFileNamesList));

        // Update drop down list and show it
        executableFileNamesAdaptor.clear();
        executableFileNamesAdaptor.addAll(new ArrayList<>(Arrays.asList(executableFileNamesList)));
        executableFileNamesAdaptor.notifyDataSetChanged();
        if (mExecutablePathText.isFocused() && mExecutablePathText.getWindowToken() != null)
            mExecutablePathText.showDropDown();
    }

    private void processWorkingDirectoryPath(String workingDirectory) {
        if (mWorkingDirectoryPathText == null) return;

        boolean validate = true;
        boolean workingDirectoryDefined = true;

        mWorkingDirectoryPathTextLayout.setError(null);
        mWorkingDirectoryAbsolutePathText.setVisibility(View.GONE);
        mWorkingDirectoryAbsolutePathText.setText(null);

        if (workingDirectory == null || workingDirectory.isEmpty()) {
            validate = false;
            workingDirectoryDefined = false;
        }

        workingDirectory = TermuxFileUtils.getCanonicalPath(workingDirectory, null, true);

        // If workingDirectory text contains a variable, then no need to set absolute path or validate the path
        if (PluginUtils.isPluginHostAppVariableContainingString(workingDirectory)) {
            mWorkingDirectoryAbsolutePathText.setText(this.getString(R.string.msg_absolute_path, this.getString(R.string.msg_variable_in_string)));
            mWorkingDirectoryAbsolutePathText.setVisibility(View.VISIBLE);
            workingDirectory = null;
            validate = false;
        } else if (workingDirectoryDefined) {
            mWorkingDirectoryAbsolutePathText.setText(this.getString(R.string.msg_absolute_path, workingDirectory));
            mWorkingDirectoryAbsolutePathText.setVisibility(View.VISIBLE);
        }

        if (validate) {
            // If workingDirectory is not a directory or cannot be read, then show warning
            // Existence and missing permissions checks are to be ignored if path is under allowed
            // termux working directory paths since FireReceiver will automatically create and set
            // permissions on execution.
            Error error = TermuxFileUtils.validateDirectoryFileExistenceAndPermissions("working", workingDirectory,
                    false, false, false,
                    false, true);
            if (error != null) {
                Error shortError = FileUtils.getShortFileUtilsError(error);
                mWorkingDirectoryPathTextLayout.setError(shortError.getMessage());
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
        if (workingDirectory == null || workingDirectory.isEmpty() ||
                workingDirectoryParentFile == null || !workingDirectoryParentFile.isDirectory()) {
            files = new File[0];
        }
        // If workingDirectory is a substring of FILES_PATH, then show files in FILES_PATH
        else if (TermuxConstants.TERMUX_FILES_DIR_PATH.contains(workingDirectory)) {
            workingDirectoryParentFile = TermuxConstants.TERMUX_FILES_DIR;
            files = workingDirectoryParentFile.listFiles();
        }
        // If workingDirectory path in text field ends with "/", then show files in current directory
        // instead of parent directory
        else if (workingDirectoryPathText.endsWith("/")) {
            workingDirectoryParentFile = workingDirectoryFile;
            files = workingDirectoryParentFile.listFiles();
        }
        // Else show files in parent directory
        else {
            files = workingDirectoryParentFile.listFiles();
        }

        //Logger.logVerbose(LOG_TAG, "workingDirectory: " + workingDirectory);
        //Logger.logVerbose(LOG_TAG, "workingDirectoryPathText: " + workingDirectoryPathText);

        if (files != null && files.length > 0) {
            Arrays.sort(files);
            String workingDirectoryFileNamesPrefix = "";

            // If workingDirectory is not null, empty or workingDirectory parent is not null
            String workingDirectoryParentPath = workingDirectoryParentFile.getAbsolutePath();
            //Logger.logVerbose(LOG_TAG, "workingDirectoryParentPath: " + workingDirectoryParentPath);

            // If workingDirectory path in text field starts with "/", then prefix file names with the
            // parent directory in the drop down list
            if (workingDirectoryPathText.startsWith("/"))
                workingDirectoryFileNamesPrefix = workingDirectoryParentPath + "/";
                // If workingDirectory path in text field starts with "$PREFIX/" or "~/", then prefix
                // file names with the unexpanded path in the drop down list
            else if (workingDirectoryPathText.startsWith("$PREFIX/") || workingDirectoryPathText.startsWith("~/")) {
                workingDirectoryFileNamesPrefix = TermuxFileUtils.getUnExpandedTermuxPath(workingDirectoryParentPath + "/");
            }

            // Create a string array of filenames with the optional prefix for the drop down list
            workingDirectoriesNamesList = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                workingDirectoriesNamesList[i] = workingDirectoryFileNamesPrefix + files[i].getName();
            }
        } else {
            workingDirectoriesNamesList = new String[0];
        }

        //Logger.logVerbose(LOG_TAG, Arrays.toString(workingDirectoriesNamesList));

        // Update drop down list and show it
        workingDirectoriesNamesAdaptor.clear();
        workingDirectoriesNamesAdaptor.addAll(new ArrayList<>(Arrays.asList(workingDirectoriesNamesList)));
        workingDirectoriesNamesAdaptor.notifyDataSetChanged();
        if (mWorkingDirectoryPathText.isFocused() && mWorkingDirectoryPathText.getWindowToken() != null)
            mWorkingDirectoryPathText.showDropDown();
    }

    private void processSessionAction(String sessionActionString) {
        processIntFieldValue(mSessionActionLayout, sessionActionString,
                TERMUX_SERVICE.MIN_VALUE_EXTRA_SESSION_ACTION, TERMUX_SERVICE.MAX_VALUE_EXTRA_SESSION_ACTION);
    }

    private void processBackgroundCustomLogLevel(String backgroundCustomLogLevelString) {
        processIntFieldValue(mBackgroundCustomLogLevelLayout, backgroundCustomLogLevelString,
                Logger.LOG_LEVEL_OFF, Logger.MAX_LOG_LEVEL);
    }

    private void processIntFieldValue(TextInputLayout editText, String stringValue, int min, int max) {
        if (editText == null) return;
        editText.setError(null);
        if (DataUtils.isNullOrEmpty(stringValue)) return;
        if (PluginUtils.isPluginHostAppVariableContainingString(stringValue)) return;

        Integer value = null;
        boolean invalid = false;

        try {
            value = Integer.parseInt(stringValue);
        }
        catch (Exception e) {
            invalid = true;
        }

        if (invalid || value < min || value > max) {
            editText.setError(getString(R.string.error_int_not_in_range, min, max));
        }
    }





    @Override
    public void finish() {
        if (isCanceled()) {
            super.finish();
            return;
        }

        final String executable = DataUtils.getDefaultIfUnset(mExecutablePathText.getText() == null ? null : mExecutablePathText.getText().toString(), null);
        final String arguments =  DataUtils.getDefaultIfUnset(mArgumentsText.getText() == null ? null : mArgumentsText.getText().toString(), null);
        final String workingDirectory = DataUtils.getDefaultIfUnset(mWorkingDirectoryPathText.getText() == null ? null : mWorkingDirectoryPathText.getText().toString(), null);
        final String sessionAction = DataUtils.getDefaultIfUnset(mSessionAction.getText() == null ? null : mSessionAction.getText().toString(), null);
        final String backgroundCustomLogLevel = DataUtils.getDefaultIfUnset(mBackgroundCustomLogLevel.getText() == null ? null : mBackgroundCustomLogLevel.getText().toString(), null);
        final boolean inTerminal = mInTerminalCheckbox.isChecked();
        final boolean waitForResult = mWaitForResult.isChecked();

        if (executable == null || executable.length() <= 0) {
            super.finish();
            return;
        }

        final Intent resultIntent = new Intent();

        /*
         * This extra is the data to ourselves: either for the Activity or the BroadcastReceiver. Note
         * that anything placed in this Bundle must be available to Locale's class loader. So storing
         * String, int, and other standard objects will work just fine. Parcelable objects are not
         * acceptable, unless they also implement Serializable. Serializable objects must be standard
         * Android platform objects (A Serializable class private to this plug-in's APK cannot be
         * stored in the Bundle, as Locale's classloader will not recognize it).
         */
        final Bundle resultBundle = PluginBundleManager.generateBundle(getApplicationContext(),
                executable, arguments, workingDirectory, mStdin, sessionAction, backgroundCustomLogLevel, inTerminal, waitForResult);
        if (resultBundle == null) {
            Logger.showToast(this, getString(R.string.error_generate_plugin_bundle_failed), true);
            setResult(RESULT_CODE_FAILED, resultIntent);
            super.finish();
            return;
        }

        Logger.logDebug(LOG_TAG, "Result bundle size: " + PluginBundleManager.getBundleSize(resultBundle));

        // The blurb is a concise status text to be displayed in the host's UI.
        final String blurb = PluginBundleManager.generateBlurb(this, executable, arguments,
                workingDirectory, mStdin, sessionAction, backgroundCustomLogLevel, inTerminal, waitForResult);

        // If host supports variable replacement when running plugin action, then
        // request it to replace variables in following fields
        if (TaskerPlugin.Setting.hostSupportsOnFireVariableReplacement(this)){
            TaskerPlugin.Setting.setVariableReplaceKeys(resultBundle,new String[] {
                    PluginBundleManager.EXTRA_EXECUTABLE,
                    PluginBundleManager.EXTRA_ARGUMENTS,
                    PluginBundleManager.EXTRA_WORKDIR,
                    PluginBundleManager.EXTRA_STDIN,
                    PluginBundleManager.EXTRA_SESSION_ACTION,
                    PluginBundleManager.EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL
            });
        }

        resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, resultBundle);
        resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, blurb);

        // Configuration information for Tasker variables returned from the executed task
        if(waitForResult) {
            List<String> relevantVariableList = new ArrayList<>();
            relevantVariableList.add(PluginUtils.PLUGIN_VARIABLE_STDOUT + "\nStandard Output\nThe <B>stdout</B> of the command.");
            relevantVariableList.add(PluginUtils.PLUGIN_VARIABLE_STDOUT_ORIGINAL_LENGTH + "\nStandard Output Original Length\nThe original length of <B>stdout</B>.");

            // For foreground commands, the session transcript is returned which will contain
            // both stdout and stderr combined, basically anything sent to the the pseudo
            // terminal /dev/pts, including PS1 prefixes for interactive sessions.
            if (!inTerminal) {
                relevantVariableList.add(PluginUtils.PLUGIN_VARIABLE_STDERR + "\nStandard Error\nThe <B>stderr</B> of the command.");
                relevantVariableList.add(PluginUtils.PLUGIN_VARIABLE_STDERR_ORIGINAL_LENGTH + "\nStandard Error Original Length\nThe original length of <B>stderr</B>.");
            }

            relevantVariableList.add(PluginUtils.PLUGIN_VARIABLE_EXIT_CODE + "\nExit Code\nThe <B>exit code</B> of the command." +
                    "0 often means success and anything else is usually a failure of some sort.");

            if (TaskerPlugin.hostSupportsRelevantVariables(getIntent().getExtras())) {
                TaskerPlugin.addRelevantVariableList(resultIntent, relevantVariableList.toArray(new String[0]));
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
        super.finish();
    }





    private void setStartTextIOActivityForResult() {
        mStartTextIOActivityForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent intent = result.getData();
                if (intent == null) return;

                Bundle bundle = intent.getExtras();
                if (bundle == null) return;

                TextIOInfo textIOInfo = (TextIOInfo) bundle.getSerializable(TextIOActivity.EXTRA_TEXT_IO_INFO_OBJECT);
                if (textIOInfo == null) return;

                switch (textIOInfo.getAction()) {
                    case ACTION_GET_STDIN:
                        mStdin = textIOInfo.getText();
                        updateStdinViewText();
                }
            }
        });
    }

    static class AfterTextChangedWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void afterTextChanged(Editable editable) {
        }
    }

}
