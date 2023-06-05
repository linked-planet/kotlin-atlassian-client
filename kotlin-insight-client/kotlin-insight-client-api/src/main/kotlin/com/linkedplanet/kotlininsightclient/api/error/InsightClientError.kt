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
@file:Suppress("CanBeParameter", "unused") // we want clients to access the additional information

package com.linkedplanet.kotlininsightclient.api.error

import arrow.core.Either
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectTypeId
import com.linkedplanet.kotlininsightclient.api.model.ObjectAttributeValue

@Suppress("unused")
sealed class InsightClientError(
    val error: String,
    val message: String
) {
    val stacktrace: String = Exception(message).stackTraceToString()

    companion object {
        private const val internalErrorString = "Jira/Insight hat ein internes Problem festgestellt"
        fun fromException(e: Exception): InsightClientError =
            ExceptionInsightClientError(e.message ?: internalErrorString, e.stackTraceToString())
        fun fromException(e: Throwable): InsightClientError =
            ExceptionInsightClientError(e.message ?: "Interner Fehler", e.stackTraceToString())

        fun internalError(message: String): Either<InsightClientError, ObjectAttributeValue> =
            InternalInsightClientError("Interner Fehler", message).asEither()

    }
}
fun <T> InsightClientError.asEither(): Either<InsightClientError, T> = Either.Left(this)

class InvalidArgumentInsightClientError(message: String) : InsightClientError("Unerwarteter Parameter", message)

class InternalInsightClientError(error: String, message: String) : InsightClientError(error, message)
class ExceptionInsightClientError(error: String, message: String) : InsightClientError(error, message)

class AuthenticationError(message: String) : InsightClientError("Authentifizierung fehlgeschlagen", message)

class ObjectNotFoundError(val objectId: InsightObjectId):
    InsightClientError("Objekt nicht gefunden", "Das Objekt mit der InsightObjectId=$objectId wurde nicht gefunden.")

class ObjectTypeNotFoundError(val rootObjectTypeId: InsightObjectTypeId) :
    InsightClientError("Objekttyp unbekannt", "Der Objekttyp mit der angegebenen InsightObjectTypeId=$rootObjectTypeId wurde nicht gefunden.")

class OtherNotFoundError(message: String) : InsightClientError("Nicht gefunden.", message)

open class OtherInsightClientError(error: String, message: String) : InsightClientError(error, message)

/**
 * Somewhere inside an HTTP connection failed.
 */
class HttpInsightClientError(val statusCode: Int, error: String, message: String) : InsightClientError(error,
    "$message StatusCode:$statusCode"
)
