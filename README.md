# Termux:Task

[![Build status](https://api.cirrus-ci.com/github/termux/termux-tasker.svg?branch=master)](https://cirrus-ci.com/termux/termux-tasker)
[![Join the chat at https://gitter.im/termux/termux](https://badges.gitter.im/termux/termux.svg)](https://gitter.im/termux/termux)

A [Termux](https://termux.com) add-on app allowing Termux programs to be executed
from [Tasker](https://tasker.dinglisch.net/).

- [Termux:Task on Google Play](https://play.google.com/store/apps/details?id=com.termux.tasker)
- [Termux:Task on F-Droid](https://f-droid.org/repository/browse/?fdid=com.termux.tasker)

## License

Released under [the GPLv3 license](https://www.gnu.org/licenses/gpl.html).

## How to use

1. Create a new Tasker Action.
2. In the resulting Select Action Category dialog, select Plugin.
3. In the resulting Termux:Task dialog, select Termux:Task.
4. Edit the configuration to specify the executable in `~/.termux/tasker/` to
   execute, and if it should be executed in the background (the default) or in a
   new terminal session.
