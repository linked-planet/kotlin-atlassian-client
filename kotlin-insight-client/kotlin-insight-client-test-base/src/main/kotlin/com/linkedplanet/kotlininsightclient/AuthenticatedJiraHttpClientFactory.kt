/*
 * #%L
 * jcp
 * %%
 * Copyright (C) 2021 - 2022 The Plugin Authors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.linkedplanet.kotlininsightclient

import arrow.core.Either
import arrow.core.right
import com.google.gson.Gson
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import org.slf4j.LoggerFactory
import org.http4k.client.Java8HttpClient
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.cookie.BasicCookieStorage
import org.http4k.filter.cookie.CookieStorage
import java.util.*

/**
 * Provides access to Jira itself via a logged in AuthenticatedHttpHandler
 */
class AuthenticatedJiraHttpClientFactory(
    val jiraOrigin: String
) {
    companion object {
        data class Credentials(val username: String, val password: String)
    }
    private val log = LoggerFactory.getLogger(this::class.java)

    private val storage: CookieStorage = BasicCookieStorage() // this is just a HashMap
    private val httpHandler: HttpHandler = ClientFilters.Cookies(storage = storage).then(Java8HttpClient())
    private val gson = Gson()

    fun login(credentials: Credentials): Either<InsightClientError, AuthenticatedHttpHandler> {
        val username = credentials.username
        val password = credentials.password
        val body = gson.toJson(credentials)
        val request = Request(Method.POST, "$jiraOrigin/rest/auth/1/session")
            .header("content-type", "application/json")
            .body(Body(body))
        val loginResponse = httpHandler(request)
        if (loginResponse.status != Status.OK) {
            log.debug("Continue despite 'session' login failing with HTTP StatusCode:${loginResponse.status.code}")
        }
        val privateHandler = object : AuthenticatedHttpHandler, HttpHandler by httpHandler {
            override fun getWithRelativePath(path: String): Response {
                val absolutePath = "$jiraOrigin$path"
                val getRequest = Request(Method.GET, absolutePath)
                    .header(
                        "Authorization",
                        "Basic ${Base64.getEncoder().encodeToString("$username:$password".toByteArray())}"
                    )
                return this(getRequest)
            }
        }
        return privateHandler.right()
    }
}

interface AuthenticatedHttpHandler : HttpHandler {
    fun getWithRelativePath(path: String): Response
}
