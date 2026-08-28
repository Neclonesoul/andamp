import type { RepeatMode } from './types';

export const PROTOCOL_VERSION = 1 as const;

export type PlayerCommand =
  | { protocolVersion: 1; type: 'requestSnapshot' }
  | { protocolVersion: 1; type: 'playTrack'; mediaId: string }
  | { protocolVersion: 1; type: 'play' }
  | { protocolVersion: 1; type: 'pause' }
  | { protocolVersion: 1; type: 'togglePlayback' }
  | { protocolVersion: 1; type: 'seekTo'; positionMs: number }
  | { protocolVersion: 1; type: 'seekBy'; deltaMs: number }
  | { protocolVersion: 1; type: 'next' }
  | { protocolVersion: 1; type: 'previous' }
  | { protocolVersion: 1; type: 'setShuffle'; enabled: boolean }
  | { protocolVersion: 1; type: 'setRepeat'; mode: RepeatMode };

export function isPlayerCommand(value: unknown): value is PlayerCommand {
  if (!value || typeof value !== 'object') return false;
  const v = value as Record<string, unknown>;
  if (v.protocolVersion !== PROTOCOL_VERSION || typeof v.type !== 'string') return false;
  switch (v.type) {
    case 'requestSnapshot':
    case 'play':
    case 'pause':
    case 'togglePlayback':
    case 'next':
    case 'previous':
      return true;
    case 'playTrack':
      return typeof v.mediaId === 'string' && v.mediaId.length > 0;
    case 'seekTo':
      return typeof v.positionMs === 'number' && Number.isFinite(v.positionMs) && v.positionMs >= 0;
    case 'seekBy':
      return typeof v.deltaMs === 'number' && Number.isFinite(v.deltaMs);
    case 'setShuffle':
      return typeof v.enabled === 'boolean';
    case 'setRepeat':
      return v.mode === 'off' || v.mode === 'all' || v.mode === 'one';
    default:
      return false;
  }
}
