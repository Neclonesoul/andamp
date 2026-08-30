import type {
  LibrarySnapshot,
  PlaybackSnapshot,
  RepeatMode
} from '@andamp/core';
import type { PlayerAdapter } from './interface';

declare global {
  interface Window {
    AndampNative?: { postMessage(payload:string):void };
    __andampReceive?: (payload:string)=>void;
  }
}

const idle:PlaybackSnapshot={
  protocolVersion:1,
  status:'idle',
  currentMediaId:null,
  currentTrack:null,
  queue:[],
  queueIndex:-1,
  positionMs:0,
  bufferedPositionMs:0,
  durationMs:0,
  repeatMode:'off',
  shuffle:false,
  error:null,
  availableActions:[]
};

const emptyLibrary:LibrarySnapshot={
  protocolVersion:1,
  tracks:[],
  scannedAt:0
};

export class NativeAndroidPlayerAdapter implements PlayerAdapter {
  private state=idle;
  private library=emptyLibrary;

  private listeners=new Set<(s:PlaybackSnapshot)=>void>();
  private libraryListeners=new Set<(s:LibrarySnapshot)=>void>();

  constructor(){
    window.__andampReceive=(payload)=>{
      try{
        const msg=JSON.parse(payload);

        if(msg.type==='snapshot' && msg.payload?.protocolVersion===1){
          this.state=msg.payload;
          this.listeners.forEach(l=>l(this.state));
        }

        if(msg.type==='librarySnapshot' && msg.payload?.protocolVersion===1){
          this.library=msg.payload;
          this.libraryListeners.forEach(l=>l(this.library));
        }
      }catch{}
    };

    this.send({protocolVersion:1,type:'requestSnapshot'});
    this.send({protocolVersion:1,type:'requestLibrary'});
  }

  private send(message:object){
    window.AndampNative?.postMessage(JSON.stringify(message));
  }

  subscribe(l:(s:PlaybackSnapshot)=>void){
    this.listeners.add(l);
    l(this.state);
    return()=>this.listeners.delete(l);
  }

  subscribeLibrary(l:(s:LibrarySnapshot)=>void){
    this.libraryListeners.add(l);
    l(this.library);
    return()=>this.libraryListeners.delete(l);
  }

  async refreshLibrary(){
    this.send({protocolVersion:1,type:'requestLibrary'});
  }

  async playTrack(mediaId:string){
    this.send({protocolVersion:1,type:'playTrack',mediaId});
  }

  async play(){this.send({protocolVersion:1,type:'play'})}
  async pause(){this.send({protocolVersion:1,type:'pause'})}
  async togglePlayback(){this.send({protocolVersion:1,type:'togglePlayback'})}
  async seekTo(positionMs:number){this.send({protocolVersion:1,type:'seekTo',positionMs})}
  async seekBy(deltaMs:number){this.send({protocolVersion:1,type:'seekBy',deltaMs})}
  async next(){this.send({protocolVersion:1,type:'next'})}
  async previous(){this.send({protocolVersion:1,type:'previous'})}
  async setShuffle(enabled:boolean){this.send({protocolVersion:1,type:'setShuffle',enabled})}
  async setRepeat(mode:RepeatMode){this.send({protocolVersion:1,type:'setRepeat',mode})}

  async getSnapshot(){
    this.send({protocolVersion:1,type:'requestSnapshot'});
    return this.state;
  }
}
