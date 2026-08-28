import type { PlaybackSnapshot, RepeatMode, Track } from '@andamp/core';
export interface PlayerAdapter {
  subscribe(listener:(state:PlaybackSnapshot)=>void):()=>void;
  importFiles?(files:FileList|File[]):Promise<Track[]>;
  playTrack(id:string):Promise<void>;
  play():Promise<void>; pause():Promise<void>; togglePlayback():Promise<void>;
  seekTo(ms:number):Promise<void>; seekBy(ms:number):Promise<void>;
  next():Promise<void>; previous():Promise<void>;
  setShuffle(enabled:boolean):Promise<void>; setRepeat(mode:RepeatMode):Promise<void>;
  getSnapshot():Promise<PlaybackSnapshot>;
  getAnalyser?():AnalyserNode|null;
  setEq?(enabled:boolean,gains:number[],preampDb:number):void;
}
