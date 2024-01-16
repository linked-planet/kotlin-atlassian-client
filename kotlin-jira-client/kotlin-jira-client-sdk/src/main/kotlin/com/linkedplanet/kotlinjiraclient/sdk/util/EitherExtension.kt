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
package com.linkedplanet.kotlinjiraclient.sdk.util

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.fold
import arrow.core.right
import com.atlassian.jira.bc.ServiceResult
import com.atlassian.jira.util.ErrorCollection
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import org.jetbrains.kotlin.util.removeSuffixIfPresent

/**
 * Allows you to call toEither().bind() on all ServiceResults
 */
fun <T : ServiceResult> T.toEither(errorTitle: String? = null): Either<JiraClientError, T> =
    when {
        this.isValid -> Either.Right(this)
        else -> Either.Left(jiraClientError(this.errorCollection, errorTitle
            ?: "${this::class.simpleName?.removeSuffixIfPresent("ServiceResult")}Error"))
    }

fun ErrorCollection.toEither(errorTitle: String = "SdkError") : Either<JiraClientError, Unit> =
    when {
        this.hasAnyErrors() -> jiraClientError(this, errorTitle).left()
        else -> Unit.right()
    }

fun jiraClientError(errorCollection: ErrorCollection, errorTitle: String = "SdkError"): JiraClientError {
    val worstReason = ErrorCollection.Reason.getWorstReason(errorCollection.reasons)
    return JiraClientError(
        errorTitle,
        errorCollection.errorMessages.joinToString() + " (${worstReason.httpStatusCode})"
    )
}

inline fun <B> Either.Companion.catchJiraClientError(
    error: String? = null,
    message: String? = null,
    f: () -> B
): Either<JiraClientError, B> = catch(f).mapLeft {
    JiraClientError(
        error = error ?: "Jira-Fehler",
        message = message ?: it.message ?: "-",
        stacktrace = it.stackTraceToString()
    )
}

inline fun <A : Any> eitherAndCatch(block: Raise<JiraClientError>.() -> A): Either<JiraClientError, A> =
    fold({
        Either.catchJiraClientError{
            block.invoke(this)
        }.bind()
    }, { Either.Left(it) }, { Either.Right(it) })

