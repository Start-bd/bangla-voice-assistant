package com.banglavoiceassistant.data

import com.google.gson.annotations.SerializedName

data class AgentResponse(
    @SerializedName("transcriptBn")
    val transcriptBn: String,
    
    @SerializedName("englishText")
    val englishText: String,
    
    @SerializedName("intent")
    val intent: String,
    
    @SerializedName("contactName")
    val contactName: String?,
    
    @SerializedName("searchQuery")
    val searchQuery: String?,
    
    @SerializedName("promptForBuilder")
    val promptForBuilder: String?,
    
    @SerializedName("raw")
    val raw: Map<String, Any>?
)

enum class AgentIntent {
    TRANSLATE_ONLY,
    CALL_CONTACT,
    OPEN_CAMERA,
    OPEN_YOUTUBE,
    LOVABLE_BUILD,
    UNKNOWN
}

data class VoiceCommandRequest(
    @SerializedName("audioBase64")
    val audioBase64: String,
    
    @SerializedName("metadata")
    val metadata: Metadata
)

data class Metadata(
    @SerializedName("language")
    val language: String = "bn",
    
    @SerializedName("mode")
    val mode: String = "agent"
)
