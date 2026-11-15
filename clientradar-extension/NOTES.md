# ClientRadar Capture — implementation notes

## Scope and safety boundary
This is a user-initiated capture extension, not an automatic Facebook monitoring sensor. The extension does not install a MutationObserver, poll the feed, refresh the page, navigate to posts, inspect profiles, or interact with Facebook controls. It reads only text the user selects and submits only after the user presses the extension button.

## API assumption
The relay uses the existing ClientRadar endpoint `POST /api/companion/share` with `{ deviceToken, text, sourceUrl }`, as implemented for the Android share-to-companion flow. The endpoint is expected to perform AI qualification and return `qualified`, `title`, `reason`, and optionally `message`. The separate `/api/facebook-lead` contract from the proposed brief is not used because its authentication and server implementation were not verified.

## Data limitations
- `sourceUrl` is the Facebook search page URL unless a permalink can be identified in the selected text's containing article.
- The extension does not infer the original post timestamp or comment count. The user should check freshness and comments in Facebook before capture; the server's existing AI qualification does not prove either value.
- The keyword is not independently trusted as a qualification signal. The existing server-side AI remains the qualification authority.

## Browser compatibility
Manifest V3 support in Kiwi Browser varies by version. Test installation and extension messaging on the exact Kiwi version/device before relying on it. Facebook may alter its DOM, and permalink extraction is best-effort.

## Credentials
The extension stores the paired device token in `chrome.storage.local`. Do not commit tokens or secrets. The browser extension does not contain a hard-coded shared secret.
