package com.talentiva.sisko.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talentiva.happyquiz.helpers.AnswerHistoryManager
import com.talentiva.happyquiz.helpers.PreferenceManager
import com.talentiva.happyquiz.models.LeaderboardEntry
import com.talentiva.happyquiz.models.Soal
import com.talentiva.sisko.helper.DatabaseHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DatabaseViewModel (
    private val connectivityManager: ConnectivityManager?
): ViewModel() {

    private val _data = MutableStateFlow<List<Map<String, String>>>(emptyList())
    val data: StateFlow<List<Map<String, String>>> = _data

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _soalList = MutableStateFlow<List<Soal>>(emptyList())
    val soalList: StateFlow<List<Soal>> = _soalList

    private val _userAnswers = mutableMapOf<String, String>()
    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score


    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

    private val _kategoriList = MutableStateFlow<List<String>>(emptyList())
    val kategoriList: StateFlow<List<String>> = _kategoriList

    private val _leaderboardList = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboardList: StateFlow<List<LeaderboardEntry>> = _leaderboardList



    init {
        checkNetworkConnection()
    }

    fun checkNetworkConnection() {
        viewModelScope.launch {
            val capabilities = connectivityManager?.getNetworkCapabilities(
                connectivityManager.activeNetwork
            )
            _isOnline.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        }
    }

    fun fetchAll(sheetName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = DatabaseHelper.readAll(sheetName)
                _data.value = result
            } catch (e: Exception) {
                _error.value = e.message
                _data.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun create(sheetName: String, data: Map<String, String>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = DatabaseHelper.create(sheetName, data)
                if (success) fetchAll(sheetName)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun update(sheetName: String, data: Map<String, String>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = DatabaseHelper.update(sheetName, data)
                if (success) fetchAll(sheetName)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun delete(sheetName: String, uuid: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = DatabaseHelper.delete(sheetName, uuid)
                if (success) fetchAll(sheetName)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchSoal(sheetName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = DatabaseHelper.readAll(sheetName)
                val soalMapped = result.map { Soal.fromMap(it) }
                val soalShuffled = soalMapped.shuffled()
                _soalList.value = soalShuffled
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchKategoriList(sheetName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = DatabaseHelper.readAll(sheetName)
                val soalMapped = result.map { Soal.fromMap(it) }
                val kategoriUnik = soalMapped.map { it.kategori.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .sorted()
                _kategoriList.value = kategoriUnik
            } catch (e: Exception) {
                _error.value = e.message
                _kategoriList.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchSoalDenganKategori(sheetName: String, kategori: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = DatabaseHelper.readAll(sheetName)
                val soalMapped = result.map { Soal.fromMap(it) }
                val filtered = soalMapped.filter { it.kategori.equals(kategori, ignoreCase = true) }
                val soalShuffled = filtered.shuffled()
                _soalList.value = soalShuffled
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchLeaderboard() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                if (!isOnline.value) {
                    _error.value = "Tidak ada koneksi internet untuk mengambil leaderboard."
                    return@launch
                }
                val result = DatabaseHelper.readAll("leaderBoard") // Mengambil semua data dari sheet "leaderBoard"
                // Mengurutkan berdasarkan skor tertinggi (descending)
                _leaderboardList.value = result.map { LeaderboardEntry.fromMap(it) }
                    .sortedByDescending { it.score }
            } catch (e: Exception) {
                _error.value = "Gagal mengambil data leaderboard: ${e.message}"
                _leaderboardList.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun saveAnswer(answer: String, currentSoal: Soal?) {
        currentSoal?.let { soal ->
            _userAnswers[soal.uuid] = answer
        }
    }

    fun tambahSkor() {
        _score.value += 10
    }

    fun kurangiSkor(){
        if (_score.value > 0) _score.value -= 5
    }

    fun hitungSkor() {
        val list = _soalList.value
        var benar = 0

        for (soal in list) {
            val jawabanUser = _userAnswers[soal.uuid]
            if (jawabanUser != null && jawabanUser.equals(soal.jawaban, ignoreCase = true)) {
                benar++
            }
        }

        _score.value = benar
    }

    fun sendUsernameToSheet(uuid: String, username: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val data = mapOf(
                    "action" to "add_leaderboard",
                    "uuid" to uuid,
                    "username" to username
                )
                val success = DatabaseHelper.create("leaderBoard", data)
                if (!success) {
                    _error.value = "Gagal mengirim data username"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sinkronUserAnswer(context: Context, uuid: String, username: String) {
        viewModelScope.launch {
            val (benar, salah) = AnswerHistoryManager.getScoreSummary(context)
            val data = mapOf(
                "uuid" to uuid,
                "username" to username,
                "benar" to benar.toString(),
                "salah" to salah.toString()
            )
            try {
                DatabaseHelper.create("userAnswer", data)
            } catch (e: Exception) {
                // bisa ditambahkan notifikasi error log jika perlu
            }
        }
    }

    fun resetQuiz(kategori: String) {
        _userAnswers.clear()
        _score.value = 0
        fetchSoalDenganKategori("dataQuiz", kategori)
    }

    fun sendUserScoreToLeaderboard(uuid: String, username: String, score: Int, totalAnswered: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                if (!isOnline.value) {
                    _error.value = "Tidak ada koneksi internet untuk mengirim skor."
                    return@launch
                }

                // Kita perlu membaca data leaderboard yang ada untuk memeriksa apakah user sudah ada
                // atau asumsikan DatabaseHelper.create bisa menangani update jika UUID sudah ada
                // (tergantung implementasi DatabaseHelper Anda)
                // Jika DatabaseHelper.create hanya menambah, Anda mungkin perlu logic 'read, then update/create'.

                val data = mapOf(
                    "uuid" to uuid,
                    "username" to username,
                    "score" to score.toString(),
                    "total" to totalAnswered.toString(),
                    "lastUpdated" to System.currentTimeMillis().toString()
                )

                // Asumsi DatabaseHelper.create akan menimpa/mengupdate jika 'uuid' sudah ada.
                // Jika tidak, Anda perlu implementasi 'update' terpisah atau logic di backend
                // untuk menangani skor terbaik.
                val success = DatabaseHelper.create("leaderBoard", data)
                if (success) {
                    _error.value = null // Bersihkan error jika berhasil
                    fetchLeaderboard() // Refresh leaderboard setelah mengirim skor baru
                } else {
                    _error.value = "Gagal mengirim skor ke leaderboard."
                }
            } catch (e: Exception) {
                _error.value = "Terjadi kesalahan saat mengirim skor: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

}
