package ru.contlog.mobile.helper.model

import kotlinx.serialization.Serializable

@Serializable
data class GenericApiResponse<T> (
    val status: Boolean,
    val error: Boolean,
    val message: String,
    val data: T,
)

