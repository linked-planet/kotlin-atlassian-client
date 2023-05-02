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
import arrow.core.computations.either
import com.atlassian.jira.component.ComponentAccessor.getOSGiComponentInstanceOfType
import com.linkedplanet.kotlininsightclient.api.error.InsightClientError
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightAttachmentOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightAttachment
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlininsightclient.sdk.services.ReverseEngineeredAttachmentUrlResolver
import com.linkedplanet.kotlininsightclient.sdk.services.ReverseEngineeredFileManager
import com.linkedplanet.kotlininsightclient.sdk.util.catchAsInsightClientError
import com.linkedplanet.kotlininsightclient.sdk.util.toISOString
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.services.model.AttachmentBean
import java.io.ByteArrayOutputStream
import java.net.URLConnection
import java.nio.file.Path
import java.text.CharacterIterator
import java.text.StringCharacterIterator
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempFile


object SdkInsightAttachmentOperator : InsightAttachmentOperator {

    private val objectFacade by lazy { getOSGiComponentInstanceOfType(ObjectFacade::class.java) }

    private val fileManager by lazy { ReverseEngineeredFileManager() }
    private val attachmentUrlResolver by lazy { ReverseEngineeredAttachmentUrlResolver() }

    override suspend fun getAttachments(objectId: InsightObjectId): Either<InsightClientError, List<InsightAttachment>> =
        catchAsInsightClientError {
            objectFacade
                .findAttachmentBeans(objectId.value)
                .map(::beanToInsightAttachment)
        }

    override suspend fun downloadAttachment(url: String): Either<InsightClientError, ByteArray> =
        catchAsInsightClientError {
            val attachmentId = attachmentUrlResolver.parseAttachmentIdFromPathInformation(url)
            val attachmentBean = objectFacade.loadAttachmentBeanById(attachmentId)
            val inputStream =
                fileManager.getObjectAttachmentContent(attachmentBean.objectId, attachmentBean.nameInFileSystem)
            inputStream.readBytes()
        }

    override suspend fun downloadAttachmentZip(objectId: InsightObjectId): Either<InsightClientError, ByteArray> =
        either {
            val attachments = getAttachments(objectId).bind()
            val fileMap: List<Pair<String, ByteArray>> = attachments.map { attachment ->
                val attachmentContent = downloadAttachment(attachment.url).bind()
                attachment.filename to attachmentContent
            }
            ByteArrayOutputStream().use { byteOutputStream ->
                ZipOutputStream(byteOutputStream).use { zip ->
                    fileMap.forEach {
                        val zipEntry1 = ZipEntry(it.first)
                        zip.putNextEntry(zipEntry1)
                        zip.write(it.second)
                        zip.closeEntry()
                    }
                }

                val toByteArray = byteOutputStream.toByteArray()
                toByteArray
            }
        }

    override suspend fun uploadAttachment(
        objectId: InsightObjectId,
        filename: String,
        byteArray: ByteArray
    ): Either<InsightClientError, List<InsightAttachment>> =
        catchAsInsightClientError {
            val tempFilePath: Path = createTempFile(filename)
            val tempFile = tempFilePath.toFile()
            tempFile.writeBytes(byteArray)
            val mimeType = URLConnection.guessContentTypeFromName(filename)
            val bean = objectFacade.addAttachmentBean(objectId.value, tempFile, filename, mimeType, null)
            val insightAttachment = beanToInsightAttachment(bean)
            listOf(insightAttachment)
        }

    override suspend fun deleteAttachment(attachmentId: Int): Either<InsightClientError, Unit> =
        catchAsInsightClientError {
            objectFacade.deleteAttachmentBean(attachmentId).toString()
        }

    private fun beanToInsightAttachment(bean: AttachmentBean): InsightAttachment = bean.run {
        val url = attachmentUrlResolver.buildUrlForAttachment(bean)
        val humanReadableFileSize = humanReadableByteCountSI(fileSize)
        InsightAttachment(
            id!!, // we assume that when getting existing attachments, the id is always set
            author,
            mimeType,
            filename,
            humanReadableFileSize,
            created.toISOString(),
            comment ?: "",
            url,
        )
    }

    private fun humanReadableByteCountSI(bytesIn: Long): String {
        var bytes = bytesIn
        if (-1000 < bytes && bytes < 1000) {
            return "$bytes B"
        }
        val ci: CharacterIterator = StringCharacterIterator("kMGTPE")
        while (bytes <= -999950 || bytes >= 999950) {
            bytes /= 1000
            ci.next()
        }
        return java.lang.String.format("%.1f %cB", bytes / 1000.0, ci.current())
    }

}
