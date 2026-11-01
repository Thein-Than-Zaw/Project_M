// Minimal STOMP1.2 client for a single read-only pairing queue. No feature SEND commands.
export function watchPairing(onUpdate,onStatus){
  let socket,timer,heartbeat,stopped=false,delay=1000,buffer='';
  const clear=()=>{clearTimeout(timer);clearInterval(heartbeat);};
  async function connect(){
    if(stopped)return;
    try{
      const response=await fetch('/api/csrf',{cache:'no-store',signal:AbortSignal.timeout(10000)});
      if(!response.ok)throw new Error('csrf');
      const csrf=await response.json();
      socket=new WebSocket(`${location.protocol==='https:'?'wss:':'ws:'}//${location.host}/ws`,['v12.stomp']);buffer='';
      socket.onopen=()=>socket.send(`CONNECT\naccept-version:1.2\nhost:${location.hostname}\nheart-beat:10000,0\n${csrf.headerName}:${csrf.token}\n\n\0`);
      socket.onmessage=event=>{
        buffer+=event.data;
        while(buffer.includes('\0')){
          const index=buffer.indexOf('\0'),frame=buffer.slice(0,index).replace(/^\s+/,'').replace(/\r\n/g,'\n');buffer=buffer.slice(index+1);
          const split=frame.indexOf('\n\n'),head=frame.slice(0,split),body=split<0?'':frame.slice(split+2),command=head.split('\n')[0];
          if(command==='CONNECTED'){
            delay=1000;onStatus('Live updates connected.');
            socket.send('SUBSCRIBE\nid:pairing\ndestination:/user/queue/pairing\nack:auto\n\n\0');
            heartbeat=setInterval(()=>{if(socket?.readyState===WebSocket.OPEN)socket.send('\n');},10000);
            onUpdate(); // close subscribe-vs-commit race by REST reconciliation
          }else if(command==='MESSAGE'){let update;try{update=JSON.parse(body);}catch{}onUpdate(update);}
          else if(command==='ERROR')socket.close();
        }
        if(buffer.length>16384)socket.close();
      };
      socket.onclose=()=>{clear();if(!stopped){onStatus('Reconnecting… Your space stays saved.');timer=setTimeout(connect,delay+Math.random()*400);delay=Math.min(delay*2,30000);}};
      socket.onerror=()=>socket.close();
    }catch{if(!stopped){onStatus('Live updates unavailable. Checking your space in the background.');timer=setTimeout(connect,delay);delay=Math.min(delay*2,30000);}}
  }
  connect();
  return()=>{stopped=true;clear();if(socket){socket.onclose=null;socket.close();}};
}
