package ru.contlog.mobile.helper.repo

import android.util.Log
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.contlog.mobile.helper.BuildConfig
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

    const val API_ENDPOINT = BuildConfig.API_HOST

    private val client = OkHttpClient()

    private val jsonCoder = Json {
        ignoreUnknownKeys = true
    }

    object Service {
        suspend fun serviceAvailable() : Boolean {
            return try {
                val request = Request.Builder()
                    .url(API_ENDPOINT)
                    .build()

                val call = client.newCall(request)
                call.await()

                true
            } catch (e: Exception) {
                false
            }
        }
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
                    .url("$API_ENDPOINT/auth/v2/get_sms")
                    .addHeader("Application-Name", "WHAndroid-MobileCabinet")
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
                            "Запрос вернул ошибку: ${response.message}",
                            "Не удалось отправить СМС: произошла ошибка при чтении ответа от сервера"
                        ))
                    }

                    return Result.success(Unit)
                }

                return Result.failure(ApiRequestException(
                    "getSms",
                    "Запрос вернул ошибку: ${response.message}",
                    "Не удалось отправить СМС: произошла ошибка при чтении ответа от сервера"
                ))
            } catch (e: Exception) {
                Log.e(TAG, "getSms: Error sending request", e)

                return Result.failure(ApiRequestException(
                    "getSms",
                    "Ошибка во время выполнения запроса: ${e.message ?: "неизвестная ошибка"}",
                    "Не удалось отправить СМС"
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
                    .url("$API_ENDPOINT/auth/v2/check_sms")
                    .addHeader("Application-Name", "WHAndroid-MobileCabinet")
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
                            "Запрос вернул ошибку: ${response.message}",
                            "Не удалось проверить код: произошла ошибка при чтении ответа от сервера"
                        ))
                    }

                    if (data.data == null) {
                        return Result.failure(ApiRequestException(
                            "checkSms",
                            "Сервер вернул пустой ответ: ${response.message}",
                            "Не удалось проверить код: произошла ошибка при чтении ответа от сервера"
                        ))
                    }

                    return Result.success(data.data)
                }

                return Result.failure(ApiRequestException(
                    "checkSms",
                    "Запрос вернул ошибку: ${response.message}",
                    "Не удалось проверить код: произошла ошибка при чтении ответа от сервера"
                ))
            } catch (e: Exception) {
                Log.e(TAG, "checkSms: Error sending request", e)

                return Result.failure(ApiRequestException(
                    "checkSms",
                    "Ошибка во время выполнения запроса: ${e.message ?: "неизвестная ошибка"}",
                    "Не удалось проверить код"
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
                            "Запрос вернул ошибку: ${response.message}",
                            "Не удалось получить данные пользователя: произошла ошибка при чтении ответа от сервера"
                        ))
                    }

                    if (data.data == null) {
                        return Result.failure(ApiRequestException(
                            "getUserData",
                            "Сервер вернул пустой ответ: ${response.message}",
                            "Не удалось получить данные пользователя: произошла ошибка при чтении ответа от сервера"
                        ))
                    }

                    return Result.success(data.data)
                }

                return Result.failure(ApiRequestException(
                    "getUserData",
                    "Запрос вернул ошибку: ${response.message}",
                    "Не удалось получить данные пользователя: произошла ошибка при чтении ответа от сервера"
                ))
            } catch (e: Exception) {
                Log.e(TAG, "checkSms: Error sending request", e)

                return Result.failure(ApiRequestException(
                    "getUserData",
                    "Ошибка во время выполнения запроса: ${e.message ?: "неизвестная ошибка"}",
                    "Не удалось получить данные пользователя"
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
                            "Запрос вернул ошибку: ${response.message}",
                            "Не удалось получить список площадок: произошла ошибка при чтении ответа от сервера"
                        ))
                    }

                    if (data.data == null) {
                        return Result.failure(ApiRequestException(
                            "getDivisions",
                            "Сервер вернул пустой ответ: ${response.message}",
                            "Не удалось получить список площадок: произошла ошибка при чтении ответа от сервера"
                        ))
                    }

                    return Result.success(data.data)
                }

                return Result.failure(ApiRequestException(
                    "getDivisions",
                    "Запрос вернул ошибку: ${response.message}",
                    "Не удалось получить список площадок: произошла ошибка при чтении ответа от сервера"
                ))
            } catch (e: Exception) {
                Log.e(TAG, "checkSms: Error sending request", e)

                return Result.failure(ApiRequestException(
                    "getDivisions",
                    "Ошибка во время выполнения запроса: ${e.message ?: "неизвестная ошибка"}",
                    "Не удалось получить список площадок"
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
                                "Запрос вернул ошибку: ${response.message}",
                                "Не удалось получить информацию о продукте: произошла ошибка при чтении ответа от сервера"
                            ))
                        }

                        if (data.data == null) {
                            return Result.failure(ApiRequestException(
                                "getDivisions",
                                "Сервер вернул пустой ответ: ${response.message}",
                                "Не удалось получить информацию о продукте: произошла ошибка при чтении ответа от сервера"
                            ))
                        }

                        return Result.success(data.data)
                    }

                    return Result.failure(ApiRequestException(
                        "getDivisions",
                        "Запрос вернул ошибку: ${response.message}",
                        "Не удалось получить информацию о продукте: произошла ошибка при чтении ответа от сервера"
                    ))
                } catch (e: Exception) {
                    Log.e(TAG, "checkSms: Error sending request", e)

                    return Result.failure(ApiRequestException(
                        "getDivisions",
                        "Ошибка во время выполнения запроса: ${e.message ?: "неизвестная ошибка"}",
                        "Не удалось получить информацию о продукте"
                    ))
                }
            }
        }
    }
}