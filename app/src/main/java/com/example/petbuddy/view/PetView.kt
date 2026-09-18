package com.example.petbuddy.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.example.petbuddy.R
import com.example.petbuddy.util.PetAnimations
import kotlin.math.abs

/**
 * 悬浮布偶视图。
 * 手势约定：
 *  - 单击：摸摸互动（onPet）
 *  - 双击：打开操作面板（onDoubleTap）
 *  - 拖动：移动布偶（onMove / onMoveEnd 触发边缘吸附）
 */
@SuppressLint("ViewConstructor")
class PetView(
    context: Context,
    private val onPet: () -> Unit,
    private val onDoubleTap: () -> Unit,
    private val onMoveStart: () -> Unit,
    private val onMove: (Float, Float) -> Unit,
    private val onMoveEnd: () -> Unit,
) : FrameLayout(context) {

    private val imageView: ImageView
    private val bubble: TextView
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var downRawX = 0f
    private var downRawY = 0f
    private var dragging = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onPet()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                onDoubleTap()
                return true
            }
        }
    )

    init {
        imageView = ImageView(context).apply {
            setImageResource(R.drawable.pet_normal)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

        bubble = TextView(context).apply {
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(context.getColor(R.color.text_dark))
            setBackgroundResource(R.drawable.mood_bubble)
            visibility = GONE
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            ).apply { topMargin = dp(2) }
        }

        addView(imageView)
        addView(bubble)
    }

    private val hideBubbleRunnable = Runnable { bubble.visibility = GONE }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                dragging = false
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                if (!dragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                    dragging = true
                    onMoveStart()
                }
                if (dragging) {
                    onMove(dx, dy)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    dragging = false
                    onMoveEnd()
                    return true
                }
            }
        }
        gestureDetector.onTouchEvent(event)
        return true
    }

    /** 切换布偶表情 */
    fun setMood(mood: Mood) {
        val res = when (mood) {
            Mood.NORMAL -> R.drawable.pet_normal
            Mood.HAPPY -> R.drawable.pet_happy
            Mood.HUNGRY -> R.drawable.pet_hungry
            Mood.SLEEPY -> R.drawable.pet_sleeping
        }
        imageView.setImageResource(res)
    }

    /** 显示气泡文字（自动消失） */
    fun showBubble(text: String) {
        bubble.text = text
        bubble.visibility = VISIBLE
        PetAnimations.popIn(bubble)
        mainHandler.removeCallbacks(hideBubbleRunnable)
        mainHandler.postDelayed(hideBubbleRunnable, 1800)
    }

    /** 摸摸反馈 */
    fun playPet() {
        PetAnimations.stopFloat(imageView)
        PetAnimations.nod(imageView)
        mainHandler.postDelayed({ PetAnimations.floatLoop(imageView) }, 800)
    }

    /** 开心跳跃 */
    fun playHappy() {
        PetAnimations.stopFloat(imageView)
        PetAnimations.jump(imageView)
        mainHandler.postDelayed({ PetAnimations.floatLoop(imageView) }, 800)
    }

    /** 投喂反馈 */
    fun playFeed() {
        PetAnimations.stopFloat(imageView)
        PetAnimations.wiggle(imageView)
        mainHandler.postDelayed({ PetAnimations.floatLoop(imageView) }, 800)
    }

    /** 饥饿提醒 */
    fun playHungry() {
        PetAnimations.stopFloat(imageView)
        PetAnimations.shake(imageView)
        mainHandler.postDelayed({ PetAnimations.floatLoop(imageView) }, 800)
    }

    /** 开始待机浮空动画 */
    fun startIdleAnimation() {
        PetAnimations.floatLoop(imageView)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    enum class Mood { NORMAL, HAPPY, HUNGRY, SLEEPY }
}
