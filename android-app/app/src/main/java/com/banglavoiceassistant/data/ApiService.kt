package com.banglavoiceassistant.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    @GET("/health")
    suspend fun healthCheck(): Response<Map<String, Any>>
    
    @Multipart
    @POST("/voice/command")
    suspend fun sendVoiceCommand(
        @Part audio: MultipartBody.Part,
        @Part("mode") mode: RequestBody,
        @Part("language") language: RequestBody
    ): Response<AgentResponse>
    
    @POST("/voice/command")
    suspend fun sendVoiceCommandJson(
        @Body request: VoiceCommandRequest
    ): Response<AgentResponse>
    
    @POST("/voice/command/test")
    suspend fun testVoiceCommand(
        @Body request: TestRequest
    ): Response<AgentResponse>
}

data class TestRequest(
    val testIntent: String
)
