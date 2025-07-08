package com.countingfees.andckaps

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.countingfees.andckaps.databinding.FragmentProfileBinding
import com.countingfees.andckaps.db.AppDatabase
import com.countingfees.andckaps.db.Receipt
import com.countingfees.andckaps.utils.MyData
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayList

class ProfileFragment : Fragment() {
    private var typingTimer: Handler? = null
    private val typingDelay = 1000L // 1 second
    lateinit var db: AppDatabase
    private var list = arrayListOf<Receipt>()
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var binding: FragmentProfileBinding
    private var imagePath: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(layoutInflater)
        db = AppDatabase.getInstance(requireContext())
        currency()
        dataformat()
        transitions()
        fetchReceiptsFromDb()
        binding.clearAllData.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete all your receipts? This action cannot be undone.")
                .setPositiveButton("Yes") { _, _ ->
                    lifecycleScope.launch {
                        val db = AppDatabase.getInstance(requireContext())
                        db.receiptDao().deleteAll()
                        MyData.list?.clear()
                        Toast.makeText(requireContext(), "All receipts deleted", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                .setNegativeButton("No", null)
                .show()
        }

        binding.instructions.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }
        binding.privacyPolicy.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_privacyPolicyFragment)
        }
        binding.shareApp.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Check out this app!")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Hey! Check out Betsson - a simple receipt tracker: https://play.google.com/store/apps/details?id=${requireContext().packageName}"
                )
            }
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }

        binding.rateApp.setOnClickListener {
            val uri = Uri.parse("market://details?id=${requireContext().packageName}")
            val goToMarket = Intent(Intent.ACTION_VIEW, uri)
            goToMarket.addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY or
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
            try {
                startActivity(goToMarket)
            } catch (e: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=${requireContext().packageName}")
                    )
                )
            }
        }

        lifecycleScope.launch {
            val user = db.userDao().getUser()

            if (user!!.photoUrl != null) {
                Glide.with(binding.root)
                    .load(File(user.photoUrl!!))
                    .centerCrop() // Recommended for profile photos
                    .into(binding.profilePhoto)


            } else {
                binding.profilePhoto.setImageResource(R.drawable.beautiful_girl)
            }
            binding.username.setText(user.username)

            MyData.user = user
        }
        binding.profilePhoto.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.username.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Cancel previous timer
                typingTimer?.removeCallbacksAndMessages(null)
            }

            override fun afterTextChanged(s: Editable?) {
                // Start a new delay
                if (typingTimer == null) typingTimer = Handler(Looper.getMainLooper())
                typingTimer?.postDelayed({
                    lifecycleScope.launch {
                        val txt = binding.username.text.toString().trim()
                        if (txt.isNotBlank()) {
                            MyData.user!!.username = txt
                            db.userDao().insertOrUpdate(MyData.user!!)
                        }

                    }
                }, typingDelay)
            }
        })
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val savedPath = copyImageToInternalStorage(it)
                Glide.with(this)
                    .load(File(savedPath))
                    .centerCrop() // Recommended for profile photos
                    .into(binding.profilePhoto)
                imagePath = savedPath
                lifecycleScope.launch {
                    MyData.user!!.photoUrl = imagePath
                    db.userDao().insertOrUpdate(MyData.user!!)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        typingTimer?.removeCallbacksAndMessages(null)
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
            val db = AppDatabase.getInstance(requireContext())
            val allReceipts = db.receiptDao().getAll()

            list = ArrayList(allReceipts)
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

    private fun currency() {
        val currencies = arrayOf("USD", "EUR", "GBP")
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, currencies)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        binding.currencySpinner.adapter = adapter

        // Set current selection from user data
        val currentCurrency = MyData.user?.currency ?: "USD"
        binding.currencySpinner.setSelection(currencies.indexOf(currentCurrency))

        // Correct way to listen for item selection
        binding.currencySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedCurrency = currencies[position]
                    if (MyData.user?.currency != selectedCurrency) {
                        MyData.user?.currency = selectedCurrency
                        saveUser()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }


    private fun dataformat() {
        val formats = arrayOf("YYYY-MM-DD", "DD-MM-YYYY", "MM-DD-YYYY")
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, formats)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        binding.dataFormat.adapter = adapter

        // Set default selection
        val current = MyData.user?.dateFormat ?: "YYYY-MM-DD"
        binding.dataFormat.setSelection(formats.indexOf(current))

        // Correct listener
        binding.dataFormat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedFormat = formats[position]
                if (MyData.user?.dateFormat != selectedFormat) {
                    MyData.user?.dateFormat = selectedFormat
                    saveUser()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun saveUser() {
        lifecycleScope.launch {
            MyData.user?.let { user ->
                db.userDao().insertOrUpdate(user)
            }
        }
    }

}