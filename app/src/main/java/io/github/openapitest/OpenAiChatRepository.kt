package io.github.openapitest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

class OpenAiChatRepository(
    private val apiKey: String,
    private val model: String,
    private val vectorStoreId: String
) {
    suspend fun answer(question: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw ChatApiException(
                "Configure OPENAI_API_KEY no ambiente ou em local.properties e reconstrua o app."
            )
        }

        val requestBody = JSONObject()
            .put("model", model)
            .put(
                "instructions",
                "Responda em português, com clareza. Considere somente a pergunta recebida nesta chamada."
            )
            .put("input", question)
            .put("store", false)

        if (vectorStoreId.isNotBlank()) {
            requestBody.put(
                "tools",
                JSONArray().put(
                    JSONObject()
                        .put("type", "file_search")
                        .put("vector_store_ids", JSONArray().put(vectorStoreId))
                )
            )
        }

        val connection = (URL(RESPONSES_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }

        try {
            connection.outputStream.use { output ->
                output.write(requestBody.toString().toByteArray(Charsets.UTF_8))
            }

            val statusCode = connection.responseCode
            val responseStream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseText = responseStream
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()

            if (statusCode !in 200..299) {
                throw ChatApiException(errorMessage(statusCode, responseText))
            }

            extractAnswer(responseText)
        } catch (exception: SocketTimeoutException) {
            throw ChatApiException(
                "A OpenAI demorou para responder. Tente novamente.",
                exception
            )
        } catch (exception: UnknownHostException) {
            throw ChatApiException(
                "Não foi possível acessar a OpenAI. Verifique sua conexão com a internet.",
                exception
            )
        } catch (exception: IOException) {
            throw ChatApiException(
                "Falha de conexão com a OpenAI. Tente novamente.",
                exception
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun errorMessage(statusCode: Int, responseText: String): String {
        val apiMessage = runCatching {
            JSONObject(responseText)
                .optJSONObject("error")
                ?.optString("message")
                ?.takeIf(String::isNotBlank)
        }.getOrNull()

        return when (statusCode) {
            HttpURLConnection.HTTP_UNAUTHORIZED -> {
                "A chave da API foi recusada. Confira OPENAI_API_KEY e gere o app novamente."
            }

            HttpURLConnection.HTTP_FORBIDDEN -> {
                apiMessage ?: "A chave não tem permissão para fazer essa chamada."
            }

            HttpURLConnection.HTTP_NOT_FOUND -> {
                apiMessage ?: "O modelo configurado não está disponível para esta API key."
            }

            429 -> "A API atingiu um limite ou não há saldo disponível. Confira o uso da API."
            in 500..599 -> "A OpenAI está com uma falha temporária. Tente novamente."
            else -> apiMessage ?: "A OpenAI retornou o erro HTTP $statusCode."
        }
    }

    private fun extractAnswer(responseText: String): String {
        val response = try {
            JSONObject(responseText)
        } catch (exception: JSONException) {
            throw ChatApiException("A resposta da OpenAI veio em um formato inesperado.", exception)
        }

        val output = response.optJSONArray("output")
            ?: throw ChatApiException("A resposta da OpenAI não contém texto.")

        val answerParts = buildList {
            for (index in 0 until output.length()) {
                val item = output.optJSONObject(index) ?: continue
                if (item.optString("type") != "message") continue

                val content = item.optJSONArray("content") ?: continue
                for (contentIndex in 0 until content.length()) {
                    val part = content.optJSONObject(contentIndex) ?: continue
                    when (part.optString("type")) {
                        "output_text" -> part.optString("text").takeIf(String::isNotBlank)?.let(::add)
                        "refusal" -> part.optString("refusal").takeIf(String::isNotBlank)?.let(::add)
                    }
                }
            }
        }

        return answerParts.joinToString("\n\n").takeIf(String::isNotBlank)
            ?: throw ChatApiException("A OpenAI retornou uma resposta vazia.")
    }

    private companion object {
        const val RESPONSES_URL = "https://api.openai.com/v1/responses"
        const val CONNECT_TIMEOUT_MS = 20_000
        const val READ_TIMEOUT_MS = 90_000
    }
}

class ChatApiException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
