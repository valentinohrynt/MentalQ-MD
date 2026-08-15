package com.c242_ps246.mentalq.data.remote.response

import com.google.gson.annotations.SerializedName

data class PsychologistResponse(
    @field:SerializedName("users")
    val listPsychologists: List<PsychologistItem>? = null,

    @field:SerializedName("error")
    val error: Boolean? = null,

    @field:SerializedName("message")
    val message: String? = null
)

data class SinglePsychologistResponse(
    @field:SerializedName("psychologist")
    val psychologist: PsychologistItem? = null,

    @field:SerializedName("error")
    val error: Boolean? = null,

    @field:SerializedName("message")
    val message: String? = null
)


data class PsychologistItem(

    @field:SerializedName("psychologist_id")
    val id: String,

    @field:SerializedName("prefix_title")
    val prefixTitle: String,

    @field:SerializedName("suffix_title")
    val suffixTitle: String,

    @field:SerializedName("isVerified")
    val isVerified: Boolean,

    @field:SerializedName("certificate")
    val certificate: String,

    @field:SerializedName("price")
    val price: Int,

    @field:SerializedName("user_id")
    val userId: String,

    @field:SerializedName("role")
    val role: String,

    @field:SerializedName("users")
    val users: User
)

data class User(
    @field:SerializedName("name")
    val name: String,

    @field:SerializedName("profile_photo_url")
    val profilePhotoUrl: String? = null
)
