package com.skyplayer.pro.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Modèle pour un programme du guide (EPG)
 */
@Entity(
    tableName = "epg_programs",
    indices = [
        Index(value = ["epgId"]),
        Index(value = ["start"]),
        Index(value = ["stop"])
    ]
)
data class EpgProgram(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val epgId: String, // Identifiant du canal dans le XMLTV (tvg-id)
    val start: Long,   // Timestamp début
    val stop: Long,    // Timestamp fin
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val posterUrl: String? = null
) {
    /**
     * Vérifie si le programme est en cours de diffusion
     */
    fun isCurrent(): Boolean {
        val now = System.currentTimeMillis()
        return now in start until stop
    }

    /**
     * Calcule le pourcentage de progression du programme
     */
    fun getProgress(): Float {
        val now = System.currentTimeMillis()
        if (now < start) return 0f
        if (now > stop) return 1f
        val total = (stop - start).toFloat()
        val elapsed = (now - start).toFloat()
        return (elapsed / total).coerceIn(0f, 1f)
    }
}
