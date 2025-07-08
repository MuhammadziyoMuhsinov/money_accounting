package com.countingfees.andckaps

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.countingfees.andckaps.databinding.FragmentReceiptBinding
import com.countingfees.andckaps.databinding.FragmentReceiptDetailBinding
import com.countingfees.andckaps.db.AppDatabase
import com.countingfees.andckaps.db.Receipt
import com.countingfees.andckaps.utils.MyData
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.Locale


class ReceiptDetailFragment : Fragment() {
    private var list = arrayListOf<Receipt>()
    private var selectedDate: Date? = null
    lateinit var db: AppDatabase
    private lateinit var binding: FragmentReceiptDetailBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentReceiptDetailBinding.inflate(layoutInflater)
        db = AppDatabase.getInstance(requireContext())
        setCurrentDate()
        fetchReceiptsFromDb()
        setData()
        category()
        transitions()
        save()
        delete()
        binding.imageView.setOnClickListener {
            MyData.imagePath = MyData.receipt!!.imagePath
            findNavController().navigate(R.id.action_receiptDetailFragment_to_receiptFragment)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val receipt = MyData.receipt ?: return

        // Load image if available
        receipt.imagePath?.let {
            val file = File(it)
            if (file.exists()) {
                Glide.with(binding.root)
                    .load(file)
                    .centerCrop() // Recommended for profile photos
                    .into(binding.imageView)
            } else {
                binding.imageView.setImageResource(R.drawable.sample_receipt)
            }
        }

        // Set store
        binding.edtStore.setText(receipt.store)

        // Set amount
        binding.edtAmount.setText(receipt.amount.toString())

        // Set tax
        binding.edtTax.setText(receipt.tax?.toString() ?: "")

        // Set date
        binding.txtData.text = receipt.date

        // Set category spinner
        val categories = listOf("Supplies", "Fuel/Energy", "Meal", "Healthcare", "Transportation")
        val categoryIndex = categories.indexOf(receipt.category)
        binding.spinnerCategory.setSelection(if (categoryIndex >= 0) categoryIndex else 0)

    }



    private fun save() {
        binding.save.setOnClickListener {
            val postCategory = binding.spinnerCategory.selectedItem as String
            val postDate =
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate!!)
            val postStore = binding.edtStore.text.trim().toString()
            val postAmount = binding.edtAmount.text.trim().toString()
            val postTax = binding.edtTax.text.trim().toString()
            val postImage = MyData.receipt!!.imagePath

            if (postStore.isNotBlank() && postAmount.isNotBlank() && postImage != null) {
                MyData.receipt!!.date = postDate
                MyData.receipt!!.category = postCategory
                MyData.receipt!!.store = postStore
                MyData.receipt!!.tax = if (postTax.isNotBlank()) postTax.toDouble() else 0.0
                MyData.receipt!!.amount = postAmount.toDouble()


                // Show progress
                binding.progressBar.visibility = View.VISIBLE
                binding.nested.visibility = View.GONE

                lifecycleScope.launch {
                    try {
                        db.receiptDao().update(MyData.receipt!!)

                        val snackbar = Snackbar.make(it, "Updated!", Snackbar.LENGTH_LONG)

                        snackbar.show()
                        fetchReceiptsFromDb()
                    } catch (e: Exception) {
                        val snackbar =
                            Snackbar.make(it, "Error: ${e.message}", Snackbar.LENGTH_LONG)


                        snackbar.show()
                    } finally {
                        // Hide progress
                        binding.progressBar.visibility = View.GONE
                        binding.nested.visibility = View.VISIBLE

                    }
                }
            } else {
                val snackbar =
                    Snackbar.make(it, "Please fill all the required fields !", Snackbar.LENGTH_LONG)


                snackbar.show()
            }
        }
    }


    private fun delete() {
        binding.delete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Receipt")
                .setMessage("Are you sure you want to delete this receipt?")
                .setPositiveButton("Yes") { _, _ ->
                    lifecycleScope.launch {
                        db.receiptDao().delete(MyData.receipt!!)

                        Toast.makeText(requireContext(), "Receipt deleted", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack() // Go back after deletion
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }


    private fun setCurrentDate() {
        // Set current date as default
        val calendar = Calendar.getInstance()
        selectedDate = calendar.time

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        binding.txtData.text = format.format(selectedDate!!)

    }

    private fun setData() {
        val clickListener = View.OnClickListener {
            val calendar = Calendar.getInstance()

            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    // Month is 0-based, so add +1
                    val chosendate = Calendar.getInstance()
                    chosendate.set(year, month, dayOfMonth)

                    // Optional: format to string
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dateString = format.format(chosendate.time)

                    // Example: set to TextView or save
                    binding.txtData.text = dateString

                    // Example: store as Date
                    selectedDate = chosendate.time
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            datePicker.show()
        }
        binding.txtData.setOnClickListener(clickListener)
        binding.dataClick.setOnClickListener(clickListener)
    }

    private fun category() {
        val categories = listOf("Supplies", "Fuel/Energy", "Meal", "Healthcare", "Transportation")

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            categories
        )
        adapter.setDropDownViewResource(R.layout.spinner_item)

        binding.spinnerCategory.adapter = adapter

    }

    private fun fetchReceiptsFromDb() {
        lifecycleScope.launch {
            val allReceipts = db.receiptDao().getAll()

            list = ArrayList(allReceipts)
            binding.progressBar.visibility = View.GONE
            MyData.list = ArrayList(allReceipts)
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


}