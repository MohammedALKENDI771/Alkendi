package com.alkendi.quran.data

data class Surah(
    val number: Int,
    val arabicName: String,
    val englishName: String,
    val translation: String,
    val type: String,
    val ayahCount: Int,
    val ayahs: List<String>
)

data class SurahTadabbur(
    val number: Int,
    val title: String,
    val summary: String,
    val tadabbur: String,
    val hidayat: List<String>,
    val amal: String,
    val question: String
)
