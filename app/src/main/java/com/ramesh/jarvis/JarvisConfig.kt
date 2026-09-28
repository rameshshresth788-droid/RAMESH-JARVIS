package com.ramesh.jarvis

import android.content.Context

data class JarvisConfig(
    val endpoint: String,
    val apiKey: String,
    val model: String,
    val systemPrompt: String
)

class JarvisConfigStore(context: Context) {
    private val secure = SecureStore(context)

    fun load(): JarvisConfig = JarvisConfig(
        endpoint = secure.get("endpoint", "https://api.openai.com/v1/chat/completions"),
        apiKey = secure.get("apiKey"),
        model = secure.get("model", "gpt-4o-mini"),
        systemPrompt = secure.get(
            "systemPrompt",
            "You are RAMESH JARVIS, a concise, helpful Android voice assistant. Answer naturally and clearly."
        )
    )

    fun save(config: JarvisConfig) {
        secure.put("endpoint", config.endpoint.trim())
        secure.put("apiKey", config.apiKey.trim())
        secure.put("model", config.model.trim())
        secure.put("systemPrompt", config.systemPrompt.trim())
    }
}
