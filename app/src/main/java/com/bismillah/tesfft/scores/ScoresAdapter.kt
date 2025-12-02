package com.bismillah.tesfft.scores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bismillah.tesfft.databinding.ItemScoresBinding

class ScoresAdapter(private val list: ArrayList<Score>) :
    RecyclerView.Adapter<ScoresAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemScoresBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScoresBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.binding.apply {
            tvName.text = item.username
            tvTheme.text = item.theme
            tvScore.text = item.score?.toString() ?: "0"
            tvDate.text = item.date
        }
    }

    override fun getItemCount(): Int = list.size
}