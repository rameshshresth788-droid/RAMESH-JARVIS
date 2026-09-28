package com.ramesh.jarvis

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object AIClient {
    fun ask(config: JarvisConfig, conversation: List<Pair<String, String>>): String {
        if (config.apiKey.isBlank()) {
            return "AI is not configured yet. Open Settings and add your API key."
        }
        if (config.endpoint.isBlank()) {
            return "AI endpoint is missing. Open Settings and add the endpoint."
        }

        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", config.systemPrompt))
        conversation.takeLast(12).forEach { (role, content) ->
            messages.put(JSONObject().put("role", role).put("content", content))
        }

        val body = JSONObject()
            .put("model", config.model)
            .put("messages", messages)
            .put("temperature", 0.7)
            .toString()

        val connection = (URL(config.endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${config.apiKey}")
        }

        return try {
            connection.outputStream.use {
                it.write(body.toByteArray(StandardCharsets.UTF_8))
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream.bufferedReader().use { it.readText() }

            if (code !in 200..299) {
                "AI request failed ($code). Check Settings, endpoint, model and API key."
            } else {
                val json = JSONObject(response)
                val choices = json.optJSONArray("choices") ?: JSONArray()
                if (choices.length() == 0) {
                    "AI returned no response."
                } else {
                    choices.getJSONObject(0)
                        .optJSONObject("message")
                        ?.optString("content")
                        ?.trim()
                        .orEmpty()
                        .ifBlank { "AI returned an empty response." }
                }
            }
        } catch (e: Exception) {
            "Connection error. Check your internet connection and AI Settings."
        } finally {
            connection.disconnect()
        }
    }
}
