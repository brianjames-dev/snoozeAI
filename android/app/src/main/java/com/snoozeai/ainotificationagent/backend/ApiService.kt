package com.snoozeai.ainotificationagent.backend

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @GET("/health")
    suspend fun health(): Map<String, Boolean>

    @POST("/summarize")
    suspend fun summarize(@Body body: SummarizeRequest): SummarizeResponse

    @POST("/classify")
    suspend fun classify(@Body body: ClassifyRequest): ClassifyResponse

    @POST("/store")
    suspend fun store(@Body body: StoreRequest): StoreResponse

    @GET("/items")
    suspend fun items(@Query("limit") limit: Int = 50): ItemsResponse
}

object ApiClient {
    fun create(baseUrl: String): ApiService {
        val logger = okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
        }

        val okHttp = okhttp3.OkHttpClient.Builder()
            .addInterceptor(logger)
            .build()

        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttp)
            .addConverterFactory(
                retrofit2.converter.moshi.MoshiConverterFactory.create()
            )
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
