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

import com.linkedplanet.kotlininsightclient.api.interfaces.identity
import com.linkedplanet.kotlininsightclient.InsightObjectType.Country
import com.linkedplanet.kotlininsightclient.TestAttributes.CountryName
import com.linkedplanet.kotlininsightclient.TestAttributes.CountryShortName
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightAttachmentOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.model.AttachmentId
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute.Companion.toValue
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.security.MessageDigest
import java.util.*
import java.util.zip.ZipInputStream

interface InsightAttachmentOperatorTest {
    val insightObjectOperator: InsightObjectOperator
    val insightAttachmentOperator: InsightAttachmentOperator

    @Test
    fun attachmentTestGetAttachments() = runBlocking {
        println("TimeZone.getDefault().displayName=${TimeZone.getDefault().displayName}")

        val country = insightObjectOperator.getObjectByName(Country.id, "Germany", ::identity).orFail()!!
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
        val sha256HashIS = calculateSha256(downloadContent.readBytes())
        assertThat(sha256HashIS, equalTo("fd411837a51c43670e8d7367e64f72dbbcda5016f59988547c12d067505ef75b"))
    }

    private fun calculateSha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).fold("") { str, it -> str + "%02x".format(it) }

    @Test
    fun attachmentTestAttachmentCRUD() = runBlocking {
        insightObjectOperator.makeSureObjectWithNameDoesNotExist(Country.id, "Attachistan")
        try {
            val disclaimer = "created by Test and should only exist during test run. Deutsches ß und ä."
            val country = insightObjectOperator.createObject(
                Country.id,
                CountryName.attributeId toValue "Attachistan",
                CountryShortName.attributeId toValue disclaimer,
                toDomain =  ::identity
            ).orFail()

            val attachment = insightAttachmentOperator.uploadAttachment(
                country.id, "attachistan.txt", "content".byteInputStream()
            ).orFail()

            assertThat(attachment.filename, equalTo("attachistan.txt"))

            val downloadContent = insightAttachmentOperator.downloadAttachment(attachment.url).orFail()
            val downloadContentString = String(downloadContent.readBytes())
            assertThat(downloadContentString, equalTo("content"))

            insightAttachmentOperator.deleteAttachment(attachment.id).orFail()
            assertThat(insightAttachmentOperator.downloadAttachment(attachment.url).isLeft(), equalTo(true))
        } finally {
            insightObjectOperator.makeSureObjectWithNameDoesNotExist(Country.id, "Attachistan")
        }
    }


    @Test
    fun attachmentTestGetAttachmentsForNotExistingObject() = runBlocking {
        val responseError = insightAttachmentOperator.getAttachments(InsightObjectId.notPersistedObjectId).asError()
        assertThat(responseError.message, containsString("-1"))
    }

    @Test
    fun attachmentTestDownloadNonExistingAttachment() = runBlocking {
        insightObjectOperator.makeSureObjectWithNameDoesNotExist(Country.id, "NoAttachment")

        val disclaimer = "'NoAttachment' created by Test and should only exist during test run."
        val country = insightObjectOperator.createObject(
            Country.id,
            CountryName.attributeId toValue "NoAttachment",
            CountryShortName.attributeId toValue disclaimer,
            toDomain = ::identity
        ).orFail()

        val emptyList = insightAttachmentOperator.getAttachments(country.id).orFail()
        assertThat(emptyList, equalTo(emptyList()))

        insightObjectOperator.makeSureObjectWithNameDoesNotExist(Country.id, "NoAttachment")
    }

    @Test
    fun attachmentTestDownloadZip() = runBlocking {
        insightObjectOperator.makeSureObjectWithNameDoesNotExist(Country.id, "Zipistan")
        try {
            val disclaimer = "'Zipistan' created by Test and should only exist during test run."
            val country = insightObjectOperator.createObject(
                Country.id,
                CountryName.attributeId toValue "Zipistan",
                CountryShortName.attributeId toValue disclaimer,
                toDomain = ::identity
            ).orFail()

            val files = mapOf(
                "firstFile.txt" to "firstFileContent",
                "secondFile.txt" to "secondFileContent"
            )

            // GIVEN an object with two attachment files
            insightAttachmentOperator.uploadAttachment(
                country.id, files.keys.first(), files.values.first().byteInputStream(),
            ).orFail()
            insightAttachmentOperator.uploadAttachment(
                country.id, files.keys.last(), files.values.last().byteInputStream(),
            ).orFail()

            // WHEN downloading attachment zip
            val downloadAttachmentZip = insightAttachmentOperator.downloadAttachmentZip(country.id).orFail()

            // then both files are contained inside the zip archive
            val zip = ZipInputStream(downloadAttachmentZip)
            val firstZipFileName = zip.nextEntry?.name
            assertThat(String(zip.readBytes()), equalTo(files[firstZipFileName]))
            val secondZipEntryName = zip.nextEntry?.name
            assertThat(String(zip.readBytes()), equalTo(files[secondZipEntryName]))
            assertThat(zip.nextEntry, equalTo(null))
        } finally {
            insightObjectOperator.makeSureObjectWithNameDoesNotExist(Country.id, "Zipistan")
        }
    }

    @Test
    fun attachmentTestDownloadZipForNotExistingObject() = runBlocking {
        val responseError = insightAttachmentOperator.downloadAttachmentZip(InsightObjectId.notPersistedObjectId).asError()
        assertThat(responseError.message, containsString("-1"))
    }

}
