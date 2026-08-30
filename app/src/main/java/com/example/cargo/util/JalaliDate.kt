package com.example.cargo.util

import java.util.Calendar

/**
 * تبدیل تاریخ میلادی به شمسی (Jalali) - ساده
 */
object JalaliDate {

    private val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    data class Date(val year: Int, val month: Int, val day: Int) {
        fun format(): String = "${year}/${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')}"
        fun formatWithMonthName(): String = "$day ${monthNames[month - 1]} $year"
    }

    fun today(): Date {
        val cal = Calendar.getInstance()
        val gYear = cal.get(Calendar.YEAR)
        val gMonth = cal.get(Calendar.MONTH) + 1
        val gDay = cal.get(Calendar.DAY_OF_MONTH)
        return gregorianToJalali(gYear, gMonth, gDay)
    }

    /**
     * تبدیل میلادی به شمسی (الگوریتم ساده)
     */
    private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Date {
        val g_d_m = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        var jy: Int
        if (gy <= 1600) {
            jy = 0
            gy -= 621
        } else {
            jy = 979
            gy -= 1600
        }
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 - 80 + gd + g_d_m[gm - 1]
        jy += 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm = if (days < 186) 1 + days / 31 else 7 + (days - 186) / 30
        val jd = if (days < 186) 1 + days % 31 else 1 + (days - 186) % 30
        return Date(jy, jm, jd)
    }
}
