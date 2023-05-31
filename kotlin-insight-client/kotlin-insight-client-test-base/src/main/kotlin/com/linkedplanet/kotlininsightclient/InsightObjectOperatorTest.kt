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
import com.linkedplanet.kotlininsightclient.TestAttributes.*
import com.linkedplanet.kotlininsightclient.AuthenticatedJiraHttpClientFactory.Companion.Credentials
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectTypeOperator
import com.linkedplanet.kotlininsightclient.api.interfaces.InsightSchemaOperator
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute.Companion.toReference
import com.linkedplanet.kotlininsightclient.api.model.InsightAttribute.Companion.toValue
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectId
import com.linkedplanet.kotlininsightclient.api.model.InsightObjectTypeId
import com.linkedplanet.kotlininsightclient.api.model.ObjectAttributeValue
import com.linkedplanet.kotlininsightclient.api.model.ObjectTypeSchemaAttribute
import com.linkedplanet.kotlininsightclient.api.model.addSelectValue
import com.linkedplanet.kotlininsightclient.api.model.getAttributeByName
import com.linkedplanet.kotlininsightclient.api.model.getAttributeIdByName
import com.linkedplanet.kotlininsightclient.api.model.getMultiReferenceValue
import com.linkedplanet.kotlininsightclient.api.model.getSelectValues
import com.linkedplanet.kotlininsightclient.api.model.getSingleReferenceValue
import com.linkedplanet.kotlininsightclient.api.model.getStringValue
import com.linkedplanet.kotlininsightclient.api.model.getUserList
import com.linkedplanet.kotlininsightclient.api.model.removeSelectValue
import com.linkedplanet.kotlininsightclient.api.model.setValue
import com.linkedplanet.kotlininsightclient.repositories.CompanyRepositoryBasedOnNameMapping
import com.linkedplanet.kotlininsightclient.repositories.CompanyTestRepositoryBasedOnAbstractImpl
import com.linkedplanet.kotlininsightclient.repositories.CompanyTestRepositoryManualImpl
import com.linkedplanet.kotlininsightclient.repositories.CountryRepositoryBasedOnNameMapping
import com.linkedplanet.kotlininsightclient.repositories.CountryTestRepositoryBasedOnAbstractImpl
import com.linkedplanet.kotlininsightclient.repositories.CountryTestRepositoryManualImpl
import com.linkedplanet.kotlininsightclient.repositories.SimpleObjectRepositoryBasedOnNameMapping
import com.linkedplanet.kotlininsightclient.repositories.TestsWithListRepositoryBasedOnNameMapping
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.greaterThan
import org.junit.Test
import java.net.URI

interface InsightObjectOperatorTest {
    val insightObjectOperator: InsightObjectOperator
    val insightObjectTypeOperator: InsightObjectTypeOperator
    val insightSchemaOperator: InsightSchemaOperator

    @Test
    fun testVariousInsightObjectRepositories() = runBlocking {
        val countryOperatorFromGeneric = CountryRepositoryBasedOnNameMapping(
            insightObjectOperator, insightObjectTypeOperator, insightSchemaOperator)
        val companyOperatorFromGeneric = CompanyRepositoryBasedOnNameMapping(
            insightObjectOperator, insightObjectTypeOperator, insightSchemaOperator)
        val countryManualImpl = CountryTestRepositoryManualImpl(insightObjectOperator)
        val companyManualImpl = CompanyTestRepositoryManualImpl(insightObjectOperator, countryManualImpl)
        val countryFromAbstract = CountryTestRepositoryBasedOnAbstractImpl(insightObjectOperator)
        val companyFromAbstract = CompanyTestRepositoryBasedOnAbstractImpl(insightObjectOperator, countryFromAbstract)
        // maybe use a parameter based testing framework
        listOf(
            Pair(countryOperatorFromGeneric, companyOperatorFromGeneric),
            Pair(countryManualImpl, companyManualImpl),
            Pair(countryFromAbstract, companyFromAbstract)
        ).forEach{(countryOperator, companyOperator) ->
            val className = countryOperator.javaClass.simpleName
            val country = Country(name = "United States of America ($className)", shortName = "USA ($className)")
            val company = Company(name = "Boring Company (${companyOperator.javaClass.simpleName})", country = country)
            companyOperator.delete(company).orFail()
            countryOperator.delete(country).orFail()
            try {
                countryOperator.create(country).orFail()
                val countryByName = countryOperator.getByName(country.name).orFail()
                assertThat(countryByName, equalTo(country))
                companyOperator.create(company).orFail()
                val companyByName = companyOperator.getByName(company.name).orFail()
                assertThat(companyByName, equalTo(company))
            } finally {
                companyOperator.delete(company).orFail()
                countryOperator.delete(country).orFail()
            }
        }
    }

    @Test
    fun testInsightObjectOperatorCrudWithListAttribute() = runBlocking {
        val simpleObjectOperator = SimpleObjectRepositoryBasedOnNameMapping(
            insightObjectOperator, insightObjectTypeOperator, insightSchemaOperator)

        val testWithListsOperator = TestsWithListRepositoryBasedOnNameMapping(
            insightObjectOperator, insightObjectTypeOperator, insightSchemaOperator)

        val simpleObjects = simpleObjectOperator.getByIQL("Name in (Object2, Object3)").orFail()
        val objWithLists = TestWithLists(
            name = "CreatedByIntegrationTest",
            itemList = simpleObjects.items,
            stringList = listOf("A", "B", "C")
        )

        testWithListsOperator.delete(objWithLists).orFail()
        try {
            testWithListsOperator.create(objWithLists).orFail()
            val byName = testWithListsOperator.getByName(objWithLists.name).orFail()
            assertThat(byName, equalTo(objWithLists))
        } finally {
            testWithListsOperator.delete(objWithLists).orFail()
        }
    }

    @Test
    fun testObjectListWithFlatReference() {
        val companies = runBlocking {
            insightObjectOperator.getObjects(InsightObjectType.Company.id, toDomain = ::identity).orNull()!!.objects
        }
        assertThat(companies.size, equalTo(2))

        val firstCompany = companies.firstOrNull { it.id == InsightObjectId(1) }
        assertThat(firstCompany, notNullValue())
        assertThat(firstCompany!!.id, equalTo(InsightObjectId(1)))
        assertThat(firstCompany.objectKey, equalTo("IT-1"))
        assertThat(firstCompany.label, equalTo("Test GmbH"))

        // Name
        assertThat(firstCompany.getAttributeByName(CompanyName.attributeName), notNullValue())
        assertThat(
            firstCompany.getAttributeIdByName(CompanyName.attributeName),
            equalTo(CompanyName.attributeId)
        )
        assertThat(firstCompany.getStringValue(CompanyName.attributeId), equalTo("Test GmbH"))

        // Country
        assertThat(firstCompany.getAttributeByName(CompanyCountry.attributeName), notNullValue())
        assertThat(
            firstCompany.getAttributeIdByName(CompanyCountry.attributeName),
            equalTo(CompanyCountry.attributeId)
        )
        assertThat(
            firstCompany.getSingleReferenceValue(CompanyCountry.attributeId)!!.objectName,
            equalTo("Germany")
        )
        assertThat(firstCompany.attachmentsExist, equalTo(false))

        val secondCompany = companies.firstOrNull { it.id == InsightObjectId(2) }
        assertThat(secondCompany, notNullValue())
        assertThat(secondCompany!!.id, equalTo(InsightObjectId(2)))
        assertThat(secondCompany.objectKey, equalTo("IT-2"))
        assertThat(secondCompany.label, equalTo("Test AG"))

        // Name
        assertThat(secondCompany.getAttributeByName(CompanyName.attributeName), notNullValue())
        assertThat(
            secondCompany.getAttributeIdByName(CompanyName.attributeName),
            equalTo(CompanyName.attributeId)
        )
        assertThat(secondCompany.getStringValue(CompanyName.attributeId), equalTo("Test AG"))

        // Country
        assertThat(secondCompany.getAttributeByName(CompanyCountry.attributeName), notNullValue())
        assertThat(
            secondCompany.getAttributeIdByName(CompanyCountry.attributeName),
            equalTo(CompanyCountry.attributeId)
        )
        assertThat(
            secondCompany.getSingleReferenceValue(CompanyCountry.attributeId)!!.objectName,
            equalTo("Germany")
        )
        assertThat(secondCompany.attachmentsExist, equalTo(true))
    }

    @Test
    fun testObjectListWithResolvedReference() {
        val companies = runBlocking {
            insightObjectOperator.getObjects(InsightObjectType.Company.id, toDomain = ::identity).orNull()?.objects
        }
        assertThat(companies, notNullValue())
        assertThat(companies!!.size, equalTo(2))

        val company = companies.firstOrNull { it.id == InsightObjectId(1) }
        assertThat(company, notNullValue())

        val country = runBlocking {
            insightObjectOperator.getObjectById(
                company!!.getSingleReferenceValue(CompanyCountry.attributeId)!!.objectId,
                toDomain = ::identity
            ).orNull()!!
        }
        assertThat(country.getStringValue(CountryName.attributeId), equalTo("Germany"))
        assertThat(country.getStringValue(CountryShortName.attributeId), equalTo("DE"))
    }

    @Test
    fun testObjectById() {
        val company = runBlocking {
            insightObjectOperator.getObjectById(InsightObjectId(1), toDomain = ::identity).orFail()!!
        }
        assertThat(company.id, equalTo(InsightObjectId(1)))
        assertThat(company.objectKey, equalTo("IT-1"))
        assertThat(company.label, equalTo("Test GmbH"))
        assertThat(company.getStringValue(CompanyName.attributeId), equalTo("Test GmbH"))
        assertThat(
            company.getSingleReferenceValue(CompanyCountry.attributeId)!!.objectName,
            equalTo("Germany")
        )
        assertThat(company.objectTypeName, equalTo("Company"))
        assertThat(company.objectTypeId, equalTo(InsightObjectTypeId(1)))

        val created = company.getAttributeByName("Created")!!.value as ObjectAttributeValue.DateTime
//        assertThat(created.value?.toInstant()?.toString(), equalTo("2022-10-27T09:15:53.212Z"))
        assertThat(created.value?.toInstant()?.toString(), endsWith(":15:53.212Z"))
        assertThat(created.value?.toInstant()?.toString(), startsWith("2022-10-27T"))
        assertThat(created.displayValue, equalTo("27/Oct/22 11:15 AM"))
        val updated = company.getAttributeByName("Updated")!!.value as ObjectAttributeValue.DateTime
//        assertThat(updated.value?.toInstant()?.toString(), equalTo("2023-02-21T07:10:25.993Z"))
        assertThat(updated.value?.toInstant()?.toString(), endsWith(":10:25.993Z"))
        assertThat(updated.value?.toInstant()?.toString(), startsWith("2023-02-21T"))
        assertThat(updated.displayValue, equalTo("21/Feb/23 8:10 AM"))

        assertThat(company.attachmentsExist, equalTo(false))
    }

    @Test
    fun testObjectSelfLink() = runBlocking {
        val company = insightObjectOperator.getObjectById(InsightObjectId(1), ::identity).orFail()!!
        // first check if the URL is correct
        assertThat(company.objectSelf, endsWith("secure/insight/assets/IT-1"))
        assertThat(company.objectSelf, startsWith("http"))

        // check if Atlassian did change the URL of the Assets app (Insight)
        val uri = URI(company.objectSelf)
        val httpClientFactory = AuthenticatedJiraHttpClientFactory(jiraOrigin = uri.scheme + "://" + uri.authority)
        val httpClient = httpClientFactory.login(Credentials("admin", "admin")).orFail()
        val response = httpClient.getWithRelativePath(uri.path)
        assertThat(response.status.code, equalTo(200)) // also 200 if not logged in, but 404 if url is unknown
        assertThat(response.bodyString(), containsString("<title>Assets Search"))
    }

    @Test
    fun testGetObjectsByObjectTypeName() = runBlocking {
        val objs = insightObjectOperator.getObjectsByObjectTypeName("Country", toDomain = ::identity).orFail()
        val allCountryNames = objs.map { it.getStringValue(CountryName.attributeId) }
        assertThat(allCountryNames, Matchers.hasItem("Germany")) // among other items
    }

    @Test
    fun testObjectWithListAttributes() {
        val obj = runBlocking {
            insightObjectOperator.getObjects(InsightObjectType.TestWithLists.id, toDomain = ::identity).orNull()
        }!!.objects.first()

        val references = obj.getMultiReferenceValue(TestWithListsItemList.attributeId)
        val idList = references.map { it.objectId }
        val nameList = references.map { it.objectName }
        val refList = references.map { insightReference ->
            runBlocking {
                insightObjectOperator.getObjectById(insightReference.objectId, toDomain = ::identity).orNull()!!
            }
        }
        val firstNameList = refList.map { it.getStringValue(SimpleObjectFirstname.attributeId) }

        assertThat(idList, equalTo(listOf(35, 36, 37).map { InsightObjectId(it) }))
        assertThat(nameList, equalTo(listOf("Object1", "Object2", "Object3")))
        assertThat(firstNameList, equalTo(listOf("F1", "F2", "F3")))
    }

    @Test
    fun testAddingSelectList() {
        val obj = runBlocking {
            insightObjectOperator.getObjects(InsightObjectType.TestWithLists.id, toDomain = ::identity).orNull()
        }!!.objects.first()

        // check if options are A B C
        val selectSchema = runBlocking {
                insightObjectTypeOperator.getObjectType(InsightObjectType.TestWithLists.id).orFail()
        }.attributes.firstOrNull { it.id == TestWithListsStringList.attributeId } as? ObjectTypeSchemaAttribute.Select
        assertThat(selectSchema?.options, containsInAnyOrder("A", "B", "C"))

        val results = obj.getSelectValues(TestWithListsStringList.attributeId)
        assertThat(results, equalTo(emptyList()))
        obj.addSelectValue(TestWithListsStringList.attributeId, "A")
        obj.addSelectValue(TestWithListsStringList.attributeId, "B")
        runBlocking { insightObjectOperator.updateInsightObject(obj).orNull() }

        val obj2 = runBlocking {
            insightObjectOperator.getObjects(InsightObjectType.TestWithLists.id, toDomain = ::identity).orNull()
        }!!.objects.first()
        val results2 = obj2.getSelectValues(TestWithListsStringList.attributeId)
        assertThat(results2, containsInAnyOrder("A", "B"))
        obj2.removeSelectValue(TestWithListsStringList.attributeId, "B")
        runBlocking { insightObjectOperator.updateInsightObject(obj2).orNull() }

        val obj3 = runBlocking {
            insightObjectOperator.getObjects(InsightObjectType.TestWithLists.id, toDomain = ::identity).orNull()
        }!!.objects.first()
        val results3 = obj3.getSelectValues(TestWithListsStringList.attributeId)
        assertThat(results3, equalTo(listOf("A")))
        obj3.removeSelectValue(TestWithListsStringList.attributeId, "A")
        runBlocking { insightObjectOperator.updateInsightObject(obj3).orNull() }

        val obj4 = runBlocking {
            insightObjectOperator.getObjects(InsightObjectType.TestWithLists.id, toDomain = ::identity).orNull()
        }!!.objects.first()
        val results4 = obj4.getSelectValues(TestWithListsStringList.attributeId)
        assertThat(results4, equalTo(emptyList()))
    }

    @Test
    fun testCreateAndDelete() {
        runBlocking {
            // Check England does not exist
            insightObjectOperator.makeSureObjectWithNameDoesNotExist(InsightObjectType.Country.id, "England")
            insightObjectOperator.makeSureObjectWithNameDoesNotExist(InsightObjectType.Company.id, "MyTestCompany GmbH")

            // Create and check direct result
            val country1 = insightObjectOperator.createObject(
                InsightObjectType.Country.id,
                CountryName.attributeId toValue "England",
                CountryShortName.attributeId toValue "GB",
                toDomain = ::identity
            ).orFail()

            val company1 = insightObjectOperator.createObject(
                InsightObjectType.Company.id,
                CompanyName.attributeId toValue "MyTestCompany GmbH",
                CompanyCountry.attributeId toReference country1.id,
                toDomain = ::identity
            ).orFail()

            assertThat(country1.id.raw, greaterThan(0))
            assertThat(country1.getStringValue(CountryKey.attributeId)!!.isNotBlank(), equalTo(true))
            assertThat(company1.getSingleReferenceValue(CompanyCountry.attributeId)!!.objectId.raw, greaterThan(0))
            assertThat(
                company1.getSingleReferenceValue(CompanyCountry.attributeId)!!.objectKey.isNotBlank(),
                equalTo(true)
            )

            // Check England does exist
            val countryReference = company1.getSingleReferenceValue(CompanyCountry.attributeId)!!
            val countryAfterCreate =
                insightObjectOperator.getObjectByName(InsightObjectType.Country.id, "England", toDomain = ::identity)
                    .orNull()!!
            val companyAfterCreate =
                insightObjectOperator.getObjectByName(
                    InsightObjectType.Company.id,
                    "MyTestCompany GmbH",
                    toDomain = ::identity
                ).orNull()!!
            assertThat(countryAfterCreate.id, equalTo(countryReference.objectId))
            assertThat(
                countryAfterCreate.getStringValue(CountryKey.attributeId),
                equalTo(countryReference.objectKey)
            )
            assertThat(
                countryAfterCreate.getStringValue(CountryName.attributeId),
                equalTo(countryReference.objectName)
            )
            assertThat(companyAfterCreate.id, equalTo(company1.id))

            // Check Delete
            insightObjectOperator.deleteObject(countryReference.objectId)
            insightObjectOperator.deleteObject(company1.id)
            val companyAfterDelete = insightObjectOperator.getObjectById(countryReference.objectId, ::identity).orNull()
            val countryAfterDelete = insightObjectOperator.getObjectById(country1.id, ::identity).orNull()
            assertThat(companyAfterDelete, equalTo(null))
            assertThat(countryAfterDelete, equalTo(null))
        }
    }

    @Test
    fun testFilter() {
        runBlocking {
            val countries =
                insightObjectOperator.getObjectsByIQL(
                    InsightObjectType.Country.id,
                    """"ShortName"="DE"""",
                    toDomain = ::identity
                )
                    .orNull()!!.objects
            assertThat(countries.size, equalTo(1))
            assertThat(countries.first().getStringValue(CountryShortName.attributeId), equalTo("DE"))
            assertThat(countries.first().getStringValue(CountryName.attributeId), equalTo("Germany"))
        }
    }

    @Test
    fun testUpdate() = runBlocking {
        val countryId = InsightObjectType.Country.id
        var country = insightObjectOperator.getObjectByName(countryId, "Germany", ::identity).orFail()!!
        assertThat(country.getStringValue(CountryName.attributeId), equalTo("Germany"))
        if (country.getStringValue(CountryShortName.attributeId) == "ED") {
            //likely an old test run failed, try to fix it:
            country.setValue(CountryShortName.attributeId, "DE")
            insightObjectOperator.updateInsightObject(country).orFail()
            country = insightObjectOperator.getObjectByName(countryId, "Germany", ::identity).orFail()!!
        }
        assertThat(country.getStringValue(CountryShortName.attributeId), equalTo("DE"))
        country.setValue(CountryShortName.attributeId, "ED")
        insightObjectOperator.updateInsightObject(country).orFail()

        val updatedCountry = insightObjectOperator.getObjectByName(countryId, "Germany", ::identity).orFail()!!
        assertThat(updatedCountry.getStringValue(CountryName.attributeId), equalTo("Germany"))
        assertThat(updatedCountry.getStringValue(CountryShortName.attributeId), equalTo("ED"))
        updatedCountry.setValue(CountryShortName.attributeId, "DE")
        insightObjectOperator.updateInsightObject(updatedCountry).orFail()

        val restoredCountry = insightObjectOperator.getObjectByName(countryId, "Germany", ::identity).orFail()!!
        assertThat(restoredCountry.getStringValue(CountryName.attributeId), equalTo("Germany"))
        assertThat(restoredCountry.getStringValue(CountryShortName.attributeId), equalTo("DE"))
    }

    @Test
    fun testGetObjectsWithoutChildren() {
        val objectResponse = runBlocking {
            insightObjectOperator.getObjects(InsightObjectType.Abstract.id, withChildren = false, toDomain = ::identity)
                .orNull()!!
        }
        assertThat(objectResponse.totalFilterCount, equalTo(0))

        val objects = objectResponse.objects
        assertThat(objects, equalTo(emptyList()))

    }

    @Test
    fun testGetObjectsWithChildren() {
        val objectResponse = runBlocking {
            insightObjectOperator.getObjects(InsightObjectType.Abstract.id, withChildren = true, toDomain = ::identity)
                .orNull()!!
        }
        assertThat(objectResponse.totalFilterCount, equalTo(2))

        val objects = objectResponse.objects
        assertThat(objects.size, equalTo(2))

        val firstObj = objects.first()
        assertThat(firstObj.id, equalTo(InsightObjectId(94)))

        val secondObj = objects[1]
        assertThat(secondObj.id, equalTo(InsightObjectId(95)))
    }

    @Test
    fun testGetObjectsWithChildrenPaginated() {

        // results 1 and 2
        val allINSIGHTOBJECTList = runBlocking {
            insightObjectOperator.getObjects(
                InsightObjectType.Abstract.id,
                withChildren = true,
                pageIndex = 0,
                pageSize = 2,
                toDomain = ::identity
            ).orNull()!!
        }
        assertThat(allINSIGHTOBJECTList.totalFilterCount, equalTo(2))
        val allObjects = allINSIGHTOBJECTList.objects
        assertThat(allObjects.size, equalTo(2))
        assertThat(allObjects[0].id, equalTo(InsightObjectId(94)))
        assertThat(allObjects[1].id, equalTo(InsightObjectId(95)))

        // results 1 and 2
        val allExplINSIGHTOBJECTList = runBlocking {
            insightObjectOperator.getObjects(
                InsightObjectType.Abstract.id,
                withChildren = true,
                pageIndex = 0,
                pageSize = 5,
                toDomain = ::identity
            ).orNull()!!
        }
        assertThat(allExplINSIGHTOBJECTList.totalFilterCount, equalTo(2))
        val allExplObjects = allExplINSIGHTOBJECTList.objects
        assertThat(allExplObjects.size, equalTo(2))
        assertThat(allExplObjects[0].id, equalTo(InsightObjectId(94)))
        assertThat(allExplObjects[1].id, equalTo(InsightObjectId(95)))

        // result 1
        val firstINSIGHTOBJECTList = runBlocking {
            insightObjectOperator.getObjects(
                InsightObjectType.Abstract.id,
                withChildren = true,
                pageIndex = 0,
                pageSize = 1,
                toDomain = ::identity
            ).orNull()!!
        }
        assertThat(firstINSIGHTOBJECTList.totalFilterCount, equalTo(2))
        val firstObjects = firstINSIGHTOBJECTList.objects
        assertThat(firstObjects.size, equalTo(1))
        assertThat(firstObjects[0].id, equalTo(InsightObjectId(94)))

        // result 2
        val secondINSIGHTOBJECTList = runBlocking {
            insightObjectOperator.getObjects(
                InsightObjectType.Abstract.id,
                withChildren = true,
                pageIndex = 1,
                pageSize = 1,
                toDomain = ::identity
            ).orNull()!!
        }
        assertThat(secondINSIGHTOBJECTList.totalFilterCount, equalTo(2))
        val secondObjects = secondINSIGHTOBJECTList.objects
        assertThat(secondObjects.size, equalTo(1))
        assertThat(secondObjects[0].id, equalTo(InsightObjectId(95)))

        // page doesn't exist
        val emptyINSIGHTOBJECTList = runBlocking {
            insightObjectOperator.getObjects(
                InsightObjectType.Abstract.id,
                withChildren = true,
                pageIndex = 2,
                pageSize = 2,
                toDomain = ::identity
            ).orNull()!!
        }
        assertThat(firstINSIGHTOBJECTList.totalFilterCount, equalTo(2))
        val emptyObjects = emptyINSIGHTOBJECTList.objects
        assertThat(emptyObjects, equalTo(emptyList()))

    }

    @Test
    fun testUserAttribute() {

        val obj = runBlocking {
            insightObjectOperator.getObjects(InsightObjectType.User.id, toDomain = ::identity)
                .map { it.objects.firstOrNull() }.orFail()
        }
        assertThat(obj, notNullValue())
        val userAttr = obj!!.getUserList(UserTestUser.attributeId)
        assertThat(userAttr.size, equalTo(1))
        assertThat(userAttr.first().name, equalTo("admin"))
        val usersAttr = obj.getUserList(UserTestUsers.attributeId)
        assertThat(usersAttr.size, equalTo(2))
        assertThat(usersAttr.map { it.name }, hasItems("admin", "test1"))

    }

    @Test
    fun testObjectCount() = runBlocking {
        val count = insightObjectOperator.getObjectCount("objectType = Many").orFail()
        assertThat(count, equalTo(55))
    }

}
