package com.example.equiride

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.equiride.databinding.ItemRideBinding
import java.text.SimpleDateFormat
import java.util.*

class RideListAdapter : ListAdapter<Ride, RideListAdapter.ViewHolder>(DIFF) {

    private val dateFmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    private val dfPct = java.text.DecimalFormat("0")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRideBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val vb: ItemRideBinding) :
        RecyclerView.ViewHolder(vb.root) {

        fun bind(ride: Ride) {
            // 1) Timestamp
            vb.tvTimestamp.text = dateFmt.format(Date(ride.timestamp))

            // 2) Duration (přidali jsme sloupec durationSeconds do RIde)
            val minutes = ride.durationSeconds / 60
            val seconds = ride.durationSeconds % 60
            vb.tvDuration.text = String.format("%02d:%02d", minutes, seconds)

            // 3) Distance
            vb.tvDistance.text = "D: ${"%.1f".format(ride.distance)} m"

            // 4) Portions
            val walkPct  = dfPct.format(ride.walkPortion  * 100)
            val trotPct  = dfPct.format(ride.trotPortion  * 100)
            val gallPct  = dfPct.format(ride.gallopPortion * 100)
            vb.tvPortions.text = "K: $walkPct%   Kl: $trotPct%   Cv: $gallPct%"
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Ride>() {
            override fun areItemsTheSame(old: Ride, new: Ride) = old.id == new.id
            override fun areContentsTheSame(old: Ride, new: Ride) = old == new
        }
    }
}
