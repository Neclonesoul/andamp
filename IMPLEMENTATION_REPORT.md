# Implementation report

## Implemented
- Monorepo with web, shared core, Android, docs and CI.
- Versioned player protocol.
- Deterministic queue helpers, normalized search, sorting and EQ presets.
- Browser-local audio import/playback, queue, repeat/shuffle controls, functional Web Audio EQ and analyser-driven spectrum.
- PWA manifest/service worker/offline shell.
- Android MediaStore repository.
- Media3 ExoPlayer + MediaLibraryService + MediaSession foundation.
- Native WebView bridge with strict protocol version and fixed commands.
- Android Auto browse-root foundation.
- Ubuntu CI, Cloudflare deployment and Android artifact workflows.

## Android
Implemented code paths: MediaStore querying, native player service, media session, queue, playback controls, local bridge snapshot emission.
Architecturally prepared: Android Auto Albums/Artists/Playlists/Favorites/Recent population, robust native EQ/DSP, richer persistence.
Real-device validation required: screen-off resilience, Bluetooth/headset controls, notification/lock-screen behavior, audio-focus edge cases, OEM process behavior.

## Web
Implemented local file import, playback, seek, queue rendering, shuffle/repeat state, EQ, spectrum visualizer, themes, PWA shell and privacy-first local behavior.

## Tests
Static repository verification was run while producing the archive. TypeScript/Android dependency builds require networked Ubuntu CI and are configured there.

## Build
This generation environment did not install external npm/Gradle dependencies. GitHub Actions is the authoritative build environment.

## Cloudflare
Required GitHub secrets:
- CLOUDFLARE_API_TOKEN
- CLOUDFLARE_ACCOUNT_ID

## Android release
Still required before Play:
- choose permanent package ID;
- release signing;
- signed AAB workflow;
- real-device QA;
- Play listing/data safety/testing tracks.
