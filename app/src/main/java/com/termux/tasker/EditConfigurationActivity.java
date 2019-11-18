package com.termux.tasker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.termux.tasker.Global.TASKER_DIR;
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
public class EditConfigurationActivity extends AbstractPluginActivity {

//    public static final File TASKER_DIR = new File("/data/data/com.termux/files/home/.termux/tasker/");

    private AutoCompleteTextView mExecutableText;
    private EditText mArgumentsText;
    private CheckBox mInTerminalCheckbox;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.app_name);
        actionBar.setDisplayHomeAsUpEnabled(true);
        verifyStoragePermissions(this);
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

        var listoffile=(Spinner)findViewById(R.id.listoffile);
        ArrayAdapter<String> fileadapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,getFilesAllName(Tas))
        Button setfolder=(Button) findViewById(R.id.setfolder);
        setfolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ChooserDialog(EditConfigurationActivity.this)
                        .withFilter(true, true)
                        // to handle the result(s)
                        .withChosenListener(new ChooserDialog.Result() {
                            @Override
                            public void onChoosePath(String path, File pathFile) {
                                Global.TASKER_DIRstr=path;
                                Global.TASKER_DIR=new File(path);
                            }
                        })
                        .build()
                        .show();
            }
        });
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
                    TaskerPlugin.Setting.setVariableReplaceKeys(resultBundle,new String[] {PluginBundleManager.EXTRA_EXECUTABLE,
                            PluginBundleManager.EXTRA_ARGUMENTS,PluginBundleManager.EXTRA_TERMINAL});
                }

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
    String generateBlurb(final String executable, final String arguments, boolean inTerminal) {
        final int stringResource = inTerminal ? R.string.blurb_in_terminal : R.string.blurb_in_background;
        final String message =getString(stringResource, executable,arguments);
        final int maxBlurbLength = 60; // R.integer.twofortyfouram_locale_maximum_blurb_length.
        return (message.length() > maxBlurbLength) ? message.substring(0, maxBlurbLength) : message;
    }

    public static void verifyStoragePermissions(Activity activity) {
        try {
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==REQUEST_EXTERNAL_STORAGE)
        {

        }
    }

    public static List<String> getFilesAllName(String path) {
        File file=new File(path);
        File[] files=file.listFiles();
        if (files == null)
        return null;
        ArrayList<String> s = new ArrayList<>();
        for(int i =0;i<files.length;i++){
            s.add(files[i].getAbsolutePath());
        }
        return s;
    }
}
