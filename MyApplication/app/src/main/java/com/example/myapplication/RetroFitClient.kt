package com.example.myapplication

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetroFitClient {
    private const val BASE_URL = "http://quizquestions.dam.inspedralbes.cat:20133/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        retrofit.create(ApiService::class.java)

    }
}

