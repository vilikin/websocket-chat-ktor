package com.example

import com.google.gson.Gson
import java.time.LocalDateTime

sealed class Broadcast(
    val type: Type,
    val timestamp: String = LocalDateTime.now().toString()
) {
    enum class Type {
        CHAT_MESSAGE,
        USER_JOINED,
        USER_LEFT
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }
}

class ChatMessageBroadcast(
    val userId: String,
    val userName: String,
    val message: String
): Broadcast(Broadcast.Type.CHAT_MESSAGE)

class UserJoinedBroadcast(
    val userId: String,
    val userName: String
): Broadcast(Broadcast.Type.USER_JOINED)

class UserLeftBroadcast(
    val userId: String,
    val userName: String
): Broadcast(Broadcast.Type.USER_LEFT)
