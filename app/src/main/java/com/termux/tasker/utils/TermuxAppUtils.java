package com.termux.tasker.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.termux.tasker.Constants;
import com.termux.tasker.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class TermuxAppUtils {

    /**
     * Check if Termux app is installed and accessible. This is done by checking if
     * {@link com.termux.tasker.Constants#TERMUX_PACKAGE} is installed and
     * {@link com.termux.tasker.Constants#PREFIX_PATH} is a directory and has read and execute
     * permissions. The {@link com.termux.tasker.Constants#PREFIX_PATH} directory would not exist
     * if termux has not been installed or setup or {@link com.termux.tasker.Constants#PREFIX_PATH}
     * was deleted by the user.
     *
     * @param context to get error string.
     * @return errmsg if termux package is not installed, disabled or
     *         {@link com.termux.tasker.Constants#PREFIX_PATH} is not a directory, or does not have
     *         read or execute permissions, otherwise null.
     */
    public static String checkIfTermuxAppIsInstalledAndAccessible(final Context context) {

        String errmsg = null;

        PackageManager packageManager = context.getPackageManager();

        ApplicationInfo applicationInfo;
        try {
            applicationInfo = packageManager.getApplicationInfo(Constants.TERMUX_PACKAGE, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        boolean termuxAppEnabled = (applicationInfo != null && applicationInfo.enabled);

        // If Termux app is not installed or is disabled
        if (!termuxAppEnabled) {
            errmsg = context.getString(R.string.termux_app_not_installed_or_disabled_warning);
        }
        // If Termux PREFIX_PATH is not a directory or does not have read or execute permissions
        else if (!Constants.PREFIX_DIR.isDirectory() || !Constants.PREFIX_DIR.canRead() || !Constants.PREFIX_DIR.canExecute()) {
            errmsg = context.getString(R.string.termux_app_prefix_path_inaccessible_warning);
        }

        return errmsg;
    }

    /**
     * Get value of termux property in ~/.termux/termux.properties.
     *
     * @param context for logging.
     * @param property name.
     * @param defaultValue if property not found.
     * .
     *
     * @return property value if it exists, otherwise defaultValue.
     */
    public static String getTermuxProperty(final Context context, final String property, final String defaultValue) {
        File propsFile = new File(Constants.HOME_PATH + "/.termux/termux.properties");
        if (!propsFile.exists())
            propsFile = new File(Constants.HOME_PATH + "/.config/termux/termux.properties");

        Properties props = new Properties();
        try {
            if (propsFile.isFile() && propsFile.canRead()) {
                try (FileInputStream in = new FileInputStream(propsFile)) {
                    props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            Logger.logStackTrace(context, "Error loading termux.properties", e);
        }

        return props.getProperty(property, defaultValue);
    }

    /**
     * Determines whether {@link Constants#ALLOW_EXTERNAL_APPS_PROPERTY} property is set to "true" in
     * ~/.termux/termux.properties.
     *
     * @param context for logging.
     * @return true if property exists and value is "true", otherwise false.
     */
    public static boolean isAllowExternalApps(final Context context) {
        return getTermuxProperty(context, Constants.ALLOW_EXTERNAL_APPS_PROPERTY, Constants.ALLOW_EXTERNAL_APPS_PROPERTY_DEFAULT_VALUE).equals("true");
    }
}
