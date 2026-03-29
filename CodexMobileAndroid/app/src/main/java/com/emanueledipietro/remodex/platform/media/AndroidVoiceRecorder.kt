package com.emanueledipietro.remodex.platform.media

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class AndroidVoiceMeteringSnapshot(
    val audioLevels: List<Float> = emptyList(),
    val durationSeconds: Double = 0.0,
)

data class AndroidVoiceRecordingClip(
    val file: File,
    val durationSeconds: Double,
    val byteCount: Int,
)

sealed class AndroidVoiceRecorderException(
    override val message: String,
) : Exception(message) {
    class AlreadyRecording : AndroidVoiceRecorderException(
        "Voice recording is already running.",
    )

    class MicrophonePermissionDenied : AndroidVoiceRecorderException(
        "Microphone access is required for voice transcription.",
    )

    class MissingMicrophoneInput : AndroidVoiceRecorderException(
        "No valid microphone input is available right now.",
    )

    class UnableToPrepareAudioRecorder : AndroidVoiceRecorderException(
        "Unable to prepare the microphone recorder.",
    )

    class UnableToCreateOutputFile : AndroidVoiceRecorderException(
        "Unable to create the temporary audio file.",
    )
}

interface AndroidVoiceRecorder {
    val meteringState: StateFlow<AndroidVoiceMeteringSnapshot>

    suspend fun startRecording()

    suspend fun stopRecording(): AndroidVoiceRecordingClip?

    fun cancelRecording()
}

class DefaultAndroidVoiceRecorder(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AndroidVoiceRecorder {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val meteringStateMutable = MutableStateFlow(AndroidVoiceMeteringSnapshot())
    private val recorderMutex = Mutex()

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private var pcmBuffer = ByteArrayOutputStream()
    private var captureSampleRate: Int = TargetSampleRate
    private var totalSamplesCaptured: Long = 0

    override val meteringState: StateFlow<AndroidVoiceMeteringSnapshot> = meteringStateMutable.asStateFlow()

    @SuppressLint("MissingPermission")
    override suspend fun startRecording() {
        recorderMutex.withLock {
            if (captureJob != null || audioRecord != null) {
                throw AndroidVoiceRecorderException.AlreadyRecording()
            }

            ensureMicrophonePermission()

            val config = resolveAudioConfig()
                ?: throw AndroidVoiceRecorderException.MissingMicrophoneInput()
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                config.sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                config.bufferSize,
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                throw AndroidVoiceRecorderException.UnableToPrepareAudioRecorder()
            }

            captureSampleRate = config.sampleRate
            pcmBuffer = ByteArrayOutputStream()
            totalSamplesCaptured = 0
            meteringStateMutable.value = AndroidVoiceMeteringSnapshot()
            audioRecord = recorder

            recorder.startRecording()
            captureJob = scope.launch {
                captureLoop(
                    recorder = recorder,
                    scratchBuffer = ShortArray(config.bufferSize / BytesPerSample),
                )
            }
        }
    }

    override suspend fun stopRecording(): AndroidVoiceRecordingClip? {
        val localRecorder: AudioRecord
        val localJob: Job
        val sampleRate: Int
        val pcmBytes: ByteArray

        recorderMutex.withLock {
            val recorder = audioRecord ?: return null
            val job = captureJob ?: return null
            localRecorder = recorder
            localJob = job
            sampleRate = captureSampleRate
        }

        stopRecorder(localRecorder)
        localJob.cancelAndJoin()

        recorderMutex.withLock {
            audioRecord = null
            captureJob = null
            pcmBytes = pcmBuffer.toByteArray()
            pcmBuffer = ByteArrayOutputStream()
        }

        releaseRecorder(localRecorder)

        if (pcmBytes.isEmpty()) {
            meteringStateMutable.value = AndroidVoiceMeteringSnapshot()
            return null
        }

        val capturedSamples = decodePcm16(pcmBytes)
        val normalizedSamples = if (sampleRate == TargetSampleRate) {
            capturedSamples
        } else {
            resamplePcm16(
                samples = capturedSamples,
                fromSampleRate = sampleRate,
                toSampleRate = TargetSampleRate,
            )
        }
        if (normalizedSamples.isEmpty()) {
            meteringStateMutable.value = AndroidVoiceMeteringSnapshot()
            return null
        }

        val wavBytes = encodeMonoPcm16Wav(
            samples = normalizedSamples,
            sampleRate = TargetSampleRate,
        )
        val outputFile = withContext(ioDispatcher) {
            runCatching {
                File.createTempFile("remodex-voice-", ".wav", context.cacheDir).apply {
                    writeBytes(wavBytes)
                }
            }.getOrElse {
                throw AndroidVoiceRecorderException.UnableToCreateOutputFile()
            }
        }

        meteringStateMutable.value = AndroidVoiceMeteringSnapshot()
        return AndroidVoiceRecordingClip(
            file = outputFile,
            durationSeconds = normalizedSamples.size.toDouble() / TargetSampleRate.toDouble(),
            byteCount = wavBytes.size,
        )
    }

    override fun cancelRecording() {
        scope.launch {
            val localRecorder: AudioRecord?
            val localJob: Job?
            recorderMutex.withLock {
                localRecorder = audioRecord
                localJob = captureJob
                audioRecord = null
                captureJob = null
                pcmBuffer = ByteArrayOutputStream()
                totalSamplesCaptured = 0
            }

            localRecorder?.let(::stopRecorder)
            localJob?.cancelAndJoin()
            localRecorder?.let(::releaseRecorder)
            meteringStateMutable.value = AndroidVoiceMeteringSnapshot()
        }
    }

    private suspend fun captureLoop(
        recorder: AudioRecord,
        scratchBuffer: ShortArray,
    ) {
        while (true) {
            val readCount = recorder.read(scratchBuffer, 0, scratchBuffer.size)
            if (readCount <= 0) {
                break
            }

            recorderMutex.withLock {
                for (index in 0 until readCount) {
                    val sample = scratchBuffer[index].toInt()
                    pcmBuffer.write(sample and 0xFF)
                    pcmBuffer.write(sample shr 8 and 0xFF)
                }
                totalSamplesCaptured += readCount.toLong()
                meteringStateMutable.value = AndroidVoiceMeteringSnapshot(
                    audioLevels = appendMeterLevel(
                        current = meteringStateMutable.value.audioLevels,
                        level = normalizedLevel(scratchBuffer, readCount),
                    ),
                    durationSeconds = totalSamplesCaptured.toDouble() / captureSampleRate.toDouble(),
                )
            }
        }
    }

    private fun ensureMicrophonePermission() {
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            throw AndroidVoiceRecorderException.MicrophonePermissionDenied()
        }
    }

    private fun resolveAudioConfig(): AudioConfig? {
        return CandidateSampleRates.firstNotNullOfOrNull { sampleRate ->
            val minBufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            if (minBufferSize <= 0) {
                null
            } else {
                AudioConfig(
                    sampleRate = sampleRate,
                    bufferSize = max(minBufferSize, sampleRate / 5),
                )
            }
        }
    }

    private fun normalizedLevel(
        samples: ShortArray,
        count: Int,
    ): Float {
        if (count <= 0) {
            return 0f
        }

        var sumOfSquares = 0.0
        for (index in 0 until count) {
            val normalized = samples[index] / Short.MAX_VALUE.toFloat()
            sumOfSquares += normalized * normalized
        }
        val rms = sqrt(sumOfSquares / count.toDouble())
        val db = 20.0 * log10(max(rms, 1e-6))
        return ((db + 50.0) / 50.0).toFloat().coerceIn(0f, 1f)
    }

    private fun appendMeterLevel(
        current: List<Float>,
        level: Float,
    ): List<Float> {
        if (current.size < MaxAudioLevels) {
            return current + level
        }
        return buildList(MaxAudioLevels) {
            addAll(current.drop(current.size - MaxAudioLevels + 1))
            add(level)
        }
    }

    private fun stopRecorder(recorder: AudioRecord) {
        runCatching {
            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop()
            }
        }
    }

    private fun releaseRecorder(recorder: AudioRecord) {
        runCatching { recorder.release() }
    }

    private data class AudioConfig(
        val sampleRate: Int,
        val bufferSize: Int,
    )

    companion object {
        private const val BytesPerSample = 2
        private const val TargetSampleRate = 24_000
        private const val MaxAudioLevels = 240
        private val CandidateSampleRates = listOf(24_000, 16_000, 44_100, 48_000)
    }
}

internal fun decodePcm16(bytes: ByteArray): ShortArray {
    val sampleCount = bytes.size / 2
    val samples = ShortArray(sampleCount)
    for (index in 0 until sampleCount) {
        val lo = bytes[index * 2].toInt() and 0xFF
        val hi = bytes[index * 2 + 1].toInt()
        samples[index] = ((hi shl 8) or lo).toShort()
    }
    return samples
}

internal fun resamplePcm16(
    samples: ShortArray,
    fromSampleRate: Int,
    toSampleRate: Int,
): ShortArray {
    if (samples.isEmpty() || fromSampleRate <= 0 || toSampleRate <= 0) {
        return ShortArray(0)
    }
    if (fromSampleRate == toSampleRate) {
        return samples.copyOf()
    }

    val ratio = toSampleRate.toDouble() / fromSampleRate.toDouble()
    val outputSize = max(1, (samples.size * ratio).roundToInt())
    return ShortArray(outputSize) { index ->
        val sourceIndex = index / ratio
        val baseIndex = sourceIndex.toInt().coerceIn(0, samples.lastIndex)
        val nextIndex = min(baseIndex + 1, samples.lastIndex)
        val fraction = (sourceIndex - baseIndex).toFloat()
        val interpolated = samples[baseIndex] + (samples[nextIndex] - samples[baseIndex]) * fraction
        interpolated.roundToInt().toShort()
    }
}

internal fun encodeMonoPcm16Wav(
    samples: ShortArray,
    sampleRate: Int,
): ByteArray {
    val dataSize = samples.size * 2
    val output = ByteArrayOutputStream(44 + dataSize)

    output.write("RIFF".toByteArray())
    writeLittleEndianInt(output, 36 + dataSize)
    output.write("WAVE".toByteArray())
    output.write("fmt ".toByteArray())
    writeLittleEndianInt(output, 16)
    writeLittleEndianShort(output, 1)
    writeLittleEndianShort(output, 1)
    writeLittleEndianInt(output, sampleRate)
    writeLittleEndianInt(output, sampleRate * 2)
    writeLittleEndianShort(output, 2)
    writeLittleEndianShort(output, 16)
    output.write("data".toByteArray())
    writeLittleEndianInt(output, dataSize)
    samples.forEach { sample ->
        writeLittleEndianShort(output, sample)
    }
    return output.toByteArray()
}

private fun writeLittleEndianInt(
    output: ByteArrayOutputStream,
    value: Int,
) {
    output.write(value and 0xFF)
    output.write(value shr 8 and 0xFF)
    output.write(value shr 16 and 0xFF)
    output.write(value shr 24 and 0xFF)
}

private fun writeLittleEndianShort(
    output: ByteArrayOutputStream,
    value: Short,
) {
    output.write(value.toInt() and 0xFF)
    output.write(value.toInt() shr 8 and 0xFF)
}
