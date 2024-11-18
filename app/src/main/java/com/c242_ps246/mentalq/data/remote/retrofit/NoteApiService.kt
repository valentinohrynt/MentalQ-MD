package com.c242_ps246.mentalq.data.remote.retrofit

import com.c242_ps246.mentalq.data.remote.response.DetailNoteResponse
import com.c242_ps246.mentalq.data.remote.response.NoteResponse
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface NoteApiService {
    @GET("notes")
    suspend fun getNotes(
        @Query("user_id") userId: String
    ): NoteResponse

    @FormUrlEncoded
    @POST("notes")
    suspend fun createNote(
        @Field("title") title: String,
        @Field("content") content: String,
        @Field("emotion") emotion: String
    ): DetailNoteResponse

    @FormUrlEncoded
    @PUT("notes")
    suspend fun updateNote(
        @Path("id") id: String,
        @Field("title") title: String,
        @Field("content") content: String,
        @Field("emotion") emotion: String
    ): DetailNoteResponse

    @DELETE("notes")
    suspend fun deleteNote(
        @Path("id") id: String
    ): Response<Void>
}