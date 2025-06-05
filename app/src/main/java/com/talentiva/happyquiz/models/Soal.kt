package com.talentiva.happyquiz.models


data class Soal(
    val uuid: String,
    val soal: String,
    val opsiA: String,
    val opsiB: String,
    val opsiC: String,
    val opsiD: String,
    val jawaban: String,
    val penjelasan: String,
    val kategori: String,
    val creator: String,
    var sudahDijawab: Boolean = false
){
    companion object {
        fun fromMap(map: Map<String, String>): Soal {
            return Soal(
                uuid = map["id"] ?: "",
                soal = map["soal"] ?: "",
                opsiA = map["opsia"] ?: "",
                opsiB = map["opsib"] ?: "",
                opsiC = map["opsic"] ?: "",
                opsiD = map["opsid"] ?: "",
                jawaban = map["kunciJawaban"] ?: "",
                penjelasan = map["penjelasan"] ?: "",
                kategori = map["kategori"] ?: "",
                creator = map["creator"] ?: ""
            )
        }
    }
}

