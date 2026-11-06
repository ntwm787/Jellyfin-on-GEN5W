/* Exercises the shipped plugin with the playbackManager.stop contract used by Jellyfin. */
const fs=require('fs'),vm=require('vm'),assert=require('assert');
const asset=fs.readFileSync(process.argv[2],'utf8');
const callbacks=[],commands=[],audioSwitches=[];
let nativeState={},loads=0,lastLoad;
const context={location:{hash:'#/details?id=fixture'},localStorage:{getItem:()=>null,setItem:()=>{}},
    document:{createElement:()=>({}),head:{appendChild:()=>{}},addEventListener:()=>{}},
    NativeInterface:{getDeviceInformation:()=>'{"deviceId":"fixture"}'},
    NativePlayback:{selectAudio:(index,generation)=>audioSwitches.push({index,generation}),state:()=>JSON.stringify(nativeState),command:(name)=>commands.push(name),
        load:(json)=>{loads++;lastLoad=JSON.parse(json);nativeState={generation:lastLoad.generation,state:'ready',paused:false,position:9000,duration:60000};},
        videoPolicy:()=>'{"allowed":true}'},
    setInterval:()=>1,setTimeout:(fn)=>{callbacks.push(fn);return 1;},addEventListener:()=>{}};
context.window=context;vm.createContext(context);vm.runInContext(asset,context);
function create(){
    const events=[],manager={autoNext:true,stops:0,next:0,
        stop(player){this.autoNext=false;this.stops++;return player.stop();}};
    const Plugin=context.Gen5WNativePlayer();
    const player=new Plugin({appSettings:{enableAutomaticBitrateDetection:()=>{},enableSmoothScroll:()=>{}},
        events:{trigger:(target,name)=>{events.push(name);if(name==='stopped'&&manager.autoNext)manager.next++;}},playbackManager:manager});
    player.play({item:{Id:'fixture',MediaType:'Video',Name:'Fixture'},url:'http://fixture.invalid/stream.mp4'});
    player._poll();
    return {player,manager,events};
}
let t=create();
nativeState.state='stopped';t.player._poll();
assert.equal(t.manager.stops,1);assert.equal(t.manager.next,0);assert.equal(t.player.currentSrc(),null);
assert.equal(t.player.paused(),true);t.player._poll();assert.equal(t.manager.stops,1);
while(callbacks.length)callbacks.shift()();
assert(!t.events.includes('playing'),'A late playing event survived manual stop');
t=create();context.location.hash='#/video';context.__gen5wReturnToBrowse();
assert.equal(t.manager.stops,1);assert.equal(t.manager.next,0);
while(callbacks.length)callbacks.shift()();
assert.equal(context.location.hash,'#/details?id=fixture');assert.equal(t.player.currentSrc(),null);
t=create();nativeState.state='ended';t.player._poll();
assert.equal(t.manager.stops,0);assert.equal(t.manager.next,1,'Natural completion must retain normal queue handling');
t=create();
const changes=[];
t.manager.getAudioStreamIndex=()=>2;t.manager.getSubtitleStreamIndex=()=>-1;
t.manager.setAudioStreamIndex=(index,player)=>{assert.equal(player,t.player);changes.push('audio:'+index);};
t.manager.setSubtitleStreamIndex=(index,player)=>{assert.equal(player,t.player);changes.push('subtitle:'+index);};
t.player.play({item:{Id:'fixture',MediaType:'Video',Name:'Fixture'},url:'http://fixture.invalid/stream.mp4',
    mediaSource:{DefaultAudioStreamIndex:4,DefaultSubtitleStreamIndex:6,
        MediaStreams:[{Type:'Audio',Index:2},{Type:'Audio',Index:4},{Type:'Subtitle',Index:6}]}});
assert.equal(lastLoad.audioStreamIndex,2);assert.equal(lastLoad.subtitleStreamIndex,-1);
assert.equal(context.__gen5wSelectTrack('audio',4),true);
assert.equal(context.__gen5wSelectTrack('subtitle',6),true);
assert.equal(context.__gen5wSelectTrack('subtitle',-1),true);
assert.equal(context.__gen5wSelectTrack('audio',999),false);
assert.equal(context.__gen5wSelectTrack('audio',6),false);
assert.equal(context.__gen5wSelectTrack('other',2),false);
assert.deepEqual(changes,['audio:4','subtitle:6','subtitle:-1']);
// Restored server-managed switching: local audio hook must stay disabled.
assert.equal(t.player.canSetAudioStreamIndex(),false);
assert.equal(audioSwitches.length,0);
t.manager.stop(t.player);assert.equal(t.player.canSetAudioStreamIndex(),false);assert.equal(context.__gen5wSelectTrack('audio',2),false);
console.log(JSON.stringify({manual_stop_disables_auto_next:true,back_uses_manager_stop:true,
    stopped_source_cleared:true,late_playing_suppressed:true,browse_route_restored:true,natural_end_queue_retained:true,
    selected_manager_indices_forwarded:true,audio_selection_forwarded:true,subtitle_selection_and_off_forwarded:true,invalid_or_stopped_selection_rejected:true,local_audio_switch_disabled:true,server_audio_switch_fallback:true}));
