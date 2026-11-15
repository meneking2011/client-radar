# Test plan

1. Load the unpacked extension in a supported Chromium browser.
2. Confirm the content script is injected only on Facebook search URL patterns.
3. On the mock fixture hosted locally, verify the extension is unavailable (expected; the fixture is documentation-only because the manifest is restricted to Facebook).
4. On a permitted test Facebook search page, select a short post snippet and submit it.
5. Verify an empty selection is rejected locally and no network request is made.
6. Verify a non-Facebook active tab cannot be captured.
7. Verify missing token and non-HTTPS server URL produce clear errors.
8. Verify the relay sends only `deviceToken`, selected `text`, and `sourceUrl` to `/api/companion/share`.
9. Verify a transient network failure triggers one retry, not an unbounded loop.
10. Verify qualified and unqualified responses are reflected in the popup and local activity log.
11. Verify no automatic DOM observation, periodic timers, page reloads, Facebook clicks, or navigation exist in the extension source.
12. Test Kiwi installation and popup/content-script messaging on the target Android device; do not infer compatibility from desktop Chrome.
