package com.example.equiride

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.equiride.databinding.ItemHorseBinding

class HorseAdapter(
    private val items: MutableList<Horse>,
    private val listener: (horse: Horse, action: Action) -> Unit
) : RecyclerView.Adapter<HorseAdapter.HorseViewHolder>() {

    enum class Action { SELECT, DELETE, STATS }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HorseViewHolder {
        val binding = ItemHorseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HorseViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: HorseViewHolder, position: Int) =
        holder.bind(items[position])

    fun updateList(newList: List<Horse>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    inner class HorseViewHolder(private val binding: ItemHorseBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(horse: Horse) {
            binding.tvName.text = horse.name
            binding.root.setOnClickListener {
                listener(horse, Action.SELECT)
            }
            binding.root.setOnLongClickListener {
                showPopup(it, horse)
                true
            }
        }

        private fun showPopup(anchor: View, horse: Horse) {
            val menu = PopupMenu(anchor.context, anchor)
            menu.menu.apply {
                add(0, Action.STATS.ordinal, 0, "Statistiky")
                add(0, Action.DELETE.ordinal, 1, "Smazat")
            }
            menu.setOnMenuItemClickListener {
                when (it.itemId) {
                    Action.STATS.ordinal -> listener(horse, Action.STATS)
                    Action.DELETE.ordinal -> listener(horse, Action.DELETE)
                }
                true
            }
            menu.show()
        }
    }
}
