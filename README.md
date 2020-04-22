# Termux:Tasker

[![Build status](https://github.com/termux/termux-tasker/workflows/Build/badge.svg)](https://github.com/termux/termux-tasker/actions)
[![Join the chat at https://gitter.im/termux/termux](https://badges.gitter.im/termux/termux.svg)](https://gitter.im/termux/termux)

A [Termux](https://termux.com) add-on app allowing Termux programs to be executed
from [Tasker](https://tasker.joaoapps.com/).

## Installation

Termux:Tasker application can be obtained from:

- [Google Play](https://play.google.com/store/apps/details?id=com.termux.tasker)
- [F-Droid](https://f-droid.org/en/packages/com.termux.tasker/)
- [Kali Nethunter Store](https://store.nethunter.com/en/packages/com.termux.tasker/)

Additionally we provide per-commit debug builds for those who want to try
out the latest features or test their pull request. This build can be obtained
from one of the workflow runs listed on [Github Actions](https://github.com/termux/termux-tasker/actions)
page.

Signature keys of all offered builds are different. Before you switch the
installation source, you will have to uninstall the Termux application and
all currently installed plugins.

## How to use

1. Create a new Tasker Action.
2. In the resulting Select Action Category dialog, select Plugin.
3. In the resulting Action Plugin dialog, select Termux:Tasker.
4. Edit the configuration to specify the executable in `~/.termux/tasker/` to
   execute, and if it should be executed in the background (the default) or in a
   new terminal session.

## License

Released under [the GPLv3 license](https://www.gnu.org/licenses/gpl.html).
