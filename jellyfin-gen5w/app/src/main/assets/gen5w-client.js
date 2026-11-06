/* Modern Jellyfin window-plugin contract, with ES5 syntax for API 19. */
(function () {
    'use strict';
    if (window.__gen5wNativeInstalled) return;
    window.__gen5wNativeInstalled = true;
    // Keep a Jellyfin browsing destination independently of Android's activity stack.
    var lastBrowseHash='#/home';
    function rememberBrowsePage() {
        var hash=location.hash||'#/home';
        if(!/^#!?\/video(?:[?\/]|$)|playback|videoosd|login|selectserver|wizard/i.test(hash))lastBrowseHash=hash;
    }
    rememberBrowsePage();
    window.addEventListener('hashchange',rememberBrowsePage);
    window.__gen5wReturnToBrowse=function(){
        var player=window.__gen5wPlayer,target=lastBrowseHash||'#/home';
        if(player&&player._source){
            // playbackManager.stop disables auto-next; calling player.stop directly does not.
            if(player._manager&&player._manager.stop)player._manager.stop(player);
            else player.stop();
        }
        location.hash=target;
        setTimeout(function(){if(!player||!player._source)location.hash=target;},0);
    };
    window.__gen5wSelectTrack=function(kind,index){
        var player=window.__gen5wPlayer;
        if(!player||!player._source||player._done||!player._currentPlayOptions)return false;
        index=Number(index);if(!isFinite(index)||Math.floor(index)!==index)return false;
        var type=kind==='audio'?'Audio':kind==='subtitle'?'Subtitle':'';
        if(!type)return false;
        var streams=(player._currentPlayOptions.mediaSource||{}).MediaStreams||[];
        var valid=kind==='subtitle'&&index===-1;
        streams.forEach(function(stream){if(stream.Type===type&&stream.Index===index)valid=true;});
        if(!valid)return false;
        if(kind==='audio'&&player._manager.setAudioStreamIndex)player._manager.setAudioStreamIndex(index,player);
        else if(kind==='subtitle'&&player._manager.setSubtitleStreamIndex)player._manager.setSubtitleStreamIndex(index,player);
        else return false;
        return true;
    };
    // Keep loading indicators animated; remove decorative effects on KitKat.
    var lightStyle = document.createElement('style');
    lightStyle.id = 'gen5w-light-ui';
    lightStyle.textContent =
        '*,*:before,*:after{-webkit-transition-duration:0.01ms!important;transition-duration:0.01ms!important;' +
        '-webkit-transition-delay:0s!important;transition-delay:0s!important;}' +
        '.card,.cardBox,.cardImageContainer,.cardImage,.view,.dialog,.mainDrawer{' +
        '-webkit-animation-duration:0.01ms!important;animation-duration:0.01ms!important;' +
        '-webkit-animation-delay:0s!important;animation-delay:0s!important;}' +
        '.cardBox,.cardImageContainer{box-shadow:none!important;}' +
        '.btnDownload,[data-id="download"],[data-action="download"],a[download]{display:none!important;}';
    document.head.appendChild(lightStyle);
    function lightUiForUser(userId) {
        // Jellyfin's local display settings use the same user-prefixed store as
        // appSettings.set(key, value, userId). These keys are not synced.
        ['enableBackdrops', 'detailsBanner', 'blurhash', 'fastFadein'].forEach(function(key) {
            localStorage.setItem((userId ? userId + '-' : '') + key, 'false');
        });
        localStorage.setItem((userId ? userId + '-' : '') + 'libraryPageSize', '30');
    }
    lightUiForUser(null);
    try {
        var savedServers = JSON.parse(localStorage.getItem('jellyfin_credentials') || '{}').Servers || [];
        savedServers.forEach(function(server) { if (server.UserId) lightUiForUser(server.UserId); });
    } catch (ignored) { /* First login has no saved credentials yet. */ }
    // All video uses the guarded native player; prevent a web-player fallback.
    document.addEventListener('play', function(event) {
        if (event.target.tagName === 'VIDEO') event.target.pause();
    }, true);
    var device = JSON.parse(NativeInterface.getDeviceInformation());
    var sequence = Date.now();
    function profile() {
        return {
            Name: 'Jellyfin on GEN5W',
            MusicStreamingTranscodingBitrate: 256000,
            DirectPlayProfiles: [
                {Type:'Audio',Container:'mp3',AudioCodec:'mp3'},
                {Type:'Audio',Container:'m4a,aac,mp4',AudioCodec:'aac'},
                {Type:'Video',Container:'mp4,m4v',VideoCodec:'h264',AudioCodec:'aac,mp3'}
            ],
            TranscodingProfiles: [
                {Type:'Audio',Container:'mp3',AudioCodec:'mp3',Protocol:'http',Context:'Streaming'},
                {Type:'Video',Container:'ts',VideoCodec:'h264',AudioCodec:'aac',Protocol:'hls',Context:'Streaming',MinSegments:1,MaxAudioChannels:'2'}
            ],
            CodecProfiles: [{Type:'Video',Codec:'h264',Conditions:[
                {Condition:'EqualsAny',Property:'VideoProfile',Value:'baseline|constrained baseline'},
                {Condition:'LessThanEqual',Property:'Width',Value:'1920'},
                {Condition:'LessThanEqual',Property:'Height',Value:'1080'}
            ]}],
            // Burn selected subtitles on the server; no unsupported KitKat web text renderer.
            SubtitleProfiles: ['srt','subrip','vtt','webvtt','ass','ssa','pgssub','dvdsub','dvbsub','mov_text','text'].map(function(format){
                return {Format:format,Method:'Encode'};
            }), ContainerProfiles: [], ResponseProfiles: []
        };
    }
    window.NativeShell = {
        AppHost: {
            init:function(){return Promise.resolve(device);},
            getDefaultLayout:function(){return 'mobile';},
            supports:function(feature){return ['physicalvolumecontrol','htmlaudioautoplay','htmlvideoautoplay'].indexOf(feature.toLowerCase())>=0;},
            getDeviceProfile:profile, getSyncProfile:profile,
            deviceName:function(){return device.deviceName;}, deviceId:function(){return device.deviceId;},
            appName:function(){return 'Jellyfin on GEN5W';}, appVersion:function(){return device.appVersion;}
        },
        getPlugins:function(){return ['Gen5WNativePlayer'];},
        enableFullscreen:function(){}, disableFullscreen:function(){},
        updateMediaSession:function(){}, hideMediaSession:function(){}, updateVolumeLevel:function(){},
        onLocalUserSignedIn:function(user){if(user)lightUiForUser(user.Id);return Promise.resolve();},
        onLocalUserSignedOut:function(){NativePlayback.command('stop',0);},
        downloadFile:function(){return false;}, downloadFiles:function(){return false;},
        openUrl:function(url,target){NativeInterface.openUrl(url,target||'');},
        selectServer:function(){NativeInterface.selectServer();}
    };
    window.Gen5WNativePlayer = function () { return NativePlayerPlugin; };
    function NativePlayerPlugin(options) {
        var self=this;
        self.name='Jellyfin on GEN5W'; self.id='gen5wnative'; self.type='mediaplayer'; self.priority=-10; self.isLocalPlayer=true;
        // Let Jellyfin measure bandwidth instead of imposing a device bitrate.
        options.appSettings.enableAutomaticBitrateDetection(true,'Video',true);
        options.appSettings.enableAutomaticBitrateDetection(false,'Video',true);
        options.appSettings.enableSmoothScroll(false);
        self._settings=options.appSettings;
        self._events=options.events; self._manager=options.playbackManager; self._state={paused:true,position:0,duration:0,volume:100};
        self._source=null; self._generation=0; self._lastAction=0; self._done=true;
        window.__gen5wPlayer=self;
        setInterval(function(){self._poll();},250);
    }
    NativePlayerPlugin.prototype._event=function(name,args){this._events.trigger(this,name,args||[]);};
    NativePlayerPlugin.prototype._poll=function(){
        if (!this._source) return;
        var state;
        try { state=JSON.parse(NativePlayback.state()); } catch(ignored){return;}
        if(state.generation!==this._generation) return;
        var before=this._state; this._state=state;
        if(state.state==='ready'&&this._resolve){
            this._resolve(); this._resolve=null;
            var self=this,playedGeneration=this._generation;
            setTimeout(function(){if(self._source&&!self._done&&self._generation===playedGeneration)self._event('playing');},0);
        }
        if(state.paused!==before.paused) this._event(state.paused?'pause':'unpause');
        if(state.state==='buffering'&&before.state!=='buffering')this._event('waiting');
        if(state.state==='ready'&&before.state==='buffering')this._event('playing');
        if(state.actionRevision>this._lastAction){
            this._lastAction=state.actionRevision;
            if(state.action==='next')this._manager.nextTrack();
            if(state.action==='previous')this._manager.previousTrack();
        }
        if(state.state==='error'&&!this._done){
            this._done=true;
            if(this._reject){this._reject(new Error(state.error));this._reject=null;this._resolve=null;}
            this._event('error',[{type:'mediadecodeerror'}]);
        }
        if(state.state==='stopped'&&!this._done){
            if(this._manager&&this._manager.stop)this._manager.stop(this);
            else this.stop();
        } else if(state.state==='ended'&&!this._done){
            this._done=true; var src=this._source; this._source=null;
            this._event('stopped',[{src:src}]);
        } else if(!this._done)this._event('timeupdate');
    };
    NativePlayerPlugin.prototype.canPlayMediaType=function(type){return /^(audio|video)$/i.test(type||'');};
    NativePlayerPlugin.prototype.canPlayItem=function(){return true;};
    NativePlayerPlugin.prototype.getDeviceProfile=function(){return Promise.resolve(profile());};
    NativePlayerPlugin.prototype.play=function(options){
        if ((options.item.MediaType||'').toLowerCase()==='video') {
            var policy=JSON.parse(NativePlayback.videoPolicy());
            if (!policy.allowed) {
                window.alert(policy.message||'Video is unavailable until the car is confirmed stationary in Park.');
                return Promise.reject(new Error('Vehicle video restriction'));
            }
        }
        var self=this;
        var startPaused=!!(self._source && self._currentPlayOptions && options.item.Id &&
            self._currentPlayOptions.item.Id===options.item.Id && self._state.paused);
        self._generation=++sequence; self._source=options.url; self._done=false; self._lastAction=0;
        self._state={paused:startPaused,position:0,duration:(options.item.RunTimeTicks||0)/10000,volume:100};
        self._currentPlayOptions=options;
        var mediaSource=options.mediaSource||{};
        var audioIndex=self._manager.getAudioStreamIndex?self._manager.getAudioStreamIndex(self):null;
        var subtitleIndex=self._manager.getSubtitleStreamIndex?self._manager.getSubtitleStreamIndex(self):null;
        if(audioIndex===null||audioIndex===undefined)audioIndex=mediaSource.DefaultAudioStreamIndex;
        if(subtitleIndex===null||subtitleIndex===undefined)subtitleIndex=mediaSource.DefaultSubtitleStreamIndex;
        return new Promise(function(resolve,reject){
            self._resolve=resolve;self._reject=reject;
            NativePlayback.load(JSON.stringify({url:options.url,generation:self._generation,
                video:(options.item.MediaType||'').toLowerCase()==='video',title:options.item.Name||'Jellyfin',
                durationMs:(options.item.RunTimeTicks||0)/10000,positionMs:(options.playerStartPositionTicks||0)/10000,
                streams:(options.mediaSource||{}).MediaStreams||[],playMethod:options.playMethod||'',startPaused:startPaused,
                audioStreamIndex:audioIndex,subtitleStreamIndex:subtitleIndex}));
        });
    };
    NativePlayerPlugin.prototype.currentSrc=function(){return this._source;};
    NativePlayerPlugin.prototype.currentTime=function(ms){if(ms!==undefined)NativePlayback.command('seek',ms);return this._state.position||0;};
    NativePlayerPlugin.prototype.duration=function(){return this._state.duration||null;};
    NativePlayerPlugin.prototype.seekable=function(){return !!this._state.seekable;};
    NativePlayerPlugin.prototype.getBufferedRanges=function(){return [{start:0,end:(this._state.buffered||0)*10000}];};
    NativePlayerPlugin.prototype.pause=function(){NativePlayback.command('pause',0);};
    NativePlayerPlugin.prototype.unpause=function(){NativePlayback.command('play',0);};
    NativePlayerPlugin.prototype.resume=NativePlayerPlugin.prototype.unpause;
    NativePlayerPlugin.prototype.paused=function(){return !!this._state.paused;};
    NativePlayerPlugin.prototype.stop=function(){
        var src=this._source;this._done=true;this._source=null;
        this._state.paused=true;this._state.state='stopped';NativePlayback.command('stop',0);
        if(src)this._event('stopped',[{src:src}]);return Promise.resolve();
    };
    NativePlayerPlugin.prototype.destroy=function(){if(this._source)this.stop();};
    NativePlayerPlugin.prototype.setVolume=function(value){NativePlayback.command('volume',value);};
    NativePlayerPlugin.prototype.getVolume=function(){return this._state.volume;};
    NativePlayerPlugin.prototype.setMute=function(muted){this.setVolume(muted?0:100);};
    NativePlayerPlugin.prototype.isMuted=function(){return this._state.volume===0;};
    NativePlayerPlugin.prototype.supports=function(){return false;};
    NativePlayerPlugin.prototype.canSetAudioStreamIndex=function(){return false;};
    // Encode subtitles are changed by playbackManager's server-stream restart.
    // It also calls this hook with -1 to turn off client-rendered text tracks.
    NativePlayerPlugin.prototype.setSubtitleStreamIndex=function(){};
})();
