<script lang="ts">
import { onMount } from 'svelte';
import type { LibrarySnapshot, PlaybackSnapshot, RepeatMode, Track } from '@andamp/core';
import { EQ_PRESETS, FLAT_EQ } from '@andamp/core';
import { WebPlayerAdapter } from '../lib/player/web-adapter';
import { NativeAndroidPlayerAdapter } from '../lib/player/native-adapter';

let adapter:any;
let state:PlaybackSnapshot={protocolVersion:1,status:'idle',currentMediaId:null,currentTrack:null,queue:[],queueIndex:-1,positionMs:0,bufferedPositionMs:0,durationMs:0,repeatMode:'off',shuffle:false,error:null,availableActions:[]};
let library:LibrarySnapshot={protocolVersion:1,tracks:[],scannedAt:0};
let tab='player', query='', eqEnabled=false, eqGains=[0,0,0,0,0,0,0,0,0,0], preamp=0, preset='Flat', canvas:HTMLCanvasElement;
let raf=0;
let isNativeAndroid=false;
const fmt=(ms:number)=>{const s=Math.max(0,Math.floor(ms/1000));return `${Math.floor(s/60)}:${String(s%60).padStart(2,'0')}`};
const filtered=()=>isNativeAndroid
  ? library.tracks.filter(track=>`${track.title} ${track.artist} ${track.album}`.toLowerCase().includes(query.toLowerCase()))
  : state.queue.map(q=>q.track).filter(track=>`${track.title} ${track.artist} ${track.album}`.toLowerCase().includes(query.toLowerCase()));
onMount(()=>{
  isNativeAndroid =
    typeof window !== 'undefined' && !!window.AndampNative;

  adapter=isNativeAndroid
    ? new NativeAndroidPlayerAdapter()
    : new WebPlayerAdapter();
  const off=adapter.subscribe((s:PlaybackSnapshot)=>state=s);
  const offLibrary=adapter.subscribeLibrary?.((s:LibrarySnapshot)=>library=s) ?? (()=>{});
  adapter.refreshLibrary?.();
  const theme=localStorage.getItem('andamp-theme')||'andamp'; document.documentElement.dataset.theme=theme==='andamp'?'':theme;
  return ()=>{off();offLibrary();cancelAnimationFrame(raf)}
});
async function files(e:Event){const input=e.currentTarget as HTMLInputElement;if(input.files&&adapter.importFiles)await adapter.importFiles(input.files)}
async function cycleRepeat(){const next:RepeatMode=state.repeatMode==='off'?'all':state.repeatMode==='all'?'one':'off';await adapter.setRepeat(next)}
function applyPreset(name:string){preset=name;eqGains=[...(EQ_PRESETS[name]||EQ_PRESETS.Flat)];adapter.setEq?.(eqEnabled,eqGains,preamp)}
function applyEq(){adapter.setEq?.(eqEnabled,eqGains,preamp)}
function setTheme(v:string){document.documentElement.dataset.theme=v==='andamp'?'':v;localStorage.setItem('andamp-theme',v)}
function visualizer(node:HTMLCanvasElement){
  canvas=node; const ctx=node.getContext('2d')!;
  const draw=()=>{raf=requestAnimationFrame(draw);const a=adapter?.getAnalyser?.();if(!a)return;const d=new Uint8Array(a.frequencyBinCount);a.getByteFrequencyData(d);const r=devicePixelRatio||1;const w=node.clientWidth*r,h=node.clientHeight*r;if(node.width!==w){node.width=w;node.height=h}ctx.clearRect(0,0,w,h);const bars=48,bw=w/bars;ctx.fillStyle=getComputedStyle(document.documentElement).getPropertyValue('--signal');for(let i=0;i<bars;i++){const v=d[Math.floor(i*d.length/bars)]/255;ctx.fillRect(i*bw,h-v*h,bw-2*r,v*h)}};
  draw(); return {destroy(){cancelAnimationFrame(raf)}};
}
</script>

<div class="app">
  <header class="topbar">
    <div><div class="brand">ANDAMP</div><div class="tag">LOCAL AUDIO SYSTEM · YOUR MUSIC. YOUR PLAYER.</div></div>
    <span class="grow"></span>
    <select aria-label="Theme" on:change={(e)=>setTheme((e.currentTarget as HTMLSelectElement).value)}>
      <option value="andamp">Andamp</option><option value="oled">OLED</option><option value="classic">Classic</option>
    </select>
  </header>

  <nav class="tabs" aria-label="Sections">
    {#each ['player','library','queue','equalizer','visualizer','settings'] as t}
      <button aria-pressed={tab===t} on:click={()=>tab=t}>{t.toUpperCase()}</button>
    {/each}
  </nav>

  {#if tab==='player'}
  <div class="grid cols">
    <section class="panel" style="padding:12px">
      <div class="display" style="padding:14px;border-radius:8px">
        <div class="small">NOW PLAYING · {state.status.toUpperCase()}</div>
        <h2 style="margin:.35rem 0;color:var(--signal)">{state.currentTrack?.title ?? 'No track selected'}</h2>
        <div>{state.currentTrack?.artist ?? 'Add local music to begin'}</div>
        <div class="small">{state.currentTrack?.album ?? 'Andamp Web Preview'}</div>
        <div class="row" style="margin-top:12px">
          <span>{fmt(state.positionMs)}</span>
          <input class="grow" aria-label="Seek" type="range" min="0" max={Math.max(state.durationMs,1)} value={state.positionMs} on:change={(e)=>adapter.seekTo(Number((e.currentTarget as HTMLInputElement).value))}/>
          <span>{fmt(state.durationMs)}</span>
        </div>
      </div>
      <div class="row" style="justify-content:center;margin-top:12px;flex-wrap:wrap">
        <button aria-label="Previous" on:click={()=>adapter.previous()}>⏮</button>
        <button aria-label={state.status==='playing'?'Pause':'Play'} on:click={()=>adapter.togglePlayback()}>{state.status==='playing'?'⏸':'▶'}</button>
        <button aria-label="Next" on:click={()=>adapter.next()}>⏭</button>
        <button aria-pressed={state.shuffle} on:click={()=>adapter.setShuffle(!state.shuffle)}>SHUF {state.shuffle?'ON':'OFF'}</button>
        <button on:click={cycleRepeat}>REP {state.repeatMode.toUpperCase()}</button>
      </div>
      {#if state.error}<p style="color:var(--danger)">{state.error}</p>{/if}
    </section>
    <section class="panel" style="padding:12px">
      <div class="row"><strong>QUEUE</strong><span class="grow"></span><span class="small">{state.queue.length} TRACKS</span></div>
      {#each state.queue.slice(0,8) as q, i}
        <button class="track" style="width:100%;text-align:left" on:click={()=>adapter.playTrack(q.track.id)}>
          <span class="art">♪</span><span><b>{q.track.title}</b><br><span class="small">{q.track.artist}</span></span><span class="small">{i+1}</span>
        </button>
      {/each}
    </section>
  </div>
  {:else if tab==='library'}
    <section class="panel" style="padding:12px">
      <div class="row"><input class="grow" placeholder="Search local library" bind:value={query}/>{#if !isNativeAndroid}<label><button>ADD MUSIC<input hidden type="file" multiple accept="audio/*,.flac,.mp3,.m4a,.ogg,.opus,.wav" on:change={files}/></button></label>{/if}</div>
      {#if isNativeAndroid}<p class="small">Android library is supplied by MediaStore through the native service.</p>{/if}
      {#each filtered() as track}
        <button class="track" style="width:100%;text-align:left" on:click={()=>adapter.playTrack(track.id)}><span class="art">♪</span><span><b>{track.title}</b><br><span class="small">{track.artist} · {track.album}</span></span><span class="small">{fmt(track.durationMs)}</span></button>
      {:else}<p class="small">No local tracks yet.</p>{/each}
    </section>
  {:else if tab==='queue'}
    <section class="panel" style="padding:12px"><h2>Queue</h2>{#each state.queue as q,i}<div class="track"><span>{i+1}</span><span><b>{q.track.title}</b><br><span class="small">{q.track.artist}</span></span><button on:click={()=>adapter.playTrack(q.track.id)}>PLAY</button></div>{/each}</section>
  {:else if tab==='equalizer'}
    <section class="panel" style="padding:12px"><div class="row"><h2>Equalizer</h2><span class="grow"></span><label><input type="checkbox" bind:checked={eqEnabled} on:change={applyEq}/> ON</label><select bind:value={preset} on:change={()=>applyPreset(preset)}>{#each Object.keys(EQ_PRESETS) as p}<option>{p}</option>{/each}</select></div>
      <div class="eq">{#each [60,170,310,600,1000,3000,6000,12000,14000,16000] as f,i}<label><input type="range" min="-12" max="12" step=".5" bind:value={eqGains[i]} on:input={applyEq}/><span>{f>=1000?`${f/1000}k`:f}</span></label>{/each}</div>
      <div class="row"><span>PREAMP</span><input class="grow" type="range" min="-12" max="6" step=".5" bind:value={preamp} on:input={applyEq}/><span>{preamp} dB</span></div><p class="small">Web preview uses a real Web Audio filter chain. Native Android DSP capability is surfaced separately and must never be faked.</p>
    </section>
  {:else if tab==='visualizer'}
    <section class="panel" style="padding:12px"><h2>Spectrum</h2><canvas aria-hidden="true" use:visualizer></canvas><p class="small">The browser spectrum is driven by the active Web Audio analyser.</p></section>
  {:else}
    <section class="panel" style="padding:12px"><h2>Settings</h2><p><b>Privacy:</b> local-first; no account, ads or library upload.</p><p><b>Platform:</b> {isNativeAndroid?'Android native audio authority':'Web preview adapter'}</p><p><b>Version:</b> 0.1.0</p></section>
  {/if}

  {#if state.currentTrack}
  <div class="panel mini">
    <span class="art">♪</span><span class="grow"><b>{state.currentTrack.title}</b><br><span class="small">{state.currentTrack.artist}</span></span>
    <button aria-label={state.status==='playing'?'Pause':'Play'} on:click={()=>adapter.togglePlayback()}>{state.status==='playing'?'⏸':'▶'}</button><button aria-label="Next" on:click={()=>adapter.next()}>⏭</button>
  </div>
  {/if}
</div>
