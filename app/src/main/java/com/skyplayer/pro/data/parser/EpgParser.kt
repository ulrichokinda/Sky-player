package com.skyplayer.pro.data.parser

import android.util.Xml
import com.skyplayer.pro.data.model.EpgProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import timber.log.Timber
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPInputStream

/**
 * Parser XMLTV haute performance pour le guide des programmes (EPG)
 * Supporte les fichiers .xml et .xml.gz
 */
class EpgParser {

    companion object {
        // Format XMLTV: 20231027140000 +0200
        private val DATE_FORMAT = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
    }

    /**
     * Parse un flux XMLTV et retourne une liste de programmes
     */
    suspend fun parse(inputStream: InputStream): List<EpgProgram> = withContext(Dispatchers.Default) {
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
                            
                            if (epgId != null && start != null && stop != null) {
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
                            if (currentProgram.title.isNotEmpty()) {
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
            DATE_FORMAT.parse(dateStr)?.time
        } catch (e: Exception) {
            null
        }
    }
}
