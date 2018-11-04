package com.nickskelton.wifidelity.view.adapter

import android.databinding.DataBindingUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.nickskelton.wifidelity.R
import com.nickskelton.wifidelity.databinding.SimpleBlockListItemBinding

class SimpleBlockAdapter : RecyclerView.Adapter<SimpleBlockAdapter.BlockViewHolder>() {

    private val items = mutableListOf<BlockListItem>()

    fun updateItems(newItems: List<BlockListItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
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
            binding.root.setOnClickListener { _ ->
                binding.item?.let {
                    it.onSelected.invoke(it)
                }
            }
        }

        fun bind(item: BlockListItem) {
            binding.item = item
            binding.executePendingBindings()
        }
    }
}