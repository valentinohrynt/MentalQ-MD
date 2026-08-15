package com.c242_ps246.mentalq.data.remote.response

import com.google.gson.annotations.SerializedName

data class ChatRoomItem(
    @field:SerializedName("id")
    val id: String,

    @field:SerializedName("user_id")
    val userId: String,

    @field:SerializedName("user_name")
    val userName: String,

    @field:SerializedName("user_profile")
    val userProfile: String? = null,

    @field:SerializedName("psychologist_id")
    val psychologistId: String,

    @field:SerializedName("psychologist_prefix")
    val psychologistPrefix: String? = null,

    @field:SerializedName("psychologist_suffix")
    val psychologistSuffix: String? = null,

    @field:SerializedName("psychologist_name")
    val psychologistName: String,

    @field:SerializedName("psychologist_profile")
    val psychologistProfile: String? = null,

    @field:SerializedName("last_message_sender_id")
    val lastMessageSenderId: String? = null,

    @field:SerializedName("last_message")
    val lastMessage: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String = "",

    @field:SerializedName("created_at")
    val createdAt: String,

    @field:SerializedName("deleted_at")
    val deletedAt: String? = null
)

data class ChatMessageItem(
    @field:SerializedName("id")
    val id: String = "",

    @field:SerializedName("chat_room_id")
    val chatRoomId: String = "",

    @field:SerializedName("sender_id")
    val senderId: String = "",

    @field:SerializedName("message")
    val content: String = "",

    @field:SerializedName("createdAt")
    val createdAt: String? = "",
)
