import type { Track } from './types';
const norm = (s: string) => s.normalize('NFKD').toLowerCase().replace(/\s+/g, ' ').trim();
export function searchTracks(tracks: readonly Track[], query: string): Track[] {
  const q = norm(query);
  if (!q) return [...tracks];
  return tracks.filter(t => norm([t.title, t.artist, t.album, t.genre ?? ''].join(' ')).includes(q));
}
