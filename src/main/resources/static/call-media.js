// DEV08-FIX01: bounded, cancellable device setup. A timed-out getUserMedia request can
// still resolve later, so dispose those tracks rather than attaching them to a new call.
export function requestCallMedia({mediaDevices,mode,signal,timeoutMs=30000,onWaiting=()=>{},waitingMs=6000,audio=true,videoConstraints}){
  return new Promise((resolve,reject)=>{
    let settled=false,timeout,waiting;
    const error=(code,message)=>Object.assign(new Error(message),{code});
    const cleanup=()=>{clearTimeout(timeout);clearTimeout(waiting);signal?.removeEventListener('abort',abort);};
    const fail=value=>{if(settled)return;settled=true;cleanup();reject(value);};
    const abort=()=>fail(error('MEDIA_CANCELLED','Device setup was cancelled.'));
    if(signal?.aborted){abort();return;}
    signal?.addEventListener('abort',abort,{once:true});
    timeout=setTimeout(()=>fail(error('MEDIA_TIMEOUT','Camera/microphone did not open in time. Check the browser camera permission icon, then retry. On one computer, another browser may already be using the camera.')),Math.max(1,timeoutMs));
    waiting=setTimeout(()=>{if(!settled)onWaiting();},waitingMs);
    const constraints={audio:audio?{echoCancellation:true,noiseSuppression:true}:false,video:mode==='VIDEO'?(videoConstraints||{width:{ideal:640},height:{ideal:480},facingMode:'user'}):false};
    const rejected=cause=>{
      const messages={NotAllowedError:'Allow camera/microphone access from the browser address bar, then retry.',NotFoundError:'The requested camera or microphone was not found. Connect a device and retry.',NotReadableError:'Camera/microphone is busy or unavailable. Another browser or app may be using it. For two video participants on one computer, try a second camera or test on two devices.',AbortError:'The browser could not start the camera/microphone. Close other apps using the device and retry.',OverconstrainedError:'The selected camera could not provide a supported video stream. Try another camera.'};
      fail(error(cause?.name||'MEDIA_UNAVAILABLE',messages[cause?.name]||'Could not open the camera/microphone. Check browser permissions and devices, then retry.'));
    };
    try{Promise.resolve(mediaDevices.getUserMedia(constraints)).then(stream=>{
      if(settled){for(const track of stream.getTracks())track.stop();return;}
      settled=true;cleanup();resolve(stream);
    },rejected);}catch(cause){rejected(cause);}
  });
}

// CALL-UI06: only expose switching after permission reveals multiple cameras/directions.
export async function canSwitchCallCamera(mediaDevices,track){
  try{
    const directions=track?.getCapabilities?.().facingMode||[];
    if(directions.includes('user')&&directions.includes('environment'))return true;
    return (await mediaDevices?.enumerateDevices?.()||[]).filter(device=>device.kind==='videoinput').length>1;
  }catch{return false;}
}

/** Replace only the video sender; microphone, peer connection and call identity survive. */
export async function switchCallCamera({mediaDevices,stream,sender,facingMode,signal,isCurrent=()=>true,timeoutMs=12000}){
  const old=stream?.getVideoTracks()[0];
  if(!old||!sender?.replaceTrack||!['user','environment'].includes(facingMode))throw new Error('Camera switching is unavailable for this call.');
  const check=()=>{if(signal?.aborted||!isCurrent())throw Object.assign(new Error('Camera change cancelled.'),{code:'MEDIA_CANCELLED'});};
  const settings=old.getSettings?.()||{},enabled=old.enabled;
  let candidate,restored,released=old.readyState==='ended';
  const abortTracks=()=>{candidate?.stop();restored?.stop();};
  signal?.addEventListener('abort',abortTracks,{once:true});
  const open=async constraints=>{
    check();const media=await requestCallMedia({mediaDevices,mode:'VIDEO',audio:false,videoConstraints:{width:{ideal:640},height:{ideal:480},...constraints},signal,timeoutMs});
    const track=media.getVideoTracks()[0];for(const extra of media.getTracks())if(extra!==track)extra.stop();
    if(!track)throw new Error('The selected camera did not provide video.');
    try{check();}catch(error){track.stop();throw error;}return track;
  };
  try{
    try{candidate=await open({facingMode:{exact:facingMode}});}
    catch(error){
      // Some mobile hardware permits only one active camera. Release video only,
      // and restore the previous camera if acquisition/replacement subsequently fails.
      if(!['NotReadableError','AbortError'].includes(error.code)||released)throw error;
      check();old.stop();released=true;candidate=await open({facingMode:{exact:facingMode}});
    }
    candidate.enabled=enabled;check();await sender.replaceTrack(candidate);check();
    stream.removeTrack(old);stream.addTrack(candidate);old.stop();return candidate;
  }catch(error){
    candidate?.stop();
    if(signal?.aborted||!isCurrent())throw Object.assign(new Error('Camera change cancelled.'),{code:'MEDIA_CANCELLED'});
    if(released||old.readyState==='ended'){
      try{
        restored=await open(settings.deviceId?{deviceId:{exact:settings.deviceId}}:{facingMode:{exact:settings.facingMode||'user'}});
        restored.enabled=enabled;await sender.replaceTrack(restored);check();stream.removeTrack(old);stream.addTrack(restored);
      }catch(restoreError){restored?.stop();if(signal?.aborted||!isCurrent())throw Object.assign(new Error('Camera change cancelled.'),{code:'MEDIA_CANCELLED'});throw new Error('Camera could not restart. Audio is still connected. Tap the camera button to try again.');}
    }else if(candidate){
      // A rejected replacement normally leaves the old sender untouched.
      try{await sender.replaceTrack(old);}catch{}
    }
    throw new Error('Could not switch camera. The previous camera is kept. '+error.message);
  }finally{signal?.removeEventListener('abort',abortTracks);}
}
