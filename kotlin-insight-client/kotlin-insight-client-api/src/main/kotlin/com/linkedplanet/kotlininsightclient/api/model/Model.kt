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
    @field:NotNull val attributeId: InsightAttributeId,
    val schema: ObjectTypeSchemaAttribute?,
    @field:NotNull val type: AttributeTypeEnum
) {
    val isValueAttribute: Boolean by lazy {
        when (this) {
            is Text -> true
            is Integer -> true
            is Bool -> true
            is DoubleNumber -> true
            is Select -> true
            is Date -> true
            is Time -> true
            is DateTime -> true
            is Url -> true
            is Email -> true
            is Textarea -> true
            is Ipaddress -> true

            is Unknown -> false
            is Reference -> false
            is User -> false
            is Confluence -> false
            is Group -> false
            is Version -> false
            is Project -> false
            is Status -> false
        }
    }

    class Text(attributeId: InsightAttributeId, val value: String?, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Text)
    class Integer(attributeId: InsightAttributeId,val value: Int?, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Integer)
    class Bool(attributeId: InsightAttributeId,val value: Boolean?, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Bool)
    class DoubleNumber(attributeId: InsightAttributeId,val value: Double?, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema, AttributeTypeEnum.DoubleNumber)
    class Date(attributeId: InsightAttributeId,val value: LocalDate?, val displayValue: String?, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Date)
    /**
     * Note that time is part of the Enum inside the SDK, but is not selectable through the Insight GUI
     */
    class Time(attributeId: InsightAttributeId,val value: LocalTime?, val displayValue: String?, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Time)
    class DateTime(attributeId: InsightAttributeId,val value: ZonedDateTime?, val displayValue: String?, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema, AttributeTypeEnum.DateTime)
    class Email(attributeId: InsightAttributeId,val value: String?, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Email)
    class Textarea(attributeId: InsightAttributeId,val value: String?, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema, AttributeTypeEnum. Textarea)
    class Ipaddress(attributeId: InsightAttributeId,val value: String?, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Ipaddress)

    //  cardinality > 1
    class Url(attributeId: InsightAttributeId,val values: List<String>, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Url)
    class Select(attributeId: InsightAttributeId,val values: List<String>, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Select)

    // non default types
    class Reference(attributeId: InsightAttributeId,@field:NotNull val referencedObjects: List<ReferencedObject>, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Reference)
    class User(attributeId: InsightAttributeId,@field:NotNull val users: List<InsightUser>, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema, AttributeTypeEnum.User)
    class Confluence(attributeId: InsightAttributeId, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Confluence) // A value that describes a page in Confluence
    class Group(attributeId: InsightAttributeId, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Group) // The Insight Group type
    class Version(attributeId: InsightAttributeId, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Version) // Value describing a version in Jira
    class Project(attributeId: InsightAttributeId, schema: ObjectTypeSchemaAttribute?): InsightAttribute(attributeId, schema, AttributeTypeEnum.Project) // Value that represents a Jira project
    class Status(attributeId: InsightAttributeId, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Status) // An Insight status type that can be associated with objects Cardinality:0-1
    class Unknown(attributeId: InsightAttributeId, schema: ObjectTypeSchemaAttribute?) : InsightAttribute(attributeId, schema, AttributeTypeEnum.Unknown)

    override fun toString(): String = when (this) {
        is Text -> value ?: ""
        is Integer -> value?.toString() ?: ""
        is Bool -> value?.toString() ?: ""
        is Date -> value?.toString() ?: ""
        is DoubleNumber -> value?.toString() ?: ""
        is Email -> value ?: ""
        is Ipaddress -> value ?: ""
        is Textarea -> value ?: ""
        is DateTime -> value?.toString() ?: ""
        is Time -> value?.toString() ?: ""

        is Url ->  values.joinToString(",")
        is Select -> values.joinToString(",")

        is Reference -> referencedObjects.joinToString(",") { it.objectKey }
        is User -> users.joinToString(",") { it.key }

        // TODO support additional attribute types
        is Group -> "Group attributeId=$attributeId"
        is Project -> "Project attributeId=$attributeId"
        is Status -> "Status attributeId=$attributeId"
        is Version -> "Version attributeId=$attributeId"
        is Confluence -> "Confluence attributeId=$attributeId"
        is Unknown -> ""
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
    @field:NotNull val id: InsightAttributeId,
    @field:NotNull val name: String, // attributeName
    @field:NotNull val minimumCardinality: Int,
    @field:NotNull val maximumCardinality: Int,
    @field:NotNull val includeChildObjectTypes: Boolean,
    @field:NotNull val type: AttributeTypeEnum
) {

    val isValueAttribute: Boolean by lazy {
        when(this){
            is TextSchema -> true
            is IntegerSchema -> true
            is BoolSchema -> true
            is DoubleNumberSchema -> true
            is SelectSchema -> true
            is DateSchema -> true
            is TimeSchema -> true
            is DateTimeSchema -> true
            is UrlSchema -> true
            is EmailSchema -> true
            is TextareaSchema -> true
            is IpaddressSchema -> true

            is UnknownSchema -> false
            is ReferenceSchema -> false
            is UserSchema -> false
            is ConfluenceSchema -> false
            is GroupSchema -> false
            is VersionSchema -> false
            is ProjectSchema -> false
            is StatusSchema -> false
        }
    }

    class SelectSchema(
        id: InsightAttributeId,
        name: String,
        minimumCardinality: Int,
        maximumCardinality: Int,
        includeChildObjectTypes: Boolean,
        @field:NotNull val options: List<String>,
    ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Select)

    class ReferenceSchema(
        id: InsightAttributeId,
        name: String,
        minimumCardinality: Int,
        maximumCardinality: Int,
        includeChildObjectTypes: Boolean,
        @get:JvmName("getReferenceObjectTypeId")
        @field:NotNull val referenceObjectTypeId: InsightObjectTypeId, // objectTypeId of the referenced object
        @field:NotNull val referenceKind: ReferenceKind
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Reference)

    class UnknownSchema(
        id: InsightAttributeId,
        name: String,
        minimumCardinality: Int,
        maximumCardinality: Int,
        includeChildObjectTypes: Boolean,
        @field:NotNull val debugDescription: String
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Unknown)

    // region types having just the superclass attributes
    class TextSchema(
        id: InsightAttributeId,
        name: String,
        minimumCardinality: Int,
        maximumCardinality: Int,
        includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Text)
    class IntegerSchema(
        id: InsightAttributeId,
        name: String,
        minimumCardinality: Int,
        maximumCardinality: Int,
        includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Integer)
    class BoolSchema(
        id: InsightAttributeId,
        name: String,
        minimumCardinality: Int,
        maximumCardinality: Int,
        includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Bool)
    class DoubleNumberSchema(
        id: InsightAttributeId,
        name: String,
        minimumCardinality: Int,
        maximumCardinality: Int,
        includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.DoubleNumber)
    class DateSchema(
        id: InsightAttributeId,
        name: String,
        minimumCardinality: Int,
        maximumCardinality: Int,
        includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Date)
    class TimeSchema(
        id: InsightAttributeId,
        name: String,
        minimumCardinality: Int,
        maximumCardinality: Int,
        includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Time)
    class DateTimeSchema(
        id: InsightAttributeId,
        name: String,
        minimumCardinality: Int,
        maximumCardinality: Int,
        includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.DateTime)
    class UrlSchema(
        id: InsightAttributeId,
        name: String,
        minimumCardinality: Int,
        maximumCardinality: Int,
        includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Url)
    class EmailSchema(
        id: InsightAttributeId,
        name: String,
        minimumCardinality: Int,
        maximumCardinality: Int,
        includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Email)
    class TextareaSchema(
        id: InsightAttributeId,
        name: String,
        minimumCardinality: Int,
        maximumCardinality: Int,
        includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Textarea)
    class IpaddressSchema(
        id: InsightAttributeId,
        name: String,
        minimumCardinality: Int,
        maximumCardinality: Int,
        includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Ipaddress)

    class UserSchema(
        id: InsightAttributeId,
        name: String,
        minimumCardinality: Int,
        maximumCardinality: Int,
        includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.User)
    class ConfluenceSchema(
        id: InsightAttributeId,
        name: String,
        minimumCardinality: Int,
        maximumCardinality: Int,
        includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Confluence)
    class GroupSchema(
        id: InsightAttributeId,
        name: String,
        minimumCardinality: Int,
        maximumCardinality: Int,
        includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Group)
    class VersionSchema(
        id: InsightAttributeId,
        name: String,
        minimumCardinality: Int,
        maximumCardinality: Int,
        includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Version)
    class ProjectSchema(
        id: InsightAttributeId,
        name: String,
        minimumCardinality: Int,
        maximumCardinality: Int,
        includeChildObjectTypes: Boolean,
        ) : ObjectTypeSchemaAttribute(id, name, minimumCardinality, maximumCardinality, includeChildObjectTypes, AttributeTypeEnum.Project)
    class StatusSchema(
        id: InsightAttributeId,
        name: String,
        minimumCardinality: Int,
        maximumCardinality: Int,
        includeChildObjectTypes: Boolean,
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