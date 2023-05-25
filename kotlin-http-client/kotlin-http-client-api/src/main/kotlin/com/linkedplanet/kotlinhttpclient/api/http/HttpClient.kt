/*-
 * #%L
 * kotlin-http-client-api
 * %%
 * Copyright (C) 2022 - 2023 linked-planet GmbH
 * %%
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
 * #L%
 */
package com.linkedplanet.kotlinhttpclient.api.http

import arrow.core.Either
import com.linkedplanet.kotlinhttpclient.error.HttpDomainError
import java.io.InputStream
import java.lang.reflect.Type

interface HttpClient {
    suspend fun executeRestCall(
        method: String,
        path: String,
        params: Map<String, String>,
        body: String?,
        contentType: String?,
        headers: Map<String, String> = emptyMap()
    ): Either<HttpDomainError, HttpResponse<String>>

    suspend fun executeDownload(
        method: String,
        path: String,
        params: Map<String, String>,
        body: String?,
        contentType: String?
    ): Either<HttpDomainError, HttpResponse<InputStream>>

    suspend fun executeUpload(
        method: String,
        url: String,
        params: Map<String, String>,
        mimeType: String,
        filename: String,
        inputStream: InputStream
    ): Either<HttpDomainError, HttpResponse<InputStream>>

    suspend fun <T> executeRest(
        method: String,
        path: String,
        params: Map<String, String>,
        body: String?,
        contentType: String?,
        returnType: Type
    ): Either<HttpDomainError, HttpResponse<T?>>

    suspend fun <T> executeRestList(
        method: String,
        path: String,
        params: Map<String, String>,
        body: String?,
        contentType: String?,
        returnType: Type
    ): Either<HttpDomainError, HttpResponse<List<T>>>

    suspend fun <T> executeGet(
        path: String,
        params: Map<String, String>,
        returnType: Type
    ): Either<HttpDomainError, HttpResponse<T?>>

    suspend fun <T> executeGetReturnList(
        path: String,
        params: Map<String, String>,
        returnType: Type
    ): Either<HttpDomainError, HttpResponse<List<T>?>>

    suspend fun executeGetCall(
        path: String,
        params: Map<String, String>
    ): Either<HttpDomainError, HttpResponse<String>>

    fun encodeParams(map: Map<String, String>): String
}
