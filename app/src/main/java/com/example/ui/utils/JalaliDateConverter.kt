package com.example.ui.utils

import java.util.Calendar

class JalaliDateConverter {
    var jY: Int = 0
    var jM: Int = 0
    var jD: Int = 0

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int) {
        val g_d_m = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        var gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + 365 * gy + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + g_d_m[gm - 1]
        
        var jy = -1595 + 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + days / 31
            jd = 1 + days % 31
        } else {
            jm = 7 + (days - 186) / 30
            jd = 1 + (days - 186) % 30
        }
        this.jY = jy
        this.jM = jm
        this.jD = jd
    }

    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Long {
        var jy2 = jy + 1595
        var days = -355668 + 365 * jy2 + (jy2 / 33) * 8 + ((jy2 % 33) + 3) / 4 + jd + if (jm < 7) (jm - 1) * 31 else 186 + (jm - 7) * 30
        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            gy += 100 * ((days - 1) / 36524)
            days = (days - 1) % 36524
            if (days >= 365) {
                days++
            }
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        var gm = 0
        val g_d_m = intArrayOf(0, 31, if (gy % 4 == 0 && gy % 100 != 0 || gy % 400 == 0) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        for (i in 0..12) {
            if (gd <= g_d_m[i]) {
                gm = i
                break
            }
            gd -= g_d_m[i]
        }
        
        val c = Calendar.getInstance()
        c.set(Calendar.YEAR, gy)
        c.set(Calendar.MONTH, gm - 1)
        c.set(Calendar.DAY_OF_MONTH, gd)
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 0)
        return c.timeInMillis
    }

    companion object {
        fun formatJalali(timestamp: Long): String {
            if (timestamp <= 0L) return "بدون مهلت"
            val c = Calendar.getInstance()
            c.timeInMillis = timestamp
            val converter = JalaliDateConverter()
            converter.gregorianToJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
            
            val months = arrayOf("", "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
            val jmIndex = converter.jM.coerceIn(0, 12)
            val jmStr = months.getOrNull(jmIndex) ?: ""
            return "${converter.jD} $jmStr"
        }
    }
}
