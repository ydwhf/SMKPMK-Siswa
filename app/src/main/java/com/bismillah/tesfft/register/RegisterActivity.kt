package com.bismillah.tesfft.register

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.bismillah.tesfft.databinding.ActivityRegisterBinding
import com.google.firebase.database.*

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var db: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseDatabase.getInstance().getReference("users")

        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Isi semua field", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Cek duplikat username
            db.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            Toast.makeText(this@RegisterActivity, "Username sudah digunakan", Toast.LENGTH_SHORT).show()
                        } else {
                            val userId = db.push().key!!
                            val userMap = mapOf(
                                "username" to username,
                                "password" to password,
                                "skor" to 0,
                                "lastTestDate" to ""
                            )
                            db.child(userId).setValue(userMap).addOnCompleteListener {
                                if (it.isSuccessful) {
                                    Toast.makeText(this@RegisterActivity, "Berhasil daftar", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }
}
