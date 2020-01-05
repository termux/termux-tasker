package com.termux.tasker;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;

import java.io.File;

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

    public static final File TASKER_DIR = new File("/data/data/com.termux/files/home/.termux/tasker/");

    private AutoCompleteTextView mExecutableText;
    private EditText mArgumentsText;
    private CheckBox mInTerminalCheckbox;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.app_name);
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (!(TASKER_DIR.exists() && TASKER_DIR.isDirectory() && TASKER_DIR.listFiles().length > 0)) {
            mIsCancelled = true;
            new AlertDialog.Builder(this)
                    .setTitle(R.string.no_tasker_folder_title)
                    .setMessage(R.string.no_tasker_folder_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .setOnDismissListener(dialogInterface -> finish())
                    .show();
            return;
        }

        setContentView(R.layout.edit_activity);


        final Intent intent = getIntent();
        BundleScrubber.scrub(intent);
        final Bundle localeBundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
        BundleScrubber.scrub(localeBundle);

        mExecutableText = findViewById(R.id.executable_path);
        mArgumentsText = findViewById(R.id.arguments);
        mInTerminalCheckbox = findViewById(R.id.in_terminal);

        final File[] files = TASKER_DIR.listFiles();
        final String[] fileNames = new String[files.length];
        for (int i = 0; i < files.length; i++) fileNames[i] = files[i].getName();

        mExecutableText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                final String currentValue = editable.toString();
                for (String s : fileNames) {
                    if (s.equals(currentValue)) {
                        mExecutableText.setError(null);
                        return;
                    }
                }
                mExecutableText.setError("No such file");
            }
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, fileNames);
        mExecutableText.setAdapter(adapter);

        if (savedInstanceState == null) {
            if (PluginBundleManager.isBundleValid(localeBundle)) {
                final String selectedExecutable = localeBundle.getString(PluginBundleManager.EXTRA_EXECUTABLE);
                mExecutableText.setText(selectedExecutable);
                final String selectedArguments = localeBundle.getString(PluginBundleManager.EXTRA_ARGUMENTS);
                mArgumentsText.setText(selectedArguments);
                final boolean inTerminal = localeBundle.getBoolean(PluginBundleManager.EXTRA_TERMINAL);
                mInTerminalCheckbox.setChecked(inTerminal);
            }
        }
    }

    @Override
    public void finish() {
        if (!isCanceled()) {
            final String executable = mExecutableText.getText().toString();
            final String arguments =  mArgumentsText.getText().toString();
            final boolean inTerminal = mInTerminalCheckbox.isChecked();

            if (executable.length() > 0) {
                final Intent resultIntent = new Intent();

                /*
                 * This extra is the data to ourselves: either for the Activity or the BroadcastReceiver. Note
                 * that anything placed in this Bundle must be available to Locale's class loader. So storing
                 * String, int, and other standard objects will work just fine. Parcelable objects are not
                 * acceptable, unless they also implement Serializable. Serializable objects must be standard
                 * Android platform objects (A Serializable class private to this plug-in's APK cannot be
                 * stored in the Bundle, as Locale's classloader will not recognize it).
                 */
                final Bundle resultBundle = PluginBundleManager.generateBundle(getApplicationContext(), executable, arguments, inTerminal);

                // The blurb is a concise status text to be displayed in the host's UI.
                final String blurb = generateBlurb(executable, arguments, inTerminal);
                if (TaskerPlugin.Setting.hostSupportsOnFireVariableReplacement(this)){
                    TaskerPlugin.Setting.setVariableReplaceKeys(resultBundle,new String[] {
                            PluginBundleManager.EXTRA_EXECUTABLE,
                            PluginBundleManager.EXTRA_ARGUMENTS
                    });
                }

                resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, resultBundle);
                resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, blurb);

                // Configuration information for Tasker variables returned from the executed task
                //
                // Not run if we are opening a terminal because the user might not care about this
                // if they are running something that will literally pop up in front of them (Plus
                // getting that information requires additional work for now)
                if(!inTerminal) {
                    if (TaskerPlugin.hostSupportsRelevantVariables(getIntent().getExtras())) {
                        TaskerPlugin.addRelevantVariableList(resultIntent, new String[]{
                                "%stdout\nStandard Output\nThe <B>output</B> of running the command",
                                "%stderr\nStandard Error\nThe <B>error output</B> of running the command",
                                "%result\nExit Code\nThe exit code set by the command upon completion.\n" +
                                        "0 often means success and anything else is usually a failure of some sort"
                        });
                    }

                    // To use variables, we can't have a timeout of 0, but if someone doesn't pay
                    // attention to this and runs a task that never ends, 10 seconds seems like a
                    // reasonable timeout. If they need more time, or want this to run entirely
                    // asynchronously, that can be set
                    if (TaskerPlugin.Setting.hostSupportsSynchronousExecution(getIntent().getExtras())) {
                        TaskerPlugin.Setting.requestTimeoutMS(resultIntent, 10000);
                    }
                }

                setResult(RESULT_OK, resultIntent);
            }
        }

        super.finish();
    }

    /**
     * @param executable The toast message to be displayed by the plug-in. Cannot be null.
     * @return A blurb for the plug-in.
     */
    String generateBlurb(final String executable, final String arguments, boolean inTerminal) {
        final int stringResource = inTerminal ? R.string.blurb_in_terminal : R.string.blurb_in_background;
        final String message =getString(stringResource, executable,arguments);
        final int maxBlurbLength = 60; // R.integer.twofortyfouram_locale_maximum_blurb_length.
        return (message.length() > maxBlurbLength) ? message.substring(0, maxBlurbLength) : message;
    }
}
