import type { Track } from './types';
const text = (v: string) => v.localeCompare('', undefined);
export function sortTracks(tracks: readonly Track[], by: 'title'|'artist'|'album'|'recent'|'year'|'duration'): Track[] {
  return [...tracks].sort((a,b) => {
    switch(by) {
      case 'title': return a.title.localeCompare(b.title, undefined, { sensitivity: 'base' });
      case 'artist': return a.artist.localeCompare(b.artist, undefined, { sensitivity: 'base' }) || a.title.localeCompare(b.title);
      case 'album': return a.album.localeCompare(b.album, undefined, { sensitivity: 'base' }) || (a.trackNumber ?? 0) - (b.trackNumber ?? 0);
      case 'recent': return (b.dateAdded ?? 0) - (a.dateAdded ?? 0);
      case 'year': return (b.year ?? 0) - (a.year ?? 0);
      case 'duration': return a.durationMs - b.durationMs;
    }
  });
}
