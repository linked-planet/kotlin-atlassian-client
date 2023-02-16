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

import arrow.core.*
import com.linkedplanet.kotlinhttpclient.error.HttpDomainError

@Suppress("unused")
suspend fun <T> recursiveRestCall(
    startIndex: Int = 0,
    pageSize: Int = 100,
    call: suspend (Int, Int) -> Either<HttpDomainError, List<T>>
): Either<HttpDomainError, List<T>> {
    var index = startIndex
    val elements = mutableListOf<T>()
    do {
        val tmpElements: List<T> = call(index, pageSize).getOrHandle {
            return@recursiveRestCall it.left()
        }
        elements.addAll(tmpElements)
        index += tmpElements.size
    } while (tmpElements.size >= pageSize)
    return elements.right()
}

@Suppress("unused")
suspend fun <T> recursiveRestCallPaginated(
    startIndex: Int = 0,
    pageSize: Int = 100,
    call: suspend (Int, Int) -> Either<HttpDomainError, HttpPage<T>>
): Either<HttpDomainError, List<T>> {
    var index = startIndex
    val elements = mutableListOf<T>()
    do {
        val tmpElements: HttpPage<T> = call(index, pageSize).getOrHandle {
            return@recursiveRestCallPaginated it.left()
        }
        elements.addAll(tmpElements.getValues())
        index += tmpElements.getValues().size
    } while (tmpElements.getValues().size >= pageSize)
    return elements.right()
}

@Suppress("unused")
suspend fun <T> recursiveRestCallPaginatedRaw(
    startIndex: Int = 0,
    pageSize: Int = 100,
    call: suspend (Int, Int) -> Either<HttpDomainError, HttpPage<T>>
): Either<HttpDomainError, List<HttpPage<T>>> {
    var index = startIndex
    val elements = mutableListOf<HttpPage<T>>()
    do {
        val tmpElements: HttpPage<T> = call(index, pageSize).getOrHandle {
            return@recursiveRestCallPaginatedRaw it.left()
        }
        elements.add(tmpElements)
        index += tmpElements.getValues().size
    } while (tmpElements.getValues().size >= pageSize)
    return elements.right()
}
