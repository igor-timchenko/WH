package ru.contlog.mobile.helper.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthCheckSmsParams(
    val tel: String,
    @SerialName("sms_pin") val smsPin: String,
)
