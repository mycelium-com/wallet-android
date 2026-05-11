package com.mycelium.supportchat.data

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.tatarka.inject.annotations.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

sealed class ImageUploadResult {
    data class Success(val imageUrl: String) : ImageUploadResult()
    data class Error(val message: String) : ImageUploadResult()
}

@Inject
class ImageUploadService(
    // TODO: Move API key to BuildKonfig or secure server-side proxy
    private val apiKey: String = API_KEY
) {
    companion object {
        private const val TAG = "ImageUploadService"
        private const val UPLOAD_URL = "https://api.imgbb.com/1/upload"
        private const val API_KEY = "11a14ca5f9e0c2d17fd7c1746f610a5d"
        private const val MAX_IMAGE_SIZE = 10 * 1024 * 1024 // 10 MB
    }

    private val logger = Logger.withTag(TAG)
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = HttpClient()

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun uploadImage(imageBytes: ByteArray): ImageUploadResult {
        if (imageBytes.size > MAX_IMAGE_SIZE) {
            return ImageUploadResult.Error("Image too large. Maximum size is 10 MB")
        }

        return try {
            val base64Image = Base64.encode(imageBytes)

            val response = httpClient.submitForm(
                url = UPLOAD_URL,
                formParameters = parameters {
                    append("key", apiKey)
                    append("image", base64Image)
                }
            )

            val responseBody = response.bodyAsText()
            val jsonResponse = json.parseToJsonElement(responseBody).jsonObject

            val success = jsonResponse["success"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false

            if (success) {
                val imageUrl = jsonResponse["data"]
                    ?.jsonObject?.get("display_url")
                    ?.jsonPrimitive?.content

                if (imageUrl != null) {
                    logger.d { "Image uploaded successfully: $imageUrl" }
                    ImageUploadResult.Success(imageUrl)
                } else {
                    ImageUploadResult.Error("Failed to parse upload response")
                }
            } else {
                val errorMsg = jsonResponse["error"]
                    ?.jsonObject?.get("message")
                    ?.jsonPrimitive?.content ?: "Upload failed"
                ImageUploadResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to upload image" }
            ImageUploadResult.Error(e.message ?: "Failed to upload image")
        }
    }
}
