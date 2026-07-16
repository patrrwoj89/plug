package com.polishmediahub.app.data.remote.cloud

import com.polishmediahub.app.BuildConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface CloudProfileSyncClient {
    suspend fun uploadProfileBackup(zipBytes: ByteArray, workerUrl: String, token: String): Result<String>
    suspend fun downloadProfileBackup(workerUrl: String, token: String): Result<ByteArray>
}

@Singleton
class OkHttpCloudProfileSyncClient @Inject constructor(
    private val client: OkHttpClient
) : CloudProfileSyncClient {

    override suspend fun uploadProfileBackup(
        zipBytes: ByteArray,
        workerUrl: String,
        token: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = workerUrl.removeSuffix("/") + "/api/sync-profiles"
            val mediaType = "application/zip".toMediaTypeOrNull()
            val body = zipBytes.toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .header("X-Hub-Token", token)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("Profile backup upload failed: HTTP ${response.code}")
                    )
                }
                val bodyText = response.body?.string() ?: "ok"
                Result.success(bodyText)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.w(TAG, "uploadProfileBackup failed", e)
            Result.failure(e)
        }
    }

    override suspend fun downloadProfileBackup(
        workerUrl: String,
        token: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val url = workerUrl.removeSuffix("/") + "/api/sync-profiles"
            val request = Request.Builder()
                .url(url)
                .header("X-Hub-Token", token)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("Profile backup download failed: HTTP ${response.code}")
                    )
                }
                val bytes = response.body?.bytes() ?: byteArrayOf()
                if (bytes.isEmpty()) {
                    return@withContext Result.failure(IOException("Profile backup is empty"))
                }
                Result.success(bytes)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.w(TAG, "downloadProfileBackup failed", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "OkHttpCloudProfileSyncClient"
    }
}
