 package com.bismillah.tesfft

 import android.Manifest
 import android.content.Intent
 import android.content.pm.PackageManager
 import android.media.*
 import android.os.*
 import android.speech.RecognitionListener
 import android.speech.RecognizerIntent
 import android.speech.SpeechRecognizer
 import android.view.View
 import androidx.appcompat.app.AppCompatActivity
 import androidx.core.app.ActivityCompat
 import androidx.core.content.ContextCompat
 import com.bismillah.tesfft.databinding.ActivityMainBinding
 import org.vosk.Model
 import org.vosk.Recognizer
 import org.vosk.android.StorageService
 import java.io.*

 class MainActivity : AppCompatActivity(), RecognitionListener {
     private lateinit var binding: ActivityMainBinding

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
     private val soalList = listOf("りんご", "ねこ", "いぬ", "こんにちは")
     private var currentSoal = ""
     private lateinit var voskModel: Model

     override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)
         binding = ActivityMainBinding.inflate(layoutInflater)
         setContentView(binding.root)

         // di onCreate, sebelum panggil StorageService.unpack:
         val targetPath = "${filesDir.absolutePath}/model-small-ja"

         File(targetPath).apply { if (!exists()) mkdirs() }

//         val uuidFile = File(targetPath, "uuid")
//         if (!uuidFile.exists()) {
//             uuidFile.writeText(UUID.randomUUID().toString())
//         }

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
             setRecognitionListener(this@MainActivity)
         }
         recIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).also {
             it.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                 RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
             it.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
             it.putExtra(RecognizerIntent.EXTRA_PROMPT, "Ucapkan: $currentSoal")
         }

         binding.btnNextSoal.setOnClickListener { nextSoal() }

         nextSoal()
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
             ActivityCompat.requestPermissions(this, needed.toTypedArray(), REQUEST_PERM)
             false
         } else true
     }

     override fun onRequestPermissionsResult(requestCode: Int, perms: Array<out String>, grantResults: IntArray) {
         super.onRequestPermissionsResult(requestCode, perms, grantResults)
         if (requestCode == REQUEST_PERM && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
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
                 SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT,
                 AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
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
         val byteRate = SAMPLE_RATE * 2

         // RIFF header
         System.arraycopy("RIFF".toByteArray(), 0, header, 0, 4)
         writeInt(header, 4, totalDataLen)
         System.arraycopy("WAVE".toByteArray(), 0, header, 8, 4)
         System.arraycopy("fmt ".toByteArray(), 0, header, 12, 4)
         writeInt(header, 16, 16)
         writeShort(header, 20, 1.toShort())
         writeShort(header, 22, 1.toShort())
         writeInt(header, 24, SAMPLE_RATE)
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
             val recognizer = Recognizer(voskModel, SAMPLE_RATE.toFloat())
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
                 binding.tvResult.text = if (spoken == currentSoal) {
                     "✅ Terbaca: $spoken\nBenar!"
                 } else {
                     "❌ Terbaca: $spoken"
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

     // --- SpeechRecognizer for Pronunciation Test ---
     private fun nextSoal() {
         currentSoal = soalList.random()
         binding.tvSoal.text = currentSoal
         binding.tvResult.text = ""
         // update prompt
//         recIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Ucapkan: $currentSoal")
     }

     private fun startListening() {
         if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
             != PackageManager.PERMISSION_GRANTED) {
             ActivityCompat.requestPermissions(
                 this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_PERM
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
 }
