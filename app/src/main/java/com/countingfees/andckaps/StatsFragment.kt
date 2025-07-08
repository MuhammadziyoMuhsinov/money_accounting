package com.countingfees.andckaps

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.countingfees.andckaps.databinding.FragmentStatsBinding
import com.countingfees.andckaps.db.AppDatabase
import com.countingfees.andckaps.db.Receipt
import com.countingfees.andckaps.utils.MyData
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StatsFragment : Fragment() {
    private var list = arrayListOf<Receipt>()
    private lateinit var binding: FragmentStatsBinding
    lateinit var db: AppDatabase
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStatsBinding.inflate(layoutInflater)
        db = AppDatabase.getInstance(requireContext())
        setupListeners()
        transitions()
        return binding.root
    }

    private fun setupListeners() {
        lifecycleScope.launch {

            list = ArrayList(db.receiptDao().getAll())
            MyData.list = ArrayList(list)
            filterAndShow("all")
        }

        binding.allTime.setOnClickListener { filterAndShow("all") }
        binding.weekly.setOnClickListener { filterAndShow("week") }
        binding.monthly.setOnClickListener { filterAndShow("month") }
    }

    private fun filterAndShow(filter: String) {
        val filteredList = when (filter) {
            "week" -> filterByDate(days = 7)
            "month" -> filterByDate(days = 30)
            else -> list
        }
        setAllData(filteredList)
        myPieChart(filteredList)
        myLineChart(filteredList)
    }

    private fun filterByDate(days: Int): List<Receipt> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val threshold = calendar.time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        return list.filter {
            try {
                val receiptDate = dateFormat.parse(it.date)
                receiptDate != null && receiptDate.after(threshold)
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun setAllData(targetList: List<Receipt>) {
        var totalPrice = 0.0
        var totalTax = 0.0

        val currency = when (MyData.user!!.currency) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "UAH" -> "₴"
            else -> MyData.user!!.currency
        }

        targetList.forEach {
            totalPrice += it.amount
            totalTax += it.tax ?: 0.0
        }

        binding.total.text = "$currency${String.format("%,.2f", totalPrice)}"

        val taxPercent = if (totalPrice != 0.0) (totalTax / totalPrice * 100) else 0.0
        binding.tax.text = "Tax $currency${String.format("%,.2f", totalTax)} (${String.format("%.1f", taxPercent)}%)"
        binding.totalDocuments.text = "Total Documents: ${targetList.size}"
    }

    private fun myPieChart(dataList: List<Receipt>) {
        val pieChart = binding.pieChart

        val categorySums = dataList.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(3)

        if (categorySums.isEmpty()) return

        val totalAmount = categorySums.sumOf { it.second }
        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        val categoryColorMap = mapOf(
            "supplies" to Color.parseColor("#B70000"),
            "fuel/energy" to Color.parseColor("#FD6517"),
            "meal" to Color.parseColor("#00B700"),
            "healthcare" to Color.parseColor("#FF9800"),
            "transportation" to Color.parseColor("#03A9F4")
        )

        // Set entries, colors, and text view colors
        categorySums.forEachIndexed { index, (category, amount) ->
            val percent = if (totalAmount != 0.0) (amount / totalAmount * 100.0) else 0.0
            val color = categoryColorMap[category.trim().lowercase()] ?: Color.GRAY
            entries.add(PieEntry(percent.toFloat(), ""))
            colors.add(color)

            when (index) {
                0 -> {
                    binding.firstCategoryPercent.setTextColor(color)
                    binding.firstCategoryPrice.setTextColor(color)
                }
                1 -> {
                    binding.secondCategoryPercent.setTextColor(color)
                    binding.secondCategoryPrice.setTextColor(color)
                }
                2 -> {
                    binding.thirdCategoryPercent.setTextColor(color)
                    binding.thirdCategoryPrice.setTextColor(color)
                }
            }
        }

        // ✅ FIX: Declare the dataset here
        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            setDrawValues(false)
        }

        pieChart.data = PieData(dataSet)

        val (topCategory, topAmount) = categorySums[0]
        val topPercent = if (totalAmount != 0.0) (topAmount / totalAmount * 100.0) else 0.0
        pieChart.centerText = String.format("%,.0f\n%.1f%%", topAmount, topPercent)

        pieChart.setCenterTextColor(Color.WHITE)
        pieChart.setCenterTextSize(18f)
        pieChart.setHoleColor(Color.BLACK)
        pieChart.holeRadius = 90f
        pieChart.transparentCircleRadius = 0f
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = false
        pieChart.invalidate()

        val views = listOf(
            Triple(binding.txtFirstCategory, binding.firstCategoryPrice, binding.firstCategoryPercent),
            Triple(binding.txtSecondCategory, binding.secondCategoryPrice, binding.secondCategoryPercent),
            Triple(binding.txtThirdCategory, binding.thirdCategoryPrice, binding.thirdCategoryPercent)
        )

        categorySums.forEachIndexed { index, (cat, amt) ->
            if (index < views.size) {
                val (txt, price, percent) = views[index]
                txt.text = cat.uppercase()
                price.text = "$${String.format("%,.0f", amt)}"
                percent.text = "${String.format("%.1f", (amt / totalAmount * 100))}%"
            }
        }
    }


    private fun myLineChart(dataList: List<Receipt>) {
        val chart = binding.lineChart
        val monthlyTotals = FloatArray(12)
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (receipt in dataList) {
            val date = try {
                dateFormat.parse(receipt.date)
            } catch (e: Exception) {
                null
            }
            if (date != null) {
                calendar.time = date
                val month = calendar.get(Calendar.MONTH)
                monthlyTotals[month] += receipt.amount.toFloat()
            }
        }

        val entries = monthlyTotals.mapIndexed { index, value -> Entry(index.toFloat(), value) }
        val dataSet = LineDataSet(entries, "Expenses").apply {
            color = Color.CYAN
            setCircleColor(Color.CYAN)
            lineWidth = 2f
            circleRadius = 4f
            setDrawFilled(true)
            fillColor = Color.CYAN
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        chart.data = LineData(dataSet)

        val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = Color.WHITE
            setDrawGridLines(false)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return months.getOrNull(value.toInt()) ?: ""
                }
            }
        }

        chart.axisLeft.textColor = Color.WHITE
        chart.axisLeft.setDrawGridLines(true)
        chart.axisRight.isEnabled = false
        chart.setBackgroundColor(Color.parseColor("#121212"))
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setScaleEnabled(false)
        chart.invalidate()
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
}
