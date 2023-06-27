/*-
 * #%L
 * kotlin-insight-client-api
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
package com.linkedplanet.kotlininsightclient.api.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.Collections.emptyList
import javax.validation.constraints.NotNull

// region ID wrapper

@JvmInline
value class InsightObjectId(@field:NotNull val raw: Int) {
    companion object {
        val notPersistedObjectId = InsightObjectId(-1)
    }
}

@JvmInline
value class InsightObjectTypeId(@field:NotNull val raw: Int)

@JvmInline
value class AttachmentId(@field:NotNull val raw: Int)

@JvmInline
value class InsightSchemaId(@field:NotNull val raw: Int)

@JvmInline
value class InsightAttributeId(@field:NotNull val raw: Int)

// endregion ID wrapper

data class InsightObjectPage<T>(
    @field:NotNull val totalFilterCount: Int = -1,
    @field:NotNull val objects: List<T> = emptyList(),
)

data class Page<T> (
    @field:NotNull val items: List<T>,
    @field:NotNull val totalItems: Int,
    @field:NotNull val totalPages: Int,
    @field:NotNull val currentPageIndex: Int,
    @field:NotNull val pageSize: Int
)

fun <T> InsightObjectPage<T>.plus(insightObjectPage: InsightObjectPage<T>): InsightObjectPage<T> =
    InsightObjectPage(
        this.totalFilterCount + insightObjectPage.totalFilterCount,
        this.objects.plus(insightObjectPage.objects)
    )

data class InsightObject(
    @get:JvmName("getObjectTypeId")
    @field:NotNull  val objectTypeId: InsightObjectTypeId,
    @get:JvmName("getId")
    @field:NotNull val id: InsightObjectId,
    @field:NotNull val objectTypeName: String,
    @field:NotNull val objectKey: String,
    @field:NotNull val label: String,
    @field:NotNull var attributes: List<InsightAttribute>,
    @field:NotNull val attachmentsExist: Boolean,
    @field:NotNull val objectSelf: String
)

data class InsightReference(
    @get:JvmName("getObjectTypeId")
    @field:NotNull val objectTypeId: InsightObjectTypeId,
    @field:NotNull val objectTypeName: String,
    @get:JvmName("getObjectId")
    @field:NotNull val objectId: InsightObjectId,
    @field:NotNull val objectKey: String,
    @field:NotNull val objectName: String
)

/**
 * Holds the actual data value(s)
 */
@Schema(
    discriminatorProperty = "type", // improves compatibility with Gson serialization
    oneOf = [
        InsightAttribute.Text::class,
        InsightAttribute.Integer::class,
        InsightAttribute.Bool::class,
        InsightAttribute.DoubleNumber::class,
        InsightAttribute.Select::class,
        InsightAttribute.Date::class,
        InsightAttribute.Time::class,
        InsightAttribute.DateTime::class,
        InsightAttribute.Url::class,
        InsightAttribute.Email::class,
        InsightAttribute.Textarea::class,
        InsightAttribute.Ipaddress::class,
        InsightAttribute.Reference::class,
        InsightAttribute.User::class,
        InsightAttribute.Confluence::class,
        InsightAttribute.Group::class,
        InsightAttribute.Version::class,
        InsightAttribute.Project::class,
        InsightAttribute.Status::class,
        InsightAttribute.Unknown::class,
    ]
)
sealed class InsightAttribute(
    @get:JvmName("getAttributeId")
    @Transient @field:NotNull open val attributeId: InsightAttributeId,
    @Transient open val schema: ObjectTypeSchemaAttribute?,
    @field:NotNull val type: AttributeTypeEnum
) {
    data class Text(
        @get:JvmName("getAttributeId")
        @field:NotNull override val attributeId: InsightAttributeId,
        val value: String?,
        override val schema: ObjectTypeSchemaAttribute?
    ) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Text) {
        override fun toString() = value ?: ""
    }

    data class Integer(
        @get:JvmName("getAttributeId")
        @field:NotNull override val attributeId: InsightAttributeId,
        val value: Int?,
        override val schema: ObjectTypeSchemaAttribute?
    ) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Integer){
        override fun toString() = value?.toString() ?: ""
    }

    data class Bool(
        @get:JvmName("getAttributeId")
        @field:NotNull override val attributeId: InsightAttributeId,
        val value: Boolean?,
        override val schema: ObjectTypeSchemaAttribute?
    ) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Bool){
        override fun toString() = value?.toString() ?: ""
    }

    data class DoubleNumber(
        @get:JvmName("getAttributeId")
        @field:NotNull override val attributeId: InsightAttributeId,
        val value: Double?,
        override val schema: ObjectTypeSchemaAttribute?
    ) : InsightAttribute(attributeId, schema, AttributeTypeEnum.DoubleNumber){
        override fun toString() = value?.toString() ?: ""
    }

    data class Date(
        @get:JvmName("getAttributeId")
        @field:NotNull override val attributeId: InsightAttributeId,
        val value: LocalDate?,
        val displayValue: String?,
        override val schema: ObjectTypeSchemaAttribute?
    ) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Date){
        override fun toString() = value?.toString() ?: ""
    }

    /**
     * Note that time is part of the Enum inside the SDK, but is not selectable through the Insight GUI
     */
    data class Time(
        @get:JvmName("getAttributeId")
        @field:NotNull override val attributeId: InsightAttributeId,
        val value: LocalTime?,
        val displayValue: String?,
        override val schema: ObjectTypeSchemaAttribute?
    ) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Time){
        override fun toString() = value?.toString() ?: ""
    }

    data class DateTime(
        @get:JvmName("getAttributeId")
        @field:NotNull override val attributeId: InsightAttributeId,
        val value: ZonedDateTime?,
        val displayValue: String?,
        override val schema: ObjectTypeSchemaAttribute?
    ) : InsightAttribute(attributeId, schema, AttributeTypeEnum.DateTime){
        override fun toString() = value?.toString() ?: ""
    }

    data class Email(
        @get:JvmName("getAttributeId")
        @field:NotNull override val attributeId: InsightAttributeId,
        val value: String?,
        override val schema: ObjectTypeSchemaAttribute?
    ) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Email){
        override fun toString() = value ?: ""
    }

    data class Textarea(
        @get:JvmName("getAttributeId")
        @field:NotNull override val attributeId: InsightAttributeId,
        val value: String?,
        override val schema: ObjectTypeSchemaAttribute?
    ) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Textarea){
        override fun toString() = value ?: ""
    }

    data class Ipaddress(
        @get:JvmName("getAttributeId")
        @field:NotNull override val attributeId: InsightAttributeId,
        val value: String?,
        override val schema: ObjectTypeSchemaAttribute?
    ) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Ipaddress){
        override fun toString() = value ?: ""
    }

    //  cardinality > 1
    data class Url(
        @get:JvmName("getAttributeId")
        @field:NotNull override val attributeId: InsightAttributeId,
        val values: List<String>,
        override val schema: ObjectTypeSchemaAttribute?
    ) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Url){
        override fun toString() = values.joinToString(",")
    }

    data class Select(
        @get:JvmName("getAttributeId")
        @field:NotNull override val attributeId: InsightAttributeId,
        val values: List<String>,
        override val schema: ObjectTypeSchemaAttribute?
    ) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Select){
        override fun toString() = values.joinToString(",")
    }

    // non default types
    data class Reference(
        @get:JvmName("getAttributeId")
        @field:NotNull override val attributeId: InsightAttributeId,
        @field:NotNull val referencedObjects: List<ReferencedObject>,
        override val schema: ObjectTypeSchemaAttribute?
    ) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Reference) {
        override fun toString() = referencedObjects.joinToString(",") { it.objectKey }
    }

    data class User(
        @get:JvmName("getAttributeId")
        @field:NotNull override val attributeId: InsightAttributeId,
        @field:NotNull val users: List<InsightUser>,
        override val schema: ObjectTypeSchemaAttribute?
    ) : InsightAttribute(attributeId, schema, AttributeTypeEnum.User){
        override fun toString() = users.joinToString(",") { it.key }
    }

    /**
     * A page in Confluence
     */
    data class Confluence(
        @get:JvmName("getAttributeId")
        @field:NotNull override val attributeId: InsightAttributeId,
        override val schema: ObjectTypeSchemaAttribute?
    ) : InsightAttribute(
        attributeId,
        schema,
        AttributeTypeEnum.Confluence
    ) {
        override fun toString() = "Confluence attributeId=$attributeId"
    }

    /**
     * The Insight Group type
     */
    data class Group(
        @get:JvmName("getAttributeId")
        @field:NotNull override val attributeId: InsightAttributeId,
        override val schema: ObjectTypeSchemaAttribute?) :
        InsightAttribute(attributeId, schema, AttributeTypeEnum.Group){
        override fun toString() = "Group attributeId=$attributeId"
    }

    /**
     * Version in Jira
     */
    data class Version(
        @get:JvmName("getAttributeId")
        @field:NotNull override val attributeId: InsightAttributeId,
        override val schema: ObjectTypeSchemaAttribute?) :
        InsightAttribute(attributeId, schema, AttributeTypeEnum.Version){
        override fun toString() = "Version attributeId=$attributeId"
    }

    /**
     *  Represents a Jira project
     */
    data class Project(
        @get:JvmName("getAttributeId")
        @field:NotNull override val attributeId: InsightAttributeId,
        override val schema: ObjectTypeSchemaAttribute?) :
        InsightAttribute(attributeId, schema, AttributeTypeEnum.Project){
        override fun toString() = "Project attributeId=$attributeId"
    }

    /**
     * An Insight status type that can be associated with objects.
     * Cardinality:0-1
     */
    data class Status(
        @get:JvmName("getAttributeId")
        @field:NotNull override val attributeId: InsightAttributeId,
        override val schema: ObjectTypeSchemaAttribute?) :
            InsightAttribute(attributeId, schema, AttributeTypeEnum.Status){
        override fun toString() = "Status attributeId=$attributeId"
    }

    data class Unknown(
        @get:JvmName("getAttributeId")
        @field:NotNull override val attributeId: InsightAttributeId,
        override val schema: ObjectTypeSchemaAttribute?) :
        InsightAttribute(attributeId, schema, AttributeTypeEnum.Unknown){
        override fun toString() = ""
    }

    companion object {
        infix fun InsightAttributeId.toValue(text: String?) =
            Text(this, value = text, schema = null)
        infix fun InsightAttributeId.toValue(value: Int?) =
            Integer(this, value = value, schema = null)

        infix fun InsightAttributeId.toValue(value: Boolean?) =
            Bool(this, value = value, schema = null)
        infix fun InsightAttributeId.toValue(value: Double?) =
            DoubleNumber(this, value = value, schema = null)
        infix fun InsightAttributeId.toValue(value: LocalDate?) =
            Date(this, value = value, schema = null, displayValue = null)
        infix fun InsightAttributeId.toValue(value: LocalTime?) =
            Time(this, value = value, schema = null, displayValue = null)
        infix fun InsightAttributeId.toValue(value: ZonedDateTime?) =
            DateTime(this, value = value, schema = null, displayValue = null)
        infix fun InsightAttributeId.toEmailValue(text: String?) =
            Email(this, value = text, schema = null)
        infix fun InsightAttributeId.toTextareaValue(text: String?) =
            Textarea(this, value = text, schema = null)
        infix fun InsightAttributeId.toIpaddressValue(text: String?) =
            Ipaddress(this, value = text, schema = null)

        infix fun InsightAttributeId.toSelectValues(values: List<String>) =
            Select(this, values = values, schema = null)

        infix fun InsightAttributeId.toUrlValues(values: List<String>) =
            Url(this, values = values, schema = null)

        infix fun InsightAttributeId.toUser(user: InsightUser?) =
            User(this, listOfNotNull(user), schema = null)

        infix fun InsightAttributeId.toUsers(users: List<InsightUser>) =
            User(this, users, schema = null)

        infix fun InsightAttributeId.toReference(referencedObjectId: InsightObjectId?) =
            Reference(
                this,
                referencedObjects = listOfNotNull(referencedObjectId?.let {
                    ReferencedObject(it, "", "", null)
                }),
                schema = null,
            )

        infix fun InsightAttributeId.toReferences(referencedObjectIds: List<InsightObjectId>) =
            Reference(
                this,
                referencedObjects = referencedObjectIds.map {
                    ReferencedObject(it, "", "", null)
                },
                schema = null,
            )
    }

}

fun InsightAttribute.isValueAttribute() = this.type.isValueAttribute()
fun ObjectTypeSchemaAttribute.isValueAttribute() = this.type.isValueAttribute()

fun AttributeTypeEnum.isValueAttribute(): Boolean =
    when (this) {
        AttributeTypeEnum.Text -> true
        AttributeTypeEnum.Integer -> true
        AttributeTypeEnum.Bool -> true
        AttributeTypeEnum.DoubleNumber -> true
        AttributeTypeEnum.Select -> true
        AttributeTypeEnum.Date -> true
        AttributeTypeEnum.Time -> true
        AttributeTypeEnum.DateTime -> true
        AttributeTypeEnum.Url -> true
        AttributeTypeEnum.Email -> true
        AttributeTypeEnum.Textarea -> true
        AttributeTypeEnum.Ipaddress -> true

        AttributeTypeEnum.Unknown -> false
        AttributeTypeEnum.Reference -> false
        AttributeTypeEnum.User -> false
        AttributeTypeEnum.Confluence -> false
        AttributeTypeEnum.Group -> false
        AttributeTypeEnum.Version -> false
        AttributeTypeEnum.Project -> false
        AttributeTypeEnum.Status -> false
    }

enum class AttributeTypeEnum {
    Text,
    Integer,
    Bool,
    DoubleNumber,
    Select,
    Date,
    Time,
    DateTime,
    Url,
    Email,
    Textarea,
    Ipaddress,
    Reference,
    User,
    Confluence,
    Group,
    Version,
    Project,
    Status,
    Unknown,
}

// region InsightObjectTypeOperator
data class ObjectTypeSchema(
    @get:JvmName("getId")
    @field:NotNull val id: InsightObjectTypeId,
    @field:NotNull val name: String,
    @field:NotNull val attributes: List<ObjectTypeSchemaAttribute>,
    @get:JvmName("getParentObjectTypeId")
    val parentObjectTypeId: InsightObjectTypeId?
)

@Schema(
    discriminatorProperty = "type",
    oneOf = [
        ObjectTypeSchemaAttribute.TextSchema::class,
        ObjectTypeSchemaAttribute.IntegerSchema::class,
        ObjectTypeSchemaAttribute.BoolSchema::class,
        ObjectTypeSchemaAttribute.DoubleNumberSchema::class,
        ObjectTypeSchemaAttribute.SelectSchema::class,
        ObjectTypeSchemaAttribute.DateSchema::class,
        ObjectTypeSchemaAttribute.TimeSchema::class,
        ObjectTypeSchemaAttribute.DateTimeSchema::class,
        ObjectTypeSchemaAttribute.UrlSchema::class,
        ObjectTypeSchemaAttribute.EmailSchema::class,
        ObjectTypeSchemaAttribute.TextareaSchema::class,
        ObjectTypeSchemaAttribute.IpaddressSchema::class,
        ObjectTypeSchemaAttribute.ReferenceSchema::class,
        ObjectTypeSchemaAttribute.UserSchema::class,
        ObjectTypeSchemaAttribute.ConfluenceSchema::class,
        ObjectTypeSchemaAttribute.GroupSchema::class,
        ObjectTypeSchemaAttribute.VersionSchema::class,
        ObjectTypeSchemaAttribute.ProjectSchema::class,
        ObjectTypeSchemaAttribute.StatusSchema::class,
        ObjectTypeSchemaAttribute.UnknownSchema::class,
    ]
)
sealed class ObjectTypeSchemaAttribute(
    @get:JvmName("getId")
    @Transient @field:NotNull open val id: InsightAttributeId,
    @Transient @field:NotNull open val name: String, // attributeName
    @Transient @field:NotNull open val minimumCardinality: Int,
    @Transient @field:NotNull open val maximumCardinality: Int,
    @Transient @field:NotNull open val includeChildObjectTypes: Boolean,
    @field:NotNull val type: AttributeTypeEnum
) {

    data class SelectSchema(
        @get:JvmName("getId")
        @field:NotNull override val id: InsightAttributeId,
        @field:NotNull override val name: String, // attributeName
        @field:NotNull override val minimumCardinality: Int,
        @field:NotNull override val maximumCardinality: Int,
        @field:NotNull override val includeChildObjectTypes: Boolean,
        @field:NotNull val options: List<String>,
    ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Select)

    data class ReferenceSchema(
        @get:JvmName("getId")
        @field:NotNull override val id: InsightAttributeId,
        @field:NotNull override val name: String, // attributeName
        @field:NotNull override val minimumCardinality: Int,
        @field:NotNull override val maximumCardinality: Int,
        @field:NotNull override val includeChildObjectTypes: Boolean,
        @get:JvmName("getReferenceObjectTypeId")
        @field:NotNull val referenceObjectTypeId: InsightObjectTypeId, // objectTypeId of the referenced object
        @field:NotNull val referenceKind: ReferenceKind
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Reference)

    data class UnknownSchema(
        @get:JvmName("getId")
        @field:NotNull override val id: InsightAttributeId,
        @field:NotNull override val name: String, // attributeName
        @field:NotNull override val minimumCardinality: Int,
        @field:NotNull override val maximumCardinality: Int,
        @field:NotNull override val includeChildObjectTypes: Boolean,
        @field:NotNull val debugDescription: String
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Unknown)

    // region types having just the superclass attributes
    data class TextSchema(
        @get:JvmName("getId")
        @field:NotNull override val id: InsightAttributeId,
        @field:NotNull override val name: String, // attributeName
        @field:NotNull override val minimumCardinality: Int,
        @field:NotNull override val maximumCardinality: Int,
        @field:NotNull override val includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Text)
    data class IntegerSchema(
        @get:JvmName("getId")
        @field:NotNull override val id: InsightAttributeId,
        @field:NotNull override val name: String, // attributeName
        @field:NotNull override val minimumCardinality: Int,
        @field:NotNull override val maximumCardinality: Int,
        @field:NotNull override val includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Integer)
    data class BoolSchema(
        @get:JvmName("getId")
        @field:NotNull override val id: InsightAttributeId,
        @field:NotNull override val name: String, // attributeName
        @field:NotNull override val minimumCardinality: Int,
        @field:NotNull override val maximumCardinality: Int,
        @field:NotNull override val includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Bool)
    data class DoubleNumberSchema(
        @get:JvmName("getId")
        @field:NotNull override val id: InsightAttributeId,
        @field:NotNull override val name: String, // attributeName
        @field:NotNull override val minimumCardinality: Int,
        @field:NotNull override val maximumCardinality: Int,
        @field:NotNull override val includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.DoubleNumber)
    data class DateSchema(
        @get:JvmName("getId")
        @field:NotNull override val id: InsightAttributeId,
        @field:NotNull override val name: String, // attributeName
        @field:NotNull override val minimumCardinality: Int,
        @field:NotNull override val maximumCardinality: Int,
        @field:NotNull override val includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Date)
    data class TimeSchema(
        @get:JvmName("getId")
        @field:NotNull override val id: InsightAttributeId,
        @field:NotNull override val name: String, // attributeName
        @field:NotNull override val minimumCardinality: Int,
        @field:NotNull override val maximumCardinality: Int,
        @field:NotNull override val includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Time)
    data class DateTimeSchema(
        @get:JvmName("getId")
        @field:NotNull override val id: InsightAttributeId,
        @field:NotNull override val name: String, // attributeName
        @field:NotNull override val minimumCardinality: Int,
        @field:NotNull override val maximumCardinality: Int,
        @field:NotNull override val includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.DateTime)
    data class UrlSchema(
        @get:JvmName("getId")
        @field:NotNull override val id: InsightAttributeId,
        @field:NotNull override val name: String, // attributeName
        @field:NotNull override val minimumCardinality: Int,
        @field:NotNull override val maximumCardinality: Int,
        @field:NotNull override val includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Url)
    data class EmailSchema(
        @get:JvmName("getId")
        @field:NotNull override val id: InsightAttributeId,
        @field:NotNull override val name: String, // attributeName
        @field:NotNull override val minimumCardinality: Int,
        @field:NotNull override val maximumCardinality: Int,
        @field:NotNull override val includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Email)
    data class TextareaSchema(
        @get:JvmName("getId")
        @field:NotNull override val id: InsightAttributeId,
        @field:NotNull override val name: String, // attributeName
        @field:NotNull override val minimumCardinality: Int,
        @field:NotNull override val maximumCardinality: Int,
        @field:NotNull override val includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Textarea)
    data class IpaddressSchema(
        @get:JvmName("getId")
        @field:NotNull override val id: InsightAttributeId,
        @field:NotNull override val name: String, // attributeName
        @field:NotNull override val minimumCardinality: Int,
        @field:NotNull override val maximumCardinality: Int,
        @field:NotNull override val includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Ipaddress)

    data class UserSchema(
        @get:JvmName("getId")
        @field:NotNull override val id: InsightAttributeId,
        @field:NotNull override val name: String, // attributeName
        @field:NotNull override val minimumCardinality: Int,
        @field:NotNull override val maximumCardinality: Int,
        @field:NotNull override val includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.User)
    data class ConfluenceSchema(
        @get:JvmName("getId")
        @field:NotNull override val id: InsightAttributeId,
        @field:NotNull override val name: String, // attributeName
        @field:NotNull override val minimumCardinality: Int,
        @field:NotNull override val maximumCardinality: Int,
        @field:NotNull override val includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Confluence)
    data class GroupSchema(
        @get:JvmName("getId")
        @field:NotNull override val id: InsightAttributeId,
        @field:NotNull override val name: String, // attributeName
        @field:NotNull override val minimumCardinality: Int,
        @field:NotNull override val maximumCardinality: Int,
        @field:NotNull override val includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Group)
    data class VersionSchema(
        @get:JvmName("getId")
        @field:NotNull override val id: InsightAttributeId,
        @field:NotNull override val name: String, // attributeName
        @field:NotNull override val minimumCardinality: Int,
        @field:NotNull override val maximumCardinality: Int,
        @field:NotNull override val includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Version)
    data class ProjectSchema(
        @get:JvmName("getId")
        @field:NotNull override val id: InsightAttributeId,
        @field:NotNull override val name: String, // attributeName
        @field:NotNull override val minimumCardinality: Int,
        @field:NotNull override val maximumCardinality: Int,
        @field:NotNull override val includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Project)
    data class StatusSchema(
        @get:JvmName("getId")
        @field:NotNull override val id: InsightAttributeId,
        @field:NotNull override val name: String, // attributeName
        @field:NotNull override val minimumCardinality: Int,
        @field:NotNull override val maximumCardinality: Int,
        @field:NotNull override val includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Status)

    // endregion types having just the superclass attributes
}

/**
 * This is the "Additional Value" one can select when choosing object as the attribute type.
 */
enum class ReferenceKind(var referenceKindId: Int) {
    UNKNOWN(-1),
    DEPENDENCY(1),
    LINK(2),
    REFERENCE(3),
    FINANCIAL(4),
    TECHNICAL(5);

    companion object {
        fun parse(referenceKindId: Int?): ReferenceKind =
            ReferenceKind.values().singleOrNull { it.referenceKindId == referenceKindId } ?: UNKNOWN
    }
}

// endregion InsightObjectTypeOperator

// region InsightSchemaOperator
data class InsightSchema(
    @get:JvmName("getId")
    @field:NotNull val id: InsightSchemaId,
    @field:NotNull val name: String,
    @field:NotNull val objectCount: Int,
    @field:NotNull val objectTypeCount: Int
)
// endregion InsightSchemaOperator

data class InsightUser(
    @field:NotNull val displayName: String,
    @field:NotNull val name: String,
    @field:NotNull val emailAddress: String,
    @field:NotNull val key: String
)

data class ReferencedObject(
    var id: InsightObjectId,
    var label: String,
    var objectKey: String,
    var objectType: ReferencedObjectType?
)

data class ReferencedObjectType(
    @get:JvmName("getId")
    @field:NotNull val id: InsightObjectTypeId,
    @field:NotNull val name: String
)

// region InsightHistoryOperator
data class InsightHistory(
    @get:JvmName("getObjectId")
    @field:NotNull val objectId: InsightObjectId,
    @field:NotNull val historyItems: List<InsightHistoryItem>
)

data class InsightHistoryItem(
    @field:NotNull val id: Int,
    val affectedAttribute: String?,
    val oldValue: String?,
    val newValue: String?,
    @field:NotNull val actor: Actor,
    @field:NotNull val type: Int,
    @field:NotNull val created: String, // updated is neither available through sdk nor ktor
    @field:NotNull val objectId: InsightObjectId
)

data class Actor(
    val key: String
)
// endregion InsightHistoryOperator

// region InsightAttachmentOperator
data class InsightAttachment(
    @get:JvmName("getId")
    @field:NotNull val id: AttachmentId,
    @field:NotNull val author: String,
    @field:NotNull val mimeType: String,
    @field:NotNull val filename: String,
    @field:NotNull val filesize: String, // for human display e.g. 10.1 kB
    @field:NotNull val created: String, // ISO 8601 String
    @field:NotNull val comment: String, // we are only able to download them but can not create attachments with comments
    @field:NotNull val url: String
)
// endregion InsightAttachmentOperator