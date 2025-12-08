package com.bismillah.tesfft.materi

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.bismillah.tesfft.databinding.ActivityDaftarMateriBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList

class DaftarMateriActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDaftarMateriBinding
    private lateinit var adapter: MateriAdapter
    private val listMateri = ArrayList<MateriModel>()

    private val materiRef = FirebaseDatabase.getInstance(
        "https://adminsuarajepangku-default-rtdb.asia-southeast1.firebasedatabase.app/"
    ).getReference("materi")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDaftarMateriBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = MateriAdapter(listMateri)
        binding.rvMateri.layoutManager = LinearLayoutManager(this)
        binding.rvMateri.adapter = adapter

        loadMateri()
        }

    override fun onResume() {
        super.onResume()
        loadMateri()
    }

    private fun loadMateri() {
        materiRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listMateri.clear()

                if (snapshot.exists()) {
                    for (data in snapshot.children) {
                        val materi = data.getValue(MateriModel::class.java)
                        materi?.let { listMateri.add(it) }
                    }

                    binding.rvMateri.visibility = View.VISIBLE
                    binding.tvEmptyState.visibility = View.GONE
                } else {
                    binding.rvMateri.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}