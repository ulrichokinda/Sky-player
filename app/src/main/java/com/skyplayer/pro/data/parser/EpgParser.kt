package com.skyplayer.pro.data.parser

import android.util.Xml
import com.skyplayer.pro.data.model.EpgProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import timber.log.Timber
import java.io.InputStream
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Parser XMLTV haute performance pour le guide des programmes (EPG)
 * Supporte les fichiers .xml et .xml.gz
 */
class EpgParser {

    companion object {
        // Format XMLTV: 20231027140000 +0200
        // DateTimeFormatter est immutable et thread-safe (contrairement à SimpleDateFormat
        // partagé entre les coroutines du pool Default → dates corrompues en parallèle).
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss Z", Locale.US)

        /** Garde-fou mémoire : on ignore les programmes trop anciens et on borne la liste. */
        private const val MAX_PROGRAMS = 400_000
    }

    /**
     * Parse un flux XMLTV et retourne une liste de programmes
     */
    suspend fun parse(inputStream: InputStream): List<EpgProgram> = withContext(Dispatchers.Default) {
        // Un XMLTV peut peser 100 Mo+ : ignorer les programmes de plus de 24 h pour
        // borner la mémoire, et plafonner à MAX_PROGRAMS pour éviter l'OOM sur box TV.
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        val programs = mutableListOf<EpgProgram>()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            var currentProgram: EpgProgram? = null
            var currentTag: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = name
                        if (name == "programme") {
                            val epgId = parser.getAttributeValue(null, "channel")
                            val startStr = parser.getAttributeValue(null, "start")
                            val stopStr = parser.getAttributeValue(null, "stop")
                            
                            val start = parseDate(startStr)
                            val stop = parseDate(stopStr)
                            
                            if (epgId != null && start != null && stop != null && stop >= cutoff) {
                                currentProgram = EpgProgram(
                                    epgId = epgId,
                                    start = start,
                                    stop = stop,
                                    title = ""
                                )
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text.trim()
                        if (text.isNotEmpty() && currentProgram != null) {
                            when (currentTag) {
                                "title" -> currentProgram = currentProgram.copy(title = text)
                                "desc" -> currentProgram = currentProgram.copy(description = text)
                                "category" -> currentProgram = currentProgram.copy(category = text)
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name == "programme" && currentProgram != null) {
                            if (currentProgram.title.isNotEmpty() && programs.size < MAX_PROGRAMS) {
                                programs.add(currentProgram)
                            }
                            currentProgram = null
                        }
                        currentTag = null
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Timber.e(e, "Erreur parsing XMLTV")
        }
        programs
    }

    private fun parseDate(dateStr: String?): Long? {
        if (dateStr == null) return null
        return try {
            OffsetDateTime.parse(dateStr, DATE_FORMATTER).toInstant().toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }
}
