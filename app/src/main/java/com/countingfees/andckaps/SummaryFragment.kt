package com.countingfees.andckaps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.countingfees.andckaps.databinding.FragmentSummaryBinding
import com.countingfees.andckaps.db.AppDatabase
import com.countingfees.andckaps.db.Receipt
import com.countingfees.andckaps.utils.MyData
import com.countingfees.andckaps.utils.adapter.RvAdapterExpenses
import com.countingfees.andckaps.utils.adapter.RvAdapterSummary
import com.countingfees.andckaps.utils.adapter.RvClick
import kotlinx.coroutines.launch
import java.util.ArrayList

class SummaryFragment : Fragment() {

    private lateinit var binding: FragmentSummaryBinding
    private lateinit var rvAdapterSummary: RvAdapterSummary
    lateinit var db: AppDatabase
    private val categories =
        listOf("Supplies", "Fuel/Energy", "Meal", "Healthcare", "Transportation")
    private var list = arrayListOf<Receipt>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSummaryBinding.inflate(layoutInflater)
        db = AppDatabase.getInstance(requireContext())
        fetchReceiptsFromDb()

        transitions()

        return binding.root
    }

    private fun fetchReceiptsFromDb() {
        lifecycleScope.launch {
            val allReceipts = db.receiptDao().getAll()

            list = ArrayList(allReceipts)
            binding.progressBar.visibility = View.GONE
            MyData.list = ArrayList(allReceipts)
            setupRecyclerView()
            setAllData()
        }
    }

    private fun setAllData(targetList: List<Receipt> = list) {
        var totalPrice = 0.0
        var totalTax = 0.0

        val currency = when (MyData.user!!.currency) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "UAH" -> "₴"
            else -> MyData.user!!.currency
        }

        targetList.forEach { receipt ->
            totalPrice += receipt.amount
            totalTax += receipt.tax ?: 0.0
        }

        val formattedPrice = String.format("%,.2f", totalPrice)
        binding.total.text = "$currency$formattedPrice"

        val taxPercent = if (totalPrice != 0.0) (totalTax / totalPrice * 100) else 0.0
        val formattedTax = String.format("%,.2f", totalTax)
        val formattedPercent = String.format("%.1f", taxPercent)

        binding.tax.text = "Tax $currency$formattedTax ($formattedPercent%)"
        binding.totalDocuments.text = "Total Documents: ${targetList.size}"
    }

    private fun setupRecyclerView() {
        // Filter only receipts that belong to predefined categories

        rvAdapterSummary = RvAdapterSummary(categories, MyData.list!!,object :RvClick{
            override fun onClick(category: String) {
                MyData.category = category
                findNavController().navigate(R.id.action_summaryFragment_to_expensesFragment)
            }

        })
        binding.rv.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rvAdapterSummary
        }
    }

    private fun transitions() {
        binding.menu.setOnClickListener {
            findNavController().navigate(R.id.action_summaryFragment_to_profileFragment)
        }
        binding.expenses.setOnClickListener {
            findNavController().navigate(R.id.action_summaryFragment_to_expensesFragment)
        }
        binding.profile.setOnClickListener {
            findNavController().navigate(R.id.action_summaryFragment_to_profileFragment)
        }
        binding.stats.setOnClickListener {
            findNavController().navigate(R.id.action_summaryFragment_to_statsFragment)
        }
        binding.addReceipt.setOnClickListener {
            findNavController().navigate(R.id.action_summaryFragment_to_addReceipeFragment)
        }
    }
}
