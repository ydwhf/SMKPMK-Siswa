package com.bismillah.tesfft.materi

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bismillah.tesfft.databinding.ItemMateriBinding

class MateriAdapter(
    private val list: List<MateriModel>
) : RecyclerView.Adapter<MateriAdapter.MateriVH>() {

    inner class MateriVH(val binding: ItemMateriBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MateriVH {
        val binding = ItemMateriBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MateriVH(binding)
    }

    override fun onBindViewHolder(holder: MateriVH, position: Int) {
        val item = list[position]
        holder.binding.tvJudul.text = item.judul
        holder.binding.tvDeskripsi.text = item.deskripsi

//        holder.itemView.setOnClickListener {
//            val ctx = holder.itemView.context
//            val intent = Intent(ctx, DetailMateriActivity::class.java)
//            intent.putExtra("id", item.id)
//            intent.putExtra("judul", item.judul)
//            intent.putExtra("deskripsi", item.deskripsi)
//            ctx.startActivity(intent)
//        }
    }

    override fun getItemCount(): Int = list.size
}
