/*-
 * #%L
 * kotlin-insight-client-test-base
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
package com.linkedplanet.kotlininsightclient

import com.linkedplanet.kotlininsightclient.InsightAttribute.CountryName
import com.linkedplanet.kotlininsightclient.InsightAttribute.CountryShortName
import com.linkedplanet.kotlininsightclient.InsightObjectType.Country
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightAttachmentOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.model.AttachmentId
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlininsightclient.api.model.setValue
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.*
import java.util.zip.ZipInputStream

interface InsightAttachmentOperatorTest {
    val insightObjectOperator: InsightObjectOperator
    val insightAttachmentOperator: InsightAttachmentOperator

    @Test
    fun attachmentTestGetAttachments() = runBlocking {
        println("### START attachment_testGetAndDownloadAttachments")
        println("### TimeZone.getDefault().displayName=${TimeZone.getDefault().displayName}")

        val country = insightObjectOperator.getObjectByName(Country.id, "Germany").orFail()!!
        val attachments = insightAttachmentOperator.getAttachments(country.id).orFail()
        val firstAttachment = attachments.first()
        assertThat(attachments.size, equalTo(1))
        assertThat(firstAttachment.id, equalTo(AttachmentId(1)))
        assertThat(firstAttachment.mimeType, equalTo("application/pdf"))
        assertThat(firstAttachment.author, equalTo("admin"))
        assertThat(firstAttachment.comment, equalTo(""))
        assertThat(firstAttachment.filename, equalTo("TestAttachment.pdf"))
        assertThat(firstAttachment.filesize, equalTo("10.1 kB"))
        assertThat(firstAttachment.created, endsWith(":06:08.208Z")) // 7 works fine locally and should be correct,
        assertThat(firstAttachment.created, startsWith("2023-02-21T0")) // but github pipeline insists on 8 o'clock

        val downloadContent = insightAttachmentOperator.downloadAttachment(attachments.first().url).orNull()!!
        val sha256HashIS = calculateSha256(downloadContent)
        assertThat(sha256HashIS, equalTo("fd411837a51c43670e8d7367e64f72dbbcda5016f59988547c12d067505ef75b"))
        println("### END attachment_testGetAndDownloadAttachments")
    }

    private fun calculateSha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).fold("") { str, it -> str + "%02x".format(it) }

    @Test
    fun attachmentTestAttachmentCRUD() = runBlocking {
        println("### START attachment_testDownloadAttachment")

        insightObjectOperator.makeSureObjectWithNameDoesNotExist(Country.id, "Attachistan")
        val disclaimer = "created by Test and should only exist during test run. Deutsches ß und ä."
        val country = insightObjectOperator.createObject(Country.id) {
            it.setValue(CountryName.attributeId, "Attachistan")
            it.setValue(CountryShortName.attributeId, disclaimer)
        }.orFail()

        val attachment = insightAttachmentOperator.uploadAttachment(
            country.id, "attachistan.txt", "content".toByteArray()
        ).orFail().first()

        assertThat(attachment.filename, equalTo("attachistan.txt"))

        val downloadContent = insightAttachmentOperator.downloadAttachment(attachment.url).orFail()
        val downloadContentString = String(downloadContent)
        assertThat(downloadContentString, equalTo("content"))

        insightAttachmentOperator.deleteAttachment(attachment.id).orFail()
        assertThat(insightAttachmentOperator.downloadAttachment(attachment.url).isLeft(), equalTo(true))

        insightObjectOperator.makeSureObjectWithNameDoesNotExist(Country.id, "Attachistan")

        println("### END attachment_testDownloadAttachment")
    }


    @Test
    fun attachmentTestGetAttachmentsForNotExistingObject() = runBlocking {
        println("### Integration Test Start: testGetAttachmentsForNotExistingObject")

        val responseError = insightAttachmentOperator.getAttachments(InsightObjectId.notPersistedObjectId).asError()
        assertThat(responseError.message, containsString("-1"))

        println("### Integration Test End: testGetAttachmentsForNotExistingObject")
    }

    @Test
    fun attachmentTestDownloadNonExistingAttachment() = runBlocking {
        println("### Integration Test Start: testDownloadNonExistingAttachment")
        insightObjectOperator.makeSureObjectWithNameDoesNotExist(Country.id, "NoAttachment")

        val disclaimer = "'NoAttachment' created by Test and should only exist during test run."
        val country = insightObjectOperator.createObject(Country.id) {
            it.setValue(CountryName.attributeId, "Attachistan")
            it.setValue(CountryShortName.attributeId, disclaimer)
        }.orFail()

        val emptyList = insightAttachmentOperator.getAttachments(country.id).orFail()
        assertThat(emptyList, equalTo(emptyList()))

        insightObjectOperator.makeSureObjectWithNameDoesNotExist(Country.id, "NoAttachment")
        println("### Integration Test End: testDownloadNonExistingAttachment")
    }

    @Test
    fun attachmentTestDownloadZip() = runBlocking {
        println("### Integration Test Start: testDownloadZip")
        insightObjectOperator.makeSureObjectWithNameDoesNotExist(Country.id, "Zipistan")

        val disclaimer = "'Zipistan' created by Test and should only exist during test run."
        val country = insightObjectOperator.createObject(Country.id) {
            it.setValue(CountryName.attributeId, "Zipistan")
            it.setValue(CountryShortName.attributeId, disclaimer)
        }.orFail()

        val files = mapOf(
            "firstFile.txt" to "firstFileContent",
            "secondFile.txt" to "secondFileContent"
        )

        // GIVEN an object with two attachment files
        insightAttachmentOperator.uploadAttachment(
            country.id, files.keys.first(), files.values.first().toByteArray(),
        ).orFail().first()
        insightAttachmentOperator.uploadAttachment(
            country.id, files.keys.last(), files.values.last().toByteArray(),
        ).orFail().first()

        // WHEN downloading attachment zip
        val downloadAttachmentZip = insightAttachmentOperator.downloadAttachmentZip(country.id).orFail()

        // then both files are contained inside the zip archive
        val zip = ZipInputStream(ByteArrayInputStream(downloadAttachmentZip))
        val firstZipFileName = zip.nextEntry?.name
        assertThat(String(zip.readBytes()), equalTo(files[firstZipFileName]))
        val secondZipEntryName = zip.nextEntry?.name
        assertThat(String(zip.readBytes()), equalTo(files[secondZipEntryName]))
        Assert.assertNull(zip.nextEntry)

        insightObjectOperator.makeSureObjectWithNameDoesNotExist(Country.id, "Zipistan")
        println("### Integration Test End: testDownloadZip")
    }

    @Test
    fun attachmentTestDownloadZipForNotExistingObject() = runBlocking {
        println("### Integration Test Start: testDownloadZipForNotExistingObject")

        val responseError = insightAttachmentOperator.downloadAttachmentZip(InsightObjectId.notPersistedObjectId).asError()
        assertThat(responseError.message, containsString("-1"))

        println("### Integration Test End: testDownloadZipForNotExistingObject")
    }

}
