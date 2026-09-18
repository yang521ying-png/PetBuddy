package com.example.petbuddy.util

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View

/**
 * 布偶动画工具：点头、跳跃、摇摆、弹出等轻量动画。
 * 所有动画最终都会回到初始状态，避免残留位移。
 */
object PetAnimations {

    /** 点头（摸摸后的反馈） */
    fun nod(view: View) {
        val d = view.resources.displayMetrics.density
        val anim = ObjectAnimator.ofFloat(
            view, View.TRANSLATION_Y,
            0f, -10f * d, 0f, -5f * d, 0f
        )
        anim.duration = 600
        anim.start()
    }

    /** 开心跳跃 */
    fun jump(view: View) {
        val d = view.resources.displayMetrics.density
        val up = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, -34f * d).setDuration(260)
        val down = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, -34f * d, 0f).setDuration(260)
        val stretch = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.12f, 1f).setDuration(520)
        val seq = AnimatorSet().apply { playSequentially(up, down) }
        AnimatorSet().apply { playTogether(seq, stretch) }.start()
    }

    /** 左右摇摆（吃到好吃的） */
    fun wiggle(view: View) {
        val anim = ObjectAnimator.ofFloat(
            view, View.ROTATION,
            0f, -16f, 16f, -10f, 10f, 0f
        )
        anim.duration = 650
        anim.start()
    }

    /** 摇头（饥饿提醒） */
    fun shake(view: View) {
        val anim = ObjectAnimator.ofFloat(
            view, View.ROTATION,
            0f, -22f, 22f, -16f, 16f, 0f
        )
        anim.duration = 520
        anim.start()
    }

    /** 弹出效果（气泡/面板出现） */
    fun popIn(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.3f, 1f).setDuration(200)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.3f, 1f).setDuration(200)
        AnimatorSet().apply { playTogether(scaleX, scaleY) }.start()
    }

    /** 浮空呼吸效果（待机时循环播放） */
    fun floatLoop(view: View) {
        if (view.tag == TAG_FLOAT_LOOP) return
        view.tag = TAG_FLOAT_LOOP
        val d = view.resources.displayMetrics.density
        val up = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, -6f * d).setDuration(900)
        val down = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, -6f * d, 0f).setDuration(900)
        val seq = AnimatorSet().apply { playSequentially(up, down) }
        seq.repeatCount = ObjectAnimator.INFINITE
        seq.start()
    }

    /** 停止浮空循环并复位 */
    fun stopFloat(view: View) {
        if (view.tag == TAG_FLOAT_LOOP) {
            view.tag = null
            view.animate().translationY(0f).setDuration(200).start()
        }
    }

    /** 简易回弹：任何动画结束后把位移归零（防御残留） */
    fun resetTransforms(view: View) {
        view.animate().translationY(0f).rotation(0f).scaleY(1f).scaleX(1f).setDuration(120).start()
    }

    private const val TAG_FLOAT_LOOP = "pet_float_loop"
}
