package com.termux.tasker;

import android.content.Context;

public final class Constants {

    public static final String LOG_TAG = "termux-tasker";

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
}