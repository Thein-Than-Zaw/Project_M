// CALL-UI05: bounded, ephemeral visual effects. Never persist or render arbitrary markup.
export const CALL_REACTIONS=Object.freeze(['❤️','😂','👍','👏','🥰','🎉']);
export function createCallReactionEffects({layer,announcement,reducedMotion=()=>matchMedia('(prefers-reduced-motion: reduce)').matches,setTimer=setTimeout,clearTimer=clearTimeout}){
 const nodes=new Map();let disposed=false;
 function remove(node){clearTimer(nodes.get(node));nodes.delete(node);node.remove();}
 function clear(){for(const node of [...nodes.keys()])remove(node);announcement.textContent='';}
 return {
  show(emoji,person='Your person'){
   if(disposed||!CALL_REACTIONS.includes(emoji))return false;
   const reduced=reducedMotion();announcement.textContent=`${person} reacted ${emoji}`;
   for(let i=0;i<(reduced?1:3);i++){
    while(nodes.size>=18)remove(nodes.keys().next().value);
    const node=layer.ownerDocument.createElement('span');node.className='call-floating-reaction'+(reduced?' is-still':'');node.textContent=emoji;node.setAttribute('aria-hidden','true');
    node.style.setProperty('--reaction-x',`${24+Math.random()*52}%`);node.style.setProperty('--reaction-drift',`${Math.round(Math.random()*90-45)}px`);node.style.setProperty('--reaction-delay',`${i*100}ms`);
    node.addEventListener('animationend',()=>remove(node),{once:true});layer.append(node);nodes.set(node,setTimer(()=>remove(node),reduced?1800:3000));
   }return true;
  },clear,dispose(){clear();disposed=true;}
 };
}
