package com.bismillah.tesfft.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.bismillah.tesfft.MainActivity
import com.bismillah.tesfft.databinding.ActivityLoginBinding
import com.bismillah.tesfft.register.RegisterActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    private lateinit var db: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseDatabase.getInstance().getReference("users")

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Isi semua field", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (userSnap in snapshot.children) {
                                val pass = userSnap.child("password").value.toString()
                                if (pass == password) {
                                    val userId = userSnap.key.toString()
                                    val shared = getSharedPreferences("APP_AUTH", MODE_PRIVATE)
                                    val edit = shared.edit()
                                    edit.putString("userId", userId)
                                    edit.putString("username", username)
                                    edit.apply()
                                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this@LoginActivity, "Password salah", Toast.LENGTH_SHORT).show()
                                }
                                break
                            }
                        } else {
                            Toast.makeText(this@LoginActivity, "Username tidak ditemukan", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}