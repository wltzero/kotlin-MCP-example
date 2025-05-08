import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.RegisteredTool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import org.koin.ktor.ext.getKoin
import org.koin.ktor.plugin.koin
import tools.excelToolModule
import tools.simpleToolModule


/*TODO)) 添加全局操作系统事务*/

fun main() {
    embeddedServer(Netty, 8080) {
        mainModule()
    }.start(wait = true)
}


fun Application.mainModule() {
    koin {
        modules(simpleToolModule)
        modules(excelToolModule)
    }

    /*register the mcp server*/
    mcp {
        val server = Server(
            Implementation(
                name = "private mcp toolkit server", // Tool name is "weather"
                version = "1.0.0" // Version of the implementation
            ),
            ServerOptions(
                capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))
            )
        )
        /*get all tools by koin*/
        val tools = getKoin().getAll<RegisteredTool>()

        server.addTools(tools)
        server
    }

}

