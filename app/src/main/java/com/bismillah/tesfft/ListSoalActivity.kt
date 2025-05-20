package com.bismillah.tesfft

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bismillah.tesfft.databinding.ActivityListSoalBinding

class ListSoalActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListSoalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListSoalBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}