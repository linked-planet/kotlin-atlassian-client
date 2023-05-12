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
import com.linkedplanet.kotlininsightclient.api.model.AttachmentId
import com.linkedplanet.kotlininsightclient.api.model.InsightAttachment
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlininsightclient.sdk.services.ReverseEngineeredAttachmentUrlResolver
import com.linkedplanet.kotlininsightclient.sdk.services.ReverseEngineeredFileManager
import com.linkedplanet.kotlininsightclient.sdk.util.catchAsInsightClientError
import com.linkedplanet.kotlininsightclient.sdk.util.toISOString
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.services.model.AttachmentBean
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.net.URLConnection
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
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

    override suspend fun downloadAttachment(url: String): Either<InsightClientError, InputStream> =
        catchAsInsightClientError {
            val attachmentId = attachmentUrlResolver.parseAttachmentIdFromPathInformation(url)
            val attachmentBean = objectFacade.loadAttachmentBeanById(attachmentId)
            fileManager.getObjectAttachmentContent(attachmentBean.objectId, attachmentBean.nameInFileSystem)
        }

    override suspend fun downloadAttachmentZip(objectId: InsightObjectId): Either<InsightClientError, InputStream> =
        either {
            val fileMap = allAttachmentStreamsForInsightObject(objectId).bind()
            zipInputStreamForMultipleInputStreams(fileMap).bind()
        }

    private fun allAttachmentStreamsForInsightObject(objectId: InsightObjectId) =
        catchAsInsightClientError {
            val attachmentBeans = objectFacade.findAttachmentBeans(objectId.value)
            attachmentBeans.map { bean ->
                val attachmentContent = fileManager.getObjectAttachmentContent(bean.objectId, bean.nameInFileSystem)
                bean.filename to attachmentContent
            }
        }

    private fun zipInputStreamForMultipleInputStreams(
        fileMap: List<Pair<String, InputStream>>
    ): Either<InsightClientError, InputStream> =
        catchAsInsightClientError {
            val pipeInputStream = PipedInputStream()
            PipedOutputStream(pipeInputStream).use { outputStream ->
                ZipOutputStream(outputStream).use { zipOutputStream ->
                    fileMap.forEach { (filename, inputStream) ->
                        val zipEntry = ZipEntry(filename)
                        zipOutputStream.putNextEntry(zipEntry)
                        inputStream.copyTo(zipOutputStream)
                        zipOutputStream.closeEntry()
                    }
                }
            }
            pipeInputStream
        }

    override suspend fun uploadAttachment(
        objectId: InsightObjectId,
        filename: String,
        inputStream: InputStream
    ): Either<InsightClientError, List<InsightAttachment>> =
        catchAsInsightClientError {
            val tempFilePath: Path = createTempFile(filename)
            Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING)
            val mimeType = URLConnection.guessContentTypeFromName(filename)
            val bean = objectFacade.addAttachmentBean(objectId.value, tempFilePath.toFile(), filename, mimeType, null)
            val insightAttachment = beanToInsightAttachment(bean)
            listOf(insightAttachment)
        }

    override suspend fun deleteAttachment(attachmentId: AttachmentId): Either<InsightClientError, Unit> =
        catchAsInsightClientError {
            objectFacade.deleteAttachmentBean(attachmentId.raw).toString()
        }

    private fun beanToInsightAttachment(bean: AttachmentBean): InsightAttachment = bean.run {
        val url = attachmentUrlResolver.buildUrlForAttachment(bean)
        val humanReadableFileSize = humanReadableByteCountSI(fileSize)
        InsightAttachment(
            AttachmentId(id!!), // we assume that when getting existing attachments, the id is always set
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
