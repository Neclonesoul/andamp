import { describe, expect, it } from 'vitest';
import { isPlayerCommand, moveItem, searchTracks, stableShuffleAroundCurrent, validateEq, FLAT_EQ } from '../src';

describe('core', () => {
  it('validates bridge commands strictly', () => {
    expect(isPlayerCommand({protocolVersion:1,type:'play'})).toBe(true);
    expect(isPlayerCommand({protocolVersion:2,type:'play'})).toBe(false);
    expect(isPlayerCommand({protocolVersion:1,type:'seekTo',positionMs:-1})).toBe(false);
  });
  it('moves queue items deterministically', () => expect(moveItem(['a','b','c'],0,2)).toEqual(['b','c','a']));
  it('keeps current item first when shuffling around it', () => expect(stableShuffleAroundCurrent(['a','b','c'],1,42)[0]).toBe('b'));
  it('searches normalized track fields', () => {
    const t={id:'1',title:'Echo',artist:'Crusher-P',album:'Test',durationMs:1};
    expect(searchTracks([t],'crusher').length).toBe(1);
  });
  it('accepts default EQ', () => expect(validateEq(FLAT_EQ)).toBe(true));
});
