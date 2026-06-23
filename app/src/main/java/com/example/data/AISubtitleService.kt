package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

object AISubtitleService {
    private const val TAG = "AISubtitleService"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    /**
     * Transcribes a video file using OpenAI's Whisper API.
     */
    suspend fun transcribeWithWhisper(
        context: Context,
        videoUri: Uri,
        apiKey: String,
        targetLanguage: String
    ): List<SubtitleLine> = withContext(Dispatchers.IO) {
        val bytes = readBytesFromUri(context, videoUri) ?: throw Exception("Failed to read video file bytes")
        
        val fileBody = bytes.toRequestBody("video/mp4".toMediaType())
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "temp.mp4", fileBody)
            .addFormDataPart("model", "whisper-1")
            .addFormDataPart("response_format", "verbose_json")
            
        if (targetLanguage.isNotEmpty() && targetLanguage != "Auto") {
            val langCode = getLanguageCode(targetLanguage)
            multipartBody.addFormDataPart("language", langCode)
        }

        val request = Request.Builder()
            .url("https://api.openai.com/v1/audio/transcriptions")
            .header("Authorization", "Bearer $apiKey")
            .post(multipartBody.build())
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Whisper API Error: $errorBody")
                throw Exception("Whisper API Error (Code ${response.code}): $errorBody")
            }

            val bodyString = response.body?.string() ?: throw Exception("Empty Whisper response")
            Log.d(TAG, "Whisper Raw Response: $bodyString")
            
            val jsonObject = JSONObject(bodyString)
            val segmentsList = mutableListOf<SubtitleLine>()
            val segmentsArray = jsonObject.optJSONArray("segments")
            
            if (segmentsArray != null) {
                for (i in 0 until segmentsArray.length()) {
                    val segment = segmentsArray.getJSONObject(i)
                    val start = segment.getDouble("start")
                    val end = segment.getDouble("end")
                    val text = segment.getString("text")
                    segmentsList.add(
                        SubtitleLine(
                            projectId = 0,
                            startMs = (start * 1000).toLong(),
                            endMs = (end * 1000).toLong(),
                            text = text.trim()
                        )
                    )
                }
                segmentsList
            } else {
                val text = jsonObject.optString("text", "")
                listOf(
                    SubtitleLine(
                        projectId = 0,
                        startMs = 0,
                        endMs = 10000,
                        text = text.trim()
                    )
                )
            }
        }
    }

    /**
     * Transcribes video/audio using Google's Gemini 3.5 Flash REST API.
     * We pass the video/audio bytes in Base64 encoding.
     */
    suspend fun transcribeWithGemini(
        context: Context,
        videoUri: Uri,
        apiKey: String,
        targetLanguage: String
    ): List<SubtitleLine> = withContext(Dispatchers.IO) {
        val bytes = readBytesFromUri(context, videoUri) ?: throw Exception("Failed to read file bytes")
        
        // Base64 encode the media bytes
        val base64Data = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        
        // Construct standard prompt
        val systemPrompt = "You are an expert video subtitler. Analyze the audio in the provided video file and transcribe it into timed captions in $targetLanguage language."
        val userPrompt = """
            Transcribe the provided video audio stream.
            Output your transcription ONLY as a JSON array where each object has "startMs", "endMs", and "text" fields.
            Ensure captions are split into short readable lines (maximum 5-6 words per line) and timed precisely.
            
            Required Output JSON Format:
            [
              {"startMs": 1200, "endMs": 3500, "text": "Hello and welcome"},
              {"startMs": 3600, "endMs": 6200, "text": "to our channel today"}
            ]
            
            Respond with ONLY the raw JSON array. Do not include markdown codeblocks or any additional chat.
        """.trimIndent()

        // Construct request payload matching Direct REST API schema using org.json
        val requestPayload = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply { put("text", userPrompt) })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "video/mp4")
                                put("data", base64Data)
                            })
                        })
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            val systemInstructionObj = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                }
                put("parts", partsArray)
            }
            put("systemInstruction", systemInstructionObj)

            val generationConfigObj = JSONObject().apply {
                put("temperature", 0.1)
                put("responseFormat", JSONObject().apply {
                    put("mimeType", "application/json")
                    put("responseSchema", JSONObject().apply {
                        put("type", "ARRAY")
                        put("items", JSONObject().apply {
                            put("type", "OBJECT")
                            put("properties", JSONObject().apply {
                                put("startMs", JSONObject().apply { put("type", "INTEGER") })
                                put("endMs", JSONObject().apply { put("type", "INTEGER") })
                                put("text", JSONObject().apply { put("type", "STRING") })
                            })
                            put("required", JSONArray().apply {
                                put("startMs")
                                put("endMs")
                                put("text")
                            })
                        })
                    })
                })
            }
            put("generationConfig", generationConfigObj)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestPayload.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(requestBody)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Gemini API Error: $errorBody")
                throw Exception("Gemini API Error (Code ${response.code}): $errorBody")
            }

            val bodyString = response.body?.string() ?: throw Exception("Empty Gemini response")
            Log.d(TAG, "Gemini Raw Response: $bodyString")
            
            val jsonResponse = JSONObject(bodyString)
            val candidates = jsonResponse.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val firstPart = parts.getJSONObject(0)
            val responseText = firstPart.getString("text")
            
            Log.d(TAG, "Gemini Extracted Text: $responseText")
            
            val subtitleArray = JSONArray(responseText)
            val subtitleList = mutableListOf<SubtitleLine>()
            for (i in 0 until subtitleArray.length()) {
                val item = subtitleArray.getJSONObject(i)
                subtitleList.add(
                    SubtitleLine(
                        projectId = 0,
                        startMs = item.getLong("startMs"),
                        endMs = item.getLong("endMs"),
                        text = item.getString("text").trim()
                    )
                )
            }
            subtitleList
        }
    }

    /**
     * Highly realistic mock subtitle generator for fallback/simulation
     */
    fun generateSimulatedSubtitles(
        targetLanguage: String,
        videoDurationMs: Long
    ): List<SubtitleLine> {
        val phrases = when (targetLanguage.lowercase()) {
            "spanish" -> listOf(
                "¡Hola a todos! Bienvenidos.",
                "Hoy vamos a crear subtítulos automáticos.",
                "Esta aplicación es súper rápida y fácil.",
                "Con diez estilos virales increíbles.",
                "¡Mira cómo cambia el estilo de inmediato!",
                "Perfecto para TikTok, Reels e Shorts.",
                "Puedes editar el texto con un solo toque.",
                "Y exportar como archivo SRT al instante.",
                "¡No olvides darle un me gusta!",
                "¡Hasta la próxima amigos!"
            )
            "french" -> listOf(
                "Bonjour tout le monde ! Bienvenue.",
                "Aujourd'hui, nous créons des sous-titres.",
                "Cette application est ultra rapide.",
                "Avec dix préréglages de styles viraux.",
                "Regardez ce style incroyable s'appliquer !",
                "Parfait pour vos vidéos TikTok et Reels.",
                "Vous pouvez modifier le texte facilement.",
                "Et exporter en format SRT ou VTT.",
                "Abonnez-vous pour plus de conseils !",
                "À bientôt pour de nouvelles vidéos !"
            )
            "german" -> listOf(
                "Hallo zusammen! Herzlich willkommen.",
                "Heute erstellen wir automatische Untertitel.",
                "Diese App ist extrem schnell und einfach.",
                "Mit zehn viralen Style-Vorlagen.",
                "Schau dir dieses tolle Design an!",
                "Ideal für TikTok, Instagram Reels und Shorts.",
                "Du kannst den Text jederzeit anpassen.",
                "Und sofort als SRT-Datei exportieren.",
                "Lass gerne ein Like und ein Abo da!",
                "Bis zum nächsten Mal, Leute!"
            )
            "hindi" -> listOf(
                "नमस्ते दोस्तों! आपका स्वागत है।",
                "आज हम ऑटोमैटिक सबटाइटल बनाएंगे।",
                "यह ऐप बहुत तेज़ और आसान है।",
                "10 वायरल कैप्शन स्टाइल के साथ।",
                "देखिए यह स्टाइल कितनी शानदार दिखती है!",
                "यूट्यूब शॉर्ट्स और रील्स के लिए बेस्ट है।",
                "आप आसानी से टेक्स्ट एडिट कर सकते हैं।",
                "और तुरंत SRT फाइल एक्सपोर्ट कर सकते हैं।",
                "वीडियो को लाइक ज़रूर करें!",
                "अगली बार फिर मिलेंगे दोस्तों!"
            )
            "japanese" -> listOf(
                "みなさん、こんにちは！歓迎します。",
                "今日は自動字幕を作成していきます。",
                "このアプリは超高速で簡単です。",
                "10種類のバイラル字幕プリセット付き！",
                "この素晴らしいデザインをご覧ください！",
                "TikTokやリール動画に最適です。",
                "タップするだけでテキストを編集可能。",
                "SRTファイルとして即座にエクスポート！",
                "高評価とチャンネル登録をお願いします！",
                "それでは、またお会いしましょう！"
            )
            else -> listOf(
                "Hey everyone! Welcome to the video.",
                "Today, we are generating automatic subtitles.",
                "This app is super fast and easy to use.",
                "Loaded with 10 viral caption presets!",
                "Watch how the style updates instantly.",
                "Perfect for your TikToks and Reels.",
                "You can edit any word by tapping it.",
                "And export as standard SRT instantly.",
                "Don't forget to like and subscribe!",
                "See you in the next one, guys!"
            )
        }

        val list = mutableListOf<SubtitleLine>()
        val totalMs = if (videoDurationMs > 0) videoDurationMs else 30000L
        val segmentDuration = totalMs / phrases.size
        
        for (i in phrases.indices) {
            val start = i * segmentDuration + 500
            val end = (i + 1) * segmentDuration - 300
            list.add(
                SubtitleLine(
                    projectId = 0,
                    startMs = start,
                    endMs = end,
                    text = phrases[i]
                )
            )
        }
        return list
    }

    private fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val byteBuffer = ByteArrayOutputStream()
            val bufferSize = 1024
            val buffer = ByteArray(bufferSize)
            var len: Int
            if (inputStream != null) {
                while (inputStream.read(buffer).also { len = it } != -1) {
                    byteBuffer.write(buffer, 0, len)
                }
            }
            byteBuffer.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read bytes: ${e.message}")
            null
        }
    }

    private fun getLanguageCode(language: String): String {
        return when (language.lowercase()) {
            "english" -> "en"
            "spanish" -> "es"
            "french" -> "fr"
            "german" -> "de"
            "hindi" -> "hi"
            "japanese" -> "ja"
            "chinese" -> "zh"
            "portuguese" -> "pt"
            "arabic" -> "ar"
            "korean" -> "ko"
            else -> "en"
        }
    }
}
