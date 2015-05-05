# Notification Count
## Android Xposed Module for adding a number to notification icons (e.g. Message count)

### Origin of this module
The original author of this Mod is *bbukowski* (https://github.com/bbukowski).
This is a fork of https://github.com/bbukowski/NotifCount/ to keep developing, as the original author seems to have discontinued development (last commit mid 2014).
I will create a pull request for the changes for a possible merge into the original repo, if the original developer would like to continue.
*Info:* The last recent `master` state of the original repo is not functional on Lollipop devices, as it crashes in `InitZygote` due to a not found class.

This fork had the first goal to fix this issue, to make this mod usable for Lollipop users.

### Support for this module
Bug reports, feature requests, questions, etc.:
**XDA Developers** thread:
(coming soon)

Also feel free to open an issue on GitHub if you want.

### Mod information
This module currently has the following abilities:
- Enable the Android-built-in functionality to add a number to notifications which provide Android a number, e.g. Messaging apps (like Telegram)

- Add such a number to selected apps that do not provide it by theirselves by increasing the number every time the notification gets updated.
Apps that should get this 'workaround' (named `hookAutoIncrement...` in code) have to be selected excplicitly.

More features related to these functions may be added in the future.

### Source information
The original developer did not provide any licensing information.
I assume that he is ok with forking his work.
*bbukowski, please contact me if you don't want this to be online anymore.*

For all my changes (everything except branch master), I apply the GNU General Public License v2.
You can find a copy in this repo under `LICENSE`.

```
Branch `master`:
(C) 2014 Benjamin Bukowski
         GitHub user github.com/bbukowski
Content not licensed. General GitHub Terms of Use apply.

Branch `woalk/master` and derivative branches:
(C) 2015 Woalk       woalk.com
         GitHub user github.com/woalk
Licensed under GNU General Public License v2.
```
