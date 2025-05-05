package com.example.i_postureguard

import android.content.Context
import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST


interface OpenAiService {
    @POST("v1/chat/completions")
    suspend fun getChatResponse(
        @Header("Authorization") auth: String,
        @Body request: ChatRequest
    ): ChatResponse
}

data class ChatRequest(
    val model: String,
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: String
)

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

class PostureChatbot(private val context: Context) {
    private val apiKey = ""
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val service = retrofit.create(OpenAiService::class.java)

    suspend fun getPostureAdvice(posture: String, score: Float): String {
        val prompt = """
            You are a posture trainer. Based on the detected posture ($posture) and health score ($score), provide concise, actionable advice to improve posture. Use a friendly tone and focus on health benefits. If the score is high (>100), highlight urgency. If low (<75), praise the user.
            Examples:
            - Text Neck, score > 100: "Oh no, Text Neck with a risk score of $score! Try keeping your phone at eye level and take breaks every 20 minutes to stretch your neck."
            - Sleep on Back, score < 75: "Using phone when Sleep on back, score $score. Great choice! Prevent using phone when Sleep on back to keep your spine neutral."
        """.trimIndent()
        val request = ChatRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                Message("system", prompt),
                Message("user", "Analyze posture: $posture, score ${score.toInt()}")
            )
        )
        val response = service.getChatResponse("Bearer $apiKey", request)
        return response.choices[0].message.content
    }
}