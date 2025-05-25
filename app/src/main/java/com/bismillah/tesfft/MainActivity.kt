 package com.bismillah.tesfft

 import android.content.Intent
 import android.os.*
 import android.widget.Toast
 import androidx.appcompat.app.AppCompatActivity
 import com.bismillah.tesfft.databinding.ActivityMainBinding

 class MainActivity : AppCompatActivity() {
     private lateinit var binding: ActivityMainBinding

     override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)
         binding = ActivityMainBinding.inflate(layoutInflater)
         setContentView(binding.root)

         setupClickListeners()
     }

     private fun setupClickListeners() {
         binding.cardSoal.setOnClickListener {
             Toast.makeText(this, "Menu Soal dipilih", Toast.LENGTH_SHORT).show()
              startActivity(Intent(this, ListSoalActivity::class.java))
         }

         binding.cardMateri.setOnClickListener {
             Toast.makeText(this, "Menu Materi dipilih", Toast.LENGTH_SHORT).show()
             // Tambahkan intent untuk berpindah ke halaman Materi
             // startActivity(Intent(this, MateriActivity::class.java))
         }

         binding.cardSkor.setOnClickListener {
             Toast.makeText(this, "Menu Skor dipilih", Toast.LENGTH_SHORT).show()
             // Tambahkan intent untuk berpindah ke halaman Skor
             // startActivity(Intent(this, SkorActivity::class.java))
         }

         binding.cardTentang.setOnClickListener {
             Toast.makeText(this, "Menu Tentang dipilih", Toast.LENGTH_SHORT).show()
             // Tambahkan intent untuk berpindah ke halaman Tentang
             // startActivity(Intent(this, TentangActivity::class.java))
         }
     }
 }
