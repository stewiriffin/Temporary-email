package com.rank.tempbox

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("domains")
    suspend fun getDomains(): DomainResponse

    @Headers("Content-Type: application/json")
    @POST("accounts")
    suspend fun createAccount(@Body request: AccountRequest): JsonObject

    @Headers("Content-Type: application/json")
    @POST("token")
    suspend fun getToken(@Body request: TokenRequest): TokenResponse

    @GET("messages")
    suspend fun getMessages(
        @Header("Authorization") bearerToken: String,
        @Query("page") page: Int = 1,
        @Query("itemsPerPage") itemsPerPage: Int = 20
    ): MessagesResponse

    @GET("messages/{id}")
    suspend fun getMessage(
        @Header("Authorization") bearerToken: String,
        @Path("id") id: String
    ): MessageDetail

    @DELETE("messages/{id}")
    suspend fun deleteMessage(
        @Header("Authorization") bearerToken: String,
        @Path("id") id: String
    ): Response<Void>
}
