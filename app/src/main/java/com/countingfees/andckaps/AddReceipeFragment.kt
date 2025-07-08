package com.countingfees.andckaps

import android.app.DatePickerDialog
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.countingfees.andckaps.databinding.FragmentAddReceipeBinding
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


class AddReceipeFragment : Fragment() {
    private var list = arrayListOf<Receipt>()
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private var imageUri: Uri? = null
    private var selectedDate: Date? = null
    private var imagePath: String? = null
    private lateinit var binding: FragmentAddReceipeBinding
    lateinit var db: AppDatabase
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddReceipeBinding.inflate(layoutInflater)
        db = AppDatabase.getInstance(requireContext())
        setCurrentDate()
        fetchReceiptsFromDb()
        setData()
        category()
        transitions()
        save()
        delete()
        getImage()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success && imageUri != null) {
                    val savedPath = copyImageToInternalStorage(imageUri!!)
                    Glide.with(this)
                        .load(File(savedPath))
                        .centerCrop() // Recommended for profile photos
                        .into(binding.imageView)
                    imagePath = savedPath
                }
            }


        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val savedPath = copyImageToInternalStorage(it)
                Glide.with(this)
                    .load(File(savedPath))
                    .centerCrop() // Recommended for profile photos
                    .into(binding.imageView)
                imagePath = savedPath
            }
        }
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

    private fun getImage() {
        binding.camera.setOnClickListener {
            requestCameraPermission.launch(android.Manifest.permission.CAMERA)
        }


        binding.gallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
    }


    private fun save() {
        binding.save.setOnClickListener {
            val postCategory = binding.spinnerCategory.selectedItem as String
            val postDate =
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate!!)
            val postStore = binding.edtStore.text.trim().toString()
            val postAmount = binding.edtAmount.text.trim().toString()
            val postTax = binding.edtTax.text.trim().toString()
            val postImage = imagePath

            if (postStore.isNotBlank() && postAmount.isNotBlank() && postImage != null) {
                val postReceipte = Receipt(
                    date = postDate,
                    category = postCategory,
                    store = postStore,
                    tax = if (postTax.isNotBlank()) postTax.toDouble() else 0.0,
                    amount = postAmount.toDouble(),
                    imagePath = postImage
                )

                // Show progress
                binding.progressBar.visibility = View.VISIBLE
                binding.nested.visibility = View.GONE

                lifecycleScope.launch {
                    try {
                        db.receiptDao().insert(postReceipte)

                        val snackbar = Snackbar.make(it, "Saved", Snackbar.LENGTH_LONG)

                        snackbar.show()

                        // Optionally reset UI
                        binding.edtStore.text.clear()
                        binding.edtAmount.text.clear()
                        binding.edtTax.text.clear()
                        binding.imageView.setImageResource(R.drawable.sample_receipt)
                        fetchReceiptsFromDb()
                    } catch (e: Exception) {
                        val snackbar =
                            Snackbar.make(it, "Error: ${e.message}", Snackbar.LENGTH_LONG)


                        snackbar.show()
                    } finally {
                        // Hide progress
                        binding.progressBar.visibility = View.GONE
                        binding.nested.visibility = View.VISIBLE
                        // Clear all fields
                        binding.edtStore.text.clear()
                        binding.edtAmount.text.clear()
                        binding.edtTax.text.clear()
                        binding.spinnerCategory.setSelection(0)
                        imagePath = null
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
            binding.edtAmount.text.clear()
            binding.edtTax.text.clear()
            binding.imageView.setImageResource(R.drawable.sample_receipt)
            imagePath = null
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
        val clickListener = OnClickListener {
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

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchCamera() {
        val photoFile = File.createTempFile("temp_photo_", ".jpg", requireContext().cacheDir)
        imageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )
        imageUri?.let { cameraLauncher.launch(it) }
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