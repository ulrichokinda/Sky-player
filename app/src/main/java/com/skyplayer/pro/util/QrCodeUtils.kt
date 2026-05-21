package com.skyplayer.pro.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import timber.log.Timber

/**
 * Utilitaire de génération de QR codes via ZXing Core
 * N'utilise que com.google.zxing:core (déjà présent) — pas de journeyapps
 */
object QrCodeUtils {

    /**
     * Génère un QR code Bitmap pour une adresse MAC donnée.
     *
     * L'URL encodée: https://skyplayerapp.xyz/connect?mac=<cleanMac>
     * QR code noir sur fond transparent (couleurs configurables).
     *
     * @param rawMacAddress  Adresse MAC brute (avec espaces, tirets ou deux-points)
     * @param size           Taille en pixels du Bitmap carré (défaut: 512)
     * @param darkColor      Couleur des modules sombres (défaut: blanc pour fond noir)
     * @param lightColor     Couleur du fond (défaut: transparent)
     * @return Bitmap du QR code, ou null en cas d'erreur
     */
    fun generateQrCodeForMac(
        rawMacAddress: String,
        size: Int = 512,
        darkColor: Int = Color.WHITE,
        lightColor: Int = Color.TRANSPARENT
    ): Bitmap? {
        return try {
            // 1. Nettoyage absolu : retire espaces, retours à la ligne, espaces invisibles
            val cleanMac = rawMacAddress.trim().replace("\\s+".toRegex(), "")

            // 2. Construction de l'URL d'activation
            val connectUrl = "https://skyplayerapp.xyz/connect?mac=$cleanMac"

            Timber.d("🔲 Génération QR code pour: $connectUrl")

            // 3. Configuration de l'encodage
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1,          // marge minimale (quiet zone)
                EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M
            )

            // 4. Encodage ZXing → BitMatrix
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                connectUrl,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )

            // 5. Conversion BitMatrix → Bitmap ARGB_8888
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height) { index ->
                val x = index % width
                val y = index / width
                if (bitMatrix[x, y]) darkColor else lightColor
            }

            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                it.setPixels(pixels, 0, width, 0, 0, width, height)
            }

        } catch (e: WriterException) {
            Timber.e(e, "❌ Erreur encodage QR code ZXing")
            null
        } catch (e: Exception) {
            Timber.e(e, "❌ Erreur inattendue génération QR code")
            null
        }
    }
}
