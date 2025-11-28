package com.bismillah.tesfft.soal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bismillah.tesfft.FFTUtils
import com.bismillah.tesfft.StringUtils
import com.bismillah.tesfft.databinding.ActivitySoalTema1Binding
import com.google.firebase.database.*
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class SoalTema1Activity : AppCompatActivity(), RecognitionListener {
    private lateinit var binding: ActivitySoalTema1Binding
    private lateinit var databaseRef: DatabaseReference
    private val soalList = mutableListOf<Soal>()
    private var currentIndex = 0
    private var currentSoal: String = ""

    private var score = 0
    private var answeredCount = 0
    private var answeredCurrent = false
    private var totalQuestions = 0
    private var userId: String? = null

    companion object {
        const val REQUEST_PERM = 1001
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private lateinit var rawFile: File
    private lateinit var wavFile: File
    private var recordingThread: Thread? = null
    private var mediaPlayer: MediaPlayer? = null

    private val handler = Handler(Looper.getMainLooper())
    private var startTime = 0L
    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsed = System.currentTimeMillis() - startTime
            val secs = (elapsed / 1000) % 60
            val mins = (elapsed / (1000 * 60)) % 60
            val hrs = (elapsed / (1000 * 60 * 60))
            binding.tvTimer.text = String.format("%02d:%02d:%02d", hrs, mins, secs)
            handler.postDelayed(this, 500)
        }
    }

    // --- Speech Recognizer for Japanese Pronunciation ---
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recIntent: Intent
    private lateinit var voskModel: Model

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySoalTema1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getStringExtra("userId")
        Log.d("SoalTema1", "Received userId = $userId")

        val targetPath = "${filesDir.absolutePath}/model-small-ja"

        File(targetPath).apply { if (!exists()) mkdirs() }

        databaseRef = FirebaseDatabase.getInstance(
            "https://adminsuarajepangku-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("soal/percakapan_seharihari")

        ambilDataSoal()

        binding.btnPlaySoal.setOnClickListener{
            val url = soalList[currentIndex].audioUrl
            if (!url.isNullOrEmpty()) {
                playAudio(url)
            } else {
                Toast.makeText(this, "Audio tidak tersedia!", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnNextSoal.setOnClickListener {
            // 1. Reset state rekaman apa pun yang masih jalan
            if (isRecording) {
                stopRecording()
            }
            handler.removeCallbacks(timerRunnable)

            // 2. Reset UI ke mode sebelum rekam
            binding.tvStatus.text = "Ready to Record"
            binding.tvTimer.text = "00:00:00"
            binding.tvResult.text = ""
            binding.btnRecord.visibility = View.VISIBLE
            binding.btnRecord.isEnabled = true
            binding.btnStop.visibility = View.GONE
            binding.postRecordingControls.visibility = View.GONE

            // 3. Tampilkan soal berikutnya
            tampilkanSoalBerikutnya()
        }

        binding.btnRecord.isEnabled = false
        binding.tvStatus.text = "Loading model…"

        StorageService.unpack(
            this,
            "model-small-ja",  // kalau kamu taruh zip di assets
            targetPath,
            object : StorageService.Callback<Model> {
                override fun onComplete(model: Model) {
                    voskModel = model
                    runOnUiThread {
                        binding.tvStatus.text = "Model siap!"
                        binding.btnRecord.isEnabled = true  // baru enable di sini
                    }
                }
            },
            object : StorageService.Callback<IOException> {
                override fun onComplete(e: IOException) {
                    runOnUiThread {
                        binding.tvStatus.text = "Unpack error: ${e.message}"
                    }
                }
            }
        )
        binding.btnRecord.setOnClickListener {
            if (!isRecording) {
                if (checkPermissions()) startRecording()
            } else {
                stopRecording()
                // Setelah WAV tersimpan, langsung cek pengucapan
                recognizePronunciation()
            }
        }

        binding.btnStop.setOnClickListener {
            stopRecording()
            recognizePronunciation()}
        binding.btnSave.setOnClickListener { saveRecording() }
        binding.btnPlay.setOnClickListener { playRecording() }
        binding.btnPausePlayback.setOnClickListener { pausePlayback() }

        // --- Setup SpeechRecognizer ---
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(this@SoalTema1Activity)
        }
        recIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).also {
            it.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            it.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
            it.putExtra(RecognizerIntent.EXTRA_PROMPT, "Ucapkan: $currentSoal")
        }
    }

    private fun checkPermissions(): Boolean {
        val perms = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        return if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(),
                SoalTema1Activity.REQUEST_PERM
            )
            false
        } else true
    }

    override fun onRequestPermissionsResult(requestCode: Int, perms: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, perms, grantResults)
        if (requestCode == SoalTema1Activity.REQUEST_PERM && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startRecording()
        } else {
            binding.tvStatus.text = "Permission denied!"
        }
    }

    // --- Recording + FFT Noise Reduction ---
    private fun startRecording() {
        if (!checkPermissions()) return

        rawFile = File(externalCacheDir, "temp_audio.pcm")
        wavFile = File(externalCacheDir, "recording_${System.currentTimeMillis()}.wav")
        if (rawFile.exists()) rawFile.delete()

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SoalTema1Activity.SAMPLE_RATE, SoalTema1Activity.CHANNEL_CONFIG, SoalTema1Activity.AUDIO_FORMAT,
                AudioRecord.getMinBufferSize(SoalTema1Activity.SAMPLE_RATE, SoalTema1Activity.CHANNEL_CONFIG, SoalTema1Activity.AUDIO_FORMAT)
            ).also { it.startRecording() }
        } catch (se: SecurityException) {
            binding.tvStatus.text = "Gagal mulai rekaman: izin ditolak!"
            return
        }

        isRecording = true
        binding.tvStatus.text = "Recording..."
        binding.btnRecord.visibility = View.GONE
        binding.btnStop.visibility = View.VISIBLE
        binding.postRecordingControls.visibility = View.GONE

        startTime = System.currentTimeMillis()
        handler.post(timerRunnable)

        recordingThread = Thread {
            FileOutputStream(rawFile).use { os ->
                val buffer = ByteArray(4096)
                while (isRecording) {
                    val read = audioRecord!!.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        // **FFT Noise Reduction**
                        val processed = FFTUtils.applyNoiseReduction(buffer.copyOf(read))
                        os.write(processed, 0, processed.size)
                    }
                }
            }
        }.also { it.start() }
    }

    private fun stopRecording() {
        isRecording = false
        audioRecord?.apply { stop(); release() }
        audioRecord = null
        recordingThread = null

        handler.removeCallbacks(timerRunnable)
        binding.tvStatus.text = "Recording stopped"
        binding.btnStop.visibility = View.GONE
        binding.postRecordingControls.visibility = View.VISIBLE

        rawToWav()
    }

    private fun rawToWav() {
        val pcmSize = rawFile.length().toInt()
        val header = ByteArray(44)
        val totalDataLen = pcmSize + 36
        val byteRate = SoalTema1Activity.SAMPLE_RATE * 2

        // RIFF header
        System.arraycopy("RIFF".toByteArray(), 0, header, 0, 4)
        writeInt(header, 4, totalDataLen)
        System.arraycopy("WAVE".toByteArray(), 0, header, 8, 4)
        System.arraycopy("fmt ".toByteArray(), 0, header, 12, 4)
        writeInt(header, 16, 16)
        writeShort(header, 20, 1.toShort())
        writeShort(header, 22, 1.toShort())
        writeInt(header, 24, SoalTema1Activity.SAMPLE_RATE)
        writeInt(header, 28, byteRate)
        writeShort(header, 32, 2.toShort())
        writeShort(header, 34, 16.toShort())
        System.arraycopy("data".toByteArray(), 0, header, 36, 4)
        writeInt(header, 40, pcmSize)

        FileInputStream(rawFile).use { fis ->
            FileOutputStream(wavFile).use { fos ->
                fos.write(header)
                fis.copyTo(fos)
            }
        }
        binding.tvStatus.text = "Saved WAV: ${wavFile.name}"
    }

    private fun writeInt(buf: ByteArray, pos: Int, value: Int) {
        buf[pos] = (value and 0xff).toByte()
        buf[pos + 1] = ((value shr 8) and 0xff).toByte()
        buf[pos + 2] = ((value shr 16) and 0xff).toByte()
        buf[pos + 3] = ((value shr 24) and 0xff).toByte()
    }

    private fun writeShort(buf: ByteArray, pos: Int, value: Short) {
        buf[pos] = (value.toInt() and 0xff).toByte()
        buf[pos + 1] = ((value.toInt() shr 8) and 0xff).toByte()
    }

    // --- Playback ---
    private fun playRecording() {
        if (!wavFile.exists()) return
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(wavFile.absolutePath)
            prepare()
            start()
            setOnCompletionListener { mp ->
                binding.tvStatus.text = "Playback completed"
                binding.btnPausePlayback.visibility = View.GONE
                binding.btnPlay.visibility = View.VISIBLE
                mp.seekTo(0)
            }
        }
        binding.tvStatus.text = "Playing..."
        binding.btnPlay.visibility = View.GONE
        binding.btnPausePlayback.visibility = View.VISIBLE
    }

    private fun recognizePronunciation() {
        if (!::voskModel.isInitialized) {
            binding.tvResult.text = "Model belum siap, tunggu sebentar…"
            return
        }
        binding.tvResult.text = "Recognizing…"
        Thread {
            val recognizer = Recognizer(voskModel, SoalTema1Activity.SAMPLE_RATE.toFloat())
            FileInputStream(wavFile).use { fis ->
                val buf = ByteArray(4096)
                while (true) {
                    val read = fis.read(buf)
                    if (read <= 0) break
                    recognizer.acceptWaveForm(buf, read)
                }
            }
            val hyp = recognizer.result
            val spoken = Regex("""\"text\"\s*:\s*"([^"]*)"""")
                .find(hyp)?.groupValues?.get(1) ?: ""
            runOnUiThread {
                if (!answeredCurrent) {
                    answeredCurrent = true
                    answeredCount++

                    if (spoken == currentSoal) {
                        score += 10
                    }
                }

                // Tampilkan hasil untuk soal ini
                binding.tvResult.text =
                    if (spoken == currentSoal) "✅ Terbaca: $spoken\nBenar!"
                    else "❌ Terbaca: $spoken"

                // Jika semua soal sudah dijawab, panggil endTest()
                if (answeredCount >= totalQuestions) {
                    endTest()
                }
            }

        }.start()
    }

    private fun pausePlayback() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                binding.tvStatus.text = "Paused"
                binding.btnPausePlayback.visibility = View.GONE
                binding.btnPlay.visibility = View.VISIBLE
            }
        }
    }

    private fun saveRecording() {
        binding.tvStatus.text = "Recording ready: ${wavFile.absolutePath}"
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO), SoalTema1Activity.REQUEST_PERM
            )
            return
        }
        binding.tvResult.text = "Listening..."
        speechRecognizer.startListening(recIntent)
    }

    // RecognitionListener callbacks
    override fun onResults(results: Bundle) {
        val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val heard = matches?.firstOrNull() ?: ""
        val sim = StringUtils.similarity(heard, currentSoal)
        binding.tvResult.text = if (heard == currentSoal) {
            "✅ Terbaca: $heard\nJawaban benar!"
        } else {
            "❌ Terbaca: $heard\nSimilarity: $sim%"
        }
    }
    override fun onError(error: Int) {
        binding.tvResult.text = "Error listening: $error"
    }
    // Unused overrides
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        speechRecognizer.destroy()
    }

    private fun ambilDataSoal() {
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                soalList.clear()
                snap.children.mapNotNull { it.getValue(Soal::class.java) }
                    .also { soalList.addAll(it) }

                totalQuestions = soalList.size

                if (totalQuestions > 0) {
                    binding.btnNextSoal.isEnabled = true
                    tampilkanSoal(0)
                } else {
                    binding.tvSoal.text = "Tidak ada soal"
                    binding.tvSoalIndo.text = "-"
                }
            }
            override fun onCancelled(err: DatabaseError) {
                Toast.makeText(this@SoalTema1Activity,
                    "Gagal ambil soal", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun tampilkanSoal(index: Int) {
        currentIndex = index
        val s = soalList[index]
        currentSoal = s.japanese ?: ""
        binding.tvSoal.text = s.japanese
        binding.tvSoalIndo.text = s.indonesian

        // Reset state jawaban untuk soal ini
        answeredCurrent = false

        // Reset tampilan pengenalan & timer
        binding.tvResult.text = ""
        binding.tvStatus.text = "Ready to Record"
        binding.tvTimer.text = "00:00:00"
        binding.btnRecord.visibility = View.VISIBLE
        binding.btnRecord.isEnabled = true
        binding.btnStop.visibility = View.GONE
        binding.postRecordingControls.visibility = View.GONE

        // Setup recognizer prompt agar selalu up-to-date
        recIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).also {
            it.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
            it.putExtra(RecognizerIntent.EXTRA_PROMPT, "Ucapkan: $currentSoal")
        }
    }

    private fun playAudio(audioUrl: String) {
        val mediaPlayer = MediaPlayer().apply {
            setDataSource(audioUrl)
            prepare()
            start()
        }
    }

    private fun tampilkanSoalBerikutnya() {
        if (soalList.isEmpty()) return

        currentIndex = (currentIndex + 1) % soalList.size
        tampilkanSoal(currentIndex)
    }

    private fun endTest() {
        // Matikan tombol rekam & Next
        binding.btnRecord.isEnabled = false
        binding.btnNextSoal.isEnabled = false

        // Tampilkan dialog hasil
        AlertDialog.Builder(this)
            .setTitle("Tes Selesai")
            .setMessage("Skor Anda: $score / ${totalQuestions * 10}")
            .setPositiveButton("OK") { _, _ ->
                // Simpan skor dan tanggal ke Firebase user
                saveScoreToUser()
                finish() // kembali ke MainActivity atau mana pun
            }
            .setCancelable(false)
            .show()
    }

    private fun saveScoreToUser() {
        val uid = userId ?: return
        val dbUser = FirebaseDatabase.getInstance(
            "https://adminsuarajepangku-default-rtdb.asia-southeast1.firebasedatabase.app"
        ).getReference("users/$uid")

        val tanggalSekarang = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date())

        val updateMap = mapOf(
            "skor" to score,
            "lastTestDate" to tanggalSekarang
        )
        dbUser.updateChildren(updateMap).addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(this, "Skor berhasil disimpan", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Gagal simpan skor", Toast.LENGTH_SHORT).show()
            }
        }
    }
}