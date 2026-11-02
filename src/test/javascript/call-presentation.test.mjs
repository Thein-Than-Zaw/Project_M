import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
const source=readFileSync(new URL('../../main/resources/static/call-widget.js',import.meta.url),'utf8');

test('remote video defaults to uncropped Fit and only fills after an explicit user choice',()=>{
 const handlers=new Map(),attributes={},panel={dataset:{}},menu={open:true},button={setAttribute:(key,value)=>attributes[key]=value,addEventListener:(key,fn)=>handlers.set(key,fn)};
 const context={$:id=>id==='call-panel'?panel:id==='call-more'?menu:button};vm.createContext(context);
 const start=source.indexOf('let fillCallVideo='),end=source.indexOf('function renderFullscreenControl(',start);
 vm.runInContext(source.slice(start,end),context);
 assert.equal(panel.dataset.videoFit,'fit');assert.equal(attributes['aria-pressed'],'false');
 handlers.get('click')();assert.equal(panel.dataset.videoFit,'fill');assert.equal(attributes['aria-pressed'],'true');assert.equal(menu.open,false);
 handlers.get('click')();assert.equal(panel.dataset.videoFit,'fit');assert.equal(attributes['aria-pressed'],'false');
});

test('native fullscreen enter/exit events keep the SVG icon and update accessible state',()=>{
 const handlers=new Map(),attributes={},button={innerHTML:'',setAttribute:(key,value)=>attributes[key]=value};const doc={fullscreenElement:null,addEventListener:(name,fn)=>handlers.set(name,fn)};
 const context={$:()=>button,document:doc};vm.createContext(context);
 const start=source.indexOf('function renderFullscreenControl('),end=source.indexOf("$('call-fullscreen').addEventListener('click'",start);
 const eventLine=source.split('\n').find(line=>line.startsWith("document.addEventListener('fullscreenchange'"));
 vm.runInContext(source.slice(start,end)+eventLine,context);
 const fitIcon=button.innerHTML;assert.match(fitIcon,/^<svg /);assert.equal(attributes['aria-label'],'Enter full screen');
 for(let i=0;i<2;i++){doc.fullscreenElement={};handlers.get('fullscreenchange')();assert.match(button.innerHTML,/^<svg /);assert.notEqual(button.innerHTML,fitIcon);assert.equal(attributes['aria-label'],'Exit full screen');assert.equal(button.title,'Exit full screen');doc.fullscreenElement=null;handlers.get('fullscreenchange')();assert.equal(button.innerHTML,fitIcon);assert.equal(attributes['aria-label'],'Enter full screen');}
});
