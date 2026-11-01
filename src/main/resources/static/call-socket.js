// DEV-08: exact own call queue; state commands are acknowledged, never treated as saved on send alone.
export function callSocket({onEvent,onReady,onStatus,onExpired,onCsrf}){
  let ws,buffer='',closed=false,ready=false,connecting=false,retryTimer,connectTimer,heartbeat,delay=1000;
  const pending=new Map();
  const fail=(message,code)=>Object.assign(new Error(message),{code});
  function frame(command,headers={},body=''){
    if(ws?.readyState!==WebSocket.OPEN||ws.bufferedAmount>65536)return false;
    ws.send(`${command}\n${Object.entries(headers).map(([k,v])=>`${k}:${v}`).join('\n')}\n\n${body}\0`);return true;
  }
  function rejectPending(){for(const item of pending.values()){clearTimeout(item.timer);item.reject(fail('Call action was not confirmed. Reconnect to check its current state.','CALL_NOT_CONFIRMED'));}pending.clear();}
  function retry(){ready=false;connecting=false;clearInterval(heartbeat);clearTimeout(connectTimer);rejectPending();if(closed)return;onStatus('Call connection lost. Reconnecting…');clearTimeout(retryTimer);retryTimer=setTimeout(connect,delay+Math.random()*300);delay=Math.min(delay*2,30000);}
  async function connect(){
    if(closed||connecting||ready)return;connecting=true;
    try{
      const response=await fetch('/api/me',{cache:'no-store',signal:AbortSignal.timeout(10000)});
      if(response.status===401){closed=true;onExpired();return;}if(!response.ok)throw new Error('Session unavailable');if(!(await response.json()).paired){closed=true;onExpired();return;}
      const csrfResponse=await fetch('/api/csrf',{cache:'no-store',signal:AbortSignal.timeout(10000)});if(!csrfResponse.ok)throw new Error('Session token unavailable');const csrf=await csrfResponse.json();if(closed)return;onCsrf(csrf);
      ws=new WebSocket(`${location.protocol==='https:'?'wss:':'ws:'}//${location.host}/ws`,['v12.stomp']);buffer='';connectTimer=setTimeout(()=>ws?.close(),10000);
      ws.onopen=()=>frame('CONNECT',{'accept-version':'1.2',host:location.hostname,'heart-beat':'10000,0',[csrf.headerName]:csrf.token});
      ws.onmessage=event=>{
        buffer+=event.data;
        while(buffer.includes('\0')){
          const index=buffer.indexOf('\0'),raw=buffer.slice(0,index).replace(/^\s+/,'').replace(/\r\n/g,'\n');buffer=buffer.slice(index+1);const split=raw.indexOf('\n\n'),command=raw.slice(0,split).split('\n')[0];
          if(command==='CONNECTED'){
            clearTimeout(connectTimer);frame('SUBSCRIBE',{id:'calls',destination:'/user/queue/calls',ack:'auto'});ready=true;connecting=false;delay=1000;onStatus('Calls connected');heartbeat=setInterval(()=>{if(ws?.readyState===WebSocket.OPEN)ws.send('\n');},10000);Promise.resolve(onReady()).catch(()=>onStatus('Could not check the current call. Retry.'));
          }else if(command==='MESSAGE'){
            let data;try{data=JSON.parse(raw.slice(split+2));}catch{onStatus('A call update could not be read. Reconnect to check.');continue;}
            const item=pending.get(data.eventId);
            if(item&&(data.type==='call.ack'||data.type==='call.error')){clearTimeout(item.timer);pending.delete(data.eventId);if(data.type==='call.error')item.reject(fail(data.detail||'Call action failed.',data.code));else item.resolve(data.call);}
            else if(data.type==='call.error')onStatus(data.detail||'Call action could not be processed.');
            if(data.call||data.type?.startsWith('call.')&&data.payload)Promise.resolve(onEvent(data)).catch(()=>onStatus('Call connection could not be updated. Retry or end this call.'));
          }else if(command==='ERROR')ws.close();
        }
        if(buffer.length>65536)ws.close();
      };
      ws.onclose=retry;ws.onerror=()=>ws.close();
    }catch{connecting=false;retry();}
  }
  const online=()=>{clearTimeout(retryTimer);delay=1000;if(!ready&&!connecting)connect();};window.addEventListener('online',online);connect();
  return {
    connected:()=>ready,
    send(operation,payload){return new Promise((resolve,reject)=>{
      if(!ready)return reject(fail('Calls are reconnecting. Try again shortly.','CALL_OFFLINE'));
      const body=JSON.stringify(payload),timer=setTimeout(()=>{pending.delete(payload.eventId);reject(fail('Call action was not confirmed. Check its current state before retrying.','CALL_NOT_CONFIRMED'));},10000);
      pending.set(payload.eventId,{resolve,reject,timer});if(!frame('SEND',{destination:`/app/call.${operation}`,'content-type':'application/json','content-length':new TextEncoder().encode(body).length},body)){clearTimeout(timer);pending.delete(payload.eventId);reject(fail('Call connection is unavailable.','CALL_OFFLINE'));}
    });},
    stop(){closed=true;ready=false;clearTimeout(retryTimer);clearTimeout(connectTimer);clearInterval(heartbeat);rejectPending();window.removeEventListener('online',online);if(ws){ws.onclose=null;ws.close();}}
  };
}
