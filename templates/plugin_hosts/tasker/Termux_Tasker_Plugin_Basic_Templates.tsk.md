# Termux Tasker Plugin Basic Templates

&nbsp;
## Export Info:
**Tasker Version:** `5.11.7.beta`  
**Timestamp:** `2020-12-06 10.25.14`  
&nbsp;



&nbsp;
## Profile Names:
**Count:** `0`




## Scene Names:
**Count:** `0`




## Task Names:
**Count:** `1`

- `Termux Tasker Plugin Basic Templates`
##
&nbsp;



&nbsp;
## Profiles Info:
&nbsp;



&nbsp;
## Tasks Info:
&nbsp;

### Task 1
**Name:** `Termux Tasker Plugin Basic Templates`  
**ID:** `978`  
**Collision Handling:** `Abort New Task`  
**Keep Device Awake:** `false`  

#### Help:

A task that provides templates for running basic commands with the Termux:Tasker plugin. This task requires Termux:Tasker version `>= 0.5`. Tasker must be granted `com.termux.permission.RUN_COMMAND` permission. The `termux_tasker_basic_bash_test` and `termux_tasker_basic_python_test` scripts must be installed at `~/.termux/tasker/termux_tasker_basic_bash_test` and `~/.termux/tasker/termux_tasker_basic_python_test` respectively. The Template 4 will also require the `allow-external-apps` property to be set to `true` in `~/.termux/termux.properties` file since the `$PREFIX/bin/bash` absolute path is outside the `~/.termux/tasker/` directory, otherwise its plugin action will fail. For android `>= 10`, Termux must also be granted `Draw Over Apps` permissions so that foreground commands automatically start executing without the user having to manually click the `Termux` notification in the status bar dropdown notifications list for the commands to start.

Check [Termux:Tasker Github](https://github.com/termux/termux-tasker) for more details on plugin configuration and variables and how to handle them.


Template 1 runs the `~/.termux/tasker/termux_tasker_basic_bash_test` `bash` script in the background and sends it 2 simple constant string args and gets the result back in `%stdout`. The args sent do not contain any quotes or special characters and can simply be sent surrounded with double quotes.


Template 2 runs the `~/.termux/tasker/termux_tasker_basic_bash_test` `bash` script in the background and sends it 2 complex dynamic args that may contain quotes or special characters, basically a literal string. The result is received back in the `%stdout` variable. The `termux_tasker_basic_bash_test` executable is passed to the plugin using the `%executable` variable. The `%argument_1` and `%argument_2` variables are used to store the dynamic values to be sent as `$1` and `$2` respectively to `termux_tasker_basic_bash_test` and are sent surrounded with single quotes instead of double quotes. They are first set in the `%arguments` variable surrounded with single quotes and separated by a whitespace which is passed to the plugin. The `%argument_1` and `%argument_2` variables may contain any type of characters, even a single quote, but single quotes must be escaped before the plugin action is run. To escape the single quotes, `Variable Search Replace` action is run to replace all single quotes `'` with one single quote, followed by one backslash, followed by two single quotes `'\''`. So `%arguement_1` surrounded with single quotes that would have been passed like `'some arg with single quote ' in it'` will be passed as `'some arg with single quote '\'' in it'`. This is basically 3 parts `'some arg with single quote '`, `\'` and `' in it'` but when processed, it will be considered as one single argument with the value `some arg with single quote ' in it` that is passed to the executable as `$1`. The `Variable Search Replace` action must be used separately for each argument variable before adding it to the `%arguments` variable.  Do not set multiple arguments in the same variable and use `Variable Search Replace` action on it since that will result in incorrect quoting. This template shows how you can dynamically create plugin commands at runtime using variables and send them to the plugin for execution.


Template 3 is the same as Template 2, but it runs the `~/.termux/tasker/termux_tasker_basic_python_test` `python` script instead in the background. It can be used as a template to run `python` scripts instead of `bash` scripts.


Template 4 runs `$PREFIX/bin/bash` commamd in the foreground to print the path of the current working directory in a foreground terminal session. It does this by passing `echo "I'm in '$(pwd)'"; sleep 5;` to `bash` `-c` option after appropriate escaping. The default value of working directory is set to `~/`. Since commands will be run in a foreground terminal session, the `%stdout`, `%stderr` and `%result` variables will not be returned and only `%err` and `%errmsg` may be returned if the action fails.


The `$PREFIX/` is a shortcut for the termux prefix directory `/data/data/com.termux/files/usr/`. The `~/` is a shortcut for the termux home directory `/data/data/com.termux/files/home/`. These shortcuts can be used in the `Executable` and the `Working Directory` plugin fields. The scripts or binaries in the `~/.termux/tasker/` directory do not require them to prefixed with the full path, just set the name in the `Executable` field, like its done for `termux_tasker_basic_bash_test` script in Template 1 and 2 and `termux_tasker_basic_python_test` in Template 3.


The `%command_failed` variable will be set if the plugin action failed, this is detected by whether `%err` or `%errmsg` is set by the plugin action or if `%result` does not equal `0` for background commands. If you run multiple plugin actions in the same task or are using `Local Variable Passthrough`, then you must clear the `%command_failed` variable and optionally the `%errmsg`, `%stdout`, `%stderr` and `%result` variables with the `Variable Clear` action before running each plugin action, in case they were already set, like by a previously failed plugin action after which the task was not stopped.


To debug arguments being passed or any errors, you can check `logcat` after increasing log level to `Debug`. Check `Debugging` section of `README.md` for more details.
##


**Parameters:** `-`


**Returns:** `-`


**Control:**

```
version_name: 0.1.0
```
##
&nbsp;



&nbsp;
## Code Description:
&nbsp;

``````
Task Name: Termux Tasker Plugin Basic Templates

Actions:
    <A task that provides templates for running basic commands with the Termux:Tasker plugin. This task requires Termux:Tasker version `>= 0.5`. Tasker must be granted `com.termux.permission.RUN_COMMAND` permission. The `termux_tasker_basic_bash_test` and `termux_tasker_basic_python_test` scripts must be installed at `~/.termux/tasker/termux_tasker_basic_bash_test` and `~/.termux/tasker/termux_tasker_basic_python_test` respectively. The Template 4 will also require the `allow-external-apps` property to be set to `true` in `~/.termux/termux.properties` file since the `$PREFIX/bin/bash` absolute path is outside the `~/.termux/tasker/` directory, otherwise its plugin action will fail. For android `>= 10`, Termux must also be granted `Draw Over Apps` permissions so that foreground commands automatically start executing without the user having to manually click the `Termux` notification in the status bar dropdown notifications list for the commands to start.
    
    Check [Termux:Tasker Github](https://github.com/termux/termux-tasker) for more details on plugin configuration and variables and how to handle them.
    
    
    Template 1 runs the `~/.termux/tasker/termux_tasker_basic_bash_test` `bash` script in the background and sends it 2 simple constant string args and gets the result back in `%stdout`. The args sent do not contain any quotes or special characters and can simply be sent surrounded with double quotes.
    
    
    Template 2 runs the `~/.termux/tasker/termux_tasker_basic_bash_test` `bash` script in the background and sends it 2 complex dynamic args that may contain quotes or special characters, basically a literal string. The result is received back in the `%stdout` variable. The `termux_tasker_basic_bash_test` executable is passed to the plugin using the `%executable` variable. The `%argument_1` and `%argument_2` variables are used to store the dynamic values to be sent as `$1` and `$2` respectively to `termux_tasker_basic_bash_test` and are sent surrounded with single quotes instead of double quotes. They are first set in the `%arguments` variable surrounded with single quotes and separated by a whitespace which is passed to the plugin. The `%argument_1` and `%argument_2` variables may contain any type of characters, even a single quote, but single quotes must be escaped before the plugin action is run. To escape the single quotes, `Variable Search Replace` action is run to replace all single quotes `'` with one single quote, followed by one backslash, followed by two single quotes `'\''`. So `%arguement_1` surrounded with single quotes that would have been passed like `'some arg with single quote ' in it'` will be passed as `'some arg with single quote '\'' in it'`. This is basically 3 parts `'some arg with single quote '`, `\'` and `' in it'` but when processed, it will be considered as one single argument with the value `some arg with single quote ' in it` that is passed to the executable as `$1`. The `Variable Search Replace` action must be used separately for each argument variable before adding it to the `%arguments` variable.  Do not set multiple arguments in the same variable and use `Variable Search Replace` action on it since that will result in incorrect quoting. This template shows how you can dynamically create plugin commands at runtime using variables and send them to the plugin for execution.
    
    
    Template 3 is the same as Template 2, but it runs the `~/.termux/tasker/termux_tasker_basic_python_test` `python` script instead in the background. It can be used as a template to run `python` scripts instead of `bash` scripts.
    
    
    Template 4 runs `$PREFIX/bin/bash` commamd in the foreground to print the path of the current working directory in a foreground terminal session. It does this by passing `echo "I'm in '$(pwd)'"; sleep 5;` to `bash` `-c` option after appropriate escaping. The default value of working directory is set to `~/`. Since commands will be run in a foreground terminal session, the `%stdout`, `%stderr` and `%result` variables will not be returned and only `%err` and `%errmsg` may be returned if the action fails.
    
    
    The `$PREFIX/` is a shortcut for the termux prefix directory `/data/data/com.termux/files/usr/`. The `~/` is a shortcut for the termux home directory `/data/data/com.termux/files/home/`. These shortcuts can be used in the `Executable` and the `Working Directory` plugin fields. The scripts or binaries in the `~/.termux/tasker/` directory do not require them to prefixed with the full path, just set the name in the `Executable` field, like its done for `termux_tasker_basic_bash_test` script in Template 1 and 2 and `termux_tasker_basic_python_test` in Template 3.
    
    
    The `%command_failed` variable will be set if the plugin action failed, this is detected by whether `%err` or `%errmsg` is set by the plugin action or if `%result` does not equal `0` for background commands. If you run multiple plugin actions in the same task or are using `Local Variable Passthrough`, then you must clear the `%command_failed` variable and optionally the `%errmsg`, `%stdout`, `%stderr` and `%result` variables with the `Variable Clear` action before running each plugin action, in case they were already set, like by a previously failed plugin action after which the task was not stopped.
    
    
    To debug arguments being passed or any errors, you can check `logcat` after increasing log level to `Debug`. Check `Debugging` section of `README.md` for more details.
    ##
    
    
    **Parameters:** `-`
    
    
    **Returns:** `-`
    
    
    **Control:**
    
    ```
    version_name: 0.1.0
    ```>
    A1: Anchor 

    A2: Variable Set [ 
        Name:%task_name 
        To:Termux Tasker Plugin Basic Templates 
        Recurse Variables:Off 
        Do Maths:Off 
        Append:Off 
        Max Rounding Digits:3 ] 

    <Template 1 Start>
    A3: Anchor 

    <Goto "Template 2 Start"
    Enable this action to skip running this template>
    A4: [X] Goto [ 
        Type:Action Label 
        Number:1 
        Label:Template 2 Start ] 

    <Run `termux_tasker_basic_bash_test "hello," "termux!"` Command In Background>
    A5: Anchor 

    A6: Variable Clear [ 
        Name:%command_failed/%errmsg/%stdout/%stderr/%result 
        Pattern Matching:On 
        Local Variables Only:On 
        Clear All Variables:Off ] 

    <Run Termux:Tasker Plugin Command>
    A7: Termux [ Configuration:termux_tasker_basic_bash_test "hello," "termux!" Timeout (Seconds):10 Continue Task After Error:On ] 

    A8: Variable Set [ 
        Name:%command_failed 
        To:err = `%err`
    
    errmsg =
    ```
    %errmsg
    ```
    
    exit_code = `%result`
    
    stdout =
    ```
    %stdout
    ```
    
    stderr =
    ```
    %stderr
    ``` 
        Recurse Variables:Off 
        Do Maths:Off 
        Append:Off 
        Max Rounding Digits:3 ] If [ %err Set | %errmsg Set | %result neq 0 ]

    A9: If [ %command_failed Set ]

        <remove %err and %errmsg if not set>
        A10: Variable Search Replace [ 
            Variable:%command_failed 
            Search:^err = `\%err`[\n]+errmsg =[\n]```[\n]\%errmsg[\n]```[\n]+ 
            Ignore Case:Off 
            Multi-Line:Off 
            One Match Only:Off 
            Store Matches In Array: 
            Replace Matches:On 
            Replace With: Continue Task After Error:On ] If [ %command_failed Set ]

        A11: Text Dialog [  
            Title:Template 1 Command
        Failed 
            Text:%command_failed 
            Button 1:OK 
            Button 2: 
            Button 3: 
            Close After (Seconds):30 
            Use HTML:Off ] 

        A12: Stop [ 
            With Error:Off 
            Task: ] 

    A13: Else 

        A14: Text Dialog [  
            Title:Template 1 Command Result 
            Text:stdout =
        ```
        %stdout
        ```
        
        stderr =
        ```
        %stderr
        ``` 
            Button 1:OK 
            Button 2: 
            Button 3: 
            Close After (Seconds):30 
            Use HTML:Off ] 

    A15: End If 

    <Template 1 End>
    A16: Anchor 

    <Template 2 Start>
    A17: Anchor 

    <Goto "Template 3 Start"
    Enable this action to skip running this template>
    A18: [X] Goto [ 
        Type:Action Label 
        Number:1 
        Label:Template 3 Start ] 

    <Run `termux_tasker_basic_bash_test '%argument_1' '%argument_2'` Command In Background>
    A19: Anchor 

    A20: Variable Set [ 
        Name:%argument_1 
        To:json 
        Recurse Variables:Off 
        Do Maths:Off 
        Append:Off 
        Max Rounding Digits:3 ] 

    A21: Variable Set [ 
        Name:%argument_2 
        To:{
        "name":"I'm Termux",
        "license":"GPLv3",
        "addons": {
            "1":"Termux:API",
            "2":"Termux:Boot",
            "3":"Termux:Float",
            "4":"Termux:Styling",
            "5":"Termux:Tasker",
            "6":"Termux:Widget"
        }
    } 
        Recurse Variables:Off 
        Do Maths:Off 
        Append:Off 
        Max Rounding Digits:3 ] 

    <replace all single quotes (') with ('\'')>
    A22: Variable Search Replace [ 
        Variable:%argument_1 
        Search:' 
        Ignore Case:Off 
        Multi-Line:On 
        One Match Only:Off 
        Store Matches In Array: 
        Replace Matches:On 
        Replace With:'\\'' ] If [ %argument_1 Set ]

    <replace all single quotes (') with ('\'')>
    A23: Variable Search Replace [ 
        Variable:%argument_2 
        Search:' 
        Ignore Case:Off 
        Multi-Line:On 
        One Match Only:Off 
        Store Matches In Array: 
        Replace Matches:On 
        Replace With:'\\'' ] If [ %argument_2 Set ]

    <Set `termux_tasker_basic_bash_test` to %executable>
    A24: Variable Set [ 
        Name:%executable 
        To:termux_tasker_basic_bash_test 
        Recurse Variables:Off 
        Do Maths:Off 
        Append:Off 
        Max Rounding Digits:3 ] 

    <Set `'%argument_1' '%argument_2'` to %arguments>
    A25: Variable Set [ 
        Name:%arguments 
        To:'%argument_1' '%argument_2' 
        Recurse Variables:Off 
        Do Maths:Off 
        Append:Off 
        Max Rounding Digits:3 ] 

    A26: Variable Clear [ 
        Name:%command_failed/%errmsg/%stdout/%stderr/%result 
        Pattern Matching:On 
        Local Variables Only:On 
        Clear All Variables:Off ] 

    <Run Termux:Tasker Plugin Command>
    A27: Termux [ Configuration:%executable %arguments Timeout (Seconds):10 Continue Task After Error:On ] 

    A28: Variable Set [ 
        Name:%command_failed 
        To:err = `%err`
    
    errmsg =
    ```
    %errmsg
    ```
    
    exit_code = `%result`
    
    stdout =
    ```
    %stdout
    ```
    
    stderr =
    ```
    %stderr
    ``` 
        Recurse Variables:Off 
        Do Maths:Off 
        Append:Off 
        Max Rounding Digits:3 ] If [ %err Set | %errmsg Set | %result neq 0 ]

    A29: If [ %command_failed Set ]

        <remove %err and %errmsg if not set>
        A30: Variable Search Replace [ 
            Variable:%command_failed 
            Search:^err = `\%err`[\n]+errmsg =[\n]```[\n]\%errmsg[\n]```[\n]+ 
            Ignore Case:Off 
            Multi-Line:Off 
            One Match Only:Off 
            Store Matches In Array: 
            Replace Matches:On 
            Replace With: Continue Task After Error:On ] If [ %command_failed Set ]

        A31: Text Dialog [  
            Title:Template 2 Command
        Failed 
            Text:%command_failed 
            Button 1:OK 
            Button 2: 
            Button 3: 
            Close After (Seconds):30 
            Use HTML:Off ] 

        A32: Stop [ 
            With Error:Off 
            Task: ] 

    A33: Else 

        A34: Text Dialog [  
            Title:Template 2 Command Result 
            Text:stdout =
        ```
        %stdout
        ```
        
        stderr =
        ```
        %stderr
        ``` 
            Button 1:OK 
            Button 2: 
            Button 3: 
            Close After (Seconds):30 
            Use HTML:Off ] 

    A35: End If 

    <Template 2 End>
    A36: Anchor 

    <Template 3 Start>
    A37: Anchor 

    <Goto "Template 4 Start"
    Enable this action to skip running this template>
    A38: [X] Goto [ 
        Type:Action Label 
        Number:1 
        Label:Template 4 Start ] 

    <Run `termux_tasker_basic_python_test '%argument_1' '%argument_2'` Command In Background>
    A39: Anchor 

    A40: Variable Set [ 
        Name:%argument_1 
        To:json 
        Recurse Variables:Off 
        Do Maths:Off 
        Append:Off 
        Max Rounding Digits:3 ] 

    A41: Variable Set [ 
        Name:%argument_2 
        To:{
        "name":"I'm Termux",
        "license":"GPLv3",
        "addons": {
            "1":"Termux:API",
            "2":"Termux:Boot",
            "3":"Termux:Float",
            "4":"Termux:Styling",
            "5":"Termux:Tasker",
            "6":"Termux:Widget"
        }
    } 
        Recurse Variables:Off 
        Do Maths:Off 
        Append:Off 
        Max Rounding Digits:3 ] 

    <replace all single quotes (') with ('\'')>
    A42: Variable Search Replace [ 
        Variable:%argument_1 
        Search:' 
        Ignore Case:Off 
        Multi-Line:On 
        One Match Only:Off 
        Store Matches In Array: 
        Replace Matches:On 
        Replace With:'\\'' ] If [ %argument_1 Set ]

    <replace all single quotes (') with ('\'')>
    A43: Variable Search Replace [ 
        Variable:%argument_2 
        Search:' 
        Ignore Case:Off 
        Multi-Line:On 
        One Match Only:Off 
        Store Matches In Array: 
        Replace Matches:On 
        Replace With:'\\'' ] If [ %argument_2 Set ]

    <Set `termux_tasker_basic_python_test` to %executable>
    A44: Variable Set [ 
        Name:%executable 
        To:termux_tasker_basic_python_test 
        Recurse Variables:Off 
        Do Maths:Off 
        Append:Off 
        Max Rounding Digits:3 ] 

    <Set `'%argument_1' '%argument_2'` to %arguments>
    A45: Variable Set [ 
        Name:%arguments 
        To:'%argument_1' '%argument_2' 
        Recurse Variables:Off 
        Do Maths:Off 
        Append:Off 
        Max Rounding Digits:3 ] 

    A46: Variable Clear [ 
        Name:%command_failed/%errmsg/%stdout/%stderr/%result 
        Pattern Matching:On 
        Local Variables Only:On 
        Clear All Variables:Off ] 

    <Run Termux:Tasker Plugin Command>
    A47: Termux [ Configuration:%executable %arguments Timeout (Seconds):10 Continue Task After Error:On ] 

    A48: Variable Set [ 
        Name:%command_failed 
        To:err = `%err`
    
    errmsg =
    ```
    %errmsg
    ```
    
    exit_code = `%result`
    
    stdout =
    ```
    %stdout
    ```
    
    stderr =
    ```
    %stderr
    ``` 
        Recurse Variables:Off 
        Do Maths:Off 
        Append:Off 
        Max Rounding Digits:3 ] If [ %err Set | %errmsg Set | %result neq 0 ]

    A49: If [ %command_failed Set ]

        <remove %err and %errmsg if not set>
        A50: Variable Search Replace [ 
            Variable:%command_failed 
            Search:^err = `\%err`[\n]+errmsg =[\n]```[\n]\%errmsg[\n]```[\n]+ 
            Ignore Case:Off 
            Multi-Line:Off 
            One Match Only:Off 
            Store Matches In Array: 
            Replace Matches:On 
            Replace With: Continue Task After Error:On ] If [ %command_failed Set ]

        A51: Text Dialog [  
            Title:Template 3 Command
        Failed 
            Text:%command_failed 
            Button 1:OK 
            Button 2: 
            Button 3: 
            Close After (Seconds):30 
            Use HTML:Off ] 

        A52: Stop [ 
            With Error:Off 
            Task: ] 

    A53: Else 

        A54: Text Dialog [  
            Title:Template 3 Command Result 
            Text:stdout =
        ```
        %stdout
        ```
        
        stderr =
        ```
        %stderr
        ``` 
            Button 1:OK 
            Button 2: 
            Button 3: 
            Close After (Seconds):30 
            Use HTML:Off ] 

    A55: End If 

    <Template 3 End>
    A56: Anchor 

    <Template 4 Start>
    A57: Anchor 

    <Goto "Return"
    Enable this action to skip running this template>
    A58: [X] Goto [ 
        Type:Action Label 
        Number:1 
        Label:Return ] 

    <Run `$PREFIX/bin/bash -c "echo \"I'm in '$(pwd)'\"; sleep 5;"` Command In Foreground>
    A59: Anchor 

    A60: Variable Clear [ 
        Name:%command_failed/%errmsg/%stdout/%stderr/%result 
        Pattern Matching:On 
        Local Variables Only:On 
        Clear All Variables:Off ] 

    <Run Termux:Tasker Plugin Command>
    A61: Termux [ Configuration:$PREFIX/bin/bash -c "echo \"I'm in '$(pwd)'\"; sleep 5;"
        
        ✓  Timeout (Seconds):10 Continue Task After Error:On ] 

    A62: Variable Set [ 
        Name:%command_failed 
        To:err = `%err`
    
    errmsg =
    ```
    %errmsg
    ``` 
        Recurse Variables:Off 
        Do Maths:Off 
        Append:Off 
        Max Rounding Digits:3 ] If [ %err Set | %errmsg Set ]

    A63: If [ %command_failed Set ]

        A64: Text Dialog [  
            Title:Template 4 Command
        Failed 
            Text:%command_failed 
            Button 1:OK 
            Button 2: 
            Button 3: 
            Close After (Seconds):30 
            Use HTML:Off ] 

        A65: Stop [ 
            With Error:Off 
            Task: ] 

    A66: End If 

    <Template 4 End>
    A67: Anchor 

    <Return>
    A68: Anchor 
``````

##
&nbsp;


*This file was automatically generated using [tasker_config_utils v0.5.0](https://github.com/Taskomater/tasker_config_utils).*
