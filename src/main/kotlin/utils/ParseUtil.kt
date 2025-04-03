package utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


fun parseEdits(edits: String): MutableList<Map<String, String>> {
    val editList: MutableList<Map<String, String>> = mutableListOf()
    try {
        if (edits.startsWith("[") && edits.endsWith("]")) {
            val array = Json.parseToJsonElement(edits) as JsonArray
            array.forEach {
                editList.add(
                    mapOf<String, String>(
                        "oldText" to it.jsonObject["oldText"]!!.jsonPrimitive.content,
                        "newText" to it.jsonObject["newText"]!!.jsonPrimitive.content
                    )
                )
            }
        } else if (edits.startsWith("{") && edits.endsWith("}")) {
            val obj = Json.parseToJsonElement(edits) as JsonObject
            editList.add(
                mapOf<String, String>(
                    "oldText" to obj.jsonObject["oldText"]!!.jsonPrimitive.content,
                    "newText" to obj.jsonObject["newText"]!!.jsonPrimitive.content
                )
            )
        }
    } catch (e: Exception) {
        throw Exception("Invalid edits format: ${e.message}")
    }

    return editList
}

//fun main() {
//    val edits = """[{"oldText":"123","newText":"456"},{"oldText":"1231","newText":"4562"}]"""
//    parseEdits(edits)
//}