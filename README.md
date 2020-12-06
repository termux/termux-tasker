# Termux:Tasker

[![Build status](https://github.com/termux/termux-tasker/workflows/Build/badge.svg)](https://github.com/termux/termux-tasker/actions)
[![Join the chat at https://gitter.im/termux/termux](https://badges.gitter.im/termux/termux.svg)](https://gitter.im/termux/termux)

A [Termux] add-on app allowing `Termux` commands to be executed
from [Tasker] and other plugin apps.
##



### Contents
- [Installation](#Installation)
- [Setup Instructions](#Setup-Instructions)
- [Usage](#Usage)
- [Plugin Configuration](#Plugin-Configuration)
- [Plugin Variables](#Plugin-Variables)
- [Templates](#Templates)
- [Creating And Modifying Scripts](#Creating-And-Modifying-Scripts)
- [Debugging](#Debugging)
- [Worthy Of Note](#Worthy-Of-Note)
- [License](#License)
##



### Installation

`Termux:Tasker` application can be obtained from:

- [Google Play](https://play.google.com/store/apps/details?id=com.termux.tasker)
- [F-Droid](https://f-droid.org/en/packages/com.termux.tasker/)
- [Kali Nethunter Store](https://store.nethunter.com/en/packages/com.termux.tasker/)

Additionally we provide per-commit debug builds for those who want to try
out the latest features or test their pull request. This build can be obtained
from one of the workflow runs listed on [Github Actions](https://github.com/termux/termux-tasker/actions)
page.


The APK files in Google Play, F-Droid, Kali Nethunter Store releases (or custom builds from source code) are signed with different signature keys. The `Termux` app and all its plugins use the same [sharedUserId](https://developer.android.com/guide/topics/manifest/manifest-element) `com.termux` and so all their APKs installed on a device must have been signed with the same signature key to work together and so they must all be installed from the same source. Do not attempt to mix them together, i.e do not try to install an app or plugin from Goggle Play and another from F-Droid. Android Package Manager will also normally not allow installation of APKs with a different signatures and you will get an error on installation but this restriction can be bypassed with root or with custom roms. If you wish to install from a different source, then you must uninstall any and all existing Termux app or its plugin APKs from your device first, then install all new APKs from the same new source again. The `~/.termux/tasker/` directory will not be accessible to the plugin and commands will not execute if `Termux` and `Termux:Tasker` APKs have a different signatures.
##



### Setup Instructions

#### Install Termux app (Mandatory)
The `Termux:Tasker` plugin requires [Termux] app to run the actual commands. You need to install it and start it at least once and have it install the required files for the plugin to start working. The Termux prefix directory `/data/data/com.termux/files/usr/` and Termux home directory `/data/data/com.termux/files/home/` must also exist and must have read, write and execute permissions `(0700)` for the plugin to work. The `$PREFIX/` is shortcut for the Termux prefix directory. The `~/` is a shortcut for the Termux home directory. Permissions and ownerships can be checked with the `stat <path>` command.


#### `com.termux.permission.RUN_COMMAND` permission (Mandatory)

For `Termux:Tasker` version `>= 0.5`, the plugin host app will need to be granted the `com.termux.permission.RUN_COMMAND` permission to run **ANY** plugin commands. This is a security measure to prevent any other apps from running commands in `Termux` context which do not have the required permission granted to them. This is also required for the [RUN_COMMAND Intent] intent.

The [Tasker] app has requested the permission since `v5.9.3`, so you will need to update the app if you are using an older version. You can grant the permission using the `Permissions` activity in the `App Info` activity of your plugin host app. For `Tasker` you can grant it with: `Android Settings` -> `Apps` -> `Tasker` -> `Permissions` -> `Additional permissions` -> `Run commands in Termux environment`.

If you do not grant the permission, you will likely get errors like `receiver com.termux.tasker.FireReceiver requires permission com.termux.permission.RUN_COMMAND which we don't have` when running the `Termux:Tasker` plugin action in `Tasker`. Note that, the `%errmsg` for missing permissions will not be set if you are using `Tasker` version `< 5.11.1.beta` and `Continue Task After Error` is enabled, only `%err` will be set to `1`. You can open the plugin configuration screen to detect missing permissions of the plugin host.


#### `~/.termux/tasker/` Directory (Optional)
The `~/.termux/tasker/` directory stores the scripts that can be run with the plugin without setting `allow-external-apps` property to `true` if you do not want to use absolute paths. Open a non-root termux session and run the below command to create it and give it read, write and executable permissions `(0700)`. The `tasker` directory must have read permission, otherwise the plugin will not be able to read the script files and give errors like `No regular file found at path` for any script name entered in the `Executable` field of plugin configuration. The `tasker` directory of the script must also have executable permissions for the script to be allowed to execute.

```
mkdir -p /data/data/com.termux/files/home/.termux/tasker
chmod 700 -R /data/data/com.termux/files/home/.termux
```


#### `allow-external-apps` property (Optional)

`Termux:Tasker` version `>= 0.5` also allows commands to be run outside the `~/.termux/tasker/` directory by setting absolute paths in the `Executable` field of the plugin configuration **ONLY** if `allow-external-apps` property is set to `true` in `~/.termux/termux.properties` file. This is an added security measure. **DO NOT** set it to true if you have given a relatively untrusted app `com.termux.permission.RUN_COMMAND` permission, since it will be able to run arbitrary commands in termux or even root context (assuming termux has been granted root permissions) in the background without user intervention. The `~/.termux` directory must have read permission, otherwise the plugin will not be able to read the property values. This is also required for the [RUN_COMMAND Intent] intent.

- Automatic
    You can use the one-liner commands below to set the desired value. The commands will append the desired value using `echo` command to the file if the file doesn't exist or the property doesn't exist in the file, otherwise it will `sed` replace any existing values of the key with the desired value.  

    To set `allow-external-apps` property to `true`.  

```
    value="true"; key="allow-external-apps"; file="/data/data/com.termux/files/home/.termux/termux.properties"; mkdir -p "$(dirname "$file")"; chmod 700 "$(dirname "$file")"; if ! grep -E '^'"$key"'=.*' $file &>/dev/null; then [[ -s "$file" && ! -z "$(tail -c 1 "$file")" ]] && newline=$'\n' || newline=""; echo "$newline$key=$value" >> "$file"; else sed -i'' -E 's/^'"$key"'=.*/'"$key=$value"'/' $file; fi
```

    To set `allow-external-apps` property to `false`.  

```
    value="false"; key="allow-external-apps"; file="/data/data/com.termux/files/home/.termux/termux.properties"; mkdir -p "$(dirname "$file")"; chmod 700 "$(dirname "$file")"; if ! grep -E '^'"$key"'=.*' $file &>/dev/null; then [[ -s "$file" && ! -z "$(tail -c 1 "$file")" ]] && newline=$'\n' || newline=""; echo "$newline$key=$value" >> "$file"; else sed -i'' -E 's/^'"$key"'=.*/'"$key=$value"'/' $file; fi
```

- Manual
    You can do it manually by running the below commands to open the `nano` text editor in the terminal. Then add/update a line `allow-external-apps=true` to set the property to `true`, and press `Ctrl+o` and then `Enter` to save and `Ctrl+x` to exit.  

```
    file="/data/data/com.termux/files/home/.termux/termux.properties";
    mkdir -p "$(dirname "$file")";
    nano "$file"
```


#### `Draw Over Apps` permission (Optional)

For android `>= 10` there are new [restrictions](https://developer.android.com/guide/components/activities/background-starts) that prevent activities from starting from the background. This prevents the background `TermuxService` from starting a terminal session in the foreground and running the commands until the user manually clicks `Termux` notification in the status bar dropdown notifications list. This only affects plugin commands that are to be executed in a terminal session and not the background ones. `Termux` version `>= 0.100` requests the `Draw Over Apps` permission so that users can bypass this restriction so that commands can automatically start running without user intervention. You can grant `Termux` the `Draw Over Apps` permission from its `App Info` activity `Android Settings` -> `Apps` -> `Termux` -> `Advanced` -> `Draw over other apps`.
##



### Usage

#### Tasker
1. Create a new `Action` in a `Task`.
2. In the resulting `Select Action Category` dialog, select `Plugin`.
3. In the resulting `Action Plugin` dialog, select `Termux:Tasker`.
4. Click the edit button to edit the configuration.
5. Run the task once the configuration is complete.
##



### Plugin Configuration

The plugin configuration activity can be started by plugin host apps to configure the plugin to define what commands should be run and in which mode. Currently, there are 3 text fields, the `Executable`, `Arguments` and `Working directory path` fields and a `Execute in a terminal session` toggle. The text fields support plugin host app local variables (all lowercase) like `%executable`, `%arguments` or `%workdir`, you may use multiple variables in a single field.

- The `Executable` field defines the executable that needs to be run. It can either be set to a file in `~/.termux/tasker/` directory or to an absolute path if `allow-external-apps` property is set to `true` (check [Setup Instructions](#Setup-Instructions)). Absolute paths can be like `/data/data/com.termux/files/usr/bin/bash`. The `$PREFIX/` and `~/` prefixes are also supported, like `$PREFIX/bin/bash` or `~/some-script`.

    Execute permissions will automatically be set for the executable file if it exists inside the `~/.termux/tasker/` directory when the plugin action is run. It is the user's responsibility to set read and execute permissions for the executable file if it exists outside the `~/.termux/tasker/` directory. That can be done by running the command `chmod 700 "/path/to/executable"` from a terminal session before running the plugin action.


- The `Arguments` field defines the argument that will be passed to the executable. For `Termux:Tasker` version `>= 0.5`, arguments will be processed just like there are if commands are run in a shell like bourne shell. It uses [ArgumentTokenizer](https://sourceforge.net/p/drjava/git_repo/ci/master/tree/drjava/src/edu/rice/cs/util/ArgumentTokenizer.java) to parse the arguments string.

    Arguemnts are split on whitespaces unless quoted with single or double quotes. Double quotes and backslashes can be escaped with backslashes in arguments surrounded with double quotes.

    Any argument surrounded with single quotes is considered a literal string. However, if an argument itself contains single quotes, then they will need to be escaped properly. You can escape them by replacing all single quotes `'` in an argument value with `'\''` **before** passing the argument surrounded with single quotes. So an argument surrounded with single quotes that would have been passed like `'some arg with single quote ' in it'` will be passed as `'some arg with single quote '\'' in it'`. This is basically 3 parts `'some arg with single quote '`, `\'` and `' in it'` but when processed, it will be considered as one single argument with the value `some arg with single quote ' in it` that is passed to the executable.

    For `Tasker`, you can use the `Variable Search Replace` action on an `%argument` variable to escape the single quotes. Set the `Search` field to one single quote `'`, and enable `Replace Matches` toggle, and set `Replace With` field to one single quote, followed by two backslashes, followed by two single quotes `'\\''`. The double backslash is to escape the backslash character itself.


- The `Working directory path` field for `Termux:Tasker` version `>= 0.5` defines the working directory that should be used while running the command. The directory must be readable by the termux app. It is the user's responsibility to create the directory if its outside the `~/` directory. That can be done by running the command `mkdir -p "/path/to/workdir"` from a terminal session before running the plugin action. The `$PREFIX/` and `~/` prefixes are also supported, like `$PREFIX/some-directory` or `~/some-directory`.


- `Execute in a terminal session` toggle defines whether the commands will be run in the background or in a foreground terminal session.

    If the toggle is **enabled**, a new terminal session will open up automatically in the foreground and commands will be run inside it. Result of commands is **not returned** to the plugin host app.

    If the toggle is **not enabled**, then commands are run in the background and result of commands **is returned** to the plugin host app in `%stdout`, `%stderr` and `%result` variables.

    Check [Setup Instructions](#Setup-Instructions) for android `>= 10` restrictions that will prevent the commands from automatically starting unless notification is clicked.

Check [Templates](#Templates) section for templates that can be used for various configurations.
##



### Plugin Variables

Depending on plugin configuration, the following variables may be returned.

- `%stdout` containing `stdout` of commands.
- `%stderr` containing `stderr` of commands.
- `%result` containing `exit code` of commands. The `exit code` `0` often means success and anything else is usually a failure of some sort.
- `%err` containing `exit code` of plugin action. This will be set only if running the action itself failed like missing permissions or invalid configuration. This may be set by the plugin host app or the plugin. This will not be set if plugin action succeeded.
- `%errmsg` containing the error message of why the plugin action failed if `%err` is set.
&nbsp;

If the timeout value of the plugin action is set to `0` or `None` (slider to extreme left in Tasker), then **no variables will be returned**, regardless of whether commands need to be run in a foreground terminal session or in background. Even `%errmsg` will not be set to notify of any errors while running the plugin action since plugin host app will not wait for the plugin to return any variables. This is important for cases like if `allow-external-apps` is not set to `true` but an absolute path outside `~/.termux/tasker/` directory is set as the `Executable`, in which case the plugin action will appear to have succeeded but no commands will execute.

If the timeout value of the plugin action is set to `>0` and `Execute in a terminal session` is not enabled to run commands in background, then the result of commands will be returned in `%stdout`, `%stderr` and `%result` variables. The `%err` and `%errmsg`variables may also be set if the action failed. Note that if the timeout has passed by the time commands finish, the result of command variables will not be set in the plugin host app task and the action will exit with a timeout error, the `%err` variable will be set to `2` and `%errmsg` to `timeout`, at least in `Tasker`.

If the timeout value of the plugin action is set to `>0` and `Execute in a terminal session` is enabled, then `%stdout`, `%stderr` and `%result` variables will **not** be returned. Only the `%err` and `%errmsg`variables may be set if the action failed.

The (new) default timeout is set to `10s` for all configurations. If you are running background commands that will likely take longer to run, then increase the timeout or set it to `Never` (slider to extreme right in Tasker). The plugin host app may still get killed by android if it keeps running for long time regardless of timeout value, check [here](https://tasker.joaoapps.com/userguide/en/faqs/faq-problem.html#00) for more info. Even if the commands are to be run in a foreground terminal session, **do not** set timeout to `0` but use `10s` instead, since with timeout `0`, plugin host app will not wait for any errors to be returned by the plugin in `%err` and `%errmsg` variables and continue the task and the user wouldn't know if any error occurred. Users who already have preexisting actions with the timeout set to `0`, like for foreground terminal session commands (considering previous default was `0`) should update their tasks and use the new default `10s` instead, just opening the configuration screen and returning should automatically do it.
&nbsp;

For `Termux:Tasker` version `>= 0.5`, the `%errmsg`, `%stdout`, `%stderr` and `%result` variables will also be automatically cleared whenever the action is run if timeout is greater than `0` to solve the issue of if multiple actions are run in the same task, then variables from previous action may still be set and get mixed in with current ones. For older versions, you can use a `Variable Clear` action in Tasker with `Pattern Matching` enabled and the `Name` field set to `%errmsg/%stdout/%stderr/%result` to clear all of them before each plugin action if multiple actions are run in a task or `Local Variable Passthrough` is enabled in Tasker.

The `%err` and `%errmsg` variables will mainly only be set for `Termux:Tasker` version `>= 0.5`. These will be set if there are errors like if an executable file is not found, or if `allow-external-apps` property is not set to `true` but an absolute path is specified as the executable. `Tasker` itself may set it too like if `Tasker` has not been granted the `com.termux.permission.RUN_COMMAND` permission when running the plugin action or if a timeout occurs, etc.

The `%err` and `%errmsg` variables must be stored in another variable with the `Variable Set` action right after the plugin action if they have to be checked and used later. The plugin host app like Tasker sets and clears `%err` for each action and it is only available in the next action. To check if and what they are set to, add a `Variable Clear` action for `%command_failed` variable before the plugin action, then add a `Variable Set` action after the plugin action for the `%command_failed` variable with the value `%err %errmsg` and `If` conditions `If %err Set OR If %errmsg Set`. Then you can just check `If %command_failed Set` afterward and flash it to notify the user or exit the task if necessary. Error checking should ideally also be done based on `%result` and optionally the `%stderr` variables before continuing the task.

Check [Templates](#Templates) section for templates on how error and result variables should be handled for various configurations.
##



### Templates

#### Tasker

- `Tasks`
    - `XML`
        Download the [Termux Tasker Plugin Basic Templates Task XML](templates/plugin_hosts/tasker/Termux_Tasker_Plugin_Basic_Templates.tsk.xml) file to the android download directory. To download, right-click or hold the `Raw` button at the top after opening a file link and select `Download/Save link` or use `curl` from a termux shell. Then import the downloaded task file into Tasker by long pressing the `Task` tab button in Tasker home and selecting `Import Task`.  

        `curl -L 'https://github.com/termux/termux-tasker/raw/master/templates/plugin_hosts/tasker/Termux_Tasker_Plugin_Basic_Templates.tsk.xml' -o "/storage/emulated/0/Download/Termux_Tasker_Plugin_Basic_Templates.tsk.xml"`  

    - `Taskernet`
        Import `Termux Tasker Plugin Basic Templates Task` from `Taskernet` from [here](https://taskernet.com/shares/?user=AS35m8mXdvaT1Vj8TwkSaCaoMUv220IIGtHe3pG4MymrCUhpgzrat6njEOnDVVulhAIHLi6BPUt1&id=Task%3ATermux+Tasker+Plugin+Basic+Templates).  

    Check [Termux Tasker Plugin Basic Templates Task Info](templates/plugin_hosts/tasker/Termux_Tasker_Plugin_Basic_Templates.tsk.md) file for more info on the task.  

- `Scripts`
    To use the above task, you will also need to place the [termux_tasker_basic_bash_test](templates/scripts/termux_tasker_basic_bash_test) and [termux_tasker_basic_python_test](templates/scripts/termux_tasker_basic_python_test) scripts in `~/.termux/tasker/` directory after following its [Setup Instructions](#Setup-Instructions). They basically just print the first `2` args to `stdout` if only `2` args are received, otherwise exit with error.  

    1. Download the script files.  

        - Download to `~/.termux/tasker/` directly from github using `curl` using a non-root termux shell.  

            `curl -L 'https://github.com/termux/termux-tasker/raw/master/templates/scripts/termux_tasker_basic_bash_test' -o "/data/data/com.termux/files/home/.termux/tasker/termux_tasker_basic_bash_test"`  

            `curl -L 'https://github.com/termux/termux-tasker/raw/master/templates/scripts/termux_tasker_basic_python_test' -o "/data/data/com.termux/files/home/.termux/tasker/termux_tasker_basic_python_test"`  

        - Download them manually to android download directory and then use `cat` to copy them to `~/.termux/tasker/` or manually do it with a [SAF file browser](#Creating-And-Modifying-Scripts).  

            `cat "/storage/emulated/0/Download/termux_tasker_basic_bash_test" > "/data/data/com.termux/files/home/.termux/tasker/termux_tasker_basic_bash_test"`  

            `cat "/storage/emulated/0/Download/termux_tasker_basic_python_test" > "/data/data/com.termux/files/home/.termux/tasker/termux_tasker_basic_python_test"`  

    2. Set executable permissions.  

        `chmod 700 "/data/data/com.termux/files/home/.termux/tasker/termux_tasker_basic_bash_test"`  

        `chmod 700 "/data/data/com.termux/files/home/.termux/tasker/termux_tasker_basic_python_test"`  


    To modify the scripts you can use `nano`.  

    `nano "/data/data/com.termux/files/home/.termux/tasker/termux_tasker_basic_bash_test"`  
    `nano "/data/data/com.termux/files/home/.termux/tasker/termux_tasker_basic_python_test"`  


Termux needs to be granted `Storage` permission to allow it to access `/storage/emulated/0/Download` directory, otherwise you will get permission denied errors while running commands.
##



### Creating And Modifying Scripts

You can create scripts in `~/.termux/tasker/` directory after following its [Setup Instructions](#Setup-Instructions). Scripts can also be created elsewhere in Termux files directory but will require `allow-external-apps` to be set to `true` for the plugin to access them.

You can use `shell` based text editors like `nano`, `vim` or `emacs` to create and modify scripts.

`nano "/data/data/com.termux/files/home/.termux/tasker/some_script"`

You can also use `GUI` based text editor android apps that support `SAF`. Termux provides a [Storage Access Framework (SAF)](https://wiki.termux.com/wiki/Internal_and_external_storage) file provider to allow other apps to access its `~/` home directory. However, the `$PREFIX/` directory is not accessible to other apps. The [QuickEdit] or [QuickEdit Pro] app does support `SAF` and can handle large files without crashing, however, it is closed source and its pro version without ads is paid. You can also use [Acode editor] or [Turbo Editor] if you want an open source app.

Note that the android default `SAF` `Document` file picker may not support hidden file or directories like `~/.termux` which start with a dot `.`, so if you try to use it to open files for a text editor app, then that directory will not show. You can instead create a symlink for  `~/.termux` at `~/termux_sym` so that it is shown. Use `ln -s "/data/data/com.termux/files/home/.termux" "/data/data/com.termux/files/home/termux_sym"` to create it.
##



### Debugging

You can help debug problems like how arguments are being parsed by the plugin or if the plugin is even firing etc by setting appropriate `logcat` `Log Level` in options menu (3 dots) in the plugin configuration screen. Note that whatever log level is set will affect the entire plugin app and all plugin actions and not just for the action whose configuration you used to set it. The setting only exists inside the configuration activity of actions because creating a separate launcher activity that would be shown in the list of apps in the launcher just for this setting doesn't seem worth it. `Log Level` defaults to`Normal` and log level `Debug` currently logs additional information. Its best to revert log level to `Normal` after you have finished debugging since private data may otherwise be passed to `logcat` during normal operation and moreover, additional logging increases execution time.

For information on how to view the `logcat` logs, check official android guide [here](https://developer.android.com/studio/command-line/logcat).

##### Log Levels
- `Off` - Log nothing
- `Normal` - Start logging error, warn and info messages and stacktraces
- `Debug` - Start logging debug messages
- `Verbose` - Start logging verbose messages
##



### Worthy Of Note

##### Arguments and Result Data Limits

There are limits on the arguments size you can pass to commands or the full command string length that can be run, which is likely equal to `131072` bytes or `128KB` for an android device defined by `ARG_MAX` but after subtracting shell environment size, etc, it will roughly be around `120-125KB` but limits may vary for different android versions and kernels. You can check the limits for a given termux session by running `true | xargs --show-limits`. If you exceed the limit, you will get exceptions like `Argument list too long`. You can manually cross the limit by running something like `$PREFIX/bin/echo "$(head -c 131072 < /dev/zero | tr '\0' 'a')" | tr -d 'a'`, use full path of `echo`, otherwise the `echo` shell built-in will be called to which the limit does not apply since `exec` is not done.

Moreover, exchanging data between `Tasker` and `Termux:Tasker` is done using [Intents](https://developer.android.com/guide/components/activities/parcelables-and-bundles), like sending the command and receiving result of commands in `%stdout` and `%stderr`. However, android has limits on the size of *actual* data that can be sent through intents, it is roughly `500KB` on android `7` but may be different for different android versions.

Basically, make sure any data/arguments you send to `Termux:Tasker` is less than `120KB` (or whatever you found) and any expected result sent back is less than `500KB`, but best keep it as low as possible for greater portability. If you want to exchange an even larger data between tasker and termux, use physical files instead.

The argument data limits also apply for the [RUN_COMMAND Intent] intent.


##### Termux Environment

Termux does not load the environment fully for external plugins or [RUN_COMMAND Intent] commands, like setting `LD_PRELOAD`, so any *external* scripts which do not have shebangs to full path to termux bin directory will not work if called from inside your *plugin* scripts, since `libtermux-exec.so` is not called since `LD_PRELOAD` isn't set and you will get `bad interpreter: No such file or directory` errors. Simply setting `LD_PRELOAD` will not work either without starting a new shell. So make sure to set the shebangs correctly for any *external* scripts you want to run from inside your *plugin* script. The correct shebangs for termux scripts are like `#!/data/data/com.termux/files/usr/bin/bash` for bash scripts instead of `#!/usr/bin/bash` used in common linux distros. You can also use [termux-fix-shebang](https://wiki.termux.com/wiki/Termux-fix-shebang) command on the *external* scripts before running them with the plugin to fix the shebangs automatically.


##### Defining Scripts In Plugin Host App

Currently, any script files that need to be run need to be created in `~/.termux/tasker/` directory. It may get inconvenient to create physical script files for each type of command you want to run. These script files are also neither part of backups of plugin host apps like Tasker and require separate backup methods and nor are part of project configs shared with other people or even between your own devices, and so the scripts need to be added manually to the `~/.termux/tasker/` directory on each device. To solve such issues and to dynamically define scripts of different interpreted languages inside your plugin host app like `Tasker` and to pass them to `Termux` as arguments instead of creating script files, [tudo](https://github.com/agnostic-apollo/tudo) can be used for running commands in termux user context and [sudo](https://github.com/agnostic-apollo/sudo) for running commands with super user (root) context, check their `script` command type. These scripts will also load the termux environment properly like setting `LD_PRELOAD` etc before running the commands.
##



### License

Released under [the GPLv3 license](https://www.gnu.org/licenses/gpl.html).
##


[Termux]: https://termux.com
[Tasker]: https://tasker.joaoapps.com
[QuickEdit]: https://play.google.com/store/apps/details?id=com.rhmsoft.edit
[QuickEdit Pro]: https://play.google.com/store/apps/details?id=com.rhmsoft.edit.pro
[Acode editor]: https://github.com/deadlyjack/code-editor
[Turbo Editor]: https://github.com/vmihalachi/turbo-editor
[RUN_COMMAND Intent]: https://github.com/termux/termux-app/blob/master/app/src/main/java/com/termux/app/RunCommandService.java
