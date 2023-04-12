/*-
 * #%L
 * kotlin-insight-client-sdk
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
import com.atlassian.jira.component.ComponentAccessor.getOSGiComponentInstanceOfType
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightAttachmentOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightAttachment
import com.linkedplanet.kotlininsightclient.sdk.services.ReverseEngineeredAttachmentUrlResolver
import com.linkedplanet.kotlininsightclient.sdk.services.ReverseEngineeredFileManager
import com.linkedplanet.kotlininsightclient.sdk.util.catchInsightClientError
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.services.model.AttachmentBean
import java.nio.file.Path
import kotlin.io.path.createTempFile

object SdkInsightAttachmentOperator : InsightAttachmentOperator {

    private val objectFacade by lazy { getOSGiComponentInstanceOfType(ObjectFacade::class.java) }

    private val fileManager by lazy { ReverseEngineeredFileManager() }
    private val attachmentUrlResolver by lazy { ReverseEngineeredAttachmentUrlResolver() }

    override suspend fun getAttachments(objectId: Int): Either<InsightClientError, List<InsightAttachment>> =
        Either.catchInsightClientError {
            objectFacade
                .findAttachmentBeans(objectId)
                .map(::beanToInsightAttachment)
        }

    override suspend fun downloadAttachment(url: String): Either<InsightClientError, ByteArray?> =
        Either.catchInsightClientError {
            val attachmentId = attachmentUrlResolver.parseAttachmentIdFromPathInformation(url)
            val attachmentBean = objectFacade.loadAttachmentBeanById(attachmentId)
            val inputStream =
                fileManager.getObjectAttachmentContent(attachmentBean.objectId, attachmentBean.nameInFileSystem)
            inputStream.readBytes()
        }

    override suspend fun downloadAttachmentZip(objectId: Int): Either<InsightClientError, ByteArray> =
        Either.catchInsightClientError {
            TODO("Not yet implemented")
        }

    override suspend fun uploadAttachment(
        objectId: Int,
        filename: String,
        byteArray: ByteArray,
        comment: String
    ): Either<InsightClientError, List<InsightAttachment>> =
        Either.catchInsightClientError {
            val tempFilePath: Path = createTempFile(filename)
            val tempFile = tempFilePath.toFile()
            tempFile.writeBytes(byteArray)
            val bean = objectFacade.addAttachmentBean(objectId, tempFile, filename, "", comment) //TODO content-type
            val insightAttachment = beanToInsightAttachment(bean)
            listOf(insightAttachment)
        }

    override suspend fun deleteAttachment(attachmentId: Int): Either<InsightClientError, String> =
        Either.catchInsightClientError {
            objectFacade.deleteAttachmentBean(attachmentId).toString()
        }

    private fun beanToInsightAttachment(bean: AttachmentBean): InsightAttachment = bean.run {
        val url = attachmentUrlResolver.buildUrlForAttachment(bean)
        InsightAttachment(
            id!!, // we assume that when getting existing attachments, the id is always set
            author,
            mimeType,
            filename,
            fileSize.toString(), //TODO:use long!
            created.toString(),
            comment ?: "",
            commentOutput = "", //TODO
            url,
        )
    }

}
