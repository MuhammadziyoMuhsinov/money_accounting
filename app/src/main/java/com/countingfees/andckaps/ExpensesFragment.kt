package com.countingfees.andckaps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.countingfees.andckaps.databinding.FragmentExpensesBinding
import com.countingfees.andckaps.db.AppDatabase
import com.countingfees.andckaps.db.Receipt
import com.countingfees.andckaps.utils.MyData
import com.countingfees.andckaps.utils.adapter.RvAdapterExpenses
import com.countingfees.andckaps.utils.adapter.RvClick2
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ExpensesFragment : Fragment() {
    lateinit var db: AppDatabase
    private lateinit var rvAdapterExpenses: RvAdapterExpenses
    private var list = arrayListOf<Receipt>()
    private lateinit var binding: FragmentExpensesBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentExpensesBinding.inflate(layoutInflater)
        db = AppDatabase.getInstance(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFilters()
        fetchReceiptsFromDb()
        transitions()
    }

    private fun setupFilters() {
        binding.allTime.setOnClickListener {
            filterReceipts(null)
        }
        binding.thisMonth.setOnClickListener {
            val start = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val end = Calendar.getInstance().apply {
                // today at 23:59:59.999
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.time

            filterReceipts(start, end)
        }

        binding.lastMonth.setOnClickListener {
            val start = Calendar.getInstance().apply {
                add(Calendar.MONTH, -1)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val end = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1) // this month
                add(Calendar.DATE, -1) // go to last day of previous month
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.time

            filterReceipts(start, end)
        }

    }

    private fun filterReceipts(
        startDate: Date?,
        endDate: Date? = null,
        isLastMonth: Boolean = false
    ) {
        lifecycleScope.launch {
            val allReceipts = db.receiptDao().getAll()

            val filtered = allReceipts.filter { receipt ->
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.parse(receipt.date)

                when {
                    startDate == null -> true // All time
                    isLastMonth -> {
                        // Get end of last month
                        val endCal = Calendar.getInstance().apply {
                            set(Calendar.DAY_OF_MONTH, 1)
                            add(Calendar.DATE, -1)
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        date in startDate..endCal.time
                    }

                    endDate != null -> {
                        date in startDate..endDate
                    }

                    else -> {
                        // If no endDate and not lastMonth
                        date?.after(startDate) ?: false
                    }
                }
            }

            list = ArrayList(filtered)
            rvAdapterExpenses = RvAdapterExpenses(list, object : RvClick2 {
                override fun onClick2(receipt: Receipt) {
                    findNavController().popBackStack()
                    findNavController().navigate(R.id.action_summaryFragment_to_receiptDetailFragment)
                }

            })
            binding.rv.adapter = rvAdapterExpenses
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

    private fun transitions() {
        binding.menu.setOnClickListener {
            findNavController().popBackStack()
            findNavController().navigate(R.id.action_summaryFragment_to_profileFragment)
        }
        binding.summary.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.expenses.setOnClickListener {
            findNavController().popBackStack()
            findNavController().navigate(R.id.action_summaryFragment_to_expensesFragment)
        }
        binding.profile.setOnClickListener {
            findNavController().popBackStack()
            findNavController().navigate(R.id.action_summaryFragment_to_profileFragment)
        }
        binding.stats.setOnClickListener {
            findNavController().popBackStack()
            findNavController().navigate(R.id.action_summaryFragment_to_statsFragment)
        }
        binding.addReceipt.setOnClickListener {
            findNavController().popBackStack()
            findNavController().navigate(R.id.action_summaryFragment_to_addReceipeFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        MyData.category = null
    }

    private fun fetchReceiptsFromDb() {
        lifecycleScope.launch {
            val allReceipts = db.receiptDao().getAll()

            list = if (MyData.category != null && MyData.category!!.isNotEmpty()) {
                ArrayList(allReceipts.filter { it.category == MyData.category })
            } else {
                ArrayList(allReceipts)
            }

            rvAdapterExpenses = RvAdapterExpenses(list, object : RvClick2 {
                override fun onClick2(receipt: Receipt) {
                    findNavController().popBackStack()
                    findNavController().navigate(R.id.action_summaryFragment_to_receiptDetailFragment)
                    MyData.receipt = receipt
                }

            })
            binding.rv.adapter = rvAdapterExpenses
            binding.progressBar.visibility = View.GONE
            MyData.list = ArrayList(allReceipts)
            setAllData()
        }
    }
}
