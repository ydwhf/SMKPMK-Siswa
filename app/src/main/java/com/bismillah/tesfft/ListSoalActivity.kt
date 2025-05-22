package com.bismillah.tesfft

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bismillah.tesfft.databinding.ActivityListSoalBinding
import com.bismillah.tesfft.soal.SoalTema1Activity

class ListSoalActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListSoalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListSoalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnMulai.setOnClickListener()
        {
            startActivity(Intent(this, SoalTema1Activity::class.java))
        }
        binding.btnMulai2.setOnClickListener()
        {

        }
        binding.btnMulai3.setOnClickListener()
        {

        }
    }
}