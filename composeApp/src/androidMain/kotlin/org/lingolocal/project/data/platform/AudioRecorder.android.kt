package org.lingolocal.project.data.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.lingolocal.project.util.logError
import org.lingolocal.project.util.logInfo
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Cattura audio raw PCM 16-bit 16 kHz mono usando android.media.AudioRecord.
 * Whisper richiede esattamente questo formato. Niente SpeechRecognizer:
 * registriamo finché l'utente tiene premuto il bottone, poi consegnamo
 * il file WAV a WhisperEngine per la trascrizione.
 *
 * Vantaggi rispetto a SpeechRecognizer:
 * - Nessun cutoff dopo 3s di silenzio
 * - Nessuna invenzione lessicale ("frasi prefatte")
 * - Restituisce punteggiatura via Whisper
 * - 100% offline, indipendente da Google App
 */
actual class AudioRecorder actual constructor() : KoinComponent {

    private val context: Context by inject()

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var currentFilePath: String? = null

    @Volatile
    private var isRecording = false

    @Volatile
    private var rmsAmplitude = 0.0f

    private companion object {
        private const val TAG = "AudioRecorderAndroid"
        private const val SAMPLE_RATE = 16_000     // Whisper requires 16 kHz
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        // 4× il minimum per evitare buffer underruns durante lo scheduling.
        private val BUFFER_SIZE_FACTOR = 4
    }

    actual fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    actual suspend fun requestPermission(): Boolean = hasPermission()

    actual suspend fun startRecording(outputFilePath: String, targetLanguage: String) {
        if (!hasPermission()) {
            logError(TAG, "Impossibile registrare: permesso microfono mancante", null)
            return
        }
        if (isRecording) {
            logInfo(TAG, "Stop recording precedente prima di avviare nuovo")
            stopRecording()
        }

        withContext(Dispatchers.IO) {
            currentFilePath = outputFilePath
            rmsAmplitude = 0.0f

            val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            if (minBuf <= 0) {
                logError(TAG, "AudioRecord.getMinBufferSize returned $minBuf", null)
                return@withContext
            }
            val bufferSize = minBuf * BUFFER_SIZE_FACTOR

            val recorder = try {
                AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
            } catch (e: SecurityException) {
                logError(TAG, "AudioRecord constructor security exception", e)
                return@withContext
            } catch (e: IllegalArgumentException) {
                logError(TAG, "AudioRecord constructor IAE", e)
                return@withContext
            }

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                logError(TAG, "AudioRecord not initialized (state=${recorder.state})", null)
                recorder.release()
                return@withContext
            }

            // Apre il file WAV: scriveremo header placeholder + PCM, e a fine
            // recording aggiorneremo l'header con la lunghezza vera.
            val file = File(outputFilePath)
            file.parentFile?.mkdirs()
            val out = DataOutputStream(FileOutputStream(file))
            writeWavHeaderPlaceholder(out)

            audioRecord = recorder
            isRecording = true
            recorder.startRecording()

            val readBuf = ShortArray(bufferSize / 2)
            recordingThread = Thread({
                var totalSamples = 0L
                try {
                    while (isRecording) {
                        val n = recorder.read(readBuf, 0, readBuf.size)
                        if (n <= 0) continue
                        totalSamples += n

                        // Calcola RMS per la UI (visualizer onde).
                        var sumSq = 0.0
                        var peak = 0
                        for (i in 0 until n) {
                            val s = readBuf[i].toInt()
                            sumSq += (s * s).toDouble()
                            val a = abs(s)
                            if (a > peak) peak = a
                        }
                        val rms = sqrt(sumSq / n) / 32768.0
                        rmsAmplitude = (rms.toFloat() * 4f).coerceIn(0f, 1f)

                        // Scrivi i sample come PCM 16-bit little-endian.
                        val bb = ByteBuffer.allocate(n * 2).order(ByteOrder.LITTLE_ENDIAN)
                        for (i in 0 until n) bb.putShort(readBuf[i])
                        out.write(bb.array(), 0, n * 2)
                    }
                } catch (e: Exception) {
                    logError(TAG, "Errore nel loop di registrazione", e)
                } finally {
                    try {
                        out.flush()
                        out.close()
                        // Aggiorna header WAV con dimensioni reali.
                        finalizeWavHeader(file, totalSamples)
                        logInfo(TAG, "Registrazione completata: ${totalSamples} samples (~${"%.1f".format(totalSamples / SAMPLE_RATE.toFloat())}s)")
                    } catch (e: Exception) {
                        logError(TAG, "Errore chiusura file WAV", e)
                    }
                }
            }, "AudioRecorder-PCM").apply { start() }

            logInfo(TAG, "AudioRecord avviato: PCM16 ${SAMPLE_RATE}Hz mono → $outputFilePath")
        }
    }

    /**
     * Trascrizione "nativa" non più disponibile — sempre vuota.
     * La trascrizione vera la fa WhisperEngine sul file WAV.
     */
    actual fun getLastTranscription(): String = ""

    actual suspend fun stopRecording(): ByteArray? {
        if (!isRecording) return null

        isRecording = false

        return withContext(Dispatchers.IO) {
            try {
                val recorder = audioRecord
                recordingThread?.join(2000)
                recordingThread = null

                if (recorder != null) {
                    try {
                        if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                            recorder.stop()
                        }
                    } catch (e: IllegalStateException) {
                        logError(TAG, "AudioRecord.stop() IllegalStateException", e)
                    }
                    recorder.release()
                }
                audioRecord = null

                // Ritorna i bytes del WAV (li consuma WhisperEngine via path).
                val path = currentFilePath
                if (path != null) {
                    val f = File(path)
                    if (f.exists() && f.length() > 44) {
                        f.readBytes()
                    } else {
                        logInfo(TAG, "WAV vuoto o non esistente")
                        null
                    }
                } else null
            } catch (e: Exception) {
                logError(TAG, "Errore stopRecording", e)
                null
            }
        }
    }

    actual fun getAmplitude(): Float = if (isRecording) rmsAmplitude else 0.0f

    actual fun release() {
        try {
            isRecording = false
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            logError(TAG, "Errore release()", e)
        }
    }

    // -----------------------------------------------------------------------
    // WAV header utilities (RIFF/WAVE PCM 16-bit mono)
    // -----------------------------------------------------------------------
    private fun writeWavHeaderPlaceholder(out: DataOutputStream) {
        // Scriviamo 44 byte di header dummy: verranno sovrascritti in finalize.
        val channels = 1
        val bitsPerSample = 16
        val byteRate = SAMPLE_RATE * channels * bitsPerSample / 8
        val placeholder = 0

        // "RIFF" chunk
        out.writeBytes("RIFF")
        out.writeIntLE(placeholder)   // ChunkSize (fixed in finalize)
        out.writeBytes("WAVE")
        // "fmt " sub-chunk
        out.writeBytes("fmt ")
        out.writeIntLE(16)            // Subchunk1Size for PCM
        out.writeShortLE(1)           // AudioFormat = PCM
        out.writeShortLE(channels)
        out.writeIntLE(SAMPLE_RATE)
        out.writeIntLE(byteRate)
        out.writeShortLE(channels * bitsPerSample / 8) // BlockAlign
        out.writeShortLE(bitsPerSample)
        // "data" sub-chunk
        out.writeBytes("data")
        out.writeIntLE(placeholder)   // Subchunk2Size (fixed in finalize)
    }

    private fun finalizeWavHeader(file: File, totalSamples: Long) {
        val dataSize = (totalSamples * 2).toInt() // 16-bit
        val chunkSize = 36 + dataSize
        java.io.RandomAccessFile(file, "rw").use { raf ->
            raf.seek(4)
            raf.write(intToBytesLE(chunkSize))
            raf.seek(40)
            raf.write(intToBytesLE(dataSize))
        }
    }

    private fun DataOutputStream.writeIntLE(v: Int) {
        write(intToBytesLE(v))
    }

    private fun DataOutputStream.writeShortLE(v: Int) {
        write(byteArrayOf((v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte()))
    }

    private fun intToBytesLE(v: Int): ByteArray = byteArrayOf(
        (v and 0xFF).toByte(),
        ((v shr 8) and 0xFF).toByte(),
        ((v shr 16) and 0xFF).toByte(),
        ((v shr 24) and 0xFF).toByte(),
    )
}
