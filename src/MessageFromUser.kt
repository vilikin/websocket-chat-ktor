package com.example

import com.google.gson.typeadapters.RuntimeTypeAdapterFactory
import com.google.gson.GsonBuilder

sealed class MessageFromUser {
    enum class Type {
        CHAT_MESSAGE,
        JOIN_REQUEST
    }

    companion object {
        fun parseFromString(string: String): MessageFromUser {
            val runtimeTypeAdapterFactory = RuntimeTypeAdapterFactory
                .of(MessageFromUser::class.java, "type")
                .registerSubtype(JoinRequestFromUser::class.java, MessageFromUser.Type.JOIN_REQUEST.name)
                .registerSubtype(ChatMessageFromUser::class.java, MessageFromUser.Type.CHAT_MESSAGE.name)

            return GsonBuilder()
                .registerTypeAdapterFactory(runtimeTypeAdapterFactory)
                .create()
                .fromJson(string, MessageFromUser::class.java)
        }
    }
}

class JoinRequestFromUser(
    val userName: String
) : MessageFromUser()

class ChatMessageFromUser(
    val message: String
) : MessageFromUser()