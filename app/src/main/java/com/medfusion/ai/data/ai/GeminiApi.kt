package com.medfusion.ai.data.ai

import com.medfusion.ai.data.ai.dto.GeminiRequest
import com.medfusion.ai.data.ai.dto.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Retrofit contract for Google's Gemini generateContent endpoint. Uses an
 * absolute @Url so it can share the app's existing Retrofit/OkHttp/Moshi stack
 * without a second base URL. The API key travels in the x-goog-api-key header
 * (kept out of the logged URL).
 */
interface GeminiApi {
    @POST
    suspend fun generateContent(
        @Url url: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body body: GeminiRequest,
    ): GeminiResponse
}
