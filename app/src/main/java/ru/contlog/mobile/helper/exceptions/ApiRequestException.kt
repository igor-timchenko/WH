package ru.contlog.mobile.helper.exceptions

class ApiRequestException(
    val apiMethod: String,
    val exceptionMessage: String,
) : Exception() {
    override val message: String
        get() = "[${apiMethod}] ${exceptionMessage}"
}