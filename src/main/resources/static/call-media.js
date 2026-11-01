// DEV08-FIX01: bounded, cancellable device setup. A timed-out getUserMedia request can
// still resolve later, so dispose those tracks rather than attaching them to a new call.
export function requestCallMedia({mediaDevices,mode,signal,timeoutMs=30000,onWaiting=()=>{},waitingMs=6000}){
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
    const constraints={audio:{echoCancellation:true,noiseSuppression:true},video:mode==='VIDEO'?{width:{ideal:640},height:{ideal:480},facingMode:'user'}:false};
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
