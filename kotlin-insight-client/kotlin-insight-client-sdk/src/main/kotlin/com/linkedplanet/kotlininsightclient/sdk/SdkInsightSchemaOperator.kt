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
import com.atlassian.jira.component.ComponentAccessor
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightSchemaOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightSchema
import com.linkedplanet.kotlininsightclient.sdk.util.catchAsInsightClientError
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectSchemaFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeFacade
import com.riadalabs.jira.plugins.insight.services.model.ObjectSchemaBean

object SdkInsightSchemaOperator : InsightSchemaOperator {

    private val objectSchemaFacade by lazy { ComponentAccessor.getOSGiComponentInstanceOfType(ObjectSchemaFacade::class.java) }
    private val objectTypeFacade by lazy { ComponentAccessor.getOSGiComponentInstanceOfType(ObjectTypeFacade::class.java) }
    private val objectFacade by lazy { ComponentAccessor.getOSGiComponentInstanceOfType(ObjectFacade::class.java) }

    override suspend fun getSchemas(): Either<InsightClientError, List<InsightSchema>> =
        catchAsInsightClientError {
            objectSchemaFacade.findObjectSchemaBeans().map { schemaBean: ObjectSchemaBean ->
                insightSchemaFromBean(schemaBean)
            }
        }

    private fun insightSchemaFromBean(schemaBean: ObjectSchemaBean): InsightSchema {
        val objectTypeBeans = objectTypeFacade.findObjectTypeBeansFlat(schemaBean.id)
        val objectCount = objectTypeBeans.fold(0) { count, objectTypeBean ->
            count + objectFacade.countObjectBeans(objectTypeBean.id)
        }
        return InsightSchema(
            id = schemaBean.id,
            name = schemaBean.name,
            objectCount = objectCount,
            objectTypeCount = objectTypeBeans.count(),
        )
    }

    override suspend fun getSchema(id: Int): Either<InsightClientError, InsightSchema> =
        catchAsInsightClientError {
            val objectSchemaBean = objectSchemaFacade.loadObjectSchema(id)
            insightSchemaFromBean(objectSchemaBean)
        }

}
