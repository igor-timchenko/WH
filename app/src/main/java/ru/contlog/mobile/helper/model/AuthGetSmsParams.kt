package ru.contlog.mobile.helper.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthGetSmsParams(
    val tel: String,
)
