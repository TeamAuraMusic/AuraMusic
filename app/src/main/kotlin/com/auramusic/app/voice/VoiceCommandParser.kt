package com.auramusic.app.voice

object VoiceCommandParser {

    fun parseCommand(text: String): VoiceCommand {
        val lowerText = text.lowercase().trim()
        
        // Search commands
        if (lowerText.contains("search") || lowerText.contains("find") || lowerText.contains("play")) {
            val query = lowerText
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
        val forwardMatch = Regex("skip ([0-9]+) (second|seconds|minute|minutes)|forward ([0-9]+) (second|seconds|minute|minutes)").find(lowerText)
        if (forwardMatch != null) {
            val num = forwardMatch.groupValues[1].ifEmpty { forwardMatch.groupValues[3] }.toIntOrNull() ?: 30
            val unit = if (forwardMatch.groupValues[2].contains("minute") || forwardMatch.groupValues[4].contains("minute")) 60 else 1
            return VoiceCommand.SeekForward(num * unit * 1000L)
        }
        
        // Seek backward
        val backwardMatch = Regex("go back|rewind|back ([0-9]+) (second|seconds|minute|minutes)|previous ([0-9]+) (second|seconds|minute|minutes)").find(lowerText)
        if (backwardMatch != null) {
            val num = backwardMatch.groupValues[2].ifEmpty { backwardMatch.groupValues[3] }.toIntOrNull() ?: 10
            val unit = if (backwardMatch.groupValues[3].contains("minute")) 60 else 1
            return VoiceCommand.SeekBackward(num * unit * 1000L)
        }

        // Playback commands
        return when {
            // Play commands
            lowerText.contains("play") || lowerText.contains("start") || lowerText.contains("resume") -> {
                VoiceCommand.Play
            }
            
            // Pause commands
            lowerText.contains("pause") || lowerText.contains("stop") -> {
                VoiceCommand.Pause
            }
            
            // Toggle play/pause
            lowerText.contains("toggle") && lowerText.contains("play") -> {
                VoiceCommand.TogglePlayPause
            }
            
            // Next commands
            lowerText.contains("next") || lowerText.contains("skip") || lowerText.contains("forward") -> {
                VoiceCommand.Next
            }
            
            // Previous commands  
            lowerText.contains("previous") || lowerText.contains("back") || lowerText.contains("last") -> {
                VoiceCommand.Previous
            }
            
            // Shuffle commands
            lowerText.contains("shuffle on") -> {
                VoiceCommand.ShuffleOn
            }
            lowerText.contains("shuffle off") -> {
                VoiceCommand.ShuffleOff
            }
            lowerText.contains("shuffle") -> {
                VoiceCommand.Shuffle
            }
            
            // Repeat commands
            lowerText.contains("repeat one") || lowerText.contains("loop one") -> {
                VoiceCommand.RepeatOne
            }
            lowerText.contains("repeat all") || lowerText.contains("loop all") -> {
                VoiceCommand.RepeatAll
            }
            lowerText.contains("repeat off") || lowerText.contains("loop off") -> {
                VoiceCommand.RepeatOff
            }
            lowerText.contains("repeat") || lowerText.contains("loop") -> {
                VoiceCommand.Repeat
            }
            
            // Volume commands
            lowerText.contains("volume up") || lowerText.contains("louder") || lowerText.contains("increase volume") || lowerText.contains("volume higher") -> {
                VoiceCommand.VolumeUp
            }
            lowerText.contains("volume down") || lowerText.contains("quieter") || lowerText.contains("decrease volume") || lowerText.contains("volume lower") -> {
                VoiceCommand.VolumeDown
            }
            lowerText.contains("mute") || lowerText.contains("silent") -> {
                VoiceCommand.Mute
            }
            lowerText.contains("unmute") -> {
                VoiceCommand.Unmute
            }
            
            // Speed commands
            lowerText.contains("speed up") || lowerText.contains("faster") -> {
                VoiceCommand.SpeedUp
            }
            lowerText.contains("slow down") || lowerText.contains("slower") -> {
                VoiceCommand.SlowDown
            }
            lowerText.contains("normal speed") || lowerText.contains("reset speed") -> {
                VoiceCommand.ResetSpeed
            }
            
            // Settings commands
            lowerText.contains("dark mode on") || lowerText.contains("dark theme on") || lowerText.contains("enable dark mode") -> {
                VoiceCommand.SetDarkMode(true)
            }
            lowerText.contains("dark mode off") || lowerText.contains("dark theme off") || lowerText.contains("disable dark mode") -> {
                VoiceCommand.SetDarkMode(false)
            }
            lowerText.contains("dark mode") || lowerText.contains("dark theme") -> {
                VoiceCommand.SetDarkMode(true)
            }
            lowerText.contains("light mode on") || lowerText.contains("light theme on") || lowerText.contains("enable light mode") -> {
                VoiceCommand.SetDarkMode(false)
            }
            lowerText.contains("light mode off") || lowerText.contains("light theme off") -> {
                VoiceCommand.SetDarkMode(true)
            }
            lowerText.contains("light mode") || lowerText.contains("light theme") -> {
                VoiceCommand.SetDarkMode(false)
            }
            lowerText.contains("toggle theme") || lowerText.contains("switch theme") -> {
                VoiceCommand.ToggleTheme
            }
            
            // Lyrics commands
            lowerText.contains("show lyrics") || lowerText.contains("lyrics on") || lowerText.contains("enable lyrics") -> {
                VoiceCommand.ShowLyrics
            }
            lowerText.contains("hide lyrics") || lowerText.contains("lyrics off") || lowerText.contains("disable lyrics") -> {
                VoiceCommand.HideLyrics
            }
            lowerText.contains("toggle lyrics") -> {
                VoiceCommand.ToggleLyrics
            }
            
            // Video commands
            lowerText.contains("video on") || lowerText.contains("show video") || lowerText.contains("enable video") -> {
                VoiceCommand.EnableVideo
            }
            lowerText.contains("video off") || lowerText.contains("hide video") || lowerText.contains("disable video") -> {
                VoiceCommand.DisableVideo
            }
            lowerText.contains("toggle video") -> {
                VoiceCommand.ToggleVideo
            }
            
            // Like commands
            lowerText.contains("like") || lowerText.contains("favorite") || lowerText.contains("love") -> {
                VoiceCommand.ToggleLike
            }
            
            // Queue commands
            lowerText.contains("show queue") || lowerText.contains("view queue") || lowerText.contains("open queue") -> {
                VoiceCommand.ShowQueue
            }
            lowerText.contains("clear queue") -> {
                VoiceCommand.ClearQueue
            }
            lowerText.contains("add to queue") || lowerText.contains("queue this") -> {
                VoiceCommand.AddToQueue
            }
            
            // Open commands
            lowerText.contains("go home") || lowerText.contains("open home") -> {
                VoiceCommand.OpenHome
            }
            lowerText.contains("go library") || lowerText.contains("open library") -> {
                VoiceCommand.OpenLibrary
            }
            lowerText.contains("go search") || lowerText.contains("open search") -> {
                VoiceCommand.OpenSearch
            }
            lowerText.contains("go settings") || lowerText.contains("open settings") -> {
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
    
    // Unknown
    data class Unknown(val text: String) : VoiceCommand()
}
