/*-
 * #%L
 * kotlin-jira-client-api
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
package com.linkedplanet.kotlininsightclient.api.error

import arrow.core.Either
import com.linkedplanet.kotlininsightclient.api.model.ObjectAttributeValue

@Suppress("unused")
open class InsightClientError(
    val error: String,
    val message: String
) {
    val stacktrace: String = Exception(message).stackTraceToString()

    companion object {
        fun fromException(e: Exception): InsightClientError =
            InsightClientError(e.message ?: "Interner Fehler", e.stackTraceToString())
        fun fromException(e: Throwable): InsightClientError =
            InsightClientError(e.message ?: "Interner Fehler", e.stackTraceToString())

        fun internalError(message: String): Either<InsightClientError, ObjectAttributeValue> = Either.Left(
            InsightClientError("InternalError", message)
        )
    }
}



class ObjectTypeNotFoundError :
    InsightClientError("Nicht gefunden", "Der ObjectType mit der angegebenen Id wurde nicht gefunden.")
