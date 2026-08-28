import type { PlaybackSnapshot, QueueItem, RepeatMode, Track } from '@andamp/core';
import type { PlayerAdapter } from './interface';

const empty=():PlaybackSnapshot=>({protocolVersion:1,status:'idle',currentMediaId:null,currentTrack:null,queue:[],queueIndex:-1,positionMs:0,bufferedPositionMs:0,durationMs:0,repeatMode:'off',shuffle:false,error:null,availableActions:['play','pause','seek','next','previous']});

export class WebPlayerAdapter implements PlayerAdapter {
  private audio=new Audio();
  private ctx:AudioContext|null=null; private source:MediaElementAudioSourceNode|null=null;
  private analyser:AnalyserNode|null=null; private filters:BiquadFilterNode[]=[]; private gain:GainNode|null=null;
  private tracks=new Map<string,{track:Track,url:string}>(); private state=empty(); private listeners=new Set<(s:PlaybackSnapshot)=>void>();
  private tick=0;
  constructor(){
    this.audio.preload='metadata';
    this.audio.addEventListener('play',()=>this.patch({status:'playing'}));
    this.audio.addEventListener('pause',()=>this.patch({status:this.audio.ended?'ended':'paused'}));
    this.audio.addEventListener('timeupdate',()=>this.patch({positionMs:this.audio.currentTime*1000,durationMs:Number.isFinite(this.audio.duration)?this.audio.duration*1000:0}));
    this.audio.addEventListener('ended',()=>this.onEnded());
    this.audio.addEventListener('error',()=>this.patch({status:'error',error:'Unable to decode or access this audio file.'}));
  }
  private patch(p:Partial<PlaybackSnapshot>){this.state={...this.state,...p};this.listeners.forEach(l=>l(this.state));}
  subscribe(l:(s:PlaybackSnapshot)=>void){this.listeners.add(l);l(this.state);return()=>this.listeners.delete(l)}
  async importFiles(files:FileList|File[]){
    const added:Track[]=[];
    for(const f of Array.from(files)){
      if(!f.type.startsWith('audio/') && !/\.(mp3|m4a|aac|ogg|opus|wav|flac)$/i.test(f.name)) continue;
      const id=`web:${f.name}:${f.size}:${f.lastModified}`;
      const track:Track={id,title:f.name.replace(/\.[^.]+$/,''),artist:'Unknown Artist',album:'Unknown Album',durationMs:0,mimeType:f.type,sizeBytes:f.size};
      this.tracks.set(id,{track,url:URL.createObjectURL(f)}); added.push(track);
    }
    this.state.queue.push(...added.map((track,i):QueueItem=>({id:`q:${Date.now()}:${i}:${track.id}`,track})));
    this.patch({queue:[...this.state.queue]}); return added;
  }
  private ensureGraph(){
    if(this.ctx) return;
    this.ctx=new AudioContext(); this.source=this.ctx.createMediaElementSource(this.audio); this.gain=this.ctx.createGain();
    this.filters=[60,170,310,600,1000,3000,6000,12000,14000,16000].map((f,i)=>{const n=this.ctx!.createBiquadFilter();n.type=i===0?'lowshelf':i===9?'highshelf':'peaking';n.frequency.value=f;n.Q.value=1;return n});
    this.analyser=this.ctx.createAnalyser(); this.analyser.fftSize=512;
    let node:AudioNode=this.source; for(const f of this.filters){node.connect(f);node=f;} node.connect(this.gain);this.gain.connect(this.analyser);this.analyser.connect(this.ctx.destination);
  }
  setEq(enabled:boolean,gains:number[],preampDb:number){this.ensureGraph();this.gain!.gain.value=enabled?Math.pow(10,preampDb/20):1;this.filters.forEach((f,i)=>f.gain.value=enabled?(gains[i]??0):0)}
  getAnalyser(){this.ensureGraph();return this.analyser}
  async playTrack(id:string){
    const index=this.state.queue.findIndex(q=>q.track.id===id); if(index<0) return;
    const ref=this.tracks.get(id); if(!ref) return;
    this.ensureGraph(); await this.ctx?.resume(); this.audio.src=ref.url; this.patch({status:'loading',currentMediaId:id,currentTrack:ref.track,queueIndex:index,error:null});
    await this.audio.play();
  }
  async play(){this.ensureGraph();await this.ctx?.resume();await this.audio.play()}
  async pause(){this.audio.pause()}
  async togglePlayback(){this.audio.paused?await this.play():await this.pause()}
  async seekTo(ms:number){this.audio.currentTime=Math.max(0,ms/1000)}
  async seekBy(ms:number){await this.seekTo(this.state.positionMs+ms)}
  async next(){if(!this.state.queue.length)return; const n=Math.min(this.state.queueIndex+1,this.state.queue.length-1);await this.playTrack(this.state.queue[n].track.id)}
  async previous(){if(this.audio.currentTime>3){await this.seekTo(0);return;}const n=Math.max(this.state.queueIndex-1,0);if(this.state.queue[n])await this.playTrack(this.state.queue[n].track.id)}
  async setShuffle(enabled:boolean){this.patch({shuffle:enabled})}
  async setRepeat(mode:RepeatMode){this.patch({repeatMode:mode})}
  async getSnapshot(){return this.state}
  private async onEnded(){if(this.state.repeatMode==='one'){await this.seekTo(0);await this.play();return;} if(this.state.queueIndex<this.state.queue.length-1){await this.next();return;} if(this.state.repeatMode==='all'&&this.state.queue[0])await this.playTrack(this.state.queue[0].track.id)}
}
