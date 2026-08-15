package com.c242_ps246.mentalq.data.remote.retrofit

import com.c242_ps246.mentalq.data.remote.response.DeleteNoteResponse
import com.c242_ps246.mentalq.data.remote.response.DetailNoteResponse
import com.c242_ps246.mentalq.data.remote.response.NoteResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface NoteApiService {
    @GET("notes")
    suspend fun getNotes(): NoteResponse

    @FormUrlEncoded
    @POST("notes")
    suspend fun createNote(
        @Field("title") title: String,
        @Field("content") content: String,
        @Field("emotion") emotion: String
    ): DetailNoteResponse

    @FormUrlEncoded
    @PUT("notes/{id}")
    suspend fun updateNote(
        @Path("id") id: String,
        @Field("title") title: String,
        @Field("content") content: String,
        @Field("emotion") emotion: String
    ): DetailNoteResponse

    @PUT("notes/delete/{id}")
    suspend fun deleteNote(
        @Path("id") id: String
    ): DeleteNoteResponse
}
