package com.linkedplanet.kotlinatlassianclientcore.common.api

import javax.validation.constraints.NotNull

data class AtlassianUser(
    @field:NotNull val key: String,
    @field:NotNull val name: String,
    @field:NotNull val emailAddress: String,
    @field:NotNull val avatarUrl: String? = null,
    @field:NotNull val displayName: String
)