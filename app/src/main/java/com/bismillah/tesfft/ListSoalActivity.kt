package com.bismillah.tesfft

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bismillah.tesfft.databinding.ActivityListSoalBinding
import com.bismillah.tesfft.soal.SoalTema1Activity

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
            startActivity(intent)
        }
        binding.btnMulai2.setOnClickListener()
        {

        }
        binding.btnMulai3.setOnClickListener()
        {

        }
    }
}