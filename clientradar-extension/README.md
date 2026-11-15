# ClientRadar Capture Extension

## What it does
Capture selected text from one Facebook search-result post and submit it to the existing ClientRadar companion-share endpoint for AI qualification and draft generation.

## Install (developer / test)
1. Download this repository branch.
2. In a Chromium browser that supports unpacked Manifest V3 extensions, open the extensions manager.
3. Enable Developer mode and load the `clientradar-extension` folder as an unpacked extension.
4. Open the extension's Settings and enter the device token for an already-paired ClientRadar Companion.
5. Navigate to a Facebook search results page.
6. Select the text of one post, open the extension popup, and press **Capture selected post**.
7. Review the result in ClientRadar and manually decide whether to use the draft.

Kiwi's support for unpacked MV3 extensions depends on its current release; verify on-device.

## Session and Android notes
This build has no background monitoring session because it does not watch or refresh Facebook. Keep Kiwi available while manually capturing. Android battery optimization settings are not required for this user-initiated workflow. The existing companion app remains responsible for receiving its normal ClientRadar alerts.

## Privacy
Only selected text is submitted, and only after the user presses Capture. The extension does not send messages, comments, likes, or other Facebook actions.
