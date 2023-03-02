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
            insightObjectOperator.getObjects(InsightObject.Company.id).orNull()!!.objects
        }
        assertTrue(companies.size == 2)

        val firstCompany = companies.firstOrNull { it.id == 1 }
        assertNotNull(firstCompany)
        assertEquals(1, firstCompany!!.id)
        assertEquals("IT-1", firstCompany.objectKey)
        assertEquals("Test GmbH", firstCompany.label)

        // Name
        assertNotNull(firstCompany.getAttributeByName(InsightAttribute.CompanyName.attributeName))
        assertEquals(
            InsightAttribute.CompanyName.attributeId,
            firstCompany.getAttributeIdByName(InsightAttribute.CompanyName.attributeName)
        )
        assertEquals("Test GmbH", firstCompany.getStringValue(InsightAttribute.CompanyName.attributeId))

        // Country
        assertNotNull(firstCompany.getAttributeByName(InsightAttribute.CompanyCountry.attributeName))
        assertEquals(
            InsightAttribute.CompanyCountry.attributeId,
            firstCompany.getAttributeIdByName(InsightAttribute.CompanyCountry.attributeName)
        )
        assertEquals(
            "Germany",
            firstCompany.getSingleReferenceValue(InsightAttribute.CompanyCountry.attributeId)!!.objectName
        )
        assertFalse(firstCompany.attachmentsExist)

        val secondCompany = companies.firstOrNull { it.id == 2 }
        assertNotNull(secondCompany)
        assertEquals(2, secondCompany!!.id)
        assertEquals("IT-2", secondCompany.objectKey)
        assertEquals("Test AG", secondCompany.label)

        // Name
        assertNotNull(secondCompany.getAttributeByName(InsightAttribute.CompanyName.attributeName))
        assertEquals(
            InsightAttribute.CompanyName.attributeId,
            secondCompany.getAttributeIdByName(InsightAttribute.CompanyName.attributeName)
        )
        assertEquals("Test AG", secondCompany.getStringValue(InsightAttribute.CompanyName.attributeId))

        // Country
        assertNotNull(secondCompany.getAttributeByName(InsightAttribute.CompanyCountry.attributeName))
        assertEquals(
            InsightAttribute.CompanyCountry.attributeId,
            secondCompany.getAttributeIdByName(InsightAttribute.CompanyCountry.attributeName)
        )
        assertEquals(
            "Germany",
            secondCompany.getSingleReferenceValue(InsightAttribute.CompanyCountry.attributeId)!!.objectName
        )
        assertTrue(secondCompany.attachmentsExist)

        println("### END object_testObjectListWithFlatReference")
    }

    @Test
    fun testObjectListWithResolvedReference() {
        println("### START object_testObjectListWithResolvedReference")
        val companies = runBlocking {
            insightObjectOperator.getObjects(InsightObject.Company.id).orNull()?.objects
        }
        assertNotNull(companies)
        assertTrue(companies!!.size == 2)

        val company = companies.firstOrNull { it.id == 1 }
        assertNotNull(company)

        val country = runBlocking {
            insightObjectOperator.getObjectById(
                company!!.getSingleReferenceValue(InsightAttribute.CompanyCountry.attributeId)!!.objectId
            ).orNull()!!
        }
        assertTrue(country.getStringValue(InsightAttribute.CountryName.attributeId) == "Germany")
        assertTrue(country.getStringValue(InsightAttribute.CountryShortName.attributeId) == "DE")
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
        assertEquals("Test GmbH", company.getStringValue(InsightAttribute.CompanyName.attributeId))
        assertEquals(
            "Germany",
            company.getSingleReferenceValue(InsightAttribute.CompanyCountry.attributeId)!!.objectName
        )
        assertFalse(company.attachmentsExist)
        println("### END object_testObjectById")
    }

    @Test
    fun testObjectWithListAttributes() {
        println("### START object_testObjectWithListAttributes")
        val obj = runBlocking {
            insightObjectOperator.getObjects(InsightObject.TestWithLists.id).orNull()
        }!!.objects.first()

        val references = obj.getMultiReferenceValue(InsightAttribute.TestWithListsItemList.attributeId)
        val idList = references.map { it.objectId }
        val nameList = references.map { it.objectName }
        val refList = references.map { insightReference ->
            runBlocking {
                insightObjectOperator.getObjectById(insightReference.objectId).orNull()!!
            }
        }
        val firstNameList = refList.map { it.getStringValue(InsightAttribute.SimpleObjectFirstname.attributeId) }

        assertTrue(idList == listOf(35, 36, 37))
        assertTrue(nameList == listOf("Object1", "Object2", "Object3"))
        assertTrue(firstNameList == listOf("F1", "F2", "F3"))
        println("### END object_testObjectWithListAttributes")
    }

    @Test
    fun testAddingSelectList() {
        println("### START object_testAddingSelectList")
        val obj = runBlocking {
            insightObjectOperator.getObjects(InsightObject.TestWithLists.id).orNull()
        }!!.objects.first()
        val results = obj.getValueList(InsightAttribute.TestWithListsStringList.attributeId)
        assertTrue(results.isEmpty())
        obj.addValue(InsightAttribute.TestWithListsStringList.attributeId, "A")
        obj.addValue(InsightAttribute.TestWithListsStringList.attributeId, "B")
        runBlocking { insightObjectOperator.updateObject(obj).orNull() }

        val obj2 = runBlocking {
            insightObjectOperator.getObjects(InsightObject.TestWithLists.id).orNull()
        }!!.objects.first()
        val results2 = obj2.getValueList(InsightAttribute.TestWithListsStringList.attributeId)
        assertTrue(results2.size == 2)
        assertTrue(results2.contains("A"))
        assertTrue(results2.contains("B"))
        obj2.removeValue(InsightAttribute.TestWithListsStringList.attributeId, "B")
        runBlocking { insightObjectOperator.updateObject(obj2).orNull() }

        val obj3 = runBlocking {
            insightObjectOperator.getObjects(InsightObject.TestWithLists.id).orNull()
        }!!.objects.first()
        val results3 = obj3.getValueList(InsightAttribute.TestWithListsStringList.attributeId)
        assertTrue(results3.size == 1)
        assertTrue(results3.contains("A"))
        obj3.removeValue(InsightAttribute.TestWithListsStringList.attributeId, "A")
        runBlocking { insightObjectOperator.updateObject(obj3).orNull() }

        val obj4 = runBlocking {
            insightObjectOperator.getObjects(InsightObject.TestWithLists.id).orNull()
        }!!.objects.first()
        val results4 = obj4.getValueList(InsightAttribute.TestWithListsStringList.attributeId)
        assertTrue(results4.isEmpty())
        println("### END object_testAddingSelectList")
    }

    @Test
    fun testCreateAndDelete() {
        println("### START object_testCreateAndDelete")
        runBlocking {
            // Check England does not exist
            val countryBeforeCreate =
                insightObjectOperator.getObjectByName(InsightObject.Country.id, "England").orNull()
            val companyBeforeCreate =
                insightObjectOperator.getObjectByName(InsightObject.Company.id, "MyTestCompany GmbH").orNull()
            assertTrue(countryBeforeCreate == null)
            assertTrue(companyBeforeCreate == null)

            // Create and check direct result
            val country1 = insightObjectOperator.createObject(InsightObject.Country.id) {
                it.setValue(InsightAttribute.CountryName.attributeId, "England")
                it.setValue(InsightAttribute.CountryShortName.attributeId, "GB")
            }.orNull()!!

            val company1 = insightObjectOperator.createObject(InsightObject.Company.id) {
                it.setValue(InsightAttribute.CompanyName.attributeId, "MyTestCompany GmbH")
                it.setSingleReference(InsightAttribute.CompanyCountry.attributeId, country1.id)
            }.orNull()!!

            assertTrue(country1.id > 0)
            assertTrue(country1.getStringValue(InsightAttribute.CountryKey.attributeId)!!.isNotBlank())
            assertTrue(company1.getSingleReferenceValue(InsightAttribute.CompanyCountry.attributeId)!!.objectId > 0)
            assertTrue(company1.getSingleReferenceValue(InsightAttribute.CompanyCountry.attributeId)!!.objectKey.isNotBlank())

            // Check England does exist
            val countryReference = company1.getSingleReferenceValue(InsightAttribute.CompanyCountry.attributeId)!!
            val countryAfterCreate =
                insightObjectOperator.getObjectByName(InsightObject.Country.id, "England").orNull()!!
            val companyAfterCreate =
                insightObjectOperator.getObjectByName(InsightObject.Company.id, "MyTestCompany GmbH").orNull()!!
            assertTrue(countryAfterCreate.id == countryReference.objectId)
            assertTrue(countryAfterCreate.getStringValue(InsightAttribute.CountryKey.attributeId) == countryReference.objectKey)
            assertTrue(countryAfterCreate.getStringValue(InsightAttribute.CountryName.attributeId) == countryReference.objectName)
            assertTrue(companyAfterCreate.id == company1.id)

            // Check Delete
            insightObjectOperator.deleteObject(countryReference.objectId)
            insightObjectOperator.deleteObject(company1.id)
            val companyAfterDelete = insightObjectOperator.getObjectById(countryReference.objectId).orNull()
            val countryAfterDelete = insightObjectOperator.getObjectById(country1.id).orNull()
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
                insightObjectOperator.getObjectsByIQL(InsightObject.Country.id, false, "\"ShortName\"=\"DE\"")
                    .orNull()!!.objects
            assertTrue(countries.size == 1)
            assertTrue(countries.first().getStringValue(InsightAttribute.CountryShortName.attributeId) == "DE")
            assertTrue(countries.first().getStringValue(InsightAttribute.CountryName.attributeId) == "Germany")
        }
        println("### END object_testFilter")
    }

    @Test
    fun testUpdate() {
        println("### START object_testUpdate")
        runBlocking {
            var country = insightObjectOperator.getObjectByName(InsightObject.Country.id, "Germany").orNull()!!
            assertTrue(country.getStringValue(InsightAttribute.CountryName.attributeId) == "Germany")
            assertTrue(country.getStringValue(InsightAttribute.CountryShortName.attributeId) == "DE")
            country.setValue(InsightAttribute.CountryShortName.attributeId, "ED")
            country = runBlocking { insightObjectOperator.updateObject(country).orNull()!! }

            val country2 = insightObjectOperator.getObjectByName(InsightObject.Country.id, "Germany").orNull()!!
            assertTrue(country2.getStringValue(InsightAttribute.CountryName.attributeId) == "Germany")
            assertTrue(country2.getStringValue(InsightAttribute.CountryShortName.attributeId) == "ED")

            var countryAfterUpdate =
                insightObjectOperator.getObjectByName(InsightObject.Country.id, "Germany").orNull()!!
            assertTrue(countryAfterUpdate.getStringValue(InsightAttribute.CountryName.attributeId) == "Germany")
            assertTrue(countryAfterUpdate.getStringValue(InsightAttribute.CountryShortName.attributeId) == "ED")
            countryAfterUpdate.setValue(InsightAttribute.CountryShortName.attributeId, "DE")
            countryAfterUpdate = runBlocking { insightObjectOperator.updateObject(countryAfterUpdate).orNull()!! }

            val countryAfterReUpdate =
                insightObjectOperator.getObjectByName(InsightObject.Country.id, "Germany").orNull()!!
            assertTrue(countryAfterReUpdate.getStringValue(InsightAttribute.CountryName.attributeId) == "Germany")
            assertTrue(countryAfterReUpdate.getStringValue(InsightAttribute.CountryShortName.attributeId) == "DE")
        }
        println("### END object_testUpdate")
    }

    @Test
    fun testGetObjectsWithoutChildren() {
        println("### START object_testGetObjectsWithoutChildren")
        val objectResponse = runBlocking {
            insightObjectOperator.getObjects(InsightObject.Abstract.id, withChildren = false).orNull()!!
        }
        assertTrue(objectResponse.searchResult == 0)

        val objects = objectResponse.objects
        assertTrue(objects.size == 0)

        println("### END testGetObjectsWithoutChildren")
    }

    @Test
    fun testGetObjectsWithChildren() {
        println("### START object_testGetObjectsWithChildren")
        val objectResponse = runBlocking {
            insightObjectOperator.getObjects(InsightObject.Abstract.id, withChildren = true).orNull()!!
        }
        assertTrue(objectResponse.searchResult == 2)

        val objects = objectResponse.objects
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
        val allINSIGHTOBJECTList = runBlocking {
            insightObjectOperator.getObjects(InsightObject.Abstract.id, withChildren = true, pageFrom = 1, perPage = 1)
                .orNull()!!
        }
        assertTrue(allINSIGHTOBJECTList.searchResult == 2)
        val allObjects = allINSIGHTOBJECTList.objects
        assertTrue(allObjects.size == 2)
        assertTrue(allObjects[0].id == 94)
        assertTrue(allObjects[1].id == 95)

        // page 1 and 2 explicit
        val allExplINSIGHTOBJECTList = runBlocking {
            insightObjectOperator.getObjects(
                InsightObject.Abstract.id,
                withChildren = true,
                pageFrom = 1,
                pageTo = 2,
                perPage = 1
            ).orNull()!!
        }
        assertTrue(allExplINSIGHTOBJECTList.searchResult == 2)
        val allExplObjects = allExplINSIGHTOBJECTList.objects
        assertTrue(allExplObjects.size == 2)
        assertTrue(allExplObjects[0].id == 94)
        assertTrue(allExplObjects[1].id == 95)

        // page 1
        val firstINSIGHTOBJECTList = runBlocking {
            insightObjectOperator.getObjects(
                InsightObject.Abstract.id,
                withChildren = true,
                pageFrom = 1,
                pageTo = 1,
                perPage = 1
            )
                .orNull()!!
        }
        assertTrue(firstINSIGHTOBJECTList.searchResult == 2)
        val firstObjects = firstINSIGHTOBJECTList.objects
        assertTrue(firstObjects.size == 1)
        assertTrue(firstObjects[0].id == 94)

        // page 2
        val secondINSIGHTOBJECTList = runBlocking {
            insightObjectOperator.getObjects(
                InsightObject.Abstract.id,
                withChildren = true,
                pageFrom = 2,
                pageTo = 2,
                perPage = 1
            )
                .orNull()!!
        }
        assertTrue(secondINSIGHTOBJECTList.searchResult == 2)
        val secondObjects = secondINSIGHTOBJECTList.objects
        assertTrue(secondObjects.size == 1)
        assertTrue(secondObjects[0].id == 95)

        // page doesn't exist
        val emptyINSIGHTOBJECTList = runBlocking {
            insightObjectOperator.getObjects(InsightObject.Abstract.id, withChildren = true, pageFrom = 3, perPage = 1)
                .orNull()!!
        }
        assertTrue(emptyINSIGHTOBJECTList.searchResult == 2)
        val emptyObjects = emptyINSIGHTOBJECTList.objects
        assertTrue(emptyObjects.isEmpty())

        println("### END testGetObjectsWithChildrenPaginated")
    }
}
