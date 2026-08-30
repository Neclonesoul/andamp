export type RepeatMode = 'off' | 'all' | 'one';
export type PlaybackStatus = 'idle' | 'loading' | 'playing' | 'paused' | 'ended' | 'error';

export interface Track {
  id: string;
  uri?: string;
  title: string;
  artist: string;
  album: string;
  albumId?: string;
  artistId?: string;
  durationMs: number;
  trackNumber?: number;
  discNumber?: number;
  year?: number;
  genre?: string;
  mimeType?: string;
  sizeBytes?: number;
  artworkUri?: string;
  dateAdded?: number;
}

export interface QueueItem {
  id: string;
  track: Track;
}

export interface LibrarySnapshot {
  protocolVersion: 1;
  tracks: Track[];
  scannedAt: number;
}

export interface PlaybackSnapshot {
  protocolVersion: 1;
  status: PlaybackStatus;
  currentMediaId: string | null;
  currentTrack: Track | null;
  queue: QueueItem[];
  queueIndex: number;
  positionMs: number;
  bufferedPositionMs: number;
  durationMs: number;
  repeatMode: RepeatMode;
  shuffle: boolean;
  error: string | null;
  availableActions: string[];
}

export interface EqBand { frequencyHz: number; gainDb: number; }
export interface EqState {
  version: 1;
  enabled: boolean;
  preampDb: number;
  bands: EqBand[];
  preset: string;
}

export interface Playlist {
  id: string;
  name: string;
  trackIds: string[];
  createdAt: number;
  updatedAt: number;
}
