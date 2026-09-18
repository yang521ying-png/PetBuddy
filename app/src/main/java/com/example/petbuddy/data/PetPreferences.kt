package com.example.petbuddy.data

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.max
import kotlin.math.min

/**
 * 布偶状态持久化：心情、饱腹度会随时间衰减，需要主人互动照顾。
 * 状态每过 1 分钟结算一次自然衰减。
 */
class PetPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pet_state", Context.MODE_PRIVATE)

    var happiness: Int
        get() = prefs.getInt(KEY_HAPPINESS, 80)
        set(value) = prefs.edit().putInt(KEY_HAPPINESS, value.coerceIn(0, 100)).apply()

    var fullness: Int
        get() = prefs.getInt(KEY_FULLNESS, 80)
        set(value) = prefs.edit().putInt(KEY_FULLNESS, value.coerceIn(0, 100)).apply()

    var serviceRunning: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_RUNNING, false)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_RUNNING, value).apply()

    private var lastTickTime: Long
        get() = prefs.getLong(KEY_LAST_TICK, System.currentTimeMillis())
        set(value) = prefs.edit().putLong(KEY_LAST_TICK, value).apply()

    /** 结算从上次到现在的自然衰减，应定期调用 */
    fun settleTick() {
        val now = System.currentTimeMillis()
        val elapsedMinutes = (now - lastTickTime) / 60_000L
        if (elapsedMinutes >= 1) {
            val decay = (elapsedMinutes * DECAY_PER_MINUTE).toInt()
            happiness = max(0, happiness - decay)
            fullness = max(0, fullness - decay)
            lastTickTime = now
        }
    }

    /** 投喂：大幅提升饱腹度，小幅提升心情 */
    fun feed() {
        fullness = min(100, fullness + 25)
        happiness = min(100, happiness + 10)
    }

    /** 摸摸：提升心情 */
    fun pet() {
        happiness = min(100, happiness + 15)
    }

    companion object {
        private const val KEY_HAPPINESS = "happiness"
        private const val KEY_FULLNESS = "fullness"
        private const val KEY_LAST_TICK = "last_tick"
        private const val KEY_SERVICE_RUNNING = "service_running"
        // 每分钟衰减 1 点，约 1 个多小时从满值衰减到 0
        private const val DECAY_PER_MINUTE = 1L
    }
}
