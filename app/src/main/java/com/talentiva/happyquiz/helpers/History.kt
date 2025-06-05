package com.talentiva.happyquiz.helpers


import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object AnswerHistoryManager {
    private const val PREF_NAME = "answer_history"
    private const val KEY_HISTORY = "history"

    fun saveAnswer(
        context: Context,
        uuid: String,
        soal: String,
        kunci: String,
        jawabanUser: String
    ) {
        val result = if (jawabanUser.equals(kunci, ignoreCase = true)) "benar" else "salah"
        val historyItem = JSONObject().apply {
            put("uuid", uuid)
            put("soal", soal)
            put("kunci", kunci)
            put("jawaban_user", jawabanUser)
            put("hasil", result)
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val currentData = prefs.getString(KEY_HISTORY, "[]")
        val jsonArray = JSONArray(currentData)

        jsonArray.put(historyItem)

        prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
    }

    fun getAllAnswers(context: Context): JSONArray {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val data = prefs.getString(KEY_HISTORY, "[]")
        return JSONArray(data)
    }

    fun getScoreSummary(context: Context): Pair<Int, Int> {
        val jsonArray = getAllAnswers(context)
        var benar = 0
        var salah = 0

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val hasil = item.getString("hasil")
            if (hasil == "benar") benar++ else salah++
        }

        return Pair(benar, salah)
    }

    fun clearAnswers(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_HISTORY).apply()
    }
}
