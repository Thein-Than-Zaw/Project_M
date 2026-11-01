// Device-local preference. This engine never touches WebRTC tracks or partner audio.
export function createNotificationSounds({storage,createContext,setInterval:repeat,clearInterval:cancel}) {
  let context,muted=storage.getItem('couple.sounds.muted')==='true',ringId=null,timer=null;
  const voices=new Set(),seen=new Set();
  function stop(){for(const voice of voices){try{voice.stop();}catch{}}voices.clear();}
  function tone(frequency,delay,length){
    if(muted||context?.state!=='running')return;
    const voice=context.createOscillator(),gain=context.createGain(),at=context.currentTime+delay;
    voice.type='sine';voice.frequency.value=frequency;
    gain.gain.setValueAtTime(0,at);gain.gain.linearRampToValueAtTime(.045,at+.02);
    gain.gain.exponentialRampToValueAtTime(.001,at+length);
    voice.connect(gain);gain.connect(context.destination);voices.add(voice);
    voice.onended=()=>{voices.delete(voice);voice.disconnect();gain.disconnect();};
    voice.start(at);voice.stop(at+length+.02);
  }
  function melody(){if(ringId&&!muted){tone(523.25,0,.25);tone(659.25,.28,.25);tone(783.99,.56,.5);}}
  function ring(id){if(id===ringId)return;cancel(timer);timer=null;stop();ringId=id;if(id){melody();timer=repeat(melody,2600);}}
  return {
    get muted(){return muted;},get enabled(){return context?.state==='running';},
    async enable(){context??=createContext();await context.resume();storage.setItem('couple.sounds.enabled','true');melody();},
    mute(value,persist=true){muted=value;if(persist)storage.setItem('couple.sounds.muted',String(value));if(value)stop();},
    ring,
    message(id){if(!id||seen.has(id))return;seen.add(id);if(seen.size>300)seen.delete(seen.values().next().value);
      // Shared marker prevents duplicate chimes from chat + badge sockets and other tabs.
      if(storage.getItem('couple.sounds.lastMessage')===id)return;
      storage.setItem('couple.sounds.lastMessage',id);
      if(!ringId){tone(880,0,.12);tone(1174.66,.13,.18);}
    },
    dispose(){ring(null);context?.close().catch(()=>{});}
  };
}

// Storage may be unavailable in private/restricted browsing; sounds still work in this tab.
const fallback=new Map();
const storage={getItem(key){try{return localStorage.getItem(key);}catch{return fallback.get(key)??null;}},setItem(key,value){try{localStorage.setItem(key,value);}catch{fallback.set(key,value);}}};
export const notificationSounds=typeof window==='undefined'?null:createNotificationSounds({storage,createContext:()=>new (window.AudioContext||window.webkitAudioContext)(),setInterval,clearInterval});
if(notificationSounds){
  window.addEventListener('storage',event=>{if(event.key==='couple.sounds.muted'||event.key===null){notificationSounds.mute(storage.getItem('couple.sounds.muted')==='true',false);window.dispatchEvent(new Event('notification-sounds-change'));}});
  // Autoplay rules require a gesture on each new document, even with a saved preference.
  const unlock=()=>{if(storage.getItem('couple.sounds.enabled')!=='true')return;notificationSounds.enable().then(()=>window.dispatchEvent(new Event('notification-sounds-change'))).catch(()=>{});};
  document.addEventListener('pointerdown',unlock,{once:true});document.addEventListener('keydown',unlock,{once:true});
  window.addEventListener('pagehide',()=>notificationSounds.dispose());
}
