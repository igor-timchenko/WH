package ru.contlog.mobile.helper.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Division(
    @SerialName("division_uid") val uid: String,
    @SerialName("division_name") val name: String,
    val address: String,
    @SerialName("division_code") val divisionCode: Int,
) : java.io.Serializable
