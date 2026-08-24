# EthioGPT — Play Store corrected project

This package contains the Android release wrapper and Cloudflare Worker backend for EthioGPT.

## 1. Backend

Deploy `backend/worker.js` with Wrangler. The Worker expects these secrets/bindings:

- `GEMINI_API_KEY` — Worker Secret
- `HASAB_API_KEY` — Worker Secret
- `AI` — Workers AI binding
- `DB` — optional; only if your backend is configured to use it

From `backend/`, configure your secrets with Wrangler and deploy `worker.js`. Do not commit `.dev.vars` or real keys.

The legal pages are available at `/privacy` and `/terms`. They intentionally refer users to the developer email displayed on Google Play, so there is no fake placeholder email in the published policy.

## 2. Android

Open `android-app/` in Android Studio and sync the project. The project targets Android API 36 and uses HTTPS-only WebView navigation.

Set the production Worker URL in `android-app/app/build.gradle.kts` (`ETHIOGPT_URL`). It currently points to the Worker URL supplied with the original project; verify it before release.

Build a signed release App Bundle (`.aab`) from Android Studio. Do not ship a debug build.

## 3. Important release checks

- Replace the production Worker URL if necessary.
- Verify Privacy Policy and Terms URLs.
- Complete Google Play Data Safety, content rating, target audience, ads, and app-access declarations.
- If your Play developer account requires closed testing, complete the required tester period before requesting production access.
- Keep all provider credentials on Cloudflare; never put them in the Android app.


## Plus menu features

The **+ More Options** menu is fully wired:

- **Image Studio** — generate images through `/api/image`, choose style/ratio, download the result, and save the prompt to the local Library.
- **Library** — search quick-start prompts, save/restore recent chat snapshots, and reopen saved image prompts. Data is stored locally on the device.
- **Projects** — create custom projects, persist them locally, set an active project, and launch plan/ideas/tasks prompts.
- **Voice Settings** — choose voice-input language, select supported Hasab speakers, enable automatic read-aloud, test voice, and stop playback.

These features do not require a database for v1; the Worker continues to process AI requests server-side while the app library/preferences remain local to the user's device.
