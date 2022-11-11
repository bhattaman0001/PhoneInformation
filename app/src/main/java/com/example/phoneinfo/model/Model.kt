package com.example.phoneinfo.model

import com.google.gson.annotations.SerializedName

data class Model(
    @SerializedName("device")
    var deviceID: String?,

    @SerializedName("internet-connected")
    var internetStatus: String?,

    @SerializedName("charging")
    var batteryStatus: String?,

    @SerializedName("battery")
    var batteryPercentage: String?,

    @SerializedName("location")
    var location: String?
)