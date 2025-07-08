package com.countingfees.andckaps.utils.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.countingfees.andckaps.R
import com.countingfees.andckaps.databinding.ItemRvBinding
import com.countingfees.andckaps.db.Receipt
import com.countingfees.andckaps.utils.MyData
import java.text.NumberFormat
import java.util.Locale

class RvAdapterSummary(
    private val list: List<String>,
    private val receipts: List<Receipt>,
    private val rvClick: RvClick
) :
    RecyclerView.Adapter<RvAdapterSummary.Vh>() {

    inner class Vh(private val itemRv: ItemRvBinding) : RecyclerView.ViewHolder(itemRv.root) {

        fun onBind(category: String, position: Int) {

            itemRv.root.setOnClickListener {
                rvClick.onClick(category)
            }
            val categoryReceipts = receipts.filter { it.category == category }

            // Total amount
            val totalAmount = categoryReceipts.sumOf { it.amount }
            val formattedAmount = formatCurrency(totalAmount)

            // Total tax
            val totalTax = categoryReceipts.sumOf { it.tax ?: 0.0 }
            val taxPercent = if (totalAmount != 0.0) (totalTax / totalAmount * 100) else 0.0
            val formattedTax = formatCurrency(totalTax)

            // Set image by category
            when (category) {
                "Supplies" -> itemRv.img.setImageResource(R.drawable.img_supplies)
                "Fuel/Energy" -> itemRv.img.setImageResource(R.drawable.fuel)
                "Meal" -> itemRv.img.setImageResource(R.drawable.meal)
                "Healthcare" -> itemRv.img.setImageResource(R.drawable.healthcare)
                "Transportation" -> itemRv.img.setImageResource(R.drawable.transportation)
            }

            // Set text views
            itemRv.name.text = category
            itemRv.price.text = formattedAmount
            itemRv.text2.text =
                "Tax $formattedTax (${String.format(Locale.getDefault(), "%.1f", taxPercent)}%)"
            itemRv.text1.text = "${categoryReceipts.size} Transactions"

            // Show special design if last item
            itemRv.fake.visibility = if (position == itemCount - 1) View.VISIBLE else View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Vh {
        val binding = ItemRvBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Vh(binding)
    }

    override fun onBindViewHolder(holder: Vh, position: Int) {
        holder.onBind(list[position], position)
    }

    override fun getItemCount(): Int = list.size

    private fun formatCurrency(value: Double): String {
        val currencySymbol = when (MyData.user?.currency) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "UAH" -> "₴"
            else -> MyData.user?.currency ?: ""
        }
        return "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", value)}"
    }
}

interface RvClick {
    fun onClick(category: String)
}
