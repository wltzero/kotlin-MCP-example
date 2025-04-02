package com.fushi

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import java.io.File


private val log = KotlinLogging.logger {}

fun tool4AddTagsForMarkDownFile(server: Server) {
    server.addTool(
        name = "markdown文件标签添加",
        description = "这是一个为markdown文件添加标签的工具，默认情况下在找到第一个H1大标题后在下方新增一系列标签",
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "filePath" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("markdown文件路径")
                        )
                    ),
                    "tags" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("比较简短的文章分类标签，以英文逗号分隔")
                        )
                    )
                )
            ),
            required = listOf("filePath", "tags")
        )
    ) { request ->
        val filePath = request.arguments["filePath"]?.jsonPrimitive?.content
        val tags = request.arguments["tags"]?.jsonPrimitive?.content

        if (filePath == null || tags == null) {
            return@addTool CallToolResult(
                content = listOf(TextContent("The 'filePath' and 'tags' parameter is required."))
            )
        }
        log.info { "filePath: $filePath, tags: $tags" }

        val tag = tags.split(",").joinToString(" ") { "#$it" }
        val file = File(filePath)
        if (!file.exists()) {
            return@addTool CallToolResult(
                content = listOf(TextContent("file not exists: $filePath"))
            )
        }
        val lines = file.readLines()
        val modifiedLines = mutableListOf<String>()
        var h1Found = false
        var tagAdded = false

        // 检验是否已经存在标签
        for (line in lines) {
            if (line.matches(Regex("^#\\w+( #\\w+)*$"))){
                return@addTool CallToolResult(
                    content = listOf(TextContent("tags already exists: $line"))
                )
            }
        }

        for (line in lines) {
            modifiedLines.add(line)
            // 检查是否是 H1 标题且还未找到第一个 H1
            if (!h1Found && line.startsWith("# ")) {
                h1Found = true

                // 在 H1 标题后添加标签
                modifiedLines.add(tag)
                tagAdded = true
            }
        }

        // 如果没有找到 H1 标题，选择在文件开头添加
        if (!tagAdded) {
            log.warn { "警告: 文件中未找到 H1 标题" }
            modifiedLines.add(0, tag)
        }

        // 写回文件
        file.writeText(modifiedLines.joinToString("\n"))

        // 返回结果
        CallToolResult(content = listOf(TextContent("successfully add tags:[$tags] for markdown file $filePath")))
    }
}


fun tool4ReadFile(server: Server) {
    server.addTool(
        name = "文件内容读取",
        description = "这是一个用于读取文件内容的工具",
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "filePath" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("文件路径")
                        )
                    )
                )
            ),
            required = listOf("filePath")
        )
    ) { request ->
        val filePath = request.arguments["filePath"]?.jsonPrimitive?.content
        if (filePath == null) {
            return@addTool CallToolResult(
                content = listOf(TextContent("The 'filePath' parameter is required."))
            )
        }
        val file = File(filePath)
        if (!file.exists()) {
            return@addTool CallToolResult(
                content = listOf(TextContent("file not exists: $filePath"))
            )
        }
        val content = file.readText()
        return@addTool CallToolResult(
            content = listOf(TextContent(content))
        )
    }
}


fun tool4GetFilesInPath(server: Server){
    server.addTool(
        name = "获取指定路径下的文件",
        description = "这是一个获取指定路径下的文件列表的工具",
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "path" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("指定路径")
                        )
                    )
                )
            ),
            required = listOf("path")
        )
    ){  request->
        val path = request.arguments["path"]?.jsonPrimitive?.content
        if(path == null){
            return@addTool CallToolResult(
                content = listOf(TextContent("The 'path' parameter is required."))
            )
        }
        val file = File(path)
        if(!file.exists()){
            return@addTool CallToolResult(
                content = listOf(TextContent("file not exists: $path"))
            )
        }
        if(!file.isDirectory){
            return@addTool CallToolResult(
                content = listOf(TextContent("path is not a directory: $path"))
            )
        }
        val files = file.listFiles()?.map { it.name }
        log.info { "path: $path, files: $files" }

        CallToolResult(content = listOf(TextContent("$files")))
    }
}