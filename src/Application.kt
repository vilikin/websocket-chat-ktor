package com.example

import io.ktor.application.*
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.websocket.*
import io.ktor.sessions.*
import io.ktor.util.generateNonce
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.map
import kotlinx.coroutines.channels.mapNotNull
import java.time.Duration

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

data class ChatSession(val id: String)

val chatRoom = ChatRoom()

fun Application.module() {
    install(WebSockets)
    install(CallLogging)
    install(ContentNegotiation) {
        gson {
            // Configure Gson here
        }
    }
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

            try {
                incoming.consumeEach { frame ->
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            chatRoom.receiveMessage(text, session.id, this)
                        }

                        is Frame.Close -> {
                            chatRoom.leave(session.id)
                        }
                    }
                }
            } finally {
                chatRoom.leave(session.id)
            }
        }

        get("/users") {
            call.respond(chatRoom.getActiveUsers())
        }
    }
}

