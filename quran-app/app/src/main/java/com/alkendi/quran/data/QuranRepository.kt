package com.alkendi.quran.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object QuranRepository {
    var surahs: List<Surah> = emptyList()
        private set
    var tadabburList: List<SurahTadabbur> = emptyList()
        private set
    var ayahTadabbur: Map<String, String> = emptyMap()
        private set

    fun load(context: Context) {
        if (surahs.isNotEmpty()) return
        surahs = loadSurahs(context)
        tadabburList = loadSurahTadabbur(context)
        ayahTadabbur = loadAyahTadabbur(context)
    }

    private fun readAsset(context: Context, name: String): String {
        return context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun loadSurahs(context: Context): List<Surah> {
        val text = readAsset(context, "quran_compact.json")
        val arr = JSONArray(text)
        val out = ArrayList<Surah>(114)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val ayahsJson = o.getJSONArray("ayahs")
            val ayahs = ArrayList<String>(ayahsJson.length())
            for (j in 0 until ayahsJson.length()) ayahs.add(ayahsJson.getString(j))
            out.add(
                Surah(
                    number = o.getInt("number"),
                    arabicName = o.getString("arabicName"),
                    englishName = o.optString("name", ""),
                    translation = o.optString("translation", ""),
                    type = o.optString("type", ""),
                    ayahCount = o.optInt("ayahCount", ayahs.size),
                    ayahs = ayahs
                )
            )
        }
        return out
    }

    private fun loadSurahTadabbur(context: Context): List<SurahTadabbur> {
        return try {
            val text = readAsset(context, "tadabbur_surah.json")
            val arr = JSONArray(text)
            val out = ArrayList<SurahTadabbur>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val h = o.optJSONArray("hidayat")
                val list = ArrayList<String>()
                if (h != null) for (k in 0 until h.length()) list.add(h.getString(k))
                out.add(
                    SurahTadabbur(
                        number = o.getInt("number"),
                        title = o.optString("title", ""),
                        summary = o.optString("summary", ""),
                        tadabbur = o.optString("tadabbur", ""),
                        hidayat = list,
                        amal = o.optString("amal", ""),
                        question = o.optString("question", "")
                    )
                )
            }
            out
        } catch (_: Exception) { emptyList() }
    }

    private fun loadAyahTadabbur(context: Context): Map<String, String> {
        return try {
            val text = readAsset(context, "tadabbur_ayah.json")
            val o = JSONObject(text)
            val map = HashMap<String, String>()
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = o.getString(k)
            }
            map
        } catch (_: Exception) { emptyMap() }
    }

    fun getSurah(n: Int): Surah? = surahs.firstOrNull { it.number == n }
    fun getTadabbur(n: Int): SurahTadabbur? = tadabburList.firstOrNull { it.number == n }
    fun getAyahTadabbur(surah: Int, ayah: Int): String? = ayahTadabbur["$surah:$ayah"]
}
