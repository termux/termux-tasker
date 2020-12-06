package com.termux.tasker;

import java.io.File;

public final class Constants {

    public static final String TERMUX_PACKAGE = "com.termux";
    public static final String TERMUX_SERVICE = "com.termux.app.TermuxService";

    public static final String FILES_PATH = "/data/data/com.termux/files";
    public static final String PREFIX_PATH = FILES_PATH + "/usr";
    public static final String HOME_PATH = FILES_PATH + "/home";
    public static final String TASKER_PATH = HOME_PATH + "/.termux/tasker";

    public static final File FILES_DIR = new File(FILES_PATH);
    public static final File PREFIX_DIR = new File(PREFIX_PATH);
    public static final File HOME_DIR = new File(HOME_PATH);
    public static final File TASKER_DIR = new File(TASKER_PATH);

    public static final String PERMISSION_RUN_COMMAND = "com.termux.permission.RUN_COMMAND";

    public static final String ALLOW_EXTERNAL_APPS_PROPERTY = "allow-external-apps";
    public static final String ALLOW_EXTERNAL_APPS_PROPERTY_DEFAULT_VALUE = "false";

}
