# Audio

Android uses Media3 ExoPlayer with media audio attributes. The web preview uses HTMLMediaElement routed through Web Audio for EQ and visualization. Native EQ is capability-gated until a device-safe implementation is proven; the UI must never pretend it is active.
