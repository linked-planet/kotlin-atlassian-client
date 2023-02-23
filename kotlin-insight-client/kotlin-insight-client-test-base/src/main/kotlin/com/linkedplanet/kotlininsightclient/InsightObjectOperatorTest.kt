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

import com.linkedplanet.kotlininsightclient.api.interfaces.InsightObjectOperator
import com.linkedplanet.kotlininsightclient.api.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

interface InsightObjectOperatorTest {
    val insightObjectOperator: InsightObjectOperator

    @Test
    fun testObjectListWithFlatReference() {
        println("### START object_testObjectListWithFlatReference")
        val companies = runBlocking {
            insightObjectOperator.getObjects(OBJECTS.Company.id).orNull()!!.objects
        }
        assertTrue(companies.size == 2)

        val firstCompany = companies.firstOrNull { it.id == 1 }
        assertNotNull(firstCompany)
        assertEquals(1, firstCompany!!.id)
        assertEquals("IT-1", firstCompany.objectKey)
        assertEquals("Test GmbH", firstCompany.label)
        assertEquals("Test GmbH", firstCompany.getStringValue(COMPANY.Name.name))
        assertEquals("Germany", firstCompany.getSingleReference(COMPANY.Country.name)!!.objectName)
        assertFalse(firstCompany.attachmentsExist)


        val secondCompany = companies.firstOrNull { it.id == 2 }
        assertNotNull(secondCompany)
        assertEquals(2, secondCompany!!.id)
        assertEquals("IT-2", secondCompany.objectKey)
        assertEquals("Test AG", secondCompany.label)
        assertEquals("Test AG", secondCompany.getStringValue(COMPANY.Name.name))
        assertEquals("Germany", secondCompany.getSingleReference(COMPANY.Country.name)!!.objectName)
        assertTrue(secondCompany.attachmentsExist)
        println("### END object_testObjectListWithFlatReference")
    }

    @Test
    fun testObjectListWithResolvedReference() {
        println("### START object_testObjectListWithResolvedReference")
        val companies = runBlocking {
            insightObjectOperator.getObjects(OBJECTS.Company.id).orNull()?.objects
        }
        assertNotNull(companies)
        assertTrue(companies!!.size == 2)

        val company = companies.firstOrNull { it.id == 1 }
        assertNotNull(company)

        val country = runBlocking {
            insightObjectOperator.getObjectById(
                company!!.getSingleReference(COMPANY.Country.name)!!.objectId
            ).orNull()!!
        }
        assertTrue(country.getStringValue(COUNTRY.Name.name) == "Germany")
        assertTrue(country.getStringValue(COUNTRY.ShortName.name) == "DE")
        println("### END object_testObjectListWithResolvedReference")
    }

    @Test
    fun testObjectById() {
        println("### START object_testObjectById")
        val company = runBlocking {
            insightObjectOperator.getObjectById(1).orNull()!!
        }
        assertEquals(1, company.id)
        assertEquals("IT-1", company.objectKey)
        assertEquals("Test GmbH", company.label)
        assertEquals("Test GmbH", company.getStringValue(COMPANY.Name.name))
        assertEquals("Germany", company.getSingleReference(COMPANY.Country.name)!!.objectName)
        assertFalse(company.attachmentsExist)
        println("### END object_testObjectById")
    }

    @Test
    fun testObjectWithListAttributes() {
        println("### START object_testObjectWithListAttributes")
        val obj = runBlocking {
            insightObjectOperator.getObjects(OBJECTS.TestWithLists.id).orNull()
        }!!.objects.first()

        val references = obj.getMultiReference(TEST_WITH_LISTS.ItemList.name)
        val idList = references.map { it.objectId }
        val nameList = references.map { it.objectName }
        val refList = references.map { insightReference ->
            runBlocking {
                insightObjectOperator.getObjectById(insightReference.objectId).orNull()!!
            }
        }
        val firstNameList = refList.map { it.getStringValue(SIMPLE_OBJECT.Firstname.name) }

        assertTrue(idList == listOf(35, 36, 37))
        assertTrue(nameList == listOf("Object1", "Object2", "Object3"))
        assertTrue(firstNameList == listOf("F1", "F2", "F3"))
        println("### END object_testObjectWithListAttributes")
    }

    @Test
    fun testAddingSelectList() {
        println("### START object_testAddingSelectList")
        val obj = runBlocking {
            insightObjectOperator.getObjects(OBJECTS.TestWithLists.id).orNull()
        }!!.objects.first()
        val results = obj.getValueList("StringList")
        assertTrue(results.isEmpty())
        obj.addValue("StringList", "A")
        obj.addValue("StringList", "B")
        runBlocking { insightObjectOperator.updateObject(obj).orNull() }

        val obj2 = runBlocking {
            insightObjectOperator.getObjects(OBJECTS.TestWithLists.id).orNull()
        }!!.objects.first()
        val results2 = obj2.getValueList("StringList")
        assertTrue(results2.size == 2)
        assertTrue(results2.contains("A"))
        assertTrue(results2.contains("B"))
        obj2.removeValue("StringList", "B")
        runBlocking { insightObjectOperator.updateObject(obj2).orNull() }

        val obj3 = runBlocking {
            insightObjectOperator.getObjects(OBJECTS.TestWithLists.id).orNull()
        }!!.objects.first()
        val results3 = obj3.getValueList("StringList")
        assertTrue(results3.size == 1)
        assertTrue(results3.contains("A"))
        obj3.removeValue("StringList", "A")
        runBlocking { insightObjectOperator.updateObject(obj3).orNull() }

        val obj4 = runBlocking {
            insightObjectOperator.getObjects(OBJECTS.TestWithLists.id).orNull()
        }!!.objects.first()
        val results4 = obj4.getValueList("StringList")
        assertTrue(results4.isEmpty())
        println("### END object_testAddingSelectList")
    }

    @Test
    fun testCreateAndDelete() {
        println("### START object_testCreateAndDelete")
        runBlocking {
            // Check England does not exist
            val countryBeforeCreate = insightObjectOperator.getObjectByName(OBJECTS.Country.id, "England").orNull()
            val companyBeforeCreate =
                insightObjectOperator.getObjectByName(OBJECTS.Company.id, "MyTestCompany GmbH").orNull()
            assertTrue(countryBeforeCreate == null)
            assertTrue(companyBeforeCreate == null)

            // Create and check direct result
            val country1 = insightObjectOperator.createObject(OBJECTS.Country.id) {
                it.setStringValue(COUNTRY.Name.name, "England")
                it.setStringValue(COUNTRY.ShortName.name, "GB")
            }.orNull()!!

            val company1 = insightObjectOperator.createObject(OBJECTS.Company.id) {
                it.setStringValue(COMPANY.Name.name, "MyTestCompany GmbH")
                it.setSingleReference(COMPANY.Country.name, country1.id)
            }.orNull()!!

            assertTrue(country1.id > 0)
            assertTrue(country1.getStringValue(COUNTRY.Key.name)!!.isNotBlank())
            assertTrue(company1.getSingleReference(COMPANY.Country.name)!!.objectId > 0)
            assertTrue(company1.getSingleReference(COMPANY.Country.name)!!.objectKey.isNotBlank())

            // Check England does exist
            val countryReference = company1.getSingleReference(COMPANY.Country.name)!!
            val countryAfterCreate = insightObjectOperator.getObjectByName(OBJECTS.Country.id, "England").orNull()!!
            val companyAfterCreate =
                insightObjectOperator.getObjectByName(OBJECTS.Company.id, "MyTestCompany GmbH").orNull()!!
            assertTrue(countryAfterCreate.id == countryReference.objectId)
            assertTrue(countryAfterCreate.getStringValue(COUNTRY.Key.name) == countryReference.objectKey)
            assertTrue(countryAfterCreate.getStringValue(COUNTRY.Name.name) == countryReference.objectName)
            assertTrue(companyAfterCreate.id == company1.id)

            // Check Delete
            insightObjectOperator.deleteObject(countryReference.objectId)
            insightObjectOperator.deleteObject(company1.id)
            val companyAfterDelete = insightObjectOperator.getObjectByName(
                OBJECTS.Company.id, company1.getStringValue(
                    COMPANY.Name.name
                )!!
            ).orNull()
            val countryAfterDelete = insightObjectOperator.getObjectByName(
                OBJECTS.Country.id, company1.getStringValue(
                    COUNTRY.Name.name
                )!!
            ).orNull()
            assertTrue(companyAfterDelete == null)
            assertTrue(countryAfterDelete == null)
        }
        println("### END object_testCreateAndDelete")
    }

    @Test
    fun testFilter() {
        println("### START object_testFilter")
        runBlocking {
            val countries =
                insightObjectOperator.getObjectsByIQL(OBJECTS.Country.id, false, "\"ShortName\"=\"DE\"")
                    .orNull()!!.objects
            assertTrue(countries.size == 1)
            assertTrue(countries.first().getStringValue(COUNTRY.ShortName.name) == "DE")
            assertTrue(countries.first().getStringValue(COUNTRY.Name.name) == "Germany")
        }
        println("### END object_testFilter")
    }

    @Test
    fun testUpdate() {
        println("### START object_testUpdate")
        runBlocking {
            var country = insightObjectOperator.getObjectByName(OBJECTS.Country.id, "Germany").orNull()!!
            assertTrue(country.getStringValue(COUNTRY.Name.name) == "Germany")
            assertTrue(country.getStringValue(COUNTRY.ShortName.name) == "DE")
            country.setStringValue(COUNTRY.ShortName.name, "ED")
            country = runBlocking { insightObjectOperator.updateObject(country).orNull()!! }

            val country2 = insightObjectOperator.getObjectByName(OBJECTS.Country.id, "Germany").orNull()!!
            assertTrue(country2.getStringValue(COUNTRY.Name.name) == "Germany")
            assertTrue(country2.getStringValue(COUNTRY.ShortName.name) == "ED")

            var countryAfterUpdate = insightObjectOperator.getObjectByName(OBJECTS.Country.id, "Germany").orNull()!!
            assertTrue(countryAfterUpdate.getStringValue(COUNTRY.Name.name) == "Germany")
            assertTrue(countryAfterUpdate.getStringValue(COUNTRY.ShortName.name) == "ED")
            countryAfterUpdate.setStringValue(COUNTRY.ShortName.name, "DE")
            countryAfterUpdate = runBlocking { insightObjectOperator.updateObject(countryAfterUpdate).orNull()!! }

            val countryAfterReUpdate = insightObjectOperator.getObjectByName(OBJECTS.Country.id, "Germany").orNull()!!
            assertTrue(countryAfterReUpdate.getStringValue(COUNTRY.Name.name) == "Germany")
            assertTrue(countryAfterReUpdate.getStringValue(COUNTRY.ShortName.name) == "DE")
        }
        println("### END object_testUpdate")
    }

    @Test
    fun testGetObjectsWithoutChildren() {
        println("### START object_testGetObjectsWithoutChildren")
        val objectsList = runBlocking {
            insightObjectOperator.getObjects(OBJECTS.Abstract.id, withChildren = false).orNull()!!
        }
        assertTrue(objectsList.searchResult == 0)

        val objects = objectsList.objects
        assertTrue(objects.size == 0)

        println("### END testGetObjectsWithoutChildren")
    }

    @Test
    fun testGetObjectsWithChildren() {
        println("### START object_testGetObjectsWithChildren")
        val objectsList = runBlocking {
            insightObjectOperator.getObjects(OBJECTS.Abstract.id, withChildren = true).orNull()!!
        }
        assertTrue(objectsList.searchResult == 2)

        val objects = objectsList.objects
        assertTrue(objects.size == 2)

        val firstObj = objects.first()
        assertTrue(firstObj.id == 94)

        val secondObj = objects[1]
        assertTrue(secondObj.id == 95)

        println("### END testGetObjectsWithChildren")
    }

    @Test
    fun testGetObjectsWithChildrenPaginated() {
        println("### START object_testGetObjectsWithChildrenPaginated")

        // page 1 and 2 implicit
        val allObjectsList = runBlocking {
            insightObjectOperator.getObjects(OBJECTS.Abstract.id, withChildren = true, pageFrom = 1, perPage = 1)
                .orNull()!!
        }
        assertTrue(allObjectsList.searchResult == 2)
        val allObjects = allObjectsList.objects
        assertTrue(allObjects.size == 2)
        assertTrue(allObjects[0].id == 94)
        assertTrue(allObjects[1].id == 95)

        // page 1 and 2 explicit
        val allExplObjectsList = runBlocking {
            insightObjectOperator.getObjects(
                OBJECTS.Abstract.id,
                withChildren = true,
                pageFrom = 1,
                pageTo = 2,
                perPage = 1
            ).orNull()!!
        }
        assertTrue(allExplObjectsList.searchResult == 2)
        val allExplObjects = allExplObjectsList.objects
        assertTrue(allExplObjects.size == 2)
        assertTrue(allExplObjects[0].id == 94)
        assertTrue(allExplObjects[1].id == 95)

        // page 1
        val firstObjectsList = runBlocking {
            insightObjectOperator.getObjects(
                OBJECTS.Abstract.id,
                withChildren = true,
                pageFrom = 1,
                pageTo = 1,
                perPage = 1
            )
                .orNull()!!
        }
        assertTrue(firstObjectsList.searchResult == 2)
        val firstObjects = firstObjectsList.objects
        assertTrue(firstObjects.size == 1)
        assertTrue(firstObjects[0].id == 94)

        // page 2
        val secondObjectsList = runBlocking {
            insightObjectOperator.getObjects(
                OBJECTS.Abstract.id,
                withChildren = true,
                pageFrom = 2,
                pageTo = 2,
                perPage = 1
            )
                .orNull()!!
        }
        assertTrue(secondObjectsList.searchResult == 2)
        val secondObjects = secondObjectsList.objects
        assertTrue(secondObjects.size == 1)
        assertTrue(secondObjects[0].id == 95)

        // page doesn't exist
        val emptyObjectsList = runBlocking {
            insightObjectOperator.getObjects(OBJECTS.Abstract.id, withChildren = true, pageFrom = 3, perPage = 1)
                .orNull()!!
        }
        assertTrue(emptyObjectsList.searchResult == 2)
        val emptyObjects = emptyObjectsList.objects
        assertTrue(emptyObjects.isEmpty())

        println("### END testGetObjectsWithChildrenPaginated")
    }
}
