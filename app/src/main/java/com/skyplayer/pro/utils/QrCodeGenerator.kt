package com.skyplayer.pro.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import timber.log.Timber
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

/**
 * Générateur de QR Codes optimisé pour Android TV
 * Grand format, haute correction d'erreur pour scan à distance
 */
object QrCodeGenerator {
    
    /**
     * Génère un QR Code bitmap de grande taille pour TV
     * 
     * @param content URL ou texte à encoder
     * @param size Taille en pixels (défaut 512 pour TV visible à 3m)
     * @param foregroundColor Couleur QR (défaut blanc pour thème sombre)
     * @param backgroundColor Couleur fond (défaut noir transparent)
     */
    fun generateQrCode(
        content: String,
        size: Int = 512,
        foregroundColor: Int = Color.WHITE,
        backgroundColor: Int = Color.TRANSPARENT
    ): Bitmap? {
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                // Haute correction d'erreur pour scan facile même si partiellement caché
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                // Marge blanche autour
                put(EncodeHintType.MARGIN, 2)
                // Character set UTF-8
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
            }
            
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    val color = if (bitMatrix[x, y]) foregroundColor else backgroundColor
                    bitmap.setPixel(x, y, color)
                }
            }
            
            bitmap
        } catch (e: Exception) {
            Timber.e(e, "Erreur génération QR Code")
            null
        }
    }
    
    /**
     * Génère URL de connexion pour le portail web
     * Format: https://skyplayerapp.xyz/connect?mac=XX:XX:XX:XX:XX:XX:XX:XX
     */
    fun generateConnectUrl(macId: String): String {
        return "https://skyplayerapp.xyz/connect?mac=${macId.replace(":", "-")}"
    }
    
    /**
     * Taille recommandée selon la distance de scan
     */
    object Sizes {
        const val TV_DISTANCE_3M = 512  // Pour scan depuis canapé (3m)
        const val TV_DISTANCE_2M = 400  // Pour scan plus proche
        const val MOBILE_STANDARD = 256  // Pour affichage mobile
        const val THUMBNAIL = 128       // Petit aperçu
    }
}
