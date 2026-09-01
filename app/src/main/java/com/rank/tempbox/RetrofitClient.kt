package com.rank.tempbox

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://api.mail.tm/"
    private const val TAG = "TempBox-API"

    private val _retryAfter = MutableLiveData<Int>()
    val retryAfter: LiveData<Int> = _retryAfter

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d(TAG, message)
    }.apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
    }

    private val headerInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Content-Type", "application/json")
            .build()
        chain.proceed(request)
    }

    private val retryInterceptor = Interceptor { chain ->
        var request = chain.request()
        var response = chain.proceed(request)
        var tryCount = 0

        while (!response.isSuccessful && response.code == 429 && tryCount < 2) {
            tryCount++
            val retryAfterSeconds = response.header("Retry-After")?.toIntOrNull() ?: 3
            _retryAfter.postValue(retryAfterSeconds)
            Log.w(TAG, "Rate limited (429), retry #$tryCount — Retry-After: ${retryAfterSeconds}s")
            response.close()
            Thread.sleep(retryAfterSeconds * 1000L)
            response = chain.proceed(request)
        }
        if (response.code != 429) {
            _retryAfter.postValue(0)
        }
        response
    }

    private fun buildOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(headerInterceptor)
            .addInterceptor(retryInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    // Separate clients allow per-client interceptors in future (e.g. auth, caching)
    val unauthenticatedClient: OkHttpClient by lazy { buildOkHttpClient() }
    val authenticatedClient: OkHttpClient by lazy { buildOkHttpClient() }

    val unauthenticatedApi: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(unauthenticatedClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    val authenticatedApi: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(authenticatedClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
