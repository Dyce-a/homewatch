package com.ilya.homewatch.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ApiException(val code: Int) : Exception("HTTP $code")

object Net {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun postForm(url: String, form: Map<String, String>): JSONObject =
        withContext(Dispatchers.IO) {
            val body = FormBody.Builder().apply {
                form.forEach { (k, v) -> add(k, v) }
            }.build()
            client.newCall(Request.Builder().url(url).post(body).build()).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                JSONObject(if (text.isBlank()) "{}" else text)
            }
        }

    suspend fun getJson(url: String, token: String): JSONObject =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw ApiException(resp.code)
                JSONObject(resp.body?.string().orEmpty())
            }
        }

    suspend fun postJson(url: String, token: String, json: JSONObject): JSONObject =
        withContext(Dispatchers.IO) {
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .post(body)
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw ApiException(resp.code)
                val text = resp.body?.string().orEmpty()
                JSONObject(if (text.isBlank()) "{}" else text)
            }
        }
}
