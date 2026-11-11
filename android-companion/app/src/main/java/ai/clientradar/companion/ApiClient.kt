package ai.clientradar.companion

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    // AppDeploy's native API gateway is separate from the public frontend host.
    private const val API_BASE_URL = "https://api-v2.appdeploy.ai/app/clientradar-fxrsau"

    fun post(path: String, payload: JSONObject): JSONObject {
        val connection = (URL(API_BASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("User-Agent", "ClientRadar-Companion/1.0")
        }

        return try {
            connection.outputStream.use {
                it.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val contentType = connection.contentType.orEmpty()

            if (status !in 200..299) {
                val message = try {
                    val json = JSONObject(response)
                    json.optString("error").ifBlank {
                        json.optString("message").ifBlank { "HTTP $status" }
                    }
                } catch (_: Exception) {
                    "HTTP $status from ClientRadar API. " + response.take(180).replace("\\s+".toRegex(), " ")
                }
                throw IllegalStateException(message)
            }

            try {
                JSONObject(response)
            } catch (_: Exception) {
                val preview = response.take(180).replace("\\s+".toRegex(), " ")
                throw IllegalStateException(
                    "ClientRadar API returned non-JSON data (HTTP $status, $contentType). $preview"
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    fun exchangeCode(code: String): String {
        val result = post("/api/companion/exchange", JSONObject().put("code", code))
        return result.getString("deviceToken")
    }

    fun fetchKeywords(token: String): Set<String> {
        val result = post("/api/companion/keywords", JSONObject().put("deviceToken", token))
        val array: JSONArray = result.optJSONArray("keywords") ?: JSONArray()
        return buildSet {
            for (i in 0 until array.length()) {
                val phrase = array.optString(i).trim()
                if (phrase.length >= 2) add(phrase)
            }
        }
    }

    fun sendNotification(token: String, packageName: String, title: String, text: String) {
        post(
            "/api/companion/ingest",
            JSONObject()
                .put("deviceToken", token)
                .put("packageName", packageName)
                .put("title", title)
                .put("text", text)
        )
    }
}
