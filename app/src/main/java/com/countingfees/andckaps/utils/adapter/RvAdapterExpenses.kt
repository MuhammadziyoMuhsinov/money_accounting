package com.countingfees.andckaps.utils.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.countingfees.andckaps.R
import com.countingfees.andckaps.databinding.ItemRvBinding
import com.countingfees.andckaps.db.Receipt
import com.countingfees.andckaps.utils.MyData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RvAdapterExpenses(var list: List<Receipt>, private val rvClick2: RvClick2) :
    RecyclerView.Adapter<RvAdapterExpenses.Vh>() {

    inner class Vh(private val itemRv: ItemRvBinding) :
        RecyclerView.ViewHolder(itemRv.root) {

        fun onBind(receipt: Receipt, position: Int) {
            itemRv.root.setOnClickListener {
                rvClick2.onClick2(receipt)
            }
            // Set category image
            when (receipt.category) {
                "Supplies" -> itemRv.img.setImageResource(R.drawable.apple)
                "Fuel/Energy" -> itemRv.img.setImageResource(R.drawable.fuel)
                "Meal" -> itemRv.img.setImageResource(R.drawable.apple)
                "Healthcare" -> itemRv.img.setImageResource(R.drawable.healthcare)
                "Transportation" -> itemRv.img.setImageResource(R.drawable.transportation)
            }

            // Set basic info
            itemRv.name.text = receipt.store
            itemRv.price.text = "${receipt.amount} ${MyData.user!!.currency}"
            itemRv.text2.text = receipt.category

            // Format date
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(receipt.date)
            val calendar = Calendar.getInstance().apply { time = date!! }

            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val month = calendar.get(Calendar.MONTH) + 1 // 0-based
            val year = calendar.get(Calendar.YEAR)

            val formattedDate = when (MyData.user!!.dateFormat) {
                "YYYY-MM-DD" -> "%04d-%02d-%02d".format(year, month, day)
                "DD-MM-YYYY" -> "%02d-%02d-%04d".format(day, month, year)
                "MM-DD-YYYY" -> "%02d-%02d-%04d".format(month, day, year)
                else -> receipt.date
            }

            itemRv.text1.text = formattedDate

            // Change design for the last item
            if (itemCount == position + 1) {
                itemRv.fake.visibility = View.VISIBLE
            } else {
                itemRv.fake.visibility = View.GONE
            }
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
}

interface RvClick2 {
    fun onClick2(receipt: Receipt)
}