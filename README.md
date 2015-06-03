# Notification Count [![Build Status](https://travis-ci.org/woalk/NotifCount.svg?branch=woalk/master)](https://travis-ci.org/woalk/NotifCount)
## Android Xposed Module for adding a number to notification icons (e.g. Message count)

### Mod information
This module currently has the following abilities:

1 - Enable the Android-built-in functionality to add a number to notifications which provide Android a number, e.g. Messaging apps (like Telegram)

2 - Automatically add numbers to apps that do not provide such quantity numbers to the system. The numbers are either fetched from the notification title, summary or by counting the times an app adds content to a notification.
This is called `auto-decide` in code.
It can be manually 'arranged' for specific apps, so that they use a user-specified method (or none) when causing problems.

*_Suggestions:_ Use "Title containing number" for Download Manager (com.android.providers.downloads) and either that or "Don't add number" for e.g. music players like Google Play Music. This solved two larger bugs with this feature.*

3 - Customize the appearance of the number - Change the background shape, color, border color, text color, and size!

More features related to these functions may be added in the future.

### Support for this module
Bug reports, feature requests, questions, etc.:

**XDA Developers** thread:
http://forum.xda-developers.com/xposed/modules/mod-notification-count-t3101832

Also feel free to open an issue on GitHub if you want.

### Origin of this module
The original author of this Mod is *bbukowski* (https://github.com/bbukowski).
This is a fork of https://github.com/bbukowski/NotifCount/ to keep developing, as the original author seems to have discontinued development (last commit mid 2014).
I created a pull request for the changes up to `v1.1.1-re` for a possible merge into the original repo, if the original developer would like to continue.

### Source information
The original developer did not provide any licensing information.
I assume that he is ok with forking his work.

For all my changes (everything except branch master), I apply the GNU General Public License v2.
You can find a copy in this repo under `LICENSE`.

This project uses the library [android-colorpicker](https://github.com/woalk/android-colorpicker).

```
Branch `master`:
(C) 2014 Benjamin Bukowski
         GitHub user github.com/bbukowski
Content not licensed. General GitHub Terms of Use apply.

Branch `woalk/master` and other `woalk/*` branches:
(C) 2015 Woalk       woalk.com
         GitHub user github.com/woalk
Licensed under GNU General Public License v2.

Submodule `libs/android-colorpicker`:
(C) 2013 The Android Open Source Project
    2015 Woalk       woalk.com
Licensed under Apache License v2.0.
```
