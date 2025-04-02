package com.fushi

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp


fun main() = `run mcp server`()




// Main function to run the MCP server
fun `run mcp server`() {
    // Create the MCP Server instance with a basic implementation
    val server = Server(
        Implementation(
            name = "private mcp toolkit server", // Tool name is "weather"
            version = "1.0.0" // Version of the implementation
        ),
        ServerOptions(
            capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))
        )
    )

    // Register a tool to fetch weather alerts by state
    tool4ReadFile(server)
    tool4AddTagsForMarkDownFile(server)
    tool4GetFilesInPath(server)

    embeddedServer(Netty, 8080) {
        mcp {
            server
        }
    }.start(wait = true)
}


