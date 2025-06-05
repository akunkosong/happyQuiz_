package com.talentiva.happyquiz.models

data class LeaderboardEntry(
    val uuid: String,
    val username: String,
    val score: Int,
    val total: Int, // Total pertanyaan yang dijawab, jika relevan
    val lastUpdated: Long // Timestamp kapan terakhir diupdate
) {
    companion object {
        fun fromMap(map: Map<String, String>): LeaderboardEntry {
            return LeaderboardEntry(
                uuid = map["uuid"] ?: "",
                username = map["username"] ?: "Anonim",
                score = map["score"]?.toIntOrNull() ?: 0,
                total = map["total"]?.toIntOrNull() ?: 0,
                lastUpdated = map["lastUpdated"]?.toLongOrNull() ?: 0L
            )
        }
    }
}