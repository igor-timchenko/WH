package ru.contlog.mobile.helper.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiAuthData(
    @SerialName("fl_uid") val uid: String,
    @SerialName("api_key") val apiKey: String,
)