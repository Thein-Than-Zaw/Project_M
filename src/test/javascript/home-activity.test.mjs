import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
const source=readFileSync(new URL('../../main/resources/static/home.js',import.meta.url),'utf8');
function fixture(){const nodes=new Map();const c={$:id=>{if(!nodes.has(id))nodes.set(id,{textContent:''});return nodes.get(id);}};vm.createContext(c);vm.runInContext(source.slice(source.indexOf('function formatVideoTime('),source.indexOf('function render(){'))+';globalThis.format=formatVideoTime;globalThis.show=renderActivity;',c);return {c,nodes};}
test('video time formats seconds into complete minutes and hours without inventing precision',()=>{
 const {c}=fixture();for(const [seconds,text] of [[0,'0 min'],[1,'<1 min'],[59,'<1 min'],[60,'1 min'],[3599,'59 min'],[3600,'1h 0m'],[3661,'1h 1m'],[90061,'25h 1m']])assert.equal(c.format(seconds),text);for(const invalid of [undefined,null,-1,Infinity,'60'])assert.equal(c.format(invalid),'—');
});
test('real zero totals render as zero while missing activity is unavailable rather than fabricated zero',()=>{
 const {c,nodes}=fixture();c.show({messageCount:0,videoCallCount:0,videoCallSeconds:0});assert.equal(nodes.get('#activity-messages').textContent,'0');assert.equal(nodes.get('#activity-video-calls').textContent,'0');assert.equal(nodes.get('#activity-video-time').textContent,'0 min');c.show();for(const node of nodes.values())assert.equal(node.textContent,'—');
});
