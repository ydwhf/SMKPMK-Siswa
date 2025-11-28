package com.bismillah.tesfft

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bismillah.tesfft.databinding.ActivityListSoalBinding
import com.bismillah.tesfft.soal.SoalTema1Activity
import com.bismillah.tesfft.soal.SoalTema2Activity
import com.bismillah.tesfft.soal.SoalTema3Activity

class ListSoalActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListSoalBinding
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListSoalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getStringExtra("userId")

        binding.btnMulai.setOnClickListener()
        {
            val intent = Intent(this@ListSoalActivity, SoalTema1Activity::class.java)
            intent.putExtra("userId", userId)
            intent.putExtra("theme", "percakapan_seharihari")
            startActivity(intent)
        }
        binding.btnMulai2.setOnClickListener()
        {
            val intent = Intent(this@ListSoalActivity, SoalTema2Activity::class.java)
            intent.putExtra("userId", userId)
            intent.putExtra("theme", "sekolah")
            startActivity(intent)
        }
        binding.btnMulai3.setOnClickListener()
        {
            val intent = Intent(this@ListSoalActivity, SoalTema3Activity::class.java)
            intent.putExtra("userId", userId)
            intent.putExtra("theme", "angka")
            startActivity(intent)
        }
    }
}