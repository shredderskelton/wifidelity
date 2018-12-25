package com.nickskelton.wifidelity.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nickskelton.wifidelity.R
import com.nickskelton.wifidelity.databinding.SimpleBlockListItemBinding

class SimpleBlockAdapter : RecyclerView.Adapter<SimpleBlockAdapter.BlockViewHolder>() {

    private val items = mutableListOf<BlockListItem>()

    @Synchronized
    fun updateItems(newItems: List<BlockListItem>) {
        val diffResult = DiffCallback(items, newItems).let(DiffUtil::calculateDiff)
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<SimpleBlockListItemBinding>(
            inflater, R.layout.simple_block_list_item, parent, false
        )
        return BlockViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: BlockViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class BlockViewHolder(private val binding: SimpleBlockListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                binding.item?.let { item ->
                    item.onSelected.invoke(item)
                }
            }
        }

        fun bind(item: BlockListItem) {
            binding.item = item
            binding.executePendingBindings()
        }
    }

    private class DiffCallback(
        private val old: List<BlockListItem>,
        private val new: List<BlockListItem>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = old.size

        override fun getNewListSize() = new.size

        override fun areItemsTheSame(oldPosition: Int, newPosition: Int) =
            old[oldPosition].titleText == new[newPosition].titleText

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int) =
            old[oldPosition] == new[newPosition]
    }
}