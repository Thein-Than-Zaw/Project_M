// Device-local preference. This engine never touches WebRTC tracks or partner audio.
export function createNotificationSounds({storage,createContext,setInterval:repeat,clearInterval:cancel}) {
  let context,master,volume=Math.max(0,Math.min(100,Number(storage.getItem('couple.sounds.volume')??80))),muted=storage.getItem('couple.sounds.muted')==='true',ringId=null,ringKind='incoming',timer=null;
  if(!Number.isFinite(volume))volume=80;
  const voices=new Set(),seen=new Set(),sentSeen=new Set(),endedSeen=new Set();
  function stop(){for(const voice of voices){try{voice.stop();}catch{}}voices.clear();}
  function tone(frequency,delay,length){
    if(muted||volume===0||context?.state!=='running')return;
    const voice=context.createOscillator(),gain=context.createGain(),at=context.currentTime+delay;
    voice.type='sine';voice.frequency.value=frequency;
    gain.gain.setValueAtTime(0,at);gain.gain.linearRampToValueAtTime(.22,at+.02);
    gain.gain.exponentialRampToValueAtTime(.001,at+length);
    voice.connect(gain);gain.connect(master);voices.add(voice);
    voice.onended=()=>{voices.delete(voice);voice.disconnect();gain.disconnect();};
    voice.start(at);voice.stop(at+length+.02);
  }
  function melody(){if(ringId&&!muted){if(ringKind==='outgoing'){tone(440,0,.55);tone(480,0,.55);tone(440,.75,.55);tone(480,.75,.55);}else{tone(523.25,0,.25);tone(659.25,.28,.25);tone(783.99,.56,.5);}}}
  function ring(id,kind='incoming'){if(id===ringId&&(!id||kind===ringKind))return;cancel(timer);timer=null;stop();ringId=id;ringKind=kind;if(id){melody();timer=repeat(melody,kind==='outgoing'?4000:2600);}}
  return {
    get muted(){return muted;},get volume(){return volume;},get enabled(){return context?.state==='running';},
    setVolume(value,persist=true){const next=Number(value);if(!Number.isFinite(next))return;volume=Math.round(Math.max(0,Math.min(100,next)));master?.gain.setValueAtTime(volume/100,context.currentTime);if(persist)storage.setItem('couple.sounds.volume',String(volume));},
    async enable(){if(!context){context=createContext();master=context.createGain();master.gain.setValueAtTime(volume/100,context.currentTime);master.connect(context.destination);}const wasRunning=context.state==='running';await context.resume();storage.setItem('couple.sounds.enabled','true');if(!wasRunning)melody();},
    mute(value,persist=true){muted=value;if(persist)storage.setItem('couple.sounds.muted',String(value));if(value)stop();},
    ring,
    ringback(id){ring(id,'outgoing');},
    callEnded(id,outcome){
      if(!id||endedSeen.has(id))return;endedSeen.add(id);if(endedSeen.size>300)endedSeen.delete(endedSeen.values().next().value);
      if(storage.getItem('couple.sounds.lastCallEnd')===id)return;storage.setItem('couple.sounds.lastCallEnd',id);
      // Never interrupt the ringtone of a newer call or replay a muted outcome later.
      if(ringId)return;
      if(['MISSED','DECLINED','FAILED'].includes(outcome)){tone(330,0,.18);tone(330,.28,.18);tone(262,.56,.25);}
      else{tone(659.25,0,.13);tone(440,.16,.22);}
    },
    message(id){if(!id||seen.has(id))return;seen.add(id);if(seen.size>300)seen.delete(seen.values().next().value);
      // Shared marker prevents duplicate chimes from chat + badge sockets and other tabs.
      if(storage.getItem('couple.sounds.lastMessage')===id)return;
      storage.setItem('couple.sounds.lastMessage',id);
      if(!ringId){tone(880,0,.12);tone(1174.66,.13,.18);}
    },
    sent(id){if(!id||sentSeen.has(id))return;sentSeen.add(id);if(sentSeen.size>300)sentSeen.delete(sentSeen.values().next().value);if(!ringId){tone(740,0,.08);tone(1046.5,.09,.14);}},
    preview(){if(!ringId){tone(880,0,.16);tone(1174.66,.18,.22);}},
    dispose(){ring(null);context?.close().catch(()=>{});}
  };
}

// Storage may be unavailable in private/restricted browsing; sounds still work in this tab.
const fallback=new Map();
export const notificationStorage={getItem(key){try{return localStorage.getItem(key);}catch{return fallback.get(key)??null;}},setItem(key,value){try{localStorage.setItem(key,value);}catch{fallback.set(key,value);}}};
export const notificationSounds=typeof window==='undefined'?null:createNotificationSounds({storage:notificationStorage,createContext:()=>new (window.AudioContext||window.webkitAudioContext)(),setInterval,clearInterval});
if(notificationSounds){
  window.addEventListener('storage',event=>{if(event.key?.startsWith('couple.sounds.')||event.key===null){notificationSounds.mute(notificationStorage.getItem('couple.sounds.muted')==='true',false);notificationSounds.setVolume(notificationStorage.getItem('couple.sounds.volume')??80,false);window.dispatchEvent(new Event('notification-sounds-change'));}});
  // Autoplay rules require a gesture on each new document, even with a saved preference.
  const unlock=()=>{if(notificationStorage.getItem('couple.sounds.enabled')!=='true')return;notificationSounds.enable().then(()=>window.dispatchEvent(new Event('notification-sounds-change'))).catch(()=>{});};
  document.addEventListener('pointerdown',unlock,{once:true});document.addEventListener('keydown',unlock,{once:true});
  window.addEventListener('pagehide',()=>notificationSounds.dispose());
}
