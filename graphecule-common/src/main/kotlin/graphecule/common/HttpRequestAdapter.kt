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

import okhttp3.Interceptor
import okhttp3.OkHttpClient


/**
 * Used for updating the request that are send to the graphql server.
 *
 * This can be used to handle authentication with the graphql server
 *
 * @param requestHeaders Optionally specifies the headers to be added to the request. Add the auth headers here.
 * @param rateLimit Optionally specify milliseconds to wait between each batch of requests to prevent rate throttling from server.
 *                  Default of 0 means no delay.
 * @param maxParallelRequests Optionally specify how many maximum parallel requests can be send to server. Default 8.
 *                  Use it to reduce parallel requests to servers that throttle them.
 * @param httpClient Optionally specifies the [OkHttpClient] to use. You can then add your own [Interceptor] to handler authentication.
 */
data class HttpRequestAdapter(val requestHeaders: Map<String, String>? = null,
                              val rateLimit: Long = 0,
                              val maxParallelRequests: Int = 8,
                              val httpClient: OkHttpClient? = null)

