package com.bismillah.tesfft.register

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.bismillah.tesfft.databinding.ActivityRegisterBinding
import com.bismillah.tesfft.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                toast("Semua field wajib diisi!")
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                toast("Email tidak valid!")
                return@setOnClickListener
            }

            if (password.length < 6) {
                toast("Password minimal 6 karakter!")
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                toast("Konfirmasi password tidak cocok!")
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        toast("Registrasi berhasil!")
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    } else {
                        toast("Gagal daftar: ${it.exception?.message}")
                    }
                }
        }

        binding.tvLoginPrompt.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}