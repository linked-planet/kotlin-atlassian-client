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
import com.linkedplanet.kotlininsightclient.InsightObject.Country
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightAttachmentOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.model.setValue
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.security.MessageDigest
import java.util.*

interface InsightAttachmentOperatorTest {
    val insightObjectOperator: InsightObjectOperator
    val insightAttachmentOperator: InsightAttachmentOperator

    @Test
    fun testGetAttachments() {
        println("### START attachment_testGetAndDownloadAttachments")
        println("### TimeZone.getDefault().displayName=${TimeZone.getDefault().displayName}")
        runBlocking {
            val country = insightObjectOperator.getObjectByName(Country.id, "Germany").orFail()!!
            val attachments = insightAttachmentOperator.getAttachments(country.id).orFail()
            val firstAttachment = attachments.first()
            assertThat(attachments.size, equalTo(1))
            assertThat(firstAttachment.id, equalTo(1))
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
        }
        println("### END attachment_testGetAndDownloadAttachments")
    }

    private fun calculateSha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).fold("") { str, it -> str + "%02x".format(it) }

    @Test
    fun testAttachmentCRUD() {
        println("### START attachment_testDownloadAttachment")
        println("### TimeZone.getDefault().displayName=${TimeZone.getDefault().displayName}")
        runBlocking {
            makeSureObjectWithNameDoesNotExist(Country.id, "Attachistan")
            val disclaimer = "created by Test and should only exist during test run."
            val country = insightObjectOperator.createObject(Country.id) {
                it.setValue(CountryName.attributeId, "Attachistan")
                it.setValue(CountryShortName.attributeId, disclaimer)
            }.orFail()

            val attachment = insightAttachmentOperator.uploadAttachment(
                country.id, "attachistan.txt", "content".toByteArray(), comment = disclaimer
            ).orFail().first()

            assertThat(attachment.filename, equalTo("attachistan.txt"))
//            assertThat(attachment.comment, equalTo(disclaimer)) // comments not working with http clients

            val downloadContent = insightAttachmentOperator.downloadAttachment(attachment.url).orFail()
            val downloadContentString = String(downloadContent)
            assertThat(downloadContentString, equalTo("content"))

            insightAttachmentOperator.deleteAttachment(attachment.id).orFail()
            assertThat(insightAttachmentOperator.downloadAttachment(attachment.url).isLeft(), equalTo(true))

            makeSureObjectWithNameDoesNotExist(Country.id, "Attachistan")
        }
        println("### END attachment_testDownloadAttachment")
    }

    suspend fun makeSureObjectWithNameDoesNotExist(objectTypeId: Int, name: String) {
        val objectBeforeTest = insightObjectOperator.getObjectByName(objectTypeId, name).orFail()
        if (objectBeforeTest != null) {
            insightObjectOperator.deleteObject(objectBeforeTest.id)
        }
        assertThat(insightObjectOperator.getObjectByName(objectTypeId, name).orFail(), equalTo(null))
        //if the former assertion failed that means deleteObject is not working, so this attachment test fails too
    }

}
