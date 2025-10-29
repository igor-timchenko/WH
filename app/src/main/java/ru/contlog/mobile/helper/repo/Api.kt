package ru.contlog.mobile.helper.repo

import android.util.Log
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.contlog.mobile.helper.exceptions.ApiRequestException
import ru.contlog.mobile.helper.model.AuthGetSmsResponse
import ru.contlog.mobile.helper.model.AuthCheckSmsParams
import ru.contlog.mobile.helper.model.AuthGetSmsParams
import ru.contlog.mobile.helper.model.ApiAuthData
import ru.contlog.mobile.helper.model.GenericApiResponse
import ru.contlog.mobile.helper.model.ProductInfoParams
import ru.contlog.mobile.helper.model.UserData
import ru.contlog.mobile.helper.utils.await

object Api {
    const val TAG = "Contlog.Api"

    const val API_DOMAIN = "89.189.173.36:800"
    const val API_ENDPOINT = "http://$API_DOMAIN"

    private val client = OkHttpClient()

    private val jsonCoder = Json {
        ignoreUnknownKeys = true
    }

    object Auth {
        suspend fun getSms(
            phoneNumber: String
        ): Result<Unit> {
            try {
                val payload = jsonCoder.encodeToString(AuthGetSmsParams(phoneNumber))

                val body = FormBody.Builder()
                    .add("data", payload)
                    .build()

                val request = Request.Builder()
                    .url("$API_ENDPOINT/auth/get_sms/wh")
                    .post(body)
                    .build()

                val call = client.newCall(request)
                val response = call.await()

                if (response.isSuccessful) {
                    val responseText = response.body.string()
                    val data = jsonCoder.decodeFromString<AuthGetSmsResponse>(responseText)
                    Log.i(TAG, "getSms: message: ${data.message}")

                    if (data.error) {
                        return Result.failure(ApiRequestException(
                            "getSms",
                            "Запрос вернул ошибку: ${response.message}"
                        ))
                    }

                    return Result.success(Unit)
                }

                return Result.failure(ApiRequestException(
                    "getSms",
                    "Запрос вернул ошибку: ${response.message}"
                ))
            } catch (e: Exception) {
                Log.e(TAG, "getSms: Error sending request", e)

                return Result.failure(ApiRequestException(
                    "getSms",
                    "Ошибка во время выполнения запроса: ${e.message ?: "неизвестная ошибка"}"
                ))
            }
        }

        suspend fun checkSms(
            phoneNumber: String,
            code: String
        ): Result<ApiAuthData> {
            try {
                val payload = jsonCoder.encodeToString(AuthCheckSmsParams(phoneNumber, code))

                val body = FormBody.Builder()
                    .add("data", payload)
                    .build()

                val request = Request.Builder()
                    .url("$API_ENDPOINT/auth/check_sms/wh")
                    .post(body)
                    .build()

                val call = client.newCall(request)
                val response = call.await()

                if (response.isSuccessful) {
                    val responseText = response.body.string()
                    val data = jsonCoder.decodeFromString<GenericApiResponse<ApiAuthData?>>(responseText)

                    if (data.error) {
                        return Result.failure(ApiRequestException(
                            "checkSms",
                            "Запрос вернул ошибку: ${response.message}"
                        ))
                    }

                    if (data.data == null) {
                        return Result.failure(ApiRequestException(
                            "checkSms",
                            "Сервер вернул пустой ответ: ${response.message}"
                        ))
                    }

                    return Result.success(data.data)
                }

                return Result.failure(ApiRequestException(
                    "checkSms",
                    "Запрос вернул ошибку: ${response.message}"
                ))
            } catch (e: Exception) {
                Log.e(TAG, "checkSms: Error sending request", e)

                return Result.failure(ApiRequestException(
                    "checkSms",
                    "Ошибка во время выполнения запроса: ${e.message ?: "неизвестная ошибка"}"
                ))
            }
        }
    }

    object User {
        suspend fun getUserData(
            apiAuthData: ApiAuthData,
        ): Result<UserData> {
            try {
                val url = "$API_ENDPOINT/wh/get_user_data/${apiAuthData.uid}?api_key=${apiAuthData.apiKey}".toHttpUrl()
                val request = Request.Builder()
                    .url(url)
                    .build()

                val call = client.newCall(request)
                val response = call.await()

                if (response.isSuccessful) {
                    val responseText = response.body.string()
                    val data = jsonCoder.decodeFromString<GenericApiResponse<UserData?>>(responseText)

                    if (data.error) {
                        return Result.failure(ApiRequestException(
                            "getUserData",
                            "Запрос вернул ошибку: ${response.message}"
                        ))
                    }

                    if (data.data == null) {
                        return Result.failure(ApiRequestException(
                            "getUserData",
                            "Сервер вернул пустой ответ: ${response.message}"
                        ))
                    }

                    return Result.success(data.data)
                }

                return Result.failure(ApiRequestException(
                    "getUserData",
                    "Запрос вернул ошибку: ${response.message}"
                ))
            } catch (e: Exception) {
                Log.e(TAG, "checkSms: Error sending request", e)

                return Result.failure(ApiRequestException(
                    "getUserData",
                    "Ошибка во время выполнения запроса: ${e.message ?: "неизвестная ошибка"}"
                ))
            }
        }
    }

    object Division {
        suspend fun getDivisions(
            apiAuthData: ApiAuthData,
        ): Result<List<ru.contlog.mobile.helper.model.Division>> {
            try {
                val url = "$API_ENDPOINT/wh/get_divisions?api_key=${apiAuthData.apiKey}".toHttpUrl()
                val request = Request.Builder()
                    .url(url)
                    .build()

                val call = client.newCall(request)
                val response = call.await()

                if (response.isSuccessful) {
                    val responseText = response.body.string()
                    val data = jsonCoder.decodeFromString<GenericApiResponse<List<ru.contlog.mobile.helper.model.Division>?>>(responseText)

                    if (data.error) {
                        return Result.failure(ApiRequestException(
                            "getDivisions",
                            "Запрос вернул ошибку: ${response.message}"
                        ))
                    }

                    if (data.data == null) {
                        return Result.failure(ApiRequestException(
                            "getDivisions",
                            "Сервер вернул пустой ответ: ${response.message}"
                        ))
                    }

                    return Result.success(data.data)
                }

                return Result.failure(ApiRequestException(
                    "getDivisions",
                    "Запрос вернул ошибку: ${response.message}"
                ))
            } catch (e: Exception) {
                Log.e(TAG, "checkSms: Error sending request", e)

                return Result.failure(ApiRequestException(
                    "getDivisions",
                    "Ошибка во время выполнения запроса: ${e.message ?: "неизвестная ошибка"}"
                ))
            }
        }

        object Product {
            suspend fun getProductInfo(
                apiAuthData: ApiAuthData,
                productInfoParams: ProductInfoParams
            ): Result<List<ru.contlog.mobile.helper.model.Product>> {
                try {
                    val url = "$API_ENDPOINT/api/post_contlog/Wh.ПолучитьИнфоТовараПоШК?api_key=${apiAuthData.apiKey}".toHttpUrl()
                    val payload = jsonCoder.encodeToString(productInfoParams)

                    val body = FormBody.Builder()
                        .add("data", payload)
                        .build()

                    val request = Request.Builder()
                        .url(url)
                        .post(body)
                        .build()

                    val call = client.newCall(request)
                    val response = call.await()

                    if (response.isSuccessful) {
                        val responseText = response.body.string()
                        val data = jsonCoder.decodeFromString<GenericApiResponse<List<ru.contlog.mobile.helper.model.Product>?>>(responseText)

                        if (data.error) {
                            return Result.failure(ApiRequestException(
                                "getDivisions",
                                "Запрос вернул ошибку: ${response.message}"
                            ))
                        }

                        if (data.data == null) {
                            return Result.failure(ApiRequestException(
                                "getDivisions",
                                "Сервер вернул пустой ответ: ${response.message}"
                            ))
                        }

                        return Result.success(data.data)
                    }

                    return Result.failure(ApiRequestException(
                        "getDivisions",
                        "Запрос вернул ошибку: ${response.message}"
                    ))
                } catch (e: Exception) {
                    Log.e(TAG, "checkSms: Error sending request", e)

                    return Result.failure(ApiRequestException(
                        "getDivisions",
                        "Ошибка во время выполнения запроса: ${e.message ?: "неизвестная ошибка"}"
                    ))
                }
            }
        }
    }
}