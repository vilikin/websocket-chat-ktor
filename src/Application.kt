package com.example

import io.ktor.application.*
import io.ktor.features.CallLogging
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.*
import io.ktor.websocket.*
import io.ktor.sessions.*
import io.ktor.util.generateNonce
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.mapNotNull

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

data class ChatSession(val id: String)

val chatRoom = ChatRoom()

fun Application.module() {
    install(WebSockets)
    install(CallLogging)
    install(Sessions) {
        cookie<ChatSession>("SESSION")
    }

    intercept(ApplicationCallPipeline.Features) {
        if (call.sessions.get<ChatSession>() == null) {
            call.sessions.set(ChatSession(generateNonce()))
        }
    }

    routing {
        webSocket("/") {
            val session = call.sessions.get<ChatSession>()!!
            incoming.mapNotNull { it as? Frame.Text }.consumeEach { frame ->
                val text = frame.readText()
                chatRoom.receiveMessage(text, session.id, this)
            }
        }
    }
}

