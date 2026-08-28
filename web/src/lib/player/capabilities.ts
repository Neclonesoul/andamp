export const capabilities=()=>({
  nativePlayback: typeof window!=='undefined' && !!window.AndampNative,
  mediaStore: typeof window!=='undefined' && !!window.AndampNative,
  backgroundPlayback: typeof window!=='undefined' && !!window.AndampNative,
  mediaSession: typeof navigator!=='undefined' && 'mediaSession' in navigator,
  nativeEqualizer: false,
  visualizer: typeof window!=='undefined' && 'AudioContext' in window,
  fileSystemAccess: typeof window!=='undefined' && 'showOpenFilePicker' in window,
  androidAuto: typeof window!=='undefined' && !!window.AndampNative
});
