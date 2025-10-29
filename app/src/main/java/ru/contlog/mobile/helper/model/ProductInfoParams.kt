package ru.contlog.mobile.helper.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductInfoParams(
    @SerialName("division_uid") val divisionUID: String,
    @SerialName("division_code") val divisionCode: Int,
    @SerialName("product_code") val productCode: String,
)
