package com.countingfees.andckaps

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.countingfees.andckaps.databinding.FragmentSettingsBinding
import com.countingfees.andckaps.db.AppDatabase
import com.countingfees.andckaps.utils.MyData
import com.countingfees.andckaps.utils.adapter.FAQAdapter
import drawable.FAQItem
import java.io.File

class SettingsFragment : Fragment() {
    lateinit var db: AppDatabase
    private lateinit var binding: FragmentSettingsBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingsBinding.inflate(layoutInflater)
        db = AppDatabase.getInstance(requireContext())
        if (MyData.user?.photoUrl!=null){
            Glide.with(binding.root)
                .load(File(MyData.user!!.photoUrl!!))
                .centerCrop() // Recommended for profile photos
                .into(binding.photo)
        }

        binding.username.text = MyData.user!!.username

        val faqList = listOf(
            FAQItem(
                "How to edit or delete receipt?",
                "Go to the receipt detail screen and use the edit or delete buttons."
            ),
            FAQItem(
                "Can I change currency and date format?",
                "Yes, you can change both in the profile screen."
            ),
            FAQItem(
                "What should I do if the app crashes or freezes?",
                "Try restarting the app. If the issue persists, reinstall it."
            ),
            FAQItem(
                "Can I recover deleted receipts?",
                "Unfortunately, deleted receipts cannot be restored. Please confirm carefully before deletion."
            ),
            FAQItem(
                "How do I clear all data from the app?",
                "Go to Profile, tap [Clear All Data], and confirm your choice. Be careful, this action is irreversible!"
            ),
            FAQItem(
                "Does betsson require an internet connection?",
                "No, betsson is fully functional offline. All your data remains available without internet access."
            ),
            FAQItem(
                "How can I view a larger photo of my receipt?",
                "Tap the small photo in the Receipt Detail Screen, and it will open in full-screen mode for better visibility."
            ),
            FAQItem(
                "What categories of expenses does betsson support?",
                "By default, betsson includes common categories like Supplies, Fuel/Energy, Meal, Healthcare, and Transportation. You can easily select a category during receipt entry."
            ),
            FAQItem(
                "Can I sort my receipts differently?",
                "Yes, go to the Expenses Screen and tap the sorting icon to arrange receipts by date or category."
            )
        )


        val adapter = FAQAdapter(faqList)
        binding.faqRecyclerView.adapter = adapter
        binding.faqRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        return binding.root
    }


}