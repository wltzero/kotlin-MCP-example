import io.ktor.server.application.Application
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.RegisteredTool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import org.koin.core.qualifier.named
import org.koin.ktor.ext.getKoin
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.koin
import tools.toolModule


fun main() {
    embeddedServer(Netty, 8080) {
        mainModule()
    }.start(wait = true)
}


fun Application.mainModule() {
    koin{
        modules(toolModule)
    }

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

    /*register the mcp server*/
    mcp {
        server
    }

}

