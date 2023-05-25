/*-
 * #%L
 * kotlin-http-client-atlas
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
package com.linkedplanet.kotlinhttpclient.ktor

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.linkedplanet.kotlinhttpclient.api.http.BaseHttpClient
import com.linkedplanet.kotlinhttpclient.api.http.HttpResponse
import com.linkedplanet.kotlinhttpclient.error.HttpDomainError
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.utils.io.streams.*
import java.io.InputStream
import java.text.DateFormat

class KtorHttpClient(
    private val baseUrl: String,
    username: String,
    password: String,
    configureHttpClient: (HttpClientConfig<ApacheEngineConfig>) -> Unit = {}
) : BaseHttpClient() {

    private var httpClient: HttpClient = createHttpClient(username, password, configureHttpClient)

    private fun prepareRequest(
        requestBuilder: HttpRequestBuilder,
        path: String,
        params: Map<String, String>,
        bodyIn: String?,
        contentType: String?
    ) {
        val parsedContentType = contentType
            ?.let { ContentType.parse(it) }
            ?: ContentType.Application.Json
        val parameterString = params
            .takeIf { it.isNotEmpty() }
            ?.let { "?${encodeParams(params)}" }
            ?: ""
        requestBuilder.url("$baseUrl/$path$parameterString")
        requestBuilder.contentType(parsedContentType)
        if (bodyIn != null) {
            requestBuilder.setBody(bodyIn)
        }
    }

    override suspend fun executeRestCall(
        method: String,
        path: String,
        params: Map<String, String>,
        body: String?,
        contentType: String?,
        headers: Map<String, String>
    ): Either<HttpDomainError, HttpResponse<String>> {
        val pathWithoutPrefix = path.removePrefix("/")
        return when (method) {
            "GET" -> {
                httpClient.get {
                    prepareRequest(this, pathWithoutPrefix, params, body, contentType)
                }.handleResponse()
            }

            "POST" -> {
                httpClient.post(pathWithoutPrefix) {
                    prepareRequest(this, pathWithoutPrefix, params, body, contentType)
                }.handleResponse()
            }

            "PUT" -> {
                httpClient.put {
                    prepareRequest(this, pathWithoutPrefix, params, body, contentType)
                }.handleResponse()
            }

            "DELETE" -> {
                httpClient.delete {
                    prepareRequest(this, pathWithoutPrefix, params, body, contentType)
                }.handleResponse()
            }

            else -> {
                HttpDomainError(500, "HTTP-ERROR", "Method '$method' not available").left()
            }
        }
    }

    override suspend fun executeDownload(
        method: String,
        path: String,
        params: Map<String, String>,
        body: String?,
        contentType: String?
    ): Either<HttpDomainError, HttpResponse<InputStream>> {
        return httpClient.get {
            url(path)
        }.handleResponse()
    }

    override suspend fun executeUpload(
        method: String,
        url: String,
        params: Map<String, String>,
        mimeType: String,
        filename: String,
        inputStream: InputStream
    ): Either<HttpDomainError, HttpResponse<InputStream>> {
        val post = httpClient.submitFormWithBinaryData(
            formData = formData {
                append(
                    key = "file",
                    InputProvider(inputStream.available().toLong()) { inputStream.asInput() },
                    Headers.build {
                        append(HttpHeaders.ContentType, mimeType)
                        append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                    })
            }
        ) {
            url("$baseUrl$url")
            header("Connection", "keep-alive")
            header("Cache-Control", "no-cache")
        }
        return post.handleResponse()
    }


    private suspend inline fun <reified T> io.ktor.client.statement.HttpResponse.handleResponse(): Either<HttpDomainError, HttpResponse<T>> =
        if (this.status.value < 400) {
            HttpResponse<T>(
                this.status.value,
                this.body()
            ).right()
        } else {
            HttpDomainError(
                this.status.value,
                "HTTP-ERROR",
                this.body()
            ).left()
        }
}

private fun createHttpClient(
    username: String,
    password: String,
    configureHttpClient: (HttpClientConfig<ApacheEngineConfig>) -> Unit
) =
    HttpClient(Apache) {
        expectSuccess = false
        install(ContentNegotiation) {
            gson {
                setDateFormat(DateFormat.LONG)
                setPrettyPrinting()
            }
        }
        install(Auth) {
            basic {
                sendWithoutRequest { true }
                credentials {
                    BasicAuthCredentials(username = username, password = password)
                }
            }
        }
        configureHttpClient(this)
    }
