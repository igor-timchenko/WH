package ru.contlog.mobile.helper.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    @SerialName("fl_uid") val uid: String,
    @SerialName("fl_name") val name: String,
    val position: String,
    val photo: String,
)
