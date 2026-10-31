package com.example.ml

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LicensePlateOcrHelper {
    private const val TAG = "LicensePlateOcr"

    // Primary Indian standard vehicle registration regex as requested by user
    // e.g., KA-02-JH-2234, MH 12 AB 1234, DL-01-A-4321
    private val PLATE_REGEX = Regex("[A-Za-z]{2}[- ]?\\d{2}[- ]?[A-Za-z]{1,2}[- ]?\\d{4}")

    // Fallback broad pattern for diverse license plates
    private val BROAD_PLATE_REGEX = Regex("[A-Za-z0-9]{2,3}[- ]?[A-Za-z0-9]{1,4}[- ]?\\d{3,4}")

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Extracts vehicle license plate numbers using ML Kit Text Recognition with regex validation.
     */
    suspend fun recognizeLicensePlate(bitmap: Bitmap): Result<String> = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val fullText = visionText.text
                    Log.d(TAG, "OCR Detected raw text:\n$fullText")

                    // First search direct lines and text blocks
                    var matchedPlate: String? = null
                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val cleanLine = line.text.trim().uppercase()
                            val match = PLATE_REGEX.find(cleanLine)
                            if (match != null) {
                                matchedPlate = formatPlate(match.value)
                                break
                            }
                        }
                        if (matchedPlate != null) break
                    }

                    // Fallback to checking full string
                    if (matchedPlate == null) {
                        val match = PLATE_REGEX.find(fullText.uppercase())
                        if (match != null) {
                            matchedPlate = formatPlate(match.value)
                        }
                    }

                    // Secondary fallback to broad format
                    if (matchedPlate == null) {
                        val broadMatch = BROAD_PLATE_REGEX.find(fullText.uppercase())
                        if (broadMatch != null) {
                            matchedPlate = formatPlate(broadMatch.value)
                        }
                    }

                    if (matchedPlate != null) {
                        continuation.resume(Result.success(matchedPlate))
                    } else {
                        continuation.resume(Result.failure(Exception("No license plate matching pattern found in frame.")))
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "ML Kit OCR failed", exception)
                    continuation.resume(Result.failure(exception))
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error running ML Kit OCR", e)
            continuation.resume(Result.failure(e))
        }
    }

    /**
     * Powerful text extraction for Job Cards, capable of capturing both machine-printed headers
     * and human handwritten technician notes from physical trip sheets / work orders.
     */
    suspend fun recognizeFullDocumentText(bitmap: Bitmap): Result<String> = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val fullText = visionText.text
                    Log.d(TAG, "Full document & handwritten text captured: ${fullText.length} chars")
                    continuation.resume(Result.success(fullText))
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Full document ML Kit OCR failed", exception)
                    continuation.resume(Result.failure(exception))
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error recognizing document text", e)
            continuation.resume(Result.failure(e))
        }
    }

    private fun formatPlate(raw: String): String {
        val cleaned = raw.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
        // Standardize into XX-00-XX-0000 if 9 or 10 characters
        return when {
            cleaned.length == 10 -> {
                "${cleaned.substring(0, 2)}-${cleaned.substring(2, 4)}-${cleaned.substring(4, 6)}-${cleaned.substring(6)}"
            }
            cleaned.length == 9 -> {
                "${cleaned.substring(0, 2)}-${cleaned.substring(2, 4)}-${cleaned.substring(4, 5)}-${cleaned.substring(5)}"
            }
            else -> raw.trim().uppercase()
        }
    }
}
