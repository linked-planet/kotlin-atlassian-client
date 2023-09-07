package com.linkedplanet.kotlinatlassianclientcore.common.api

import javax.validation.constraints.NotNull

data class Page<T> (
    @field:NotNull val items: List<T>,
    @field:NotNull val totalItems: Int,
    @field:NotNull val totalPages: Int,
    @field:NotNull val currentPageIndex: Int,
    @field:NotNull val pageSize: Int
)