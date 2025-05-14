package com.example.equiride

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class RideListAdapter(
    private val dateFmt: SimpleDateFormat
) : ListAdapter<Ride, RideListAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Ride>() {
            override fun areItemsTheSame(old: Ride, new: Ride) = old.id == new.id
            override fun areContentsTheSame(old: Ride, new: Ride) = old == new
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvDuration: TextView = view.findViewById(R.id.tvDuration)
        val tvDistance: TextView = view.findViewById(R.id.tvDistance)
        val tvPercents: TextView = view.findViewById(R.id.tvPercents)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ride, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ride = getItem(position)
        // Datum
        holder.tvDate.text = dateFmt.format(Date(ride.timestamp))
        // Doba jízdy
        val secs = ride.durationSeconds.toInt()
        val m = secs / 60
        val s = secs % 60
        holder.tvDuration.text = String.format("%02d:%02d", m, s)
        // Vzdálenost
        holder.tvDistance.text = "D: ${"%.1f".format(ride.distance)} m"
        // Podíly chodu
        val walkPct  = (ride.walkPortion  * 100).toInt()
        val trotPct  = (ride.trotPortion  * 100).toInt()
        val gallPct  = (ride.gallopPortion* 100).toInt()
        holder.tvPercents.text = "K:${walkPct}% Kl:${trotPct}% Cv:${gallPct}%"
    }
}
