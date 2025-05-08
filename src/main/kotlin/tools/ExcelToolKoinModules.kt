package tools

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.RegisteredTool
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.CellType
import org.koin.core.qualifier.named
import org.koin.dsl.module
import utils.readExcel
import java.io.File
import kotlin.text.split

private val log = KotlinLogging.logger {}


val excelToolModule = module{

    single<RegisteredTool>(named("getSheets")) {
        RegisteredTool(
            Tool(
                name = "获取excel的sheets列表",
                description = "读取excel文件，获取sheets，按列表返回数据",
                inputSchema = Tool.Input(
                    properties = JsonObject(
                        mapOf(
                            "filePath" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string"),
                                    "description" to JsonPrimitive("excel文件路径")
                                )
                            ),
                        )
                    ),
                    required = listOf("filePath")
                )
            )
        ) { request ->
            val filePath = request.arguments["filePath"]?.jsonPrimitive?.content

            if (filePath == null) {
                return@RegisteredTool CallToolResult(
                    content = listOf(TextContent("The 'filePath' parameter is required."))
                )
            }
            log.info { "filePath: $filePath" }

            val file = File(filePath)
            if (!file.exists()) {
                CallToolResult(content = listOf(TextContent("file not exists: $filePath")))
            }
            val list = mutableListOf<String>()
            readExcel(filePath).sheetIterator()
            XSSFWorkbook(file).sheetIterator().forEach { list.add(it.sheetName) }
            log.info { "sheets: $list" }
            // 返回结果
            CallToolResult(content = listOf(TextContent(list.toString())))
        }
    }


    single<RegisteredTool>(named("readSheet")) {
        RegisteredTool(
            Tool(
                name = "读取sheet",
                description = "读取excel文件，获取特定名称的sheet",
                inputSchema = Tool.Input(
                    properties = JsonObject(
                        mapOf(
                            "filePath" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string"),
                                    "description" to JsonPrimitive("excel文件路径")
                                )
                            ),
                            "sheetName" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string"),
                                    "description" to JsonPrimitive("原始文件名称")
                                )
                            ),
                            "skipTitleRowNum" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string"),
                                    "description" to JsonPrimitive("标题行数，返回内容中跳过这几行")
                                )
                            )
                        )
                    ),
                    required = listOf("filePath","sheetName","skipTitleRows")
                )
            )
        ) { request ->
            val filePath = request.arguments["filePath"]?.jsonPrimitive?.content
            val sheetName = request.arguments["sheetName"]?.jsonPrimitive?.content
            val skipTitleRowNum = request.arguments["skipTitleRowNum"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            if (filePath == null || sheetName == null) {
                return@RegisteredTool CallToolResult(
                    content = listOf(TextContent("The 'filePath' and 'sheetName' parameter is required."))
                )
            }
            log.info { "filePath: $filePath, sheetName: $sheetName" }

            val file = File(filePath)
            if (!file.exists()) {
                CallToolResult(content = listOf(TextContent("file not exists: $filePath")))
            }
            val sheet = XSSFWorkbook(file).getSheet(sheetName)

            val jsonArray = buildList {
                val headerRow = sheet.getRow(skipTitleRowNum-1)
                val headers = (0 until headerRow.lastCellNum).map {
                    headerRow.getCell(it)?.stringCellValue ?: ""
                }

                for (rowIndex in skipTitleRowNum..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIndex) ?: continue
                    val rowObject = JsonObject(buildMap {
                        headers.forEachIndexed { colIndex, header ->
                            val cell = row.getCell(colIndex)
                            val value = when {
                                cell == null -> JsonPrimitive("")
                                cell.cellType == CellType.NUMERIC -> JsonPrimitive(cell.numericCellValue)
                                cell.cellType == CellType.BOOLEAN -> JsonPrimitive(cell.booleanCellValue)
                                else -> JsonPrimitive(cell.stringCellValue)
                            }
                            put(header, value)
                        }
                    })
                    add(rowObject)
                }
            }
            log.info { "sheet content: $jsonArray" }
            // 返回结果
            CallToolResult(content = listOf(TextContent(jsonArray.toString())))
        }
    }
}