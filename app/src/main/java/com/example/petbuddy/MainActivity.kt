package com.example.petbuddy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.petbuddy.data.PetPreferences
import com.example.petbuddy.service.PetFloatingService

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PetPreferences
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var progressHappiness: ProgressBar
    private lateinit var progressFullness: ProgressBar
    private lateinit var textHappiness: TextView
    private lateinit var textFullness: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PetPreferences(this)
        buildUi()
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    // ---------- 界面 ----------

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(40), dp(28), dp(32))
        }

        val petImage = ImageView(this).apply {
            setImageResource(R.drawable.pet_normal)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        root.addView(petImage, LinearLayout.LayoutParams(dp(180), dp(180)))

        val title = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.text_dark))
        }
        root.addView(title, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) })

        textHappiness = TextView(this).apply {
            text = "心情 80"
            textSize = 15f
            setTextColor(getColor(R.color.text_dark))
        }
        root.addView(textHappiness, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(24) })

        progressHappiness = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 80
        }
        root.addView(progressHappiness, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(10)
        ).apply { topMargin = dp(6) })

        textFullness = TextView(this).apply {
            text = "饱腹 80"
            textSize = 15f
            setTextColor(getColor(R.color.text_dark))
        }
        root.addView(textFullness, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(16) })

        progressFullness = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 80
        }
        root.addView(progressFullness, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(10)
        ).apply { topMargin = dp(6) })

        btnStart = Button(this).apply {
            text = getString(R.string.start_service)
            textSize = 16f
            setOnClickListener { onStartClicked() }
        }
        root.addView(btnStart, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(28) })

        btnStop = Button(this).apply {
            text = getString(R.string.stop_service)
            textSize = 16f
            setOnClickListener {
                stopService(Intent(this@MainActivity, PetFloatingService::class.java))
                refreshState()
            }
        }
        root.addView(btnStop, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12) })

        val tips = TextView(this).apply {
            text = getString(R.string.tips_click)
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.text_dark))
        }
        root.addView(tips, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(28) })

        setContentView(root)
    }

    // ---------- 交互 ----------

    private fun onStartClicked() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }
        ContextCompat.startForegroundService(
            this,
            Intent(this, PetFloatingService::class.java)
        )
        refreshState()
    }

    private fun refreshState() {
        prefs.settleTick()
        val h = prefs.happiness
        val f = prefs.fullness
        progressHappiness.progress = h
        progressFullness.progress = f
        textHappiness.text = "心情 $h"
        textFullness.text = "饱腹 $f"

        val running = prefs.serviceRunning
        btnStart.isEnabled = !running
        btnStop.isEnabled = running
        btnStart.text = if (running) getString(R.string.service_running) else getString(R.string.start_service)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQ_NOTIFICATION
            )
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQ_NOTIFICATION = 100
    }
}
