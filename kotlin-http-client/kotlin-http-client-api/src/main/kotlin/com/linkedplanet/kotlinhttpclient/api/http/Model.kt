/**
 * Copyright 2022-2023 linked-planet GmbH.
 *
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
 */
package com.linkedplanet.kotlinhttpclient.api.http

data class HttpResponse<T>(
    val statusCode: Int,
    val body: T
)

interface HttpPage<T> {
    fun getMaxResults(): Number
    fun getStartAt(): Number
    fun getTotal(): Number
    fun getValues(): List<T>
}

@Suppress("unused")
class DefaultHttpPage<T>(
    private val maxResults: Number,
    private val startAt: Number,
    private val total: Number,
    private val values: List<T>
) : HttpPage<T> {
    override fun getMaxResults() = maxResults

    override fun getStartAt() = startAt

    override fun getTotal() = total

    override fun getValues(): List<T> = values
}


