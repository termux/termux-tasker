package com.termux.tasker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

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
public final class TaskerEditActivity extends AbstractPluginActivity {

    public static final File TASKER_DIR = new File("/data/data/com.termux/files/home/.tasker/");

    private Spinner mExecutableSpinner;
    private CheckBox mInTerminalCheckbox;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!(TASKER_DIR.exists() && TASKER_DIR.isDirectory() && TASKER_DIR.listFiles().length > 0)) {
            mIsCancelled = true;
            new AlertDialog.Builder(this)
                    .setTitle("No ~/.tasker directory")
                    .setMessage("You need to create a ~/.tasker directory containing scripts to be executed.")
                    .setPositiveButton(android.R.string.ok, null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            finish();
                        }
                    })
                    .show();
            return;
        }

        setContentView(R.layout.edit_activity);

        final Intent intent = getIntent();
        BundleScrubber.scrub(intent);
        final Bundle localeBundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
        BundleScrubber.scrub(localeBundle);

        mExecutableSpinner = (Spinner) findViewById(R.id.executable_path);
        mInTerminalCheckbox = (CheckBox) findViewById(R.id.in_terminal);

        File[] files = TASKER_DIR.listFiles();
        String[] fileNames = new String[files.length];
        for (int i = 0; i < files.length; i++) fileNames[i] = files[i].getName();

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fileNames);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mExecutableSpinner.setAdapter(spinnerArrayAdapter);

        if (savedInstanceState == null) {
            if (PluginBundleManager.isBundleValid(localeBundle)) {
                final String selectedExecutable = localeBundle.getString(PluginBundleManager.EXTRA_EXECUTABLE);
                for (int i = 0; i < fileNames.length; i++) {
                    if (fileNames[i].equals(selectedExecutable)) {
                        mExecutableSpinner.setSelection(i);
                        break;
                    }
                }
                final boolean inTerminal = localeBundle.getBoolean(PluginBundleManager.EXTRA_TERMINAL);
                mInTerminalCheckbox.setChecked(inTerminal);
            }
        }
    }

    @Override
    public void finish() {
        if (!isCanceled()) {
            final String executable = mExecutableSpinner.getSelectedItem().toString();
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
                final Bundle resultBundle = PluginBundleManager.generateBundle(getApplicationContext(), executable, inTerminal);

                // The blurb is a concise status text to be displayed in the host's UI.
                final String blurb = generateBlurb(executable);

                resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, resultBundle);
                resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, blurb);
                setResult(RESULT_OK, resultIntent);
            }
        }

        super.finish();
    }

    /**
     * @param executable The toast message to be displayed by the plug-in. Cannot be null.
     * @return A blurb for the plug-in.
     */
    static String generateBlurb(final String executable) {
        // context.getResources().getInteger(R.integer.twofortyfouram_locale_maximum_blurb_length);
        String message = "Execute ~/.tasker/" + executable;
        final int maxBlurbLength = 60;
        return (message.length() > maxBlurbLength) ? message.substring(0, maxBlurbLength) : message;
    }

}