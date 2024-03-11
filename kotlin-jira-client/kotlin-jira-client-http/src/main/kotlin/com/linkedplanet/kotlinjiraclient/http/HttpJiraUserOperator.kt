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
package com.linkedplanet.kotlinjiraclient.http

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.left
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.linkedplanet.kotlinhttpclient.api.http.*
import com.linkedplanet.kotlinjiraclient.api.error.JiraClientError
import com.linkedplanet.kotlinjiraclient.api.interfaces.JiraUserOperator
import com.linkedplanet.kotlinatlassianclientcore.common.api.JiraUser
import com.linkedplanet.kotlinjiraclient.http.model.*
import com.linkedplanet.kotlinjiraclient.http.util.fromHttpDomainError

class HttpJiraUserOperator(private val context: HttpJiraClientContext) : JiraUserOperator {

    override suspend fun getUsersByProjectKey(projectKey: String): Either<JiraClientError, List<JiraUser>> = either {
        val roleIds = getProjectRoleUrls(projectKey).bind()
        val roleActors = roleIds.map { roleId -> getProjectRoleActors(projectKey, roleId).bind() }.flatten().distinctBy { it.name }
        val users = roleActors.map { actor -> mapProjectRoleActorToUsers(actor).bind() }.flatten().distinctBy { it.name }
        users
    }

    override suspend fun getProjectAdminUsers(projectKey: String): Either<JiraClientError, List<JiraUser>> =
        getUsersByProjectKeyAndPermission(projectKey, "PROJECT_ADMIN")

    override suspend fun getSystemAdminUsers(): Either<JiraClientError, List<JiraUser>> =
        getUsersPaginated("rest/api/2/group/member?groupname=jira-administrators")

    override suspend fun getAssignableUsersByProjectKey(projectKey: String): Either<JiraClientError, List<JiraUser>> =
        getUsersByProjectKeyAndPermission(projectKey, "ASSIGNABLE_USER")

    override suspend fun getUsersByProjectKeyAndPermission(
        projectKey: String,
        permissionName: String
    ): Either<JiraClientError, List<JiraUser>> =
        getUsers("/rest/api/2/user/permission/search?permissions=$permissionName&projectKey=$projectKey")

    private suspend fun getUsers(path: String): Either<JiraClientError, List<JiraUser>> = either {
        recursiveRestCall { index, pageSize ->
            context.httpClient.executeRestList<HttpJiraUser>(
                "GET",
                "$path&startAt=$index&maxResults=$pageSize",
                emptyMap(),
                null,
                "application/json",
                object : TypeToken<List<HttpJiraUser>>() {}.type
            ).map { it.body }
        }
            .mapLeft { JiraClientError.fromHttpDomainError(it) }
            .bind()
            .toJiraUsers()
    }

    private suspend fun getUsersPaginated(path: String): Either<JiraClientError, List<JiraUser>> = either {
        recursiveRestCallPaginated { index, pageSize ->
            context.httpClient.executeRest<DefaultHttpPage<HttpJiraUser>>(
                "GET",
                "$path&startAt=$index&maxResults=$pageSize",
                emptyMap(),
                null,
                "application/json",
                object : TypeToken<DefaultHttpPage<HttpJiraUser>>() {}.type
            ).map { it.body!! }
        }
            .mapLeft { JiraClientError.fromHttpDomainError(it) }
            .bind()
            .toJiraUsers()
    }

    private suspend fun getProjectRoleUrls(projectKey: String): Either<JiraClientError, List<String>> = either {
        val successResponse = context.httpClient.executeGetCall(
            "/rest/api/2/project/$projectKey/role",
            mapOf(),
        )
            .map { it.body }
            .mapLeft { JiraClientError.fromHttpDomainError(it) }.bind()

        // Keep JsonParser instantiation for downwards compatibility
        @Suppress("DEPRECATION")
        val jsonObject = JsonParser().parse(successResponse).asJsonObject
        jsonObject.entrySet().map {
            it.value
                .asString
                .substringAfterLast("/")
        }
    }

    private suspend fun getProjectRoleActors(
        projectKey: String,
        roleId: String
    ): Either<JiraClientError, List<HttpJiraRoleActor>> = either {
        context.httpClient.executeGet<HttpJiraRole>(
            "/rest/api/2/project/$projectKey/role/$roleId",
            mapOf(),
            object : TypeToken<HttpJiraRole>() {}.type
        )
            .map { it.body!!.actors }
            .mapLeft {
                JiraClientError.fromHttpDomainError(it)
            }.bind()
    }

    private suspend fun mapProjectRoleActorToUsers(actor: HttpJiraRoleActor): Either<JiraClientError, List<JiraUser>> =
        when (actor.type) {
            "atlassian-user-role-actor" -> {
                getUser(actor.name).map { listOf(it) }
            }

            "atlassian-group-role-actor" -> {
                getGroupMembers(actor.name)
            }

            else -> JiraClientError(
                "Unknown actor type",
                "Actor type ${actor.type} for actor ${actor.name} is not known.",
                statusCode = 404
            ).left()
        }


    private suspend fun getUser(username: String): Either<JiraClientError, JiraUser> = either {
        context.httpClient.executeGet<HttpJiraUser>(
            "/rest/api/2/user",
            mapOf(
                "username" to username
            ),
            object : TypeToken<HttpJiraUser>() {}.type
        )
            .map { it.body!!.toJiraUser() }
            .mapLeft {
                JiraClientError.fromHttpDomainError(it)
            }.bind()
    }

    private suspend fun getGroupMembers(groupname: String): Either<JiraClientError, List<JiraUser>> =
        getUsersPaginated("/rest/api/2/group/member?groupname=$groupname")
}
