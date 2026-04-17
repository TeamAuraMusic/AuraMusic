package com.auramusic.app.voice

object VoiceCommandParser {

    private val defaultWakeWords = listOf("hey aura", "hello aura", "aura", "ok aura", "hey aura music", "aura play")

    fun parseCommand(text: String, wakeWord: String = "aura"): VoiceCommand {
        val lowerText = text.lowercase().trim()
        
        // Check for wake word and extract command after it
        var commandText = lowerText
        for (wake in defaultWakeWords) {
            if (lowerText.startsWith(wake)) {
                commandText = lowerText.removePrefix(wake).trim()
                break
            }
        }
        
        // Also check custom wake word
        val customWake = wakeWord.lowercase()
        if (customWake.isNotEmpty() && lowerText.startsWith(customWake)) {
            commandText = lowerText.removePrefix(customWake).trim()
        }
        
        // If nothing left after wake word, return Unknown
        if (commandText.isEmpty()) {
            return VoiceCommand.WakeWordDetected
        }
        
        // Search commands
        if (commandText.contains("search") || commandText.contains("find") || commandText.contains("play")) {
            val query = commandText
                .replace("search for", "")
                .replace("search", "")
                .replace("find", "")
                .replace("play", "")
                .replace("play song", "")
                .replace("play music", "")
                .trim()
            if (query.isNotEmpty()) {
                return VoiceCommand.Search(query)
            }
        }

        // Seek forward
        val forwardMatch = Regex("skip ([0-9]+) (second|seconds|minute|minutes)|forward ([0-9]+) (second|seconds|minute|minutes)").find(commandText)
        if (forwardMatch != null) {
            val num = forwardMatch.groupValues[1].ifEmpty { forwardMatch.groupValues[3] }.toIntOrNull() ?: 30
            val unit = if (forwardMatch.groupValues[2].contains("minute") || forwardMatch.groupValues[4].contains("minute")) 60 else 1
            return VoiceCommand.SeekForward(num * unit * 1000L)
        }
        
        // Seek backward
        val backwardMatch = Regex("go back|rewind|back ([0-9]+) (second|seconds|minute|minutes)|previous ([0-9]+) (second|seconds|minute|minutes)").find(commandText)
        if (backwardMatch != null) {
            val num = backwardMatch.groupValues[2].ifEmpty { backwardMatch.groupValues[3] }.toIntOrNull() ?: 10
            val unit = if (backwardMatch.groupValues[3].contains("minute")) 60 else 1
            return VoiceCommand.SeekBackward(num * unit * 1000L)
        }

        // Playback commands
        return when {
            // Play commands
            commandText.contains("play") || commandText.contains("start") || commandText.contains("resume") -> {
                VoiceCommand.Play
            }
            
            // Pause commands
            commandText.contains("pause") || commandText.contains("stop") -> {
                VoiceCommand.Pause
            }
            
            // Toggle play/pause
            commandText.contains("toggle") && commandText.contains("play") -> {
                VoiceCommand.TogglePlayPause
            }
            
            // Next commands
            commandText.contains("next") || commandText.contains("skip") || commandText.contains("forward") -> {
                VoiceCommand.Next
            }
            
            // Previous commands  
            commandText.contains("previous") || commandText.contains("back") || commandText.contains("last") -> {
                VoiceCommand.Previous
            }
            
            // Shuffle commands
            commandText.contains("shuffle on") -> {
                VoiceCommand.ShuffleOn
            }
            commandText.contains("shuffle off") -> {
                VoiceCommand.ShuffleOff
            }
            commandText.contains("shuffle") -> {
                VoiceCommand.Shuffle
            }
            
            // Repeat commands
            commandText.contains("repeat one") || commandText.contains("loop one") -> {
                VoiceCommand.RepeatOne
            }
            commandText.contains("repeat all") || commandText.contains("loop all") -> {
                VoiceCommand.RepeatAll
            }
            commandText.contains("repeat off") || commandText.contains("loop off") -> {
                VoiceCommand.RepeatOff
            }
            commandText.contains("repeat") || commandText.contains("loop") -> {
                VoiceCommand.Repeat
            }
            
            // Volume commands
            commandText.contains("volume up") || commandText.contains("louder") || commandText.contains("increase volume") || commandText.contains("volume higher") -> {
                VoiceCommand.VolumeUp
            }
            commandText.contains("volume down") || commandText.contains("quieter") || commandText.contains("decrease volume") || commandText.contains("volume lower") -> {
                VoiceCommand.VolumeDown
            }
            commandText.contains("mute") || commandText.contains("silent") -> {
                VoiceCommand.Mute
            }
            commandText.contains("unmute") -> {
                VoiceCommand.Unmute
            }
            
            // Speed commands
            commandText.contains("speed up") || commandText.contains("faster") -> {
                VoiceCommand.SpeedUp
            }
            commandText.contains("slow down") || commandText.contains("slower") -> {
                VoiceCommand.SlowDown
            }
            commandText.contains("normal speed") || commandText.contains("reset speed") -> {
                VoiceCommand.ResetSpeed
            }
            
            // Settings commands
            commandText.contains("dark mode on") || commandText.contains("dark theme on") || commandText.contains("enable dark mode") -> {
                VoiceCommand.SetDarkMode(true)
            }
            commandText.contains("dark mode off") || commandText.contains("dark theme off") || commandText.contains("disable dark mode") -> {
                VoiceCommand.SetDarkMode(false)
            }
            commandText.contains("dark mode") || commandText.contains("dark theme") -> {
                VoiceCommand.SetDarkMode(true)
            }
            commandText.contains("light mode on") || commandText.contains("light theme on") || commandText.contains("enable light mode") -> {
                VoiceCommand.SetDarkMode(false)
            }
            commandText.contains("light mode off") || commandText.contains("light theme off") -> {
                VoiceCommand.SetDarkMode(true)
            }
            commandText.contains("light mode") || commandText.contains("light theme") -> {
                VoiceCommand.SetDarkMode(false)
            }
            commandText.contains("toggle theme") || commandText.contains("switch theme") -> {
                VoiceCommand.ToggleTheme
            }
            
            // Lyrics commands
            commandText.contains("show lyrics") || commandText.contains("lyrics on") || commandText.contains("enable lyrics") -> {
                VoiceCommand.ShowLyrics
            }
            commandText.contains("hide lyrics") || commandText.contains("lyrics off") || commandText.contains("disable lyrics") -> {
                VoiceCommand.HideLyrics
            }
            commandText.contains("toggle lyrics") -> {
                VoiceCommand.ToggleLyrics
            }
            
            // Video commands
            commandText.contains("video on") || commandText.contains("show video") || commandText.contains("enable video") -> {
                VoiceCommand.EnableVideo
            }
            commandText.contains("video off") || commandText.contains("hide video") || commandText.contains("disable video") -> {
                VoiceCommand.DisableVideo
            }
            commandText.contains("toggle video") -> {
                VoiceCommand.ToggleVideo
            }
            
            // Like commands
            commandText.contains("like") || commandText.contains("favorite") || commandText.contains("love") -> {
                VoiceCommand.ToggleLike
            }
            
            // Queue commands
            commandText.contains("show queue") || commandText.contains("view queue") || commandText.contains("open queue") -> {
                VoiceCommand.ShowQueue
            }
            commandText.contains("clear queue") -> {
                VoiceCommand.ClearQueue
            }
            commandText.contains("add to queue") || commandText.contains("queue this") -> {
                VoiceCommand.AddToQueue
            }
            
            // Open commands
            commandText.contains("go home") || commandText.contains("open home") -> {
                VoiceCommand.OpenHome
            }
            commandText.contains("go library") || commandText.contains("open library") -> {
                VoiceCommand.OpenLibrary
            }
            commandText.contains("go search") || commandText.contains("open search") -> {
                VoiceCommand.OpenSearch
            }
            commandText.contains("go settings") || commandText.contains("open settings") -> {
                VoiceCommand.OpenSettings
            }
            
            // Unknown command
            else -> VoiceCommand.Unknown(text)
        }
    }
}

sealed class VoiceCommand {
    // Playback
    data object Play : VoiceCommand()
    data object Pause : VoiceCommand()
    data object TogglePlayPause : VoiceCommand()
    data object Next : VoiceCommand()
    data object Previous : VoiceCommand()
    data object Shuffle : VoiceCommand()
    data object ShuffleOn : VoiceCommand()
    data object ShuffleOff : VoiceCommand()
    data object Repeat : VoiceCommand()
    data object RepeatOne : VoiceCommand()
    data object RepeatAll : VoiceCommand()
    data object RepeatOff : VoiceCommand()
    
    // Seek
    data class SeekForward(val milliseconds: Long) : VoiceCommand()
    data class SeekBackward(val milliseconds: Long) : VoiceCommand()
    
    // Volume
    data object VolumeUp : VoiceCommand()
    data object VolumeDown : VoiceCommand()
    data object Mute : VoiceCommand()
    data object Unmute : VoiceCommand()
    
    // Speed
    data object SpeedUp : VoiceCommand()
    data object SlowDown : VoiceCommand()
    data object ResetSpeed : VoiceCommand()
    
    // Search
    data class Search(val query: String) : VoiceCommand()
    
    // Settings
    data class SetDarkMode(val enabled: Boolean) : VoiceCommand()
    data object ToggleTheme : VoiceCommand()
    
    // Lyrics
    data object ShowLyrics : VoiceCommand()
    data object HideLyrics : VoiceCommand()
    data object ToggleLyrics : VoiceCommand()
    
    // Video
    data object EnableVideo : VoiceCommand()
    data object DisableVideo : VoiceCommand()
    data object ToggleVideo : VoiceCommand()
    
    // Media
    data object ToggleLike : VoiceCommand()
    data object ShowQueue : VoiceCommand()
    data object ClearQueue : VoiceCommand()
    data object AddToQueue : VoiceCommand()
    
    // Navigation
    data object OpenHome : VoiceCommand()
    data object OpenLibrary : VoiceCommand()
    data object OpenSearch : VoiceCommand()
    data object OpenSettings : VoiceCommand()
    
    // Wake word
    data object WakeWordDetected : VoiceCommand()
    
    // Unknown
    data class Unknown(val text: String) : VoiceCommand()
}
