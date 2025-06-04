 package com.bismillah.tesfft

 import android.content.Intent
 import android.os.*
 import android.util.Log
 import android.widget.Toast
 import androidx.appcompat.app.AppCompatActivity
 import com.bismillah.tesfft.databinding.ActivityMainBinding

 class MainActivity : AppCompatActivity() {
     private lateinit var binding: ActivityMainBinding
     private var userId: String? = null

     override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)
         binding = ActivityMainBinding.inflate(layoutInflater)
         setContentView(binding.root)

         userId = intent.getStringExtra("userId")
         setupClickListeners()
     }

     private fun setupClickListeners() {
         binding.cardSoal.setOnClickListener {
             Toast.makeText(this, "Menu Soal dipilih", Toast.LENGTH_SHORT).show()
             val intent = Intent(this@MainActivity, ListSoalActivity::class.java)
             intent.putExtra("userId", userId)
             startActivity(intent)
//              startActivity(Intent(this, ListSoalActivity::class.java))
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
