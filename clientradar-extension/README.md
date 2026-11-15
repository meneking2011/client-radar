# ClientRadar Discovery Extension

- Start manually on an open Facebook search-results tab.
- Reloads the selected search URL at a configurable interval (1–30 minutes).
- Reads visible post text and permalink, deduplicates URLs, and submits new posts to ClientRadar for AI qualification and draft generation.
- Runs only while online, for up to 3 active hours followed by a 90-minute break.
- Stops at the authorisation expiry configured locally in extension Settings, or when you press Stop.
- No likes, comments, messages, follows, or automatic outreach.

## Setup
1. Load this folder as an unpacked Manifest V3 extension in a compatible Chromium browser.
2. In Settings, enter the paired companion token and the authorisation expiry. The expiry is stored only in local extension storage.
3. Choose a refresh interval.
4. Log into the authorised Facebook account and open the desired search results. Apply Recent manually if available.
5. Press Start discovery.

## Limitations
The extension relies on visible `[role="article"]` containers and permalink anchors. Facebook DOM changes may cause missed or incomplete extraction. Kiwi compatibility, live Facebook extraction, backend CORS/authentication, and Android end-to-end notifications still require device testing. Keep the browser open and online. This branch is not merged or deployed.