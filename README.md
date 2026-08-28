# ANDAMP

**Your music. Your player.**

Andamp is a local-first music player built around one rule: on Android, native playback is authoritative. The Svelte UI sends commands; the native Media3 service owns playback state and emits snapshots back.

## Architecture

```text
Svelte UI
  │ commands / snapshots
  ▼
Versioned bridge
  ▼
MediaLibraryService + MediaSession
  ▼
Media3 ExoPlayer
  ├─ MediaStore library
  ├─ native queue
  ├─ background playback
  ├─ system media controls
  └─ Android Auto browse foundation
```

The same UI runs in a browser through a `WebPlayerAdapter`, which uses browser audio APIs and local files. Cloudflare hosts the preview only; Android local playback has no cloud dependency.

## Repository

- `web/` — Astro + Svelte application shell and PWA preview.
- `android/` — native Android host, Media3 service, MediaStore, bridge.
- `packages/core/` — shared TypeScript domain model, protocol, shuffle/search/sort utilities.
- `docs/` — architecture, testing, deployment, release and Play Store preparation.
- `.github/workflows/` — Ubuntu CI, web deployment, Android debug/release builds.

## Android behavior

Implemented architecture includes Media3 `MediaLibraryService`, `MediaSession`, native queue handling, MediaStore-backed tracks, foreground media playback, WebView-hosted local UI, versioned bridge messages and Android Auto browse roots.

Real-device verification is still required for vendor-specific screen-off behavior, Bluetooth/headset controls, lock-screen presentation and audio-focus edge cases.

## Web preview

The web build supports browser-local audio selection, playback, queue controls, shuffle/repeat, a functional Web Audio equalizer, visualizer, themes, local persistence and an offline application shell.

Browser capability limitations are surfaced rather than hidden.

## Privacy

Andamp does not require an account and does not upload the user's music library, playlists or listening history.

## Development

From Linux or GitHub Actions:

```bash
npm install
npm run typecheck
npm run test
npm run build:web
```

Android:

```bash
cd android
gradle :app:assembleDebug
```

The intended mobile workflow is Termux for editing/Git/GitHub CLI and Ubuntu CI for Node/Android/Cloudflare build tooling.

## Cloudflare

The web preview deploys from GitHub Actions. Configure:

- `CLOUDFLARE_API_TOKEN`
- `CLOUDFLARE_ACCOUNT_ID`

Do not run Wrangler locally on Android/Termux when it requires `workerd`.

## Status

- Web preview version: `0.1.0`
- Android development version: `0.1.0`
- First planned public Play Store release: `1.0.0`

See `docs/RELEASE.md` and `docs/play-store/CHECKLIST.md`.
