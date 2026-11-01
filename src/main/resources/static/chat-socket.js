// DEV-05: exact own chat queue and three authorized commands; no raw broker publishing.
export function chatSocket({onEvent,onStatus,onReady,onExpired}){
  let socket,timer,heartbeat,connectTimer,closed=false,ready=false,connecting=false,attempt=1000,buffer='';
  const clear=()=>{clearTimeout(timer);clearTimeout(connectTimer);clearInterval(heartbeat);};
  function frame(command,headers={},body=''){
    if(!socket||socket.readyState!==WebSocket.OPEN)return false;
    socket.send(`${command}\n${Object.entries(headers).map(([k,v])=>`${k}:${v}`).join('\n')}\n\n${body}\0`);return true;
  }
  function retry(){clear();ready=false;if(closed)return;onStatus(navigator.onLine?'Reconnecting… Your saved messages are safe.':'Offline. Messages stay queued in this tab.');timer=setTimeout(connect,attempt+Math.random()*400);attempt=Math.min(attempt*2,30000);}
  async function connect(){
    if(closed||connecting||ready)return;if(!navigator.onLine){retry();return;}connecting=true;
    onStatus('Connecting to your private chat…');
    try{
      const me=await fetch('/api/me',{cache:'no-store',signal:AbortSignal.timeout(10000)});
      if(me.status===401){closed=true;clear();onExpired(true);return;}
      if(!me.ok)throw new Error('session');if(!(await me.json()).paired){closed=true;onExpired(false);return;}
      const response=await fetch('/api/csrf',{cache:'no-store',signal:AbortSignal.timeout(10000)});if(!response.ok)throw new Error('csrf');const csrf=await response.json();
      if(closed)return;
      socket=new WebSocket(`${location.protocol==='https:'?'wss:':'ws:'}//${location.host}/ws`,['v12.stomp']);buffer='';
      connectTimer=setTimeout(()=>socket?.close(),10000);
      socket.onopen=()=>frame('CONNECT',{'accept-version':'1.2',host:location.hostname,'heart-beat':'10000,0',[csrf.headerName]:csrf.token});
      socket.onmessage=event=>{
        buffer+=event.data;
        while(buffer.includes('\0')){
          const end=buffer.indexOf('\0'),raw=buffer.slice(0,end).replace(/^\s+/,'');buffer=buffer.slice(end+1);
          const separator=raw.indexOf('\n\n'),head=raw.slice(0,separator).replace(/\r/g,''),body=separator<0?'':raw.slice(separator+2),command=head.split('\n')[0];
          if(command==='CONNECTED'){
            clearTimeout(connectTimer);attempt=1000;frame('SUBSCRIBE',{id:'chat',destination:'/user/queue/chat',ack:'auto'});frame('SUBSCRIBE',{id:'memories',destination:'/user/queue/pairing',ack:'auto'});ready=true;onStatus('Connected · Private chat');
            heartbeat=setInterval(()=>{if(socket?.readyState===WebSocket.OPEN)socket.send('\n');},10000);onReady();
          }else if(command==='MESSAGE'){
            try{onEvent(JSON.parse(body));}catch{onStatus('An update could not be read. Reloading saved messages…');onReady();}
          }else if(command==='ERROR')socket.close();
        }
        if(buffer.length>262144)socket.close();
      };
      socket.onclose=retry;socket.onerror=()=>socket.close();
    }catch{retry();}finally{connecting=false;}
  }
  const offline=()=>{ready=false;socket?.close();retry();},online=()=>{attempt=1000;clearTimeout(timer);if(!ready&&(!socket||socket.readyState===WebSocket.CLOSED))connect();};
  window.addEventListener('offline',offline);window.addEventListener('online',online);connect();
  return {
    send(destination,payload){if(!ready)return false;const body=JSON.stringify(payload);return frame('SEND',{destination:`/app/${destination}`,'content-type':'application/json','content-length':new TextEncoder().encode(body).length},body);},
    connected:()=>ready,
    stop(){closed=true;ready=false;clear();window.removeEventListener('offline',offline);window.removeEventListener('online',online);if(socket){socket.onclose=null;socket.close();}}
  };
}
