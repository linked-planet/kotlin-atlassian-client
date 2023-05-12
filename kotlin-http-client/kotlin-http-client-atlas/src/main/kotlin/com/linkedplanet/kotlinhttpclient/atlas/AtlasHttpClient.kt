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
import com.atlassian.applinks.api.ApplicationLinkRequest
import com.atlassian.applinks.api.ApplicationLinkResponseHandler
import com.atlassian.sal.api.net.Request
import com.atlassian.sal.api.net.RequestFilePart
import com.atlassian.sal.api.net.Response
import com.atlassian.sal.api.net.ResponseException
import com.linkedplanet.kotlinhttpclient.api.http.BaseHttpClient
import com.linkedplanet.kotlinhttpclient.api.http.HttpResponse
import com.linkedplanet.kotlinhttpclient.error.HttpDomainError
import org.apache.http.HttpHeaders
import org.jetbrains.kotlin.library.impl.javaFile
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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
            val request = applicationLinkRequest(method, params, path)
            if (body != null) {
                request
                    .setRequestBody(body)
                    .setHeader(HttpHeaders.CONTENT_TYPE, contentType)
            }
            request.execute(object : ApplicationLinkResponseHandler<Either<HttpDomainError, HttpResponse<String>>> {
                override fun credentialsRequired(response: Response) = null

                override fun handle(response: Response): Either<HttpDomainError, HttpResponse<String>> = when {
                    response.isSuccessful -> Either.Right(
                        HttpResponse(
                            response.statusCode,
                            response.responseBodyAsString
                        )
                    )
                    else -> httpDomainErrorFromResponse(path, response)
                }
            })
        } catch (e: ResponseException) {
            wrapAsGenericHttpDomainError(e)
        }

    override suspend fun executeDownload(
        method: String,
        path: String,
        params: Map<String, String>,
        body: String?,
        contentType: String?
    ): Either<HttpDomainError, HttpResponse<InputStream>> =
        try {
            val request = applicationLinkRequest(method, params, path)
            if (body != null) {
                request
                    .setRequestBody(body)
                    .setHeader(HttpHeaders.CONTENT_TYPE, contentType)
            }
            request.execute(object : ApplicationLinkResponseHandler<Either<HttpDomainError, HttpResponse<InputStream>>> {
                override fun credentialsRequired(response: Response) = null

                override fun handle(response: Response): Either<HttpDomainError, HttpResponse<InputStream>> =
                    when {
                        response.isSuccessful -> Either.Right(
                            HttpResponse(
                                response.statusCode,
                                response.responseBodyAsStream
                            )
                        )
                        else -> httpDomainErrorFromResponse(path, response)
                    }
            })
        } catch (e: ResponseException) {
            wrapAsGenericHttpDomainError(e)
        }

    override suspend fun executeUpload(
        method: String,
        url: String,
        params: Map<String, String>,
        mimeType: String,
        filename: String,
        inputStream: InputStream
    ): Either<HttpDomainError, HttpResponse<InputStream>> =
        try {
            val request: ApplicationLinkRequest = applicationLinkRequest(method, params, url)
            val file = tempFileWithData(filename, inputStream)
            val filePart = RequestFilePart(mimeType, filename, file, "file")
            request.setFiles(listOf(filePart))

            request.execute(object : ApplicationLinkResponseHandler<Either<HttpDomainError, HttpResponse<InputStream>>> {
                override fun credentialsRequired(response: Response) = null

                override fun handle(response: Response): Either<HttpDomainError, HttpResponse<InputStream>> =
                    when {
                        response.isSuccessful -> Either.Right(HttpResponse(response.statusCode, inputStream))
                        else -> httpDomainErrorFromResponse(url, response)
                    }
            })
        } catch (e: ResponseException) {
            wrapAsGenericHttpDomainError(e)
        }

    private fun tempFileWithData(filename: String, inputStream: InputStream): File {
        val file: File = org.jetbrains.kotlin.konan.file.createTempFile(filename).javaFile()
        Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        return file
    }

    private fun httpDomainErrorFromResponse(
        path: String,
        response: Response
    ): Either.Left<HttpDomainError> {
        val errorWithStatusCode = """Call to $path failed with 
                                status [${response.statusCode}]
                                statusText [${response.statusText}]
                                body [${response.responseBodyAsString}]"""
        return Either.Left(HttpDomainError(response.statusCode, "HTTP ERROR", errorWithStatusCode))
    }

    private fun wrapAsGenericHttpDomainError(e: ResponseException) = Either.Left(
        HttpDomainError(
            400,
            "Jira/Insight hat ein internes Problem festgestellt",
            e.message.toString()
        )
    )

    private fun applicationLinkRequest(
        method: String,
        params: Map<String, String>,
        path: String
    ): ApplicationLinkRequest {
        val atlasMethod = Request.MethodType.valueOf(method)
        val parameters = encodeParams(params)
        val pathWithParams = if (params.isNotEmpty()) "$path?${parameters}" else path

        val requestFactory = appLink.createAuthenticatedRequestFactory()
        val requestWithoutBody = requestFactory.createRequest(atlasMethod, pathWithParams)
        return requestWithoutBody
    }
}
