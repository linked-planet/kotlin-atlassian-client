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

import com.linkedplanet.kotlininsightclient.api.model.InsightAttributeId
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectTypeId
import com.linkedplanet.kotlininsightclient.api.model.InsightSchemaId

enum class SchemaObject(val id: InsightSchemaId) {
    ITest(InsightSchemaId(1))
}

enum class InsightObjectType(val id: InsightObjectTypeId) {
    Company(InsightObjectTypeId(1)),
    Country(InsightObjectTypeId(2)),
    TestWithLists(InsightObjectTypeId(4)),
    SimpleObject(InsightObjectTypeId(3)),
    Many(InsightObjectTypeId(5)),
    Abstract(InsightObjectTypeId(6)),
    User(InsightObjectTypeId(9))
}

enum class TestAttributes(val attributeId: InsightAttributeId, val attributeName: String) {
    CompanyName(InsightAttributeId(2), "Name"),
    CompanyCountry(InsightAttributeId(10), "Country"),

    CountryKey(InsightAttributeId(5), "Key"),
    CountryName(InsightAttributeId(6), "Name"),
    CountryShortName(InsightAttributeId(9), "ShortName"),

    TestWithListsItemList(InsightAttributeId(21), "ItemList"),
    TestWithListsStringList(InsightAttributeId(22), "StringList"),

    SimpleObjectFirstname(InsightAttributeId(15), "Firstname"),
    SimpleObjectLastname(InsightAttributeId(16), "Lastname"),

    UserTestUser(InsightAttributeId(43), "User"),
    UserTestUsers(InsightAttributeId(44), "Users"),
}

data class Company(
    val name: String,
    val country: Country?
)

data class Country(
    val name: String,
    val shortName: String,
)

data class SimpleObject(
    val name: String,
    val firstName: String,
    val lastName: String,
)

data class TestWithLists(
    val name: String,
    val itemList: List<SimpleObject>,
    val stringList: List<String>,
)