package com.bismillah.tesfft.materi

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bismillah.tesfft.databinding.ActivityDetailMateriBinding

class DetailMateriActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailMateriBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailMateriBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvJudul.text = intent.getStringExtra("judul")
        binding.tvDeskripsi.text = intent.getStringExtra("deskripsi")
    }
}