package com.example.equiride

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.equiride.databinding.ItemRideBinding
import java.text.SimpleDateFormat
import java.util.*

class RideListAdapter :
    ListAdapter<Ride, RideListAdapter.VH>(object : DiffUtil.ItemCallback<Ride>() {
        override fun areItemsTheSame(a: Ride, b: Ride) = a.id == b.id
        override fun areContentsTheSame(a: Ride, b: Ride) = a == b
    }) {

    private val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemRideBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position))

    inner class VH(private val vb: ItemRideBinding) : RecyclerView.ViewHolder(vb.root) {
        fun bind(r: Ride) {
            vb.tvDate.text = fmt.format(Date(r.timestamp))
            val distKm = r.distance / 1000.0
            val dur = formatDuration(r.timestamp, r) // implementujte převod timestamp → duration
            vb.tvSummary.text = "D: ${"%.2f".format(distKm)} km, T: $dur, " +
                    "K:${"%.0f".format(r.walkPortion*100)}% Kl:${"%.0f".format(r.trotPortion*100)}% Cv:${"%.0f".format(r.gallopPortion*100)}%"
        }
    }

    private fun formatDuration(startTs: Long, r: Ride): String {
        // Pokud zaznamenáváte dobu do Ride.timestamp + ukládáte stopy,
        // můžete sem napojit uložené duration. Jinak jako placeholder:
        return "--:--"
    }
}
