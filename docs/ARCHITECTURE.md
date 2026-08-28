# Architecture

Android native playback is authoritative. The Svelte UI is a projection of native snapshots. Browser mode swaps in `WebPlayerAdapter`. The bridge is versioned and accepts a fixed command set only.
