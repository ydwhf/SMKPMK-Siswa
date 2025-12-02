package com.bismillah.tesfft.scores

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.bismillah.tesfft.databinding.ActivityStudentScoreBinding
import com.google.firebase.database.*

class StudentScoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentScoreBinding
    private lateinit var db: DatabaseReference
    private lateinit var adapter: ScoresAdapter
    private val listScores = ArrayList<Score>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentScoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseDatabase.getInstance(
            "https://adminsuarajepangku-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("scores")

        setupRecycler()
        loadScores()
    }

    private fun setupRecycler() {
        adapter = ScoresAdapter(listScores)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadScores() {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listScores.clear()

                for (level1 in snapshot.children) {          // -OfA7CHoUGcFyVEIGlZO
                    for (level2 in level1.children) {       // -OfA9e4AThUq5VrfIGId
                        val score = level2.getValue(Score::class.java)
                        if (score != null) listScores.add(score)
                    }
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Error: ${error.message}")
            }
        })
    }
}