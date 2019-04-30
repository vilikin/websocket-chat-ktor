package com.example

import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.WebSocketSession

data class User(
    val id: String,
    var name: String,
    var socket: WebSocketSession
)

class ChatRoom {
    private val users: MutableList<User> = mutableListOf()
    private val latestBroadcasts: MutableList<Broadcast> = mutableListOf()

    private suspend fun join(id: String, name: String, socket: WebSocketSession) {
        val user: User = if (users.any { it.id == id }) {
            val existingUser = users.find { it.id == id }!!
            existingUser.name = name
            existingUser.socket.close(Throwable("New socket opened"))
            existingUser.socket = socket
            existingUser
        } else {
            val newUser = User(id, name, socket)
            users.add(newUser)
            newUser
        }

        val userJoinedBroadcast = UserJoinedBroadcast(user.id, user.name)

        broadcast(userJoinedBroadcast)
    }

    private suspend fun broadcast(broadcast: Broadcast) {
        val frame = Frame.Text(broadcast.toString())
        users.forEach {
            it.socket.send(frame)
        }

        latestBroadcasts.add(broadcast)
    }

    suspend fun receiveMessage(string: String, sessionId: String, socket: WebSocketSession) {
        val message = MessageFromUser.parseFromString(string)

        when (message) {
            is JoinRequestFromUser -> {
                join(sessionId, message.userName, socket)
            }

            is ChatMessageFromUser -> {
                val userName = users.find { it.id == sessionId }?.name ?: return
                val chatMessageBroadcast = ChatMessageBroadcast(
                    userId = sessionId,
                    userName = userName,
                    message = message.message
                )

                broadcast(chatMessageBroadcast)
            }
        }
    }
}