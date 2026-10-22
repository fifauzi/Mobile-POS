package com.example.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "variant_groups")
data class VariantGroupEntity(
    @PrimaryKey val id: String, // e.g. "size", "type", "beans_type", "grind_size", "sugar_level", "addons"
    val name: String,
    val isMultiSelect: Boolean = false,
    val optionsJson: String, // JSON array of [{"name":"Regular","price":0.0},{"name":"Large","price":5000.0}]
    val applicableCategoryIds: String = "all", // "all" or comma-separated e.g. "coffee,non_coffee"
    val sortOrder: Int = 0
)
