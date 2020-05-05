/*
 * Copyright (C) 2020 Joseph Samuel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package graphecule.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**Shared [OkHttpClient] for all graphecule use*/
private val okHttpClient: OkHttpClient by lazy { OkHttpClient() }
private val JSON = "application/json; charset=utf-8".toMediaType()

/**
 * Helper to take care of sending request
 *
 * @param apiHost The API Host
 * @param httpRequestAdapter Optionally procvide [HttpRequestAdapter] to handle authentication or handle custom request interception.
 */
class RequestSender(private val apiHost: String, private val httpRequestAdapter: HttpRequestAdapter? = null) {

    /**
     * Sends the given request JSON message.
     *
     * @param requestJson The JSON request
     * @return Returns the response message body string.
     */
    suspend fun sendRequest(requestJson: String): String? = withContext(Dispatchers.IO) {
        val requestBody = requestJson.toRequestBody(JSON)
        val requestBuilder = Request.Builder().url(apiHost).post(requestBody)
        requestBuilder.addHeader("User-Agent", "Graphecule")
        httpRequestAdapter?.requestHeaders?.forEach { key, value ->
            requestBuilder.addHeader(key, value)
        }
        val client = httpRequestAdapter?.httpClient ?: okHttpClient
        val response = client.newCall(requestBuilder.build()).execute()
        val result = response.body?.string()
        result
    }
}