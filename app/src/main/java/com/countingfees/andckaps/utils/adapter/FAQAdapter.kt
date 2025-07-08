package com.countingfees.andckaps.utils.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.countingfees.andckaps.databinding.ItemFaqBinding
import drawable.FAQItem

class FAQAdapter(private val items: List<FAQItem>) :
    RecyclerView.Adapter<FAQAdapter.FAQViewHolder>() {

    inner class FAQViewHolder(val binding: ItemFaqBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FAQViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemFaqBinding.inflate(inflater, parent, false)
        return FAQViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FAQViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvQuestion.text = item.question
        holder.binding.tvAnswer.text = item.answer
        holder.binding.tvAnswer.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
        holder.binding.ivArrow.rotation = if (item.isExpanded) 180f else 0f

        holder.binding.faqHeader.setOnClickListener {
            item.isExpanded = !item.isExpanded
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = items.size
}
