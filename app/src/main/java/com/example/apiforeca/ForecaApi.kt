package com.example.apiforeca


import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ForecaApi {

    @POST("/authorize/token?expire_hours=-1") // для использования аннотации @POST @Body  подключил библиотеку Retrofit и конвертер Json в build.gradle.kts(app) implementation("com.google.code.gson:gson:2.10.1") implementation("com.squareup.retrofit2:retrofit:2.9.0")  implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    fun authenticate(@Body request: ForecaAuthRequest): Call<ForecaAuthResponse> // запрос на аутентификацию

    @GET("/api/v1/location/search/{query}")
    fun getLocation(@Header("Authorization") token: String, @Path("query") query: String): Call<LocationsResponse> // запрос на название города

    @GET("/api/v1/current/{location}")
    fun getForecast(@Header("Authorization") token: String, @Path("location") locationId: Int): Call<ForecastResponce> // запрос на прогноз погоды
}