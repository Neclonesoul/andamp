import type { EqState } from './types';
export const EQ_FREQUENCIES = [60,170,310,600,1000,3000,6000,12000,14000,16000] as const;
export const FLAT_EQ: EqState = {
  version: 1, enabled: false, preampDb: 0, preset: 'Flat',
  bands: EQ_FREQUENCIES.map(frequencyHz => ({ frequencyHz, gainDb: 0 }))
};
export const EQ_PRESETS: Record<string, number[]> = {
  Flat: [0,0,0,0,0,0,0,0,0,0],
  Bass: [5,4,3,1,0,-1,-2,-2,-1,0],
  'Bass Reduce': [-5,-4,-3,-1,0,0,1,1,1,1],
  Treble: [-1,-1,0,0,1,2,3,4,4,4],
  Vocal: [-2,-1,0,2,3,3,2,0,-1,-2],
  Rock: [3,2,1,-1,-2,1,3,4,4,3],
  Electronic: [4,3,1,0,-1,1,2,3,4,4],
  Classical: [1,1,0,0,0,1,2,2,2,1],
  Acoustic: [1,1,0,1,2,2,1,1,0,0]
};
export function validateEq(state: EqState): boolean {
  return state.version === 1 && state.bands.length === 10 &&
    Number.isFinite(state.preampDb) &&
    state.bands.every(b => Number.isFinite(b.gainDb) && b.gainDb >= -12 && b.gainDb <= 12);
}
