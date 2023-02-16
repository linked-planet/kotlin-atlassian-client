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
package com.linkedplanet.kotlinhttpclient.atlas

import arrow.core.Either
import com.atlassian.applinks.api.ApplicationLink
import com.atlassian.applinks.api.ApplicationLinkResponseHandler
import com.atlassian.sal.api.net.*
import com.linkedplanet.kotlinhttpclient.api.http.BaseHttpClient
import com.linkedplanet.kotlinhttpclient.api.http.HttpResponse
import com.linkedplanet.kotlinhttpclient.error.HttpDomainError
import org.apache.http.HttpHeaders
import org.jetbrains.kotlin.library.impl.javaFile

class AtlasHttpClient(private val appLink: ApplicationLink) : BaseHttpClient() {

    override suspend fun executeRestCall(
        method: String,
        path: String,
        params: Map<String, String>,
        body: String?,
        contentType: String?,
        headers: Map<String, String>
    ): Either<HttpDomainError, HttpResponse<String>> =
        try {
            val atlasMethod = Request.MethodType.valueOf(method)
            val parameters = encodeParams(params)
            val pathWithParams = if (params.isNotEmpty()) "$path?${parameters}" else path

            val requestFactory = appLink.createAuthenticatedRequestFactory()
            val requestWithoutBody = requestFactory.createRequest(atlasMethod, pathWithParams)
            val request = if (body == null) {
                requestWithoutBody
            } else {
                requestWithoutBody.setRequestBody(body).setHeader(HttpHeaders.CONTENT_TYPE, contentType)
            }
            request.execute(object : ApplicationLinkResponseHandler<Either<HttpDomainError, HttpResponse<String>>> {
                override fun credentialsRequired(response: Response): Either<HttpDomainError, HttpResponse<String>>? {
                    return null
                }

                override fun handle(response: Response): Either<HttpDomainError, HttpResponse<String>> {
                    return when {
                        response.isSuccessful -> Either.Right(
                            HttpResponse(
                                response.statusCode,
                                response.responseBodyAsString
                            )
                        )
                        else -> {
                            val errorWithStatusCode = """Call to $path failed with 
                            status [${response.statusCode}]
                            statusText [${response.statusText}]
                            body [${response.responseBodyAsString}]"""
                            return Either.Left(HttpDomainError(response.statusCode, "HTTP ERROR", errorWithStatusCode))
                        }
                    }
                }
            })
        } catch (e: ResponseException) {
            Either.Left(HttpDomainError(-1, "Jira/Insight hat ein internes Problem festgestellt", ""))
        }

    override suspend fun executeDownload(
        method: String,
        path: String,
        params: Map<String, String>,
        body: String?,
        contentType: String?
    ): Either<HttpDomainError, HttpResponse<ByteArray>> =
        try {
            val atlasMethod = Request.MethodType.valueOf(method)
            val parameters = encodeParams(params)
            val pathWithParams = if (params.isNotEmpty()) "$path?${parameters}" else path

            val requestFactory = appLink.createAuthenticatedRequestFactory()
            val requestWithoutBody = requestFactory.createRequest(atlasMethod, pathWithParams)
            val request = if (body == null) {
                requestWithoutBody
            } else {
                requestWithoutBody.setRequestBody(body).setHeader(HttpHeaders.CONTENT_TYPE, contentType)
            }
            request.execute(object : ApplicationLinkResponseHandler<Either<HttpDomainError, HttpResponse<ByteArray>>> {
                override fun credentialsRequired(response: Response): Either<HttpDomainError, HttpResponse<ByteArray>>? {
                    return null
                }

                override fun handle(response: Response): Either<HttpDomainError, HttpResponse<ByteArray>> {
                    return when {
                        response.isSuccessful -> Either.Right(
                            HttpResponse(
                                response.statusCode,
                                response.responseBodyAsStream.readBytes()
                            )
                        )
                        else -> {
                            val errorWithStatusCode = """Call to $path failed with 
                            status [${response.statusCode}]
                            statusText [${response.statusText}]
                            body [${response.responseBodyAsString}]"""
                            return Either.Left(HttpDomainError(response.statusCode, "HTTP ERROR", errorWithStatusCode))
                        }
                    }
                }
            })
        } catch (e: ResponseException) {
            Either.Left(HttpDomainError(-1, "Jira/Insight hat ein internes Problem festgestellt", ""))
        }

    override suspend fun executeUpload(
        method: String,
        url: String,
        params: Map<String, String>,
        mimeType: String,
        filename: String,
        byteArray: ByteArray
    ): Either<HttpDomainError, HttpResponse<ByteArray>> =
        try {
            val atlasMethod = Request.MethodType.valueOf(method)
            val parameters = encodeParams(params)
            val pathWithParams = if (params.isNotEmpty()) "$url?${parameters}" else url

            val requestFactory = appLink.createAuthenticatedRequestFactory()
            val requestWithoutBody = requestFactory.createRequest(atlasMethod, pathWithParams)

            val file: java.io.File = org.jetbrains.kotlin.konan.file.createTempFile(filename).javaFile()
            file.writeBytes(byteArray)
            val filePart = RequestFilePart(file, "file")

            val request = requestWithoutBody.setFiles(listOf(filePart))
            request.execute(object : ApplicationLinkResponseHandler<Either<HttpDomainError, HttpResponse<ByteArray>>> {
                override fun credentialsRequired(response: Response): Either<HttpDomainError, HttpResponse<ByteArray>>? {
                    return null
                }

                override fun handle(response: Response): Either<HttpDomainError, HttpResponse<ByteArray>> {
                    return when {
                        response.isSuccessful -> Either.Right(
                            HttpResponse(
                                response.statusCode,
                                byteArray
                            )
                        )
                        else -> {
                            val errorWithStatusCode = """Call to $url failed with 
                            status [${response.statusCode}]
                            statusText [${response.statusText}]
                            body [${response.responseBodyAsString}]"""
                            return Either.Left(HttpDomainError(response.statusCode, "HTTP ERROR", errorWithStatusCode))
                        }
                    }
                }
            })
        } catch (e: ResponseException) {
            Either.Left(HttpDomainError(-1, "Jira/Insight hat ein internes Problem festgestellt", ""))
        }
}
