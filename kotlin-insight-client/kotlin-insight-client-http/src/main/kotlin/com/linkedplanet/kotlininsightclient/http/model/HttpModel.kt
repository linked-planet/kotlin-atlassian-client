package com.linkedplanet.kotlininsightclient.http.model

import com.linkedplanet.kotlininsightclient.api.model.InsightSchema

data class HttpInsightSchemaList(
    val objectschemas: List<InsightSchema>
)
