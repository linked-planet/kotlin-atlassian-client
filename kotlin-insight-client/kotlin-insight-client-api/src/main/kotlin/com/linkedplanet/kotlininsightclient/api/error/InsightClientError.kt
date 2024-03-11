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
import com.linkedplanet.kotlinatlassianclientcore.common.error.AtlassianClientError
import com.linkedplanet.kotlinatlassianclientcore.common.error.asEither
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectTypeId
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    oneOf = [
        InvalidArgumentInsightClientError::class,
        InternalInsightClientError::class,
        ExceptionInsightClientError::class,
        AuthenticationError::class,
        ObjectNotFoundError::class,
        ObjectTypeNotFoundError::class,
        OtherNotFoundError::class,
        OtherInsightClientError::class,
        HttpInsightClientError::class
    ]
)

@Suppress("unused")
sealed class InsightClientError(
    error: String,
    message: String,
    stacktrace: String = "",
    statusCode: Int? = null
) : AtlassianClientError(error, message, stacktrace, statusCode) {

    companion object {
        private const val internalErrorString = "Jira/Insight hat ein internes Problem festgestellt"
        fun fromException(e: Throwable): InsightClientError =
            ExceptionInsightClientError("Insight-Fehler", e.message ?: internalErrorString, e.stackTraceToString())

        fun internalError(message: String): Either<InsightClientError, InsightAttribute> =
            InternalInsightClientError("Interner Insight-Fehler", message).asEither()

    }
}

class InvalidArgumentInsightClientError(message: String) : InsightClientError("Unerwarteter Parameter", message)

class InternalInsightClientError(error: String, message: String) : InsightClientError(error, message)
class ExceptionInsightClientError(error: String, message: String, stacktrace: String) : InsightClientError(error, message, stacktrace)

class AuthenticationError(message: String) : InsightClientError("Authentifizierung fehlgeschlagen", message)

class ObjectNotFoundError(val objectId: InsightObjectId):
    InsightClientError("Insight Objekt nicht gefunden", "Das Objekt mit der InsightObjectId=$objectId wurde nicht gefunden.")

class ObjectTypeNotFoundError(val rootObjectTypeId: InsightObjectTypeId) :
    InsightClientError("Insight Objekttyp unbekannt", "Der Objekttyp mit der angegebenen InsightObjectTypeId=$rootObjectTypeId wurde nicht gefunden.")

class OtherNotFoundError(message: String) : InsightClientError("Nicht gefunden.", message)

open class OtherInsightClientError(error: String, message: String) : InsightClientError(error, message)

/**
 * Somewhere inside an HTTP connection failed.
 */
class HttpInsightClientError(statusCode: Int, error: String, message: String) :
    InsightClientError(
        error = error,
        message = "$message StatusCode:$statusCode",
        statusCode = statusCode
)
