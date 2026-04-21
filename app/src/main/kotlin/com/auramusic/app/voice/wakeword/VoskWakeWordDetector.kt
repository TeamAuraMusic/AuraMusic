package com.auramusic.app.voice.wakeword

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.vosk.Model
import org.vosk.Recognizer
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoskWakeWordDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) : WakeWordDetector {

    private var audioRecord: AudioRecord? = null
    private var detectionJob: Job? = null
    private var recognizer: Recognizer? = null
    private var model: Model? = null
    private val isRunning = AtomicBoolean(false)
    private var wakeWordCallback: (() -> Unit)? = null
    private var progressCallback: ((progress: Int, bytesRead: Long, totalBytes: Long) -> Unit)? = null
    private var lastWakeWordTime = 0L
    private val mainHandler = Handler(Looper.getMainLooper())
    private var echoCanceler: AcousticEchoCanceler? = null
    
    private val WAKE_WORD_COOLDOWN_MS = 2000

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val BUFFER_SIZE = 4096
        private const val MODEL_NAME = "vosk-model-small-en-us-0.15"
        private const val MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
        private const val WAKE_WORD = "aura"
        private const val WAKE_WORD_GRAMMAR = "[\"hey aura\", \"hello aura\", \"ok aura\", \"[unk]\"]"
    }

    private fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun setOnWakeWordDetectedListener(callback: () -> Unit) {
        this.wakeWordCallback = callback
    }

    override fun setOnProgressListener(callback: (progress: Int, bytesRead: Long, totalBytes: Long) -> Unit) {
        this.progressCallback = callback
    }

    override fun start() {
        if (isRunning.getAndSet(true)) return

        detectionJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val modelPath = ensureModel()
                android.util.Log.d("VoskWakeWordDetector", "Loading model from: $modelPath")
                
                validateModelDirectory(File(modelPath))
                
                model = Model(modelPath)
                recognizer = Recognizer(model, SAMPLE_RATE.toFloat(), WAKE_WORD_GRAMMAR)
                android.util.Log.d("VoskWakeWordDetector", "Model loaded with grammar, starting audio")
                
                startAudioRecording()
            } catch (e: Exception) {
                android.util.Log.e("VoskWakeWordDetector", "Failed to start", e)
                withContext(Dispatchers.Main) {
                    showToast("Wake word failed: ${e.message}")
                }
                isRunning.set(false)
            }
        }
    }

    private fun hasModelFilesAtRoot(dir: File): Boolean {
        val requiredDirs = listOf("am", "conf", "graph")
        return requiredDirs.all { fileName -> File(dir, fileName).isDirectory }
    }

    private suspend fun ensureModel(): String = withContext(Dispatchers.IO) {
        val modelDir = File(context.filesDir, MODEL_NAME)
        if (modelDir.exists()) {
            try {
                validateModelDirectory(modelDir)
                return@withContext modelDir.absolutePath
            } catch (e: Exception) {
                android.util.Log.w("VoskWakeWordDetector", "Existing model invalid, re-downloading: ${e.message}")
                modelDir.deleteRecursively()
            }
        }

        val zipFile = File(context.cacheDir, "$MODEL_NAME.zip")
        val filesDir = context.filesDir
        
        try {
            downloadModelWithProgress(zipFile) { progress, bytesRead, totalBytes ->
                progressCallback?.invoke(progress, bytesRead, totalBytes)
            }
            
            withContext(Dispatchers.Main) {
                showToast("Unpacking wake word model...")
            }
            
            unzip(zipFile, filesDir)
            zipFile.delete()

            // Check if model files ended up in expected directory
            val extractedDir = findExtractedModelDir(filesDir)
            when {
                extractedDir != null && extractedDir != modelDir -> {
                    android.util.Log.d("VoskWakeWordDetector", "Renaming ${extractedDir.name} to $MODEL_NAME")
                    extractedDir.renameTo(modelDir)
                }
                modelDir.exists() -> {
                    // Already in correct location
                }
                hasModelFilesAtRoot(filesDir) -> {
                    android.util.Log.d("VoskWakeWordDetector", "Moving model files from root to $MODEL_NAME directory")
                    modelDir.mkdirs()
                    val requiredDirs = listOf("am", "conf", "graph")
                    requiredDirs.forEach { dirName ->
                        val source = File(filesDir, dirName)
                        if (source.exists()) {
                            val dest = File(modelDir, dirName)
                            source.renameTo(dest)
                        }
                    }
                }
            }
            
            if (!modelDir.exists()) {
                val listing = filesDir.list()?.joinToString() ?: "empty"
                throw Exception("Model directory not found after unzip. Expected: $MODEL_NAME. Files in app dir: $listing")
            }
            
            validateModelDirectory(modelDir)
        } catch (e: Exception) {
            if (zipFile.exists()) zipFile.delete()
            if (modelDir.exists()) modelDir.deleteRecursively()
            throw e
        }
        
        return@withContext modelDir.absolutePath
    }
    
    private fun validateModelDirectory(modelDir: File) {
        if (!modelDir.isDirectory) {
            throw Exception("Model path is not a directory: ${modelDir.absolutePath}")
        }
        
        // VOSK requires these subdirectories
        val requiredDirs = listOf("am", "conf", "graph")
        val missingDirs = requiredDirs.filter { !File(modelDir, it).isDirectory }
        
        if (missingDirs.isNotEmpty()) {
            val contents = modelDir.list()?.joinToString() ?: "empty"
            android.util.Log.e("VoskWakeWordDetector", "Model directory contents: $contents")
            throw Exception("Model missing required directories: ${missingDirs.joinToString()}. Found: ${modelDir.list()?.joinToString()}")
        }
        
        // Verify directories have content (at least one file each)
        requiredDirs.forEach { dirName ->
            val dir = File(modelDir, dirName)
            if (dir.listFiles()?.isEmpty() == true) {
                throw Exception("Model directory '$dirName' is empty")
            }
        }
        
        android.util.Log.d("VoskWakeWordDetector", "Model validated: ${modelDir.absolutePath}, dirs: ${requiredDirs.joinToString()}")
    }
    
    private fun findExtractedModelDir(parentDir: File): File? {
        // First check for expected model directory name
        val expectedDir = File(parentDir, MODEL_NAME)
        if (expectedDir.exists() && expectedDir.isDirectory) {
            return expectedDir
        }
        
        // Check for any vosk-model-* directory
        return parentDir.listFiles { file ->
            file.isDirectory && file.name.startsWith("vosk-model")
        }?.firstOrNull()
    }

    private suspend fun downloadModelWithProgress(
        outputFile: File,
        onProgress: (progress: Int, bytesRead: Long, totalBytes: Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.MINUTES)
            .build()
        
        val request = Request.Builder()
            .url(MODEL_URL)
            .header("User-Agent", "AuraMusic/1.0")
            .build()
        
        val startTime = System.currentTimeMillis()
        var lastLogTime = 0L
        
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Download failed: HTTP ${response.code} ${response.message}")
                }
                
                val body = response.body
                    ?: throw Exception("Download failed: empty response")
                
                val totalBytes = body.contentLength()
                if (totalBytes <= 0) {
                    throw Exception("Invalid content length: $totalBytes")
                }
                
                val inputStream = BufferedInputStream(body.byteStream())
                val outputStream = FileOutputStream(outputFile)
                
                val buffer = ByteArray(8192)
                var bytesRead: Long = 0
                var lastProgress = 0
                
                try {
                    var read: Int
                    var iterations = 0
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        if (!isRunning.get()) {
                            throw Exception("Download cancelled")
                        }
                        outputStream.write(buffer, 0, read)
                        bytesRead += read
                        iterations++
                        
                        // Report progress every 1%
                        val progress = ((bytesRead * 100) / totalBytes).toInt()
                        if (progress > lastProgress) {
                            lastProgress = progress
                            onProgress(progress, bytesRead, totalBytes)
                        }
                        
                        // Periodic speed logging every 5 seconds
                        val now = System.currentTimeMillis()
                        if (now - lastLogTime > 5000) {
                            val speed = bytesRead / (now - startTime) * 1000
                            android.util.Log.d("VoskWakeWordDetector", 
                                "Download progress: $progress% ($bytesRead/$totalBytes bytes, ${speed/1024} KB/s)")
                            lastLogTime = now
                        }
                    }
                    outputStream.flush()
                    
                    // Verify we received the full file
                    if (bytesRead != totalBytes) {
                        throw Exception("Download incomplete: expected $totalBytes bytes, got $bytesRead")
                    }
                    
                    android.util.Log.d("VoskWakeWordDetector", 
                        "Download complete: ${bytesRead} bytes in ${System.currentTimeMillis() - startTime}ms")
                } finally {
                    inputStream.close()
                    outputStream.close()
                }
            }
        } catch (e: Exception) {
            throw Exception("Download error: ${e.message}", e)
        }
    }

    private fun unzip(zipFile: File, destDir: File) {
        val totalEntries = countZipEntries(zipFile)
        var extractedEntries = 0
        
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(destDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
                extractedEntries++
            }
        }
        
        android.util.Log.d("VoskWakeWordDetector", "Extracted $extractedEntries entries")
    }
    
    private fun countZipEntries(zipFile: File): Int {
        var count = 0
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            while (zis.nextEntry != null) {
                count++
            }
        }
        return count
    }

    override fun stop() {
        isRunning.set(false)

        detectionJob?.cancel()
        detectionJob = null

        try {
            echoCanceler?.release()
            echoCanceler = null
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            android.util.Log.e("VoskWakeWordDetector", "Error stopping audio", e)
        }

        try {
            recognizer?.close()
            recognizer = null
            model?.close()
            model = null
        } catch (e: Exception) {
            android.util.Log.e("VoskWakeWordDetector", "Error stopping model", e)
        }
    }

    override fun close() = stop()

    private suspend fun startAudioRecording() {
        val buffer = ShortArray(BUFFER_SIZE)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                android.util.Log.e("VoskWakeWordDetector", "AudioRecord not initialized")
                isRunning.set(false)
                return
            }

            // Enable AEC to cancel the device's own audio output from the mic input
            // (same approach as Google Assistant / Siri — let the model handle detection)
            val sessionId = audioRecord!!.audioSessionId
            if (AcousticEchoCanceler.isAvailable()) {
                echoCanceler = AcousticEchoCanceler.create(sessionId)?.apply { enabled = true }
                android.util.Log.d("VoskWakeWordDetector", "AEC enabled")
            }

            audioRecord?.startRecording()
            android.util.Log.d("VoskWakeWordDetector", "Recording started")

            while (isRunning.get()) {
                val read = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: -1
                if (read > 0) {
                    try {
                        val isFinal = recognizer?.acceptWaveForm(buffer, read)

                        // Only check final results to avoid false positives from partial hypotheses
                        if (isFinal == true) {
                            val finalJson = recognizer?.result ?: ""
                            // Require an exact phrase match from the grammar (hey aura / hello aura / ok aura)
                            val textMatch = Regex("\"text\"\\s*:\\s*\"([^\"]+)\"").find(finalJson)?.groupValues?.get(1)?.trim() ?: ""
                            val isWakePhrase = textMatch == "hey aura" || textMatch == "hello aura" || textMatch == "ok aura"
                            if (isWakePhrase) {
                                android.util.Log.d("VoskWakeWordDetector", "DETECTED in final: $finalJson")
                                
                                val now = System.currentTimeMillis()
                                if (now - lastWakeWordTime < WAKE_WORD_COOLDOWN_MS) {
                                    android.util.Log.d("VoskWakeWordDetector", "Ignoring wake word (cooldown)")
                                    continue
                                }
                                lastWakeWordTime = now
                                
                                // Reset recognizer to prevent lattice state issues
                                try {
                                    recognizer?.close()
                                    model?.let { currentModel ->
                                        recognizer = Recognizer(currentModel, SAMPLE_RATE.toFloat(), WAKE_WORD_GRAMMAR)
                                        android.util.Log.d("VoskWakeWordDetector", "Recognizer reset after detection")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("VoskWakeWordDetector", "Failed to reset recognizer", e)
                                }
                                
                                withContext(Dispatchers.Main) {
                                    showToast("Wake word detected!")
                                }
                                triggerWakeWord()
                                continue
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("VoskWakeWordDetector", "Processing error", e)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("VoskWakeWordDetector", "Audio recording failed", e)
        } finally {
            echoCanceler?.release()
            echoCanceler = null
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        }
    }

    private suspend fun triggerWakeWord() {
        android.util.Log.d("VoskWakeWordDetector", "triggerWakeWord: callbackNull=${wakeWordCallback == null}")
        isRunning.set(false)
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            android.util.Log.e("VoskWakeWordDetector", "Error releasing audio on trigger", e)
        }

        withContext(Dispatchers.Main) {
            wakeWordCallback?.invoke()
        }
    }
}
