package com.termux.tasker;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.twofortyfouram.locale.BreadCrumber;

/**
 * Superclass for plug-in Activities. This class takes care of initializing aspects of the plug-in's UI to
 * look more integrated with the plug-in host.
 */
public abstract class AbstractPluginActivity extends Activity {

    /**
     * Flag boolean that can only be set to true via the "Don't Save" menu item in
     * {@link #onMenuItemSelected(int, MenuItem)}.
     */
    /*
     * There is no need to save/restore this field's state.
     */
    protected boolean mIsCancelled = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CharSequence callingApplicationLabel = null;
        try {
            callingApplicationLabel =
                    getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(getCallingPackage(),
                            0));
        } catch (final NameNotFoundException e) {
            Log.e(Constants.LOG_TAG, "Calling package couldn't be found", e); //$NON-NLS-1$
        }
        if (null != callingApplicationLabel) {
            setTitle(callingApplicationLabel);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        // FIXME: getMenuInflater().inflate(R.menu.twofortyfouram_locale_help_save_dontsave, menu);
        setupActionBar();
        return true;
    }

    private void setupActionBar() {
        final CharSequence subtitle = BreadCrumber.generateBreadcrumb(getApplicationContext(), getIntent(),
                getString(R.string.app_name));

        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(subtitle);
        actionBar.setDisplayHomeAsUpEnabled(true);

        /*
         * Note: There is a small TOCTOU error here, in that the host could be uninstalled right after
         * launching the plug-in. That would cause getApplicationIcon() to return the default application
         * icon. It won't fail, but it will return an incorrect icon.
         *
         * In practice, the chances that the host will be uninstalled while the plug-in UI is running are very
         * slim.
         */
        try {
            actionBar.setIcon(getPackageManager().getApplicationIcon(getCallingPackage()));
        } catch (final NameNotFoundException e) {
            Log.w(Constants.LOG_TAG, "An error occurred loading the host's icon", e);
        }
    }

    @Override
    public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
        final int id = item.getItemId();

        if (android.R.id.home == id) {
            finish();
            return true;
        } else if (/*R.id.twofortyfouram_locale_menu_dontsave FIXME*/ -44445 == id) {
            mIsCancelled = true;
            finish();
            return true;
        } else if (/*R.id.twofortyfouram_locale_menu_save FIXME*/ -44445 == id) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * During {@link #finish()}, subclasses can call this method to determine whether the Activity was
     * canceled.
     *
     * @return True if the Activity was canceled. False if the Activity was not canceled.
     */
    protected boolean isCanceled() {
        return mIsCancelled;
    }

}
