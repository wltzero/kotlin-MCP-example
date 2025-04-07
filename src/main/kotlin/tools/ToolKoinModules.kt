package tools

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.RegisteredTool
import kotlinx.io.IOException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import org.jsoup.Jsoup
import org.koin.core.qualifier.named
import org.koin.dsl.module
import utils.cleanHtmlThoroughly
import utils.generateDiff
import utils.normalizeLineEnding
import utils.parseEdits
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.regex.PatternSyntaxException

private val log = KotlinLogging.logger {}


val toolModule = module {
    single<RegisteredTool>(named("addTagsForMarkdown")) {
        RegisteredTool(
            Tool(
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
            )
        ) { request ->
            val filePath = request.arguments["filePath"]?.jsonPrimitive?.content
            val tags = request.arguments["tags"]?.jsonPrimitive?.content
            if (filePath == null || tags == null) {
                return@RegisteredTool CallToolResult(
                    content = listOf(TextContent("The 'filePath' and 'tags' parameter is required."))
                )
            }
            log.info { "filePath: $filePath, tags: $tags" }

            val tag = tags.split(",").joinToString(" ") { "#$it" }
            val file = File(filePath)
            if (!file.exists()) {
                CallToolResult(content = listOf(TextContent("file not exists: $filePath")))
            }
            val lines = file.readLines()
            val modifiedLines = mutableListOf<String>()
            var h1Found = false
            var tagAdded = false

            // 检验是否已经存在标签
            for (line in lines) {
                if (line.matches(Regex("^#\\w+( #\\w+)*$"))) {
                    CallToolResult(content = listOf(TextContent("tags already exists: $line")))
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

    single<RegisteredTool>(named("readFile")) {
        RegisteredTool(
            Tool(
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
            )
        ) { request ->
            val filePath = requireNotNull(request.arguments["filePath"]?.jsonPrimitive?.content) {
                CallToolResult(content = listOf(TextContent("The 'filePath' parameter is required.")))
            }
            val file = File(filePath)
            if (!file.exists()) {
                CallToolResult(content = listOf(TextContent("file not exists: $filePath")))
            }
            val content = file.readText()
            CallToolResult(content = listOf(TextContent(content)))
        }
    }

    single<RegisteredTool>(named("getFilesInPath")) {
        RegisteredTool(
            Tool(
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
            )
        ) { request ->
            val path = requireNotNull(request.arguments["path"]?.jsonPrimitive?.content) {
                CallToolResult(content = listOf(TextContent("The 'path' parameter is required.")))
            }
            val file = File(path)
            if (!file.exists()) {
                CallToolResult(content = listOf(TextContent("文件不存在: $path")))
            }
            if (!file.isDirectory) {
                CallToolResult(content = listOf(TextContent("路径不是一个目录: $path")))
            }
            val files = file.listFiles()?.map { it.name }
            log.info { "path: $path, files: $files" }
            CallToolResult(content = listOf(TextContent("$files")))
        }
    }

    single<RegisteredTool>(named("createDirectory")) {
        RegisteredTool(
            Tool(
                name = "创建目录",
                description = "创建新目录或者确保目录存在，可以一次性创建多个目录，如果目录已经存在，则默认返回成功，适合项目设置目录结构或者确保所需的路径存在",
                inputSchema = Tool.Input(
                    properties = JsonObject(
                        mapOf(
                            "directories" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string"),
                                    "description" to JsonPrimitive("目录路径列表，用英文逗号分隔")
                                )
                            )
                        )
                    ),
                    required = listOf("directories")
                )
            )
        ) { request ->
            val directories = requireNotNull(request.arguments["directories"]?.jsonPrimitive?.content?.split(",")) {
                return@RegisteredTool CallToolResult(
                    content = listOf(TextContent("The 'directories' parameter is required."))
                )
            }
            try {
                directories.forEach {
                    val path = Paths.get(it)
                    if (!Files.exists(path)) {
                        Files.createDirectory(path)
                        log.info { "目录创建成功: $it" }
                    } else {
                        log.info { "目录已存在: $it" }
                    }
                }
                CallToolResult(content = listOf(TextContent("创建目录成功")))
            } catch (e: Exception) {
                log.error { "创建目录失败: ${e.message}" }
                CallToolResult(content = listOf(TextContent("创建目录失败: ${e.message}")))
            }
        }
    }

    single<RegisteredTool>(named("editFile")) {
        RegisteredTool(
            Tool(
                name = "编辑文件",
                description = """对文本文件进行基于行的编辑，每次编辑都会用新的内容替换确切的行序列。
                                 返回所作更改的类似git diff格式的输出。
                                 
                                 输入的修改参数允许有两种格式：
                                 1. JSON对象：{"oldText":"要替换的文本","newText":"替换文本"}
                                 2. JSON数组：[{"oldText":"要替换的文本","newText":"替换文本"},{"oldText":"要替换的文本2","newText":"替换文本2"}]
                              """.trimIndent(),
                inputSchema = Tool.Input(
                    properties = JsonObject(
                        mapOf(
                            "path" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string"),
                                    "description" to JsonPrimitive("文件路径")
                                )
                            ),
                            "edits" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string"),
                                    "description" to JsonPrimitive("修改参数，可以是JSON对象、JSON数组")
                                )
                            ),
                            "dryRun" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("boolean"),
                                    "description" to JsonPrimitive("是否为模拟运行，默认为false，如果为true，则只显示最终的diff而不会修改文件")
                                )
                            )
                        )
                    ),
                    required = listOf("path", "edits", "dryRun")
                )
            )
        ) { request ->
            val path = request.arguments["path"]?.jsonPrimitive?.content
            val edits = request.arguments["edits"]?.jsonPrimitive?.content
            val dryRun = request.arguments["dryRun"]?.jsonPrimitive?.booleanOrNull ?: false
            if (path.isNullOrBlank() || edits.isNullOrBlank()) {
                return@RegisteredTool CallToolResult(
                    content = listOf(TextContent("The 'path' and 'edits' parameters are required."))
                )
            }
            log.debug { "path: $path, edits: $edits, dryRun: $dryRun" }
            log.info { "当前正在以${if (dryRun) "模拟运行" else "正式运行"}模式运行" }
            val filePath = Paths.get(path)
            if (!Files.exists(filePath)) {
                log.warn { "文件不存在: $path" }
                CallToolResult(content = listOf(TextContent("文件不存在: $path")))
            }
            if (!Files.isRegularFile(filePath)) {
                log.warn { "输入的路径不是文件: $path" }
                CallToolResult(content = listOf(TextContent("输入的路径不是文件: $path")))
            }

            val originContent = Files.readString(filePath)
            var newContent = originContent

            log.debug { "接收到的edits参数: $edits" }
            val editList = requireNotNull(parseEdits(edits)) {
                CallToolResult(content = listOf(TextContent("没有需要修改的内容")))
            }

            val appliedEdits = mutableListOf<String>()
            editList.forEach {
                val oldText = it["oldText"]
                val newText = it["newText"]
                if (oldText.isNullOrBlank() || newText.isNullOrBlank()) {
                    log.warn { "oldText和newText不能为空" }
                    return@RegisteredTool CallToolResult(content = listOf(TextContent("oldText和newText不能为空")))
                }
                val normalizedContent = normalizeLineEnding(newContent)
                val normalizedOldText = normalizeLineEnding(oldText)
                if (!normalizedContent.contains(normalizedOldText)) {
                    log.info { "文件内容对于该替换不存在要替换的内容: $oldText, $newText" }
                    return@forEach
                }
                newContent = normalizedContent.replace(normalizedOldText, newText)
                appliedEdits.add(oldText)
            }
            val diff = generateDiff(path, originContent, newContent)
            log.info { diff }
            if (!dryRun && originContent != newContent) {
                Files.writeString(filePath, newContent)
            }
            CallToolResult(
                content = listOf(
                    TextContent(
                        "文件内容修改成功，当前运行状态为 ${if (dryRun) "模拟运行" else "正式运行"}，修改了${appliedEdits.size}处内容，应用的修改项包括${
                            appliedEdits.joinToString(
                                ","
                            )
                        }，详细修改如下：\n$diff"
                    )
                )
            )
        }
    }

    single<RegisteredTool>(named("fetchWebPageContent")) {
        RegisteredTool(
            Tool(
                name = "获取网页内容",
                description = """获取多个指定URL的网页内容，并返回一个JSON ARRAY对象。""".trimIndent(),
                inputSchema = Tool.Input(
                    properties = JsonObject(
                        mapOf(
                            "urls" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string"),
                                    "description" to JsonPrimitive("要获取的URL，用英文逗号分隔")
                                )
                            )
                        )
                    ),
                    required = listOf("url")
                )
            )
        ) { request ->
            var urls = request.arguments["urls"]?.jsonPrimitive?.content
            if (urls.isNullOrBlank()) {
                return@RegisteredTool CallToolResult(
                    content = listOf(TextContent("The 'urls' parameter is required."))
                )
            }
            try {
                val docList = urls.split(",")
                    .map { it.trim() }
                    .map {
                        val doc = Jsoup.connect(it).timeout(50000).get()
                        JsonObject(
                            mapOf(
                                "url" to JsonPrimitive(it),
                                "title" to JsonPrimitive(doc.title()),
                                "content" to JsonPrimitive(cleanHtmlThoroughly(doc.body().text()))
                            )
                        )
                    }.toList()
                log.info { docList.toString() }
                CallToolResult(content = listOf(TextContent(docList.toString())))
            } catch (e: Exception) {
                log.error(e) { "获取网页内容失败，${e.message}" }
                CallToolResult(content = listOf(TextContent("获取网页内容失败，请检查URL是否正确，错误信息为：${e.message}")))
            }
        }
    }

    single<RegisteredTool>(named("searchFiles")) {
        RegisteredTool(
            Tool(
                name = "搜索文件",
                description = """递归搜索与模式匹配的文件和目录。搜索
                                 起始路径。搜索不区分大小写，并匹配部分名称。返回所有匹配项的完整路径
                                 项目。非常适合在不知道文件的确切位置时查找文件。""".trimIndent(),
                inputSchema = Tool.Input(
                    properties = JsonObject(
                        mapOf(
                            "path" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string"),
                                    "description" to JsonPrimitive("搜索的起始路径")
                                )
                            ),
                            "pattern" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string"),
                                    "description" to JsonPrimitive("搜索用pattern")
                                )
                            )
                        )
                    ),
                    required = listOf("path", "pattern")
                )
            )
        ) { request ->
            val path = request.arguments["path"]?.jsonPrimitive?.content
            val pattern = request.arguments["pattern"]?.jsonPrimitive?.content
            if (path.isNullOrBlank() || pattern.isNullOrBlank()) {
                return@RegisteredTool CallToolResult(
                    content = listOf(TextContent("The 'path' and 'pattern' parameters are required."))
                )
            }
            try {

                val filePath = Paths.get(path)
                if (!Files.exists(filePath)) {
                    log.error { "搜索路径不存在" }
                    return@RegisteredTool CallToolResult(listOf(TextContent("搜索路径不存在")))
                }
                /*init pattern matcher*/
                var normalizedPattern = pattern
                if (!pattern.startsWith("glob:")) {
                    normalizedPattern = "glob:*$normalizedPattern*"
                }
                val matcher = FileSystems.getDefault().getPathMatcher(normalizedPattern)

                val matchingPaths = mutableListOf<String>()
                Files.walkFileTree(filePath, object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        val relativizePath = filePath.relativize(file)
                        if (matcher.matches(relativizePath) ||
                            matcher.matches(file.fileName)
                        ) {
                            matchingPaths.add(file.toString())
                        }
                        return FileVisitResult.CONTINUE
                    }

                    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                        /* exclude hidden directories (starting with '.')*/
                        if (dir.nameCount > 0 &&
                            ((dir.getName(dir.nameCount - 1).startsWith(".")) ||
                                    dir.getName(dir.nameCount - 1).startsWith("target"))
                        ) {
                            return FileVisitResult.SKIP_SUBTREE
                        }
                        val relativizePath = filePath.relativize(dir)

                        if (matcher.matches(dir.fileName) || matcher.matches(relativizePath)) {
                            matchingPaths.add(dir.toString())
                        }
                        return FileVisitResult.CONTINUE
                    }
                })
                log.info { "搜索到文件：$matchingPaths" }
                CallToolResult(listOf(TextContent(matchingPaths.toString())))
            }catch (e: PatternSyntaxException){
                log.error(e) { "搜索文件失败，pattern异常，${e.message}" }
                CallToolResult(listOf(TextContent("搜索文件失败，请检查pattern是否正确，错误信息为：${e.message}")))
            }catch (e: Exception) {
                log.error(e) { "搜索文件失败，${e.message}" }
                CallToolResult(listOf(TextContent("搜索文件失败，未知错误，错误信息为：${e.message}")))
            }
        }
    }

    single<RegisteredTool>(named("writeFile")) {
        RegisteredTool(
            Tool(
                name = "写入文件",
                description = """创建新文件或用新内容完全覆盖现有文件。须谨慎使用，因为它会在不发出警告的情况下覆盖现有文件。
                                 使用适当的编码处理文本内容。""".trimIndent(),
                inputSchema = Tool.Input(
                    properties = JsonObject(
                        mapOf(
                            "path" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string"),
                                    "description" to JsonPrimitive("写入文件的路径")
                                )
                            ),
                            "content" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string"),
                                    "description" to JsonPrimitive("写入文件的内容")
                                )
                            )
                        )
                    )
                )
            )
        ){request->
            val path = request.arguments["path"]?.jsonPrimitive?.content
            val content = request.arguments["content"]?.jsonPrimitive?.content
            if (path.isNullOrBlank() || content.isNullOrBlank()) {
                return@RegisteredTool CallToolResult(
                    content = listOf(TextContent("The 'path' and 'content' parameters are required."))
                )
            }
            try {
                val filePath = Paths.get(path)
                /*make sure the parent path exist, create parent directories if they don't exist*/
                val parent = filePath.parent
                if (parent != null && !Files.exists(parent)) {
                    log.info("创建目录：$parent")
                    Files.createDirectories(parent)
                }
                /*write content*/
                val exists = Files.exists(filePath)
                if (exists) {
                    /*备份文件*/
                    filePath.toFile().copyTo(File("$filePath.bak"), true)
                    log.info("覆盖文件：$filePath, 已备份到${filePath}.bak")
                } else {
                    log.info("创建文件：$filePath")
                }
                Files.writeString(filePath, content, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                CallToolResult(listOf(TextContent("文件写入成功, 文件路径为：$path")))
            } catch (e: Exception) {
                log.error(e) { "写入文件失败，${e.message}" }
                CallToolResult(listOf(TextContent("写入文件失败，未知错误，错误信息为：${e.message}")))
            }
        }
    }
}