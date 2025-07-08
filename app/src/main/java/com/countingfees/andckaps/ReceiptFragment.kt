package com.countingfees.andckaps

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.countingfees.andckaps.databinding.FragmentReceiptBinding
import com.countingfees.andckaps.db.AppDatabase
import com.countingfees.andckaps.db.Receipt
import com.countingfees.andckaps.utils.MyData
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ReceiptFragment : Fragment() {
    private lateinit var binding: FragmentReceiptBinding
    private var list = arrayListOf<Receipt>()
    lateinit var db: AppDatabase
    // Launch gallery to pick image
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                // Update image preview
                Glide.with(binding.root)
                    .load(uri)
                    .into(binding.photoview)

                // Save new URI to database
                MyData.receipt?.let { receipt ->
                    receipt.imagePath = copyImageToInternalStorage(uri)
                    lifecycleScope.launch {
                        db.receiptDao().update(receipt)
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReceiptBinding.inflate(layoutInflater)
        db = AppDatabase.getInstance(requireContext())
        binding.back.setOnClickListener {
            findNavController().popBackStack()
        }

        // Load image from MyData.receipe
        MyData.receipt?.imagePath?.let { path ->
            Glide.with(binding.root)
                .load(File(path))
                .into(binding.photoview)
        } ?: run {
            binding.photoview.setImageResource(R.drawable.sample_receipt)
        }

        // Edit receipt photo
        binding.editReceipt.setOnClickListener {
            pickImage.launch("image/*")
        }

        // Delete photo
        binding.deleteReceipt.setOnClickListener {
            MyData.receipt?.let { receipt ->
                receipt.imagePath = null
                binding.photoview.setImageResource(R.drawable.sample_receipt)
                lifecycleScope.launch {
                    db.receiptDao().update(receipt)
                }
            }
        }

        fetchReceiptsFromDb()
        return binding.root
    }

    private fun copyImageToInternalStorage(uri: Uri): String {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val fileName = "receipt_${System.currentTimeMillis()}.jpg"
        val file = File(requireContext().filesDir, fileName)
        val outputStream = FileOutputStream(file)

        inputStream?.copyTo(outputStream)

        inputStream?.close()
        outputStream.close()

        return file.absolutePath // 🔁 Save this to Room DB
    }

    private fun fetchReceiptsFromDb() {
        lifecycleScope.launch {
            val allReceipts = db.receiptDao().getAll()

            list = ArrayList(allReceipts)
            MyData.list = list
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
}
