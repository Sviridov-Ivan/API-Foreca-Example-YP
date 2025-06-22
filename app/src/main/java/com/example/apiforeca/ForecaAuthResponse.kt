package com.example.apiforeca

import com.google.gson.annotations.SerializedName

class ForecaAuthResponse(@SerializedName("access_token") val token: String) { // для использования аннотации @SerializedName подключил библиотеку Gson в build.gradle.kts(app) implementation("com.google.code.gson:gson:2.10.1")
}