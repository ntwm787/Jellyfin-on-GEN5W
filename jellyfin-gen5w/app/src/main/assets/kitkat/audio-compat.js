/* API 19 WebView reports duration=0 and loses ended for progressive transcodes.
 * Keep the HTML audio player and restore only the missing metadata/EOF signal.
 * ES5 syntax is required by the original KitKat WebView.
 */
(function () {
    'use strict';
    if (window.__gen5wAudioCompat) return;
    window.__gen5wAudioCompat = { version: 1, metadataLoads: 0, recoveredEnds: 0 };
    var durationProperty = Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, 'duration');
    var timeProperty = Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, 'currentTime');
    var states = [];

    function address(url) {
        var a = document.createElement('a');
        a.href = url;
        return a;
    }

    function stateFor(element) {
        for (var i = 0; i < states.length; i++) {
            if (states[i].element === element) return states[i];
        }
        if (element.__gen5wAudioState) {
            states.push(element.__gen5wAudioState);
            return element.__gen5wAudioState;
        }
        var state = { element: element, src: '', duration: 0, high: 0, seekUntil: 0, zeroSince: 0, ended: false, ownsDuration: false };
        element.__gen5wAudioState = state;
        states.push(state);
        // Override only this audio element; video and unrelated media are untouched.
        if (durationProperty && durationProperty.get) {
            Object.defineProperty(element, 'duration', { configurable: true, get: function () {
                var original = durationProperty.get.call(element);
                return original > 0 && isFinite(original) ? original : (state.duration || original);
            }});
        }
        if (timeProperty && timeProperty.get && timeProperty.set) {
            Object.defineProperty(element, 'currentTime', { configurable: true,
                get: function () { return timeProperty.get.call(element); },
                set: function (value) {
                    state.seekUntil = Date.now() + 2500;
                    state.high = 0;
                    state.zeroSince = 0;
                    state.ended = false;
                    timeProperty.set.call(element, value);
                }
            });
        }
        element.addEventListener('seeking', function () {
            state.seekUntil = Date.now() + 2500;
            state.high = 0;
            state.zeroSince = 0;
        });
        element.addEventListener('ended', function () { state.ended = true; });
        element.addEventListener('timeupdate', function () { update(state); });
        return state;
    }

    function metadata(state, src, itemId) {
        var credentials;
        try { credentials = JSON.parse(localStorage.getItem('jellyfin_credentials')); } catch (ignored) { return; }
        var servers = credentials && credentials.Servers || [];
        var source = address(src);
        for (var i = 0; i < servers.length; i++) {
            var server = servers[i];
            if (!server.ManualAddress || !server.AccessToken || !server.UserId) continue;
            var base = address(server.ManualAddress);
            if (base.protocol !== source.protocol || base.host !== source.host) continue;
            var xhr = new XMLHttpRequest();
            xhr.open('GET', server.ManualAddress.replace(/\/$/, '') + '/Items/' + itemId + '?UserId=' + encodeURIComponent(server.UserId), true);
            xhr.setRequestHeader('Authorization', 'MediaBrowser Token="' + server.AccessToken + '"');
            xhr.timeout = 15000;
            xhr.onload = function () {
                if (state.src !== src || xhr.status !== 200) return;
                try {
                    var item = JSON.parse(xhr.responseText);
                    var seconds = Number(item.RunTimeTicks) / 10000000;
                    if (!(seconds > 0 && isFinite(seconds))) return;
                    state.duration = seconds;
                    // The original KitKat binding exposes native properties as
                    // own data descriptors, without a prototype getter to wrap.
                    // For known Jellyfin items, use their authoritative runtime.
                    if (!durationProperty || !durationProperty.get) {
                        Object.defineProperty(state.element, 'duration', {
                            configurable: true,
                            get: function () { return state.duration || NaN; }
                        });
                        state.ownsDuration = true;
                    }
                    window.__gen5wAudioCompat.metadataLoads++;
                    // Let the web player's controls re-read the corrected duration.
                    var event = document.createEvent('Event');
                    event.initEvent('durationchange', false, false);
                    state.element.dispatchEvent(event);
                } catch (ignored) { /* Keep the original behavior if metadata fails. */ }
            };
            xhr.send();
            return;
        }
    }

    function update(state) {
        var element = state.element;
        var src = element.currentSrc || element.src || '';
        if (src !== state.src) {
            state.src = src;
            state.duration = 0;
            state.high = 0;
            state.zeroSince = 0;
            state.ended = false;
            var parsed = address(src);
            var page = address(location.href);
            var match = parsed.pathname.match(/\/Audio\/([a-f0-9-]{32,36})\/universal$/i);
            if (match && parsed.protocol === page.protocol && parsed.host === page.host) {
                metadata(state, src, match[1]);
            }
        }
        var time = element.currentTime;
        var now = Date.now();
        var originalDuration = durationProperty && durationProperty.get ? durationProperty.get.call(element) :
            (state.ownsDuration ? 0 : element.duration);
        if (element.paused || element.seeking || element.error || now < state.seekUntil || state.ended) {
            state.zeroSince = 0;
            return;
        }
        if (time > state.high) state.high = time;
        // KitKat's native player resets to zero at EOF for these unknown-length
        // streams. Recover only after observed playback reached the known end.
        // Never finish based on wall time, a stalled network, or a user seek.
        if (originalDuration === 0 && state.duration >= 5 && state.high >= state.duration - 1.25 &&
                time < 0.25 && element.readyState >= 2) {
            if (!state.zeroSince) state.zeroSince = now;
            if (now - state.zeroSince >= 500) {
                state.ended = true;
                window.__gen5wAudioCompat.recoveredEnds++;
                var ended = document.createEvent('Event');
                ended.initEvent('ended', false, false);
                element.dispatchEvent(ended);
            }
        } else state.zeroSince = 0;
    }

    setInterval(function () {
        var audio = document.querySelectorAll('audio');
        for (var i = 0; i < audio.length; i++) update(stateFor(audio[i]));
        // Release elements removed by the web player, including old event state.
        for (var j = states.length - 1; j >= 0; j--) {
            if (!document.documentElement.contains(states[j].element)) states.splice(j, 1);
        }
    }, 250);
})();
