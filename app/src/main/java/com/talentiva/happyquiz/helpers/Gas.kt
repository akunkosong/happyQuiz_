package com.talentiva.sisko.helper

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

object DatabaseHelper {

    private const val WEB_APP_URL = "https://script.google.com/macros/s/AKfycbzQFbr7nHlK0heR3ZT0cf_k_2nsG-CEXsKWuVBWOSauyzXHpbbaZubDGN95mFq68jR6/exec"
    private const val SHEET_ID = "1RXX41wNBSBvigIEZVPzFenz5eZj_4R5JL-cxmiQQPXA"


    suspend fun readAll(sheetName: String): List<Map<String, String>> {
        val url = "$WEB_APP_URL?sheetId=$SHEET_ID&sheet=$sheetName&action=read"
        return fetchJsonData(url)
    }

    suspend fun create(sheetName: String, data: Map<String, String>): Boolean {
        val jsonData = JSONObject(data).toString()
        val url = "$WEB_APP_URL?sheetId=$SHEET_ID&sheet=$sheetName&action=create&data=${URLEncoder.encode(jsonData, "UTF-8")}"
        return fetchStatusOnly(url)
    }

    suspend fun update(sheetName: String, data: Map<String, String>): Boolean {
        val jsonData = JSONObject(data).toString()
        val url = "$WEB_APP_URL?sheetId=$SHEET_ID&sheet=$sheetName&action=update&data=${URLEncoder.encode(jsonData, "UTF-8")}"
        return fetchStatusOnly(url)
    }

    suspend fun delete(sheetName: String, uuid: String): Boolean {
        val url = "$WEB_APP_URL?sheetId=$SHEET_ID&sheet=$sheetName&action=delete&id=$uuid"
        return fetchStatusOnly(url)
    }

    // Fungsi untuk mengambil data JSON dan mengembalikan List of Map
    private suspend fun fetchJsonData(url: String): List<Map<String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    Log.d("DatabaseHelper", "Response: $response")

                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getString("status") == "success") {
                        val dataArray = jsonResponse.getJSONArray("data")
                        val result = mutableListOf<Map<String, String>>()
                        for (i in 0 until dataArray.length()) {
                            val rowObj = dataArray.getJSONObject(i)
                            val rowMap = mutableMapOf<String, String>()
                            rowObj.keys().forEach {
                                rowMap[it] = rowObj.getString(it)
                            }
                            result.add(rowMap)
                        }
                        result
                    } else {
                        Log.e("DatabaseHelper", "Failed: ${jsonResponse.getString("message")}")
                        emptyList()
                    }
                } else {
                    Log.e("DatabaseHelper", "HTTP error: $responseCode")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Network error", e)
                emptyList()
            }
        }
    }

    // Fungsi untuk operasi create/update/delete yang hanya cek status
    private suspend fun fetchStatusOnly(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    Log.d("DatabaseHelper", "Response: $response")

                    val jsonResponse = JSONObject(response)
                    jsonResponse.getString("status") == "success"
                } else {
                    Log.e("DatabaseHelper", "HTTP error: $responseCode")
                    false
                }
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Network error", e)
                false
            }
        }
    }
}
