/*-
 * #%L
 * kotlin-insight-client-test-ktor
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
import com.linkedplanet.kotlinhttpclient.ktor.KtorHttpClient
import com.linkedplanet.kotlininsightclient.InsightClientTest
import com.linkedplanet.kotlininsightclient.api.experimental.NameMappedRepository
import com.linkedplanet.kotlininsightclient.http.*


class InsightKtorClientTest : InsightClientTest() {

    override val insightObjectOperator get() = HttpInsightObjectOperator(clientContext)
    override val insightObjectTypeOperator get() = HttpInsightObjectTypeOperator(clientContext)
    override val insightAttachmentOperator get() = HttpInsightAttachmentOperator(clientContext)
    override val insightHistoryOperator get() = HttpInsightHistoryOperator(clientContext)
    override val insightSchemaOperator get() = HttpInsightSchemaOperator(clientContext)

    private val clientContext: HttpInsightClientContext

    init {
        println("#### Starting setUp")
        val httpClient = KtorHttpClient(
            "http://localhost:2990",
            "admin",
            "admin"
        )
        clientContext = HttpInsightClientContext("http://localhost:2990", httpClient)

        NameMappedRepository.insightObjectOperator = insightObjectOperator
        NameMappedRepository.insightObjectTypeOperator = insightObjectTypeOperator
        NameMappedRepository.insightSchemaOperator = insightSchemaOperator
    }
}
