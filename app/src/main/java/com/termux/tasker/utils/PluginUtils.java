package com.termux.tasker.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.termux.tasker.Constants;
import com.termux.tasker.R;

public class PluginUtils {

    /**
     * Determines the "versionCode" in the {@code AndroidManifest}.
     *
     * @param context to read the versionCode.
     * @return versionCode of the app.
     */
    public static int getVersionCode(final Context context) {
        if (null == context) {
            throw new IllegalArgumentException("context cannot be null"); //$NON-NLS-1$
        }

        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (final UnsupportedOperationException e) {
            // This exception is thrown by test contexts.
            return 1;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Check if package has {@link com.termux.tasker.Constants#PERMISSION_RUN_COMMAND}.
     *
     * @param context to get error string.
     * @param packageName to check.
     * @return errmsg if package has not been granted
     *         {@link com.termux.tasker.Constants#PERMISSION_RUN_COMMAND}, otherwise null.
     */
    public static String checkIfPackageHasPermissionRunCommand(final Context context, final String packageName) {

        String errmsg = null;

        // Check if packageName has been granted PERMISSION_RUN_COMMAND
        PackageManager packageManager = context.getPackageManager();
        // If permission not granted
        if (packageManager.checkPermission(Constants.PERMISSION_RUN_COMMAND, packageName) != PackageManager.PERMISSION_GRANTED) {
            ApplicationInfo applicationInfo;
            try {
                applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            } catch (final PackageManager.NameNotFoundException e) {
                applicationInfo = null;
            }
            final String appName = (String) (applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo) : context.getString(R.string.unknown_app));
            errmsg = context.getString(R.string.plugin_permission_ungranted_warning, appName, packageName, Constants.PERMISSION_RUN_COMMAND);
        }

        return errmsg;
    }

    /**
     * Check if executable is not in {@link Constants#TASKER_PATH} and
     * {@link Constants#ALLOW_EXTERNAL_APPS_PROPERTY} property is not set to "true".
     *
     * @param context to get error string.
     * @param executable path to check.
     * @return errmsg if policy is violated, otherwise null.
     */
    public static String checkIfAllowExternalAppsPolicyIsViolated(final Context context, final String executable) {
        if (executable == null || executable.isEmpty()) return context.getString(R.string.null_or_empty_executable);

        String errmsg = null;

        if (!FileUtils.isPathInTaskerDir(executable) && !TermuxAppUtils.isAllowExternalApps(context)) {
            errmsg = context.getString(R.string.allow_external_apps_ungranted_warning);
        }

        return errmsg;
    }
}
