package com.example.phoneinfo.api

import com.example.phoneinfo.model.Model
import com.example.phoneinfo.model.RequestResponseModel
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiInterface {
    @Headers("Content-Type: application/json")
    @POST("/api/status")
    fun sendData(
        @Body model: Model
    ): Call<RequestResponseModel>
}