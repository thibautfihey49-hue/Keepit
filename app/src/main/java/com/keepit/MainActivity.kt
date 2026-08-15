package com.keepit

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvLocation: TextView
    private lateinit var etNote: EditText
    private lateinit var btnSave: Button
    private lateinit var btnClear: Button
    private lateinit var btnHide: Button
    private lateinit var tvTime: TextView

    private var lastLocation: Location? = null
    private var isDiscreetMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupButtons()
        updateTime()
        checkPermissions()
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvSpeed = findViewById(R.id.tvSpeed)
        tvLocation = findViewById(R.id.tvLocation)
        etNote = findViewById(R.id.etNote)
        btnSave = findViewById(R.id.btnSave)
        btnClear = findViewById(R.id.btnClear)
        btnHide = findViewById(R.id.btnHide)
        tvTime = findViewById(R.id.tvTime)
    }

    private fun setupButtons() {
        btnSave.setOnClickListener {
            val note = etNote.text.toString()
            if (note.isNotEmpty()) {
                tvStatus.text = "✅ Note sauvegardée"
                tvStatus.setTextColor(0xFF4CAF50.toInt())
                etNote.text.clear()
            } else {
                tvStatus.text = "⚠️ Rien à sauvegarder"
                tvStatus.setTextColor(0xFFFF9800.toInt())
            }
        }

        btnClear.setOnClickListener {
            etNote.text.clear()
            tvStatus.text = "🔄 Effacé"
            tvStatus.setTextColor(0xFF2196F3.toInt())
        }

        btnHide.setOnClickListener {
            toggleDiscreetMode()
        }
    }

    private fun toggleDiscreetMode() {
        isDiscreetMode = !isDiscreetMode
        if (isDiscreetMode) {
            window.decorView.setBackgroundColor(0xFF000000.toInt())
            tvStatus.setTextColor(0xFF00FF00.toInt())
            btnHide.text = "🔓 Mode Normal"
            Toast.makeText(this, "🔒 Mode discret activé", Toast.LENGTH_SHORT).show()
        } else {
            window.decorView.setBackgroundColor(0xFFFFF0F5.toInt())
            tvStatus.setTextColor(0xFFFF1493.toInt())
            btnHide.text = "🔒 Mode Discret"
            Toast.makeText(this, "🔓 Mode normal", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTime() {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        tvTime.text = "⏰ ${sdf.format(Date())}"
        android.os.Handler(mainLooper).postDelayed({ updateTime() }, 1000)
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            tvStatus.text = "✅ GPS prêt"
            tvStatus.setTextColor(0xFF4CAF50.toInt())
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
        }
    }

    fun updateSpeed(location: Location) {
        lastLocation = location
        val speedKmh = (location.speed * 3.6).toInt()
        tvSpeed.text = "🚀 Vitesse : $speedKmh km/h"
        tvLocation.text = "📍 ${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            tvStatus.text = "✅ GPS autorisé"
            tvStatus.setTextColor(0xFF4CAF50.toInt())
        } else {
            tvStatus.text = "⚠️ GPS refusé"
            tvStatus.setTextColor(0xFFFF9800.toInt())
        }
    }
}
