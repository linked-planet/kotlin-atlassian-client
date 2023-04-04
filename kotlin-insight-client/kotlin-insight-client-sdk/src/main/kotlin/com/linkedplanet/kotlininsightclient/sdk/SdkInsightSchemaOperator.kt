/*-
 * #%L
 * kotlin-insight-client-api
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
package com.linkedplanet.kotlininsightclient.sdk

import arrow.core.Either
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightSchemaOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightSchema
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectSchemaFacade
import javax.inject.Inject

object SdkInsightSchemaOperator : InsightSchemaOperator {

    @ComponentImport
    lateinit var objectSchemaFacade: ObjectSchemaFacade

    @Inject
    fun init(
        @ComponentImport objectSchemaFacade: ObjectSchemaFacade,
    ) {
        this.objectSchemaFacade = objectSchemaFacade
    }

    override suspend fun getSchemas(): Either<InsightClientError, List<InsightSchema>> {
        TODO()
//        val schemas = objectSchemaFacade.findObjectSchemaBeans().map { bean ->
//            InsightSchema(
//                bean.id,
//                bean.name,
//            )
//        }
//        return schemas
    }

    override suspend fun getSchema(id: Int): Either<InsightClientError, InsightSchema> {
        TODO()
    }

}
