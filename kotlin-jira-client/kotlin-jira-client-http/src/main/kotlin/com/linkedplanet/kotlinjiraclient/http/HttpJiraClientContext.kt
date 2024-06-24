/*-
 * #%L
 * kotlin-jira-client-http
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
package com.linkedplanet.kotlinjiraclient.http

import arrow.core.Either
import com.google.gson.reflect.TypeToken
import com.linkedplanet.kotlinhttpclient.api.http.GSON
import com.linkedplanet.kotlinhttpclient.api.http.HttpClient
import com.linkedplanet.kotlinhttpclient.api.http.HttpResponse
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.http.util.fromHttpDomainError

class HttpJiraClientContext(val baseUrl: String, val httpClient: HttpClient)


suspend inline fun <reified T> HttpClient.execute(
    method: String,
    path: String,
    params: Map<String, String> = emptyMap(),
    body: String? = null,
    contentType: String? = null,
): Either<JiraClientError, HttpResponse<T?>> =
    this.executeRest<T>(
        method,
        path,
        params,
        body,
        contentType = contentType,
        returnType = object : TypeToken<T>() {}.type
    )
        .mapLeft { JiraClientError.fromHttpDomainError(it) }

suspend inline fun <reified T> HttpClient.get(
    path: String,
    params: Map<String, String> = emptyMap(),
): Either<JiraClientError, T?> =
    this.executeGet<T>(
        path,
        params,
        returnType = object : TypeToken<T>() {}.type
    )
        .map { it.body }
        .mapLeft { JiraClientError.fromHttpDomainError(it) }

suspend fun HttpClient.delete(
    path: String,
    params: Map<String, String> = emptyMap(),
): Either<JiraClientError, Unit> =
    this.executeRestCall(
        "DELETE",
        path,
        params = params,
        body = null,
        contentType = null,
    )
        .mapLeft { JiraClientError.fromHttpDomainError(it) }
        .map { /*Unit*/ }

suspend inline fun <reified T, InputType> HttpClient.post(
    path: String,
    body: InputType,
    params: Map<String, String> = emptyMap(),
    contentType: String? = null,
): Either<JiraClientError, T?> =
    this.executeRest<T>(
        method = "POST",
        path,
        params,
        GSON.toJson(body),
        contentType = contentType,
        returnType = object : TypeToken<T>() {}.type
    )
        .map { it.body }
        .mapLeft { JiraClientError.fromHttpDomainError(it) }