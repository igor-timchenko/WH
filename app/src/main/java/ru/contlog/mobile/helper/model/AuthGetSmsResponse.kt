package ru.contlog.mobile.helper.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthGetSmsResponse (
    val status: Boolean,
    val error: Boolean,
    val message: String,
)

