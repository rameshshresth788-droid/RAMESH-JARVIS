package com.ramesh.jarvis

import android.content.Context

data class AIProvider(
    val id: String,
    val name: String,
    val endpoint: String,
    val model: String
)

object AIProviders {
    val GEMINI = AIProvider(
        id = "gemini",
        name = "Google Gemini",
        endpoint = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
        model = "gemini-2.5-flash"
    )

    val OPENAI = AIProvider(
        id = "openai",
        name = "OpenAI / ChatGPT",
        endpoint = "https://api.openai.com/v1/chat/completions",
        model = "gpt-5.6-luna"
    )

    val OPENROUTER = AIProvider(
        id = "openrouter",
        name = "OpenRouter",
        endpoint = "https://openrouter.ai/api/v1/chat/completions",
        model = "google/gemma-4-26b-a4b:free"
    )

    val ALL = listOf(GEMINI, OPENAI, OPENROUTER)

    fun byId(id: String): AIProvider = ALL.firstOrNull { it.id == id } ?: GEMINI
}

data class JarvisConfig(
    val providerId: String,
    val endpoint: String,
    val apiKey: String,
    val model: String,
    val systemPrompt: String
)

class JarvisConfigStore(context: Context) {
    private val secure = SecureStore(context)

    fun load(): JarvisConfig {
        val savedProvider = secure.get("providerId", "")
        val provider = if (savedProvider.isBlank()) {
            val oldEndpoint = secure.get("endpoint", "")
            AIProviders.ALL.firstOrNull { it.endpoint == oldEndpoint } ?: AIProviders.GEMINI
        } else {
            AIProviders.byId(savedProvider)
        }

        return JarvisConfig(
            providerId = provider.id,
            endpoint = provider.endpoint,
            apiKey = secure.get("apiKey"),
            model = provider.model,
            systemPrompt = secure.get(
                "systemPrompt",
                "You are RAMESH JARVIS, a private personal AI voice assistant.\n\nUnderstand Hindi, English and Hinglish.\nReply naturally in the language used by the user.\nKeep voice responses short, clear and natural.\nBe helpful, intelligent and polite.\nDo not claim an action was completed unless the app actually performed it."
            )
        )
    }

    fun save(config: JarvisConfig) {
        val provider = AIProviders.byId(config.providerId)
        secure.put("providerId", provider.id)
        secure.put("endpoint", provider.endpoint)
        secure.put("apiKey", config.apiKey.trim())
        secure.put("model", provider.model)
        secure.put("systemPrompt", config.systemPrompt.trim())
    }
}
