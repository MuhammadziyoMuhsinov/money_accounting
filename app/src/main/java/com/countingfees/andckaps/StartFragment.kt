package com.countingfees.andckaps

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController

import com.countingfees.andckaps.databinding.FragmentStartBinding

class StartFragment : Fragment() {

    private lateinit var binding:FragmentStartBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStartBinding.inflate(layoutInflater)
        binding.getstart.setOnClickListener {

            findNavController().navigate(
                R.id.action_startFragment_to_summaryFragment,
                null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.splashFragment, true) // ← this removes SplashFragment from back stack
                    .build()
            )
        }
        return binding.root
    }

}