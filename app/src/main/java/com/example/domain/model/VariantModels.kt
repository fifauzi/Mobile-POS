package com.example.domain.model

import org.json.JSONArray
import org.json.JSONObject

data class VariantOption(
    val name: String,
    val priceAdjustment: Double = 0.0
)

data class VariantGroup(
    val name: String,
    val isMultiSelect: Boolean = false,
    val options: List<VariantOption> = emptyList()
)

object VariantParser {
    fun parseVariantsJson(json: String?): List<VariantGroup> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<VariantGroup>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val groupName = obj.optString("name", "")
                val isMulti = obj.optBoolean("isMultiSelect", false)
                val optionsArray = obj.optJSONArray("options")
                val options = mutableListOf<VariantOption>()
                if (optionsArray != null) {
                    for (j in 0 until optionsArray.length()) {
                        val optObj = optionsArray.getJSONObject(j)
                        options.add(
                            VariantOption(
                                name = optObj.optString("name", ""),
                                priceAdjustment = optObj.optDouble("price", 0.0)
                            )
                        )
                    }
                }
                list.add(VariantGroup(name = groupName, isMultiSelect = isMulti, options = options))
            }
        } catch (_: Exception) {
        }
        return list
    }

    fun toJson(groups: List<VariantGroup>): String {
        val array = JSONArray()
        for (g in groups) {
            val obj = JSONObject()
            obj.put("name", g.name)
            obj.put("isMultiSelect", g.isMultiSelect)
            val optsArr = JSONArray()
            for (opt in g.options) {
                val optObj = JSONObject()
                optObj.put("name", opt.name)
                optObj.put("price", opt.priceAdjustment)
                optsArr.put(optObj)
            }
            obj.put("options", optsArr)
            array.put(obj)
        }
        return array.toString()
    }
}
