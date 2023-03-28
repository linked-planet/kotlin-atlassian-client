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
package com.linkedplanet.kotlinjiraclient.api.interfaces

import arrow.core.Either
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.model.JiraTransition

/**
 * Manages Jira transitions.
 */
interface JiraTransitionOperator {

    /**
     * Returns a list of available transitions for a given issue.
     * @param issueKey The key of the issue.
     * @return Either an error or a list of transitions.
     */
    suspend fun getAvailableTransitions(issueKey: String): Either<JiraClientError, List<JiraTransition>>

    /**
     * Executes a transition on the specified issue and blocks until the transition has finished.
     * @param issueKey The key of the issue to transition.
     * @param transitionId The ID of the transition.
     * @param comment An optional comment to add to the transition.
     * @return Either an error or a boolean indicating whether the transition was successful.
     */
    suspend fun doTransition(
        issueKey: String,
        transitionId: String,
        comment: String? = null
    ): Either<JiraClientError, Boolean>
}
