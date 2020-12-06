package com.termux.tasker.utils;

import android.content.Context;

import com.termux.tasker.Constants;
import com.termux.tasker.R;

import java.io.File;
import java.util.regex.Pattern;

public class FileUtils {

    /**
     * Replace "$PREFIX/" or "~/" prefix with termux absolute paths.
     *
     * @param path to expand.
     * @return expand path.
     */
    public static String getExpandedTermuxPath(String path) {
        if(path != null && !path.isEmpty()) {
            path = path.replaceAll("^\\$PREFIX$", Constants.PREFIX_PATH);
            path = path.replaceAll("^\\$PREFIX/", Constants.PREFIX_PATH + "/");
            path = path.replaceAll("^~/$", Constants.HOME_PATH);
            path = path.replaceAll("^~/", Constants.HOME_PATH + "/");
        }

        return path;
    }

    /**
     * Replace termux absolute paths with "$PREFIX/" or "~/" prefix.
     *
     * @param path to unexpand.
     * @return unexpand path.
     */
    public static String getUnExpandedTermuxPath(String path) {
        if(path != null && !path.isEmpty()) {
            path = path.replaceAll("^" + Pattern.quote(Constants.PREFIX_PATH) + "/", "\\$PREFIX/");
            path = path.replaceAll("^" + Pattern.quote(Constants.HOME_PATH) + "/", "~/");
        }

        return path;
    }

    /**
     * First calls {@link #getExpandedTermuxPath(String)} on input path,
     * then if its already an absolute path, it is returned as is, otherwise
     * {@link Constants#TASKER_PATH} is prefixed to the path.
     *
     * @param path to convert.
     * @return absolute path.
     */
    public static String getAbsolutePathForExecutable(String path) {
        if (path == null)
            path = "";
        else
            path = getExpandedTermuxPath(path);

        String absolutePath;

        // If path is already an absolute path
        if (path.startsWith("/"))
            absolutePath = path;
        // Otherwise assume executable refers to path in TASKER_PATH
        else
            absolutePath = Constants.TASKER_PATH + "/" + path;

        try {
            absolutePath = new File(absolutePath).getCanonicalPath();
        } catch(Exception e) {
        }

        return absolutePath;
    }

    /**
     * Determines whether path is in {@link Constants#TASKER_PATH}.
     *
     * @return true if path in {@link Constants#TASKER_PATH}, otherwise false.
     */
    public static boolean isPathInTaskerDir(String path) {
        try {
            path = new File(path).getCanonicalPath();
        } catch(Exception e) {
            return false;
        }

        return path.startsWith(Constants.TASKER_PATH + "/");
    }

    /**
     * Determines whether path is in {@link Constants#HOME_PATH}.
     *
     * @return true if path in {@link Constants#HOME_PATH}, otherwise false.
     */
    public static boolean isPathInTermuxHome(String path) {
        try {
            path = new File(path).getCanonicalPath();
        } catch(Exception e) {
            return false;
        }

        return path.startsWith(Constants.HOME_PATH + "/");
    }

    /**
     * Check if path is a readable and executable file.
     *
     * @param context to get error string.
     * @param path to check.
     * @param setMissingPermissions a boolean that decides if read and execute flags are
     *                              automatically set if path is a regular file in
     *                              {@link Constants#TASKER_PATH}.
     * @param ignoreErrorsForTaskerDir a boolean that decides if read and execute errors
     *                                 to be ignored if path is in
     *                                 {@link Constants#TASKER_PATH}.
     * @return errmsg if path is not a regular file, or cannot be read or executed, otherwise
     *         null.
     */
    public static String checkIfExecutableFileIsReadableAndExecutable(final Context context, final String path, final boolean setMissingPermissions, final boolean ignoreErrorsForTaskerDir) {
        if (path == null || path.isEmpty()) return context.getString(R.string.null_or_empty_executable);

        String errmsg = null;

        File file = new File(path);

        boolean isPathInTaskerDir = isPathInTaskerDir(path);

        // If file exits but not a regular file
        if (file.exists() && !file.isFile()) {
            return context.getString(R.string.non_regular_file_found);
        }

        // If path is in TASKER_PATH
        if (Constants.TASKER_DIR.isDirectory() && isPathInTaskerDir) {
            // If setMissingPermissions is enabled and path is a regular file, set read and execute flags to file
            if (setMissingPermissions && file.isFile()) {
                if (!file.canRead()) file.setReadable(true);
                if (!file.canExecute()) file.setExecutable(true);
            }
        }

        // If path is not a regular file
        if (!file.isFile()) {
            errmsg = context.getString(R.string.no_regular_file_found);
        }
        // If path is not is TASKER_PATH or if read and execute errors must not be ignored for files in TASKER_PATH
        else if (!isPathInTaskerDir || !ignoreErrorsForTaskerDir) {
            // If file is not readable
            if (!file.canRead()) {
                errmsg = context.getString(R.string.no_readable_file_found);
            }
            // If file is not executable
            // This check will give "avc: granted { execute }" warnings for target sdk 29
            else if (!file.canExecute()) {
                errmsg = context.getString(R.string.no_executable_file_found);
            }
        }

        return errmsg;

    }

    /**
     * Check if path is a readable directory.
     *
     * @param context to get error string.
     * @param path path to check.
     * @param createDirectoryIfMissing a boolean that decides if directory
     *                                 should automatically be created if
     *                                 path is in {@link Constants#HOME_PATH}.
     * @param setMissingPermissions a boolean that decides if read flags are
     *                              automatically set if path is a directory in
     *                              {@link Constants#HOME_PATH}.
     * @param ignoreErrorsForTermuxHome a boolean that decides if existence and
     *                                  readable checks are to be ignored if path is
     *                                  in {@link Constants#HOME_PATH}.
     * @return errmsg if path is not a directory or cannot be read, otherwise
     *         null.
     */
    public static String checkIfDirectoryIsReadable(final Context context, final String path, final boolean createDirectoryIfMissing, final boolean setMissingPermissions, final boolean ignoreErrorsForTermuxHome) {
        if (path == null || path.isEmpty()) return context.getString(R.string.null_or_empty_directory);

        String errmsg = null;

        File file = new File(path);

        // If file exits but not a directory file
        if (file.exists() && !file.isDirectory()) {
            return context.getString(R.string.non_directory_file_found);
        }

        boolean isPathInTermuxHome = isPathInTermuxHome(path);

        // If path is in HOME_PATH
        if (Constants.HOME_DIR.isDirectory() && isPathInTermuxHome) {
            // If createDirectoryIfMissing is enabled and no file exists at path, create directory
            if (createDirectoryIfMissing && !file.exists()) {
                try {
                    // If failed to create directory
                    if (!file.mkdirs()) {
                        return context.getString(R.string.directory_creation_failed, path);
                    }
                } catch(Exception e) {
                    return context.getString(R.string.directory_creation_failed_with_exception, path, e.getMessage());
                }
            }

            // If setMissingPermissions is enabled and path is a directory, set read flags to directory
            if (setMissingPermissions && file.isDirectory()) {
                if (!file.canRead()) file.setReadable(true);
            }
        }

        // If path is not is HOME_PATH or if existence and read checks must not be ignored for files in HOME_PATH
        if (!isPathInTermuxHome || !ignoreErrorsForTermuxHome) {
            // If path is not a directory
            if (!file.isDirectory()) {
                errmsg = context.getString(R.string.no_directory_found);
            }
            // If directory is not readable
            else if (!file.canRead()) {
                errmsg = context.getString(R.string.no_readable_directory_found);
            }
        }

        return errmsg;

    }
}
