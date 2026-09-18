package com.example.petbuddy.service

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.petbuddy.MainActivity
import com.example.petbuddy.R
import com.example.petbuddy.data.PetPreferences
import com.example.petbuddy.util.PetAnimations
import com.example.petbuddy.view.PetView
import kotlin.math.roundToInt

/**
 * 悬浮布偶核心服务：
 *  - 前台服务，常驻悬浮窗
 *  - 拖拽移动 + 松手吸附屏幕边缘
 *  - 单击摸摸、双击打开操作面板
 *  - 状态（心情/饱腹）定时衰减，低状态表情变化与提醒
 */
class PetFloatingService : Service() {

    companion object {
        const val CHANNEL_ID = "pet_buddy_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_FEED = "com.example.petbuddy.action.FEED"
        const val ACTION_STOP = "com.example.petbuddy.action.STOP"
        private const val TICK_INTERVAL_MS = 60_000L
    }

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: PetPreferences

    private var petView: PetView? = null
    private var params: WindowManager.LayoutParams? = null

    private var menuView: View? = null

    private var startX = 0
    private var startY = 0
    private var lastHungryState = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private val ticker = object : Runnable {
        override fun run() {
            prefs.settleTick()
            updatePetMood()
            updateNotification()
            mainHandler.postDelayed(this, TICK_INTERVAL_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = PetPreferences(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_FEED -> {
                feedBuddy()
                return START_STICKY
            }
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        if (petView == null) showPet()
        mainHandler.removeCallbacks(ticker)
        mainHandler.post(ticker)
        prefs.serviceRunning = true
        return START_STICKY
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(ticker)
        prefs.serviceRunning = false
        try {
            petView?.let { windowManager.removeView(it) }
        } catch (_: Exception) {
        }
        try {
            menuView?.let { windowManager.removeView(it) }
        } catch (_: Exception) {
        }
        petView = null
        menuView = null
        params = null
        super.onDestroy()
    }

    // ---------- 悬浮窗 ----------

    private fun showPet() {
        if (petView != null) return
        prefs.settleTick()

        val view = PetView(
            context = this,
            onPet = { petBuddy() },
            onDoubleTap = { showMenu() },
            onMoveStart = {
                startX = params?.x ?: 0
                startY = params?.y ?: 0
            },
            onMove = { dx, dy ->
                params?.let { lp ->
                    lp.x = startX + dx.roundToInt()
                    lp.y = startY + dy.roundToInt()
                    petView?.let { v ->
                        try {
                            windowManager.updateViewLayout(v, lp)
                        } catch (_: Exception) {
                        }
                    }
                }
            },
            onMoveEnd = { snapToEdge() }
        )
        view.setMood(moodForState())
        view.startIdleAnimation()

        val lp = WindowManager.LayoutParams(
            dp(110), dp(120),
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = defaultX()
            y = defaultY()
        }

        windowManager.addView(view, lp)
        petView = view
        params = lp
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun defaultX(): Int {
        val dm = resources.displayMetrics
        return dm.widthPixels - dp(140)
    }

    private fun defaultY(): Int {
        val dm = resources.displayMetrics
        return (dm.heightPixels * 0.35f).toInt()
    }

    /** 松手后水平吸附到屏幕左右边缘 */
    private fun snapToEdge() {
        val lp = params ?: return
        val view = petView ?: return
        val dm = resources.displayMetrics
        val screenW = dm.widthPixels
        val targetX = if (lp.x + lp.width / 2 > screenW / 2) {
            (screenW - lp.width).coerceAtLeast(0)
        } else {
            0
        }
        val targetY = lp.y.coerceAtLeast(0)

        val animator = ValueAnimator.ofInt(lp.x, targetX).apply {
            duration = 240
            addUpdateListener {
                lp.x = it.animatedValue as Int
                lp.y = targetY
                try {
                    windowManager.updateViewLayout(view, lp)
                } catch (_: Exception) {
                }
            }
        }
        animator.start()
    }

    // ---------- 互动 ----------

    private fun petBuddy() {
        prefs.pet()
        val view = petView ?: return
        view.playPet()
        view.setMood(PetView.Mood.HAPPY)
        view.showBubble("好开心～")
        mainHandler.postDelayed({
            petView?.let { v ->
                v.setMood(moodForState())
                v.playHappy()
            }
        }, 1200)
        updateNotification()
    }

    private fun feedBuddy() {
        prefs.feed()
        val view = petView ?: return
        view.playFeed()
        view.setMood(PetView.Mood.HAPPY)
        view.showBubble("好吃！谢谢主人～")
        mainHandler.postDelayed({
            petView?.let { v ->
                v.setMood(moodForState())
            }
        }, 1600)
        updatePetMood()
        updateNotification()
    }

    private fun moodForState(): PetView.Mood {
        prefs.settleTick()
        return when {
            prefs.fullness <= 20 -> PetView.Mood.HUNGRY
            prefs.happiness <= 20 -> PetView.Mood.SLEEPY
            else -> PetView.Mood.NORMAL
        }
    }

    private fun updatePetMood() {
        val view = petView ?: return
        view.setMood(moodForState())
        val hungryNow = prefs.fullness <= 20
        if (hungryNow && !lastHungryState) {
            view.playHungry()
            view.showBubble("我饿了…")
        }
        lastHungryState = hungryNow
    }

    // ---------- 操作面板（双击布偶弹出） ----------

    private fun showMenu() {
        if (menuView != null) return

        val status = TextView(this).apply {
            text = "心情 ${prefs.happiness} · 饱腹 ${prefs.fullness}"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.text_dark))
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        val feedBtn = Button(this).apply {
            text = "🍎  投喂布偶"
            textSize = 15f
            setOnClickListener {
                hideMenu()
                feedBuddy()
            }
        }

        val stopBtn = Button(this).apply {
            text = "⛔  关闭布偶"
            textSize = 15f
            setOnClickListener {
                hideMenu()
                stopSelf()
            }
        }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.menu_panel_bg)
            setPadding(dp(24), dp(20), dp(24), dp(20))
            addView(status, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) })
            addView(feedBtn, LinearLayout.LayoutParams(
                dp(170),
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) })
            addView(stopBtn, LinearLayout.LayoutParams(
                dp(170),
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }

        val overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0x59000000)
            setOnClickListener { hideMenu() }
            addView(panel)
        }
        PetAnimations.popIn(panel)

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }

        windowManager.addView(overlay, lp)
        menuView = overlay
    }

    private fun hideMenu() {
        try {
            menuView?.let { windowManager.removeView(it) }
        } catch (_: Exception) {
        }
        menuView = null
    }

    // ---------- 通知 ----------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val feedPi = PendingIntent.getService(
            this, 1,
            Intent(this, PetFloatingService::class.java).setAction(ACTION_FEED),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopPi = PendingIntent.getService(
            this, 2,
            Intent(this, PetFloatingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val openPi = PendingIntent.getActivity(
            this, 3,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_pet)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText("心情 ${prefs.happiness} · 饱腹 ${prefs.fullness}｜单击摸摸 · 双击菜单 · 长按拖动")
            .setOngoing(true)
            .setContentIntent(openPi)
            .addAction(0, getString(R.string.notification_action_feed), feedPi)
            .addAction(0, getString(R.string.notification_action_stop), stopPi)
            .build()
    }

    private fun updateNotification() {
        try {
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification())
        } catch (_: Exception) {
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).roundToInt()
}
