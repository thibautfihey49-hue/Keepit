package com.keepit

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        viewPager.adapter = ViewPagerAdapter(this)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "🏠 Accueil"
                1 -> tab.text = "💖 Journal"
                2 -> tab.text = "🔒 Messages"
            }
        }.attach()

        checkPermissions()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
        }
    }

    private inner class ViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 3
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> HomeFragment()
                1 -> JournalFragment()
                2 -> MessagesFragment()
                else -> HomeFragment()
            }
        }
    }
}

// ============== ÉCRAN 1 : ACCUEIL GPS + NOTES ==============
class HomeFragment : Fragment(R.layout.fragment_home) {
    private lateinit var tvTime: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvLocation: TextView
    private lateinit var etNote: EditText
    private lateinit var btnSave: Button
    private lateinit var btnClear: Button
    private lateinit var btnHide: Button
    private var isDiscreet = false

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvTime = view.findViewById(R.id.tvTime)
        tvStatus = view.findViewById(R.id.tvStatus)
        tvSpeed = view.findViewById(R.id.tvSpeed)
        tvLocation = view.findViewById(R.id.tvLocation)
        etNote = view.findViewById(R.id.etNote)
        btnSave = view.findViewById(R.id.btnSave)
        btnClear = view.findViewById(R.id.btnClear)
        btnHide = view.findViewById(R.id.btnHide)

        updateTime()

        btnSave.setOnClickListener {
            if (etNote.text.isNotEmpty()) {
                tvStatus.text = "✅ Note sauvegardee"
                tvStatus.setTextColor(0xFF4CAF50.toInt())
                etNote.text.clear()
            } else {
                tvStatus.text = "⚠️ Rien a sauvegarder"
                tvStatus.setTextColor(0xFFFF9800.toInt())
            }
        }

        btnClear.setOnClickListener {
            etNote.text.clear()
            tvStatus.text = "🔄 Efface"
            tvStatus.setTextColor(0xFF2196F3.toInt())
        }

        btnHide.setOnClickListener {
            isDiscreet = !isDiscreet
            if (isDiscreet) {
                requireActivity().window.decorView.setBackgroundColor(0xFF000000.toInt())
                btnHide.text = "🔓 Mode Normal"
                Toast.makeText(requireContext(), "🔒 Mode discret active", Toast.LENGTH_SHORT).show()
            } else {
                requireActivity().window.decorView.setBackgroundColor(0xFFFFF0F5.toInt())
                btnHide.text = "🔒 Mode Discret"
                Toast.makeText(requireContext(), "🔓 Mode normal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateTime() {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        tvTime.text = "⏰ ${sdf.format(Date())}"
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ updateTime() }, 1000)
    }
}

// ============== ÉCRAN 2 : JOURNAL INTIME ==============
class JournalFragment : Fragment(R.layout.fragment_journal) {
    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Toast.makeText(requireContext(), "💖 Journal Intime pret !", Toast.LENGTH_SHORT).show()
    }
}

// ============== ÉCRAN 3 : MESSAGES SÉCURISÉS ==============
class MessagesFragment : Fragment(R.layout.fragment_messages) {
    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Toast.makeText(requireContext(), "🔒 Messages Securises pret !", Toast.LENGTH_SHORT).show()
    }
}
