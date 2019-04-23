package com.example

import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.WebSocketSession

const val SERVER_ID = "server"

data class User(
    val id: String,
    var name: String,
    var socket: WebSocketSession
)

enum class CommandTypeFromUsers {
    CHAT_MESSAGE,
    JOIN_REQUEST
}

enum class CommandTypeToUsers {
    CHAT_MESSAGE,
    USER_JOINED
}

data class CommandToUsers(
    val senderId: String,
    val type: CommandTypeToUsers,
    val text: String
) {
    fun toTextFrame(): Frame.Text {
        val text = "$senderId $type $text"
        return Frame.Text(text)
    }
}

data class CommandFromUser(
    val type: CommandTypeFromUsers,
    val text: String
) {
    companion object {
        fun parseFromString(string: String): CommandFromUser {
            val splitBySpaces = string.split(' ')
            assert(splitBySpaces.size >= 2)

            val commandType = CommandTypeFromUsers.valueOf(splitBySpaces[0])
            val commandText = splitBySpaces
                .takeLast(splitBySpaces.size - 1)
                .joinToString(" ")

            return CommandFromUser(commandType, commandText)
        }
    }
}

class ChatRoom {
    private val users: MutableList<User> = mutableListOf()
    private val latestCommands: MutableList<CommandToUsers> = mutableListOf()

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

        val command = CommandToUsers(
            SERVER_ID,
            CommandTypeToUsers.USER_JOINED,
            "${user.id} ${user.name}"
        )

        broadcast(command)
    }

    private suspend fun broadcast(command: CommandToUsers) {
        users.forEach {
            it.socket.send(command.toTextFrame())
        }
    }

    suspend fun receiveCommand(string: String, sessionId: String, socket: WebSocketSession) {
        val command = CommandFromUser.parseFromString(string)

        when (command.type) {
            CommandTypeFromUsers.JOIN_REQUEST -> {
                val userName = command.text
                join(sessionId, userName, socket)
            }

            CommandTypeFromUsers.CHAT_MESSAGE -> {
                val nickname = users.find { it.id == sessionId }?.name ?: return
                val text = "<$nickname> ${command.text}"
                broadcast(CommandToUsers(sessionId, CommandTypeToUsers.CHAT_MESSAGE, text))
            }
        }
    }
}