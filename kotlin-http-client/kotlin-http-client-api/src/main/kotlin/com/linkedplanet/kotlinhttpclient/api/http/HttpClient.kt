package com.linkedplanet.kotlinhttpclient.api.http

import arrow.core.Either
import com.linkedplanet.kotlinhttpclient.error.HttpDomainError
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
    ): Either<HttpDomainError, HttpResponse<ByteArray>>

    suspend fun executeUpload(
        method: String,
        url: String,
        params: Map<String, String>,
        mimeType: String,
        filename: String,
        byteArray: ByteArray
    ): Either<HttpDomainError, HttpResponse<ByteArray>>

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
