package com.countingfees.andckaps

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.countingfees.andckaps.databinding.FragmentSplashBinding
import com.countingfees.andckaps.db.AppDatabase
import com.countingfees.andckaps.db.User
import com.countingfees.andckaps.utils.MyData
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {

    private lateinit var binding: FragmentSplashBinding
    lateinit var db: AppDatabase
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSplashBinding.inflate(layoutInflater)
        db = AppDatabase.getInstance(requireContext())
        val seekBar = binding.seekBarLuminosite
        val maxProgress = 100
        seekBar.max = maxProgress
        seekBar.isEnabled = false
        seekBar.isClickable = false

        val animator = ValueAnimator.ofInt(0, maxProgress)
        animator.duration = 2500
        animator.interpolator = LinearInterpolator()

        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Int
            seekBar.progress = progress
        }

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Delay navigation until fragment is attached
                if (isAdded) {
                    checkUserAndNavigate()
                }
            }
        })

        animator.start()

        return binding.root
    }

    private fun checkUserAndNavigate() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Use context?.let to safely access context
            context?.let { ctx ->
                val userDao = db.userDao()

                val user = userDao.getUser()
                if (user == null) {
                    val newUser = User(
                        id = 1,
                        username = "New User",
                        photoUrl = null,
                        currency = "USD",
                        dateFormat = "YYYY-MM-DD"
                    )
                    MyData.user = newUser
                    userDao.insertOrUpdate(newUser)
                } else {
                    MyData.user = user
                }

                if (isAdded) {
                    findNavController().navigate(
                        R.id.action_splashFragment_to_startFragment,
                        null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.splashFragment, true) // ← this removes SplashFragment from back stack
                            .build()
                    )

                }
            }
        }
    }
}
