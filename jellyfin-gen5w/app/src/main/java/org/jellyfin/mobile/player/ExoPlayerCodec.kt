package org.jellyfin.mobile.player

import android.media.MediaCodecInfo.CodecCapabilities
import android.os.Build
import org.jellyfin.mobile.player.ExoPlayerFormats.getAudioCodec
import org.jellyfin.mobile.player.ExoPlayerFormats.getAudioProfile
import org.jellyfin.mobile.player.ExoPlayerFormats.getVideoCodec
import org.jellyfin.mobile.player.ExoPlayerFormats.getVideoLevel
import org.jellyfin.mobile.player.ExoPlayerFormats.getVideoProfile
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class ExoPlayerCodec(private val mimeType: String, codecCapabilities: CodecCapabilities) {
    val codec: String?
    val isAudio: Boolean
    private val profiles: MutableList<String> = ArrayList()
    private val levels: MutableList<Int> = ArrayList()
    private val maxBitrate: Int
    private val maxChannels: Int?
    private val maxSampleRate: Int?

    init {
        // Check if this mimeType represents a video codec
        val videoCodec = getVideoCodec(mimeType)
        if (videoCodec != null) {
            codec = videoCodec
            isAudio = false
            // API 19 cannot report bitrate limits. Use a conservative streaming
            // policy rather than claim these are measured hardware limits.
            maxBitrate = if (Build.VERSION.SDK_INT >= 21) codecCapabilities.videoCapabilities.bitrateRange.upper else 10_000_000
            maxChannels = null
            maxSampleRate = null
        } else {
            // If not, check audio codecs
            val audioCodec = getAudioCodec(mimeType)
            if (audioCodec != null) {
                isAudio = true
                codec = audioCodec
                if (Build.VERSION.SDK_INT >= 21) {
                    maxBitrate = codecCapabilities.audioCapabilities.bitrateRange.upper
                    maxChannels = codecCapabilities.audioCapabilities.maxInputChannelCount
                    val sampleRates = codecCapabilities.audioCapabilities.supportedSampleRateRanges
                    maxSampleRate = if (sampleRates.isNotEmpty()) sampleRates.last().upper else null
                } else {
                    maxBitrate = 320_000
                    maxChannels = 2
                    maxSampleRate = 48_000
                }
            } else {
                // mimeType is neither, abort
                codec = null
                isAudio = false
                maxBitrate = 0
                maxChannels = null
                maxSampleRate = null
            }
        }
        if (codec != null) {
            val profileLevels = codecCapabilities.profileLevels
            for (profileLevel in profileLevels) {
                val profile: String?
                val level: Int?
                if (isAudio) {
                    profile = getAudioProfile(codec, profileLevel.profile)
                    level = null
                } else {
                    profile = getVideoProfile(codec, profileLevel.profile)
                    level = getVideoLevel(codec, profileLevel.level)
                }
                if (profile != null && !profiles.contains(profile)) {
                    profiles.add(profile)
                }
                if (level != null && !levels.contains(level)) {
                    levels.add(level)
                }
            }
        }
    }

    fun mergeCodec(codecToMerge: ExoPlayerCodec) {
        for (profile in codecToMerge.profiles) {
            if (!profiles.contains(profile)) {
                profiles.add(profile)
            }
        }
        for (level in codecToMerge.levels) {
            if (!levels.contains(level)) {
                levels.add(level)
            }
        }
    }

    fun toJSONObject(): JSONObject? = try {
        JSONObject().apply {
            put("mimeType", mimeType)
            put("codec", codec)
            put("isAudio", isAudio)
            put("profiles", JSONArray(profiles))
            put("levels", JSONArray(levels))
            put("maxBitrate", maxBitrate)
            put("maxChannels", maxChannels)
            put("maxSampleRate", maxSampleRate)
        }
    } catch (e: JSONException) {
        null
    }
}
