// Пакет репозитория для работы с сетевыми запросами
package ru.contlog.mobile.helper.repo


// Импорты
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

/**
 * Единый точка доступа ко всем API-методам приложения.
 *
 * Реализует:
 *   - отправку SMS для входа,
 *   - проверку SMS-кода,
 *   - получение данных пользователя,
 *   - получение списка подразделений,
 *   - получение информации о продукте по штрихкоду.
 *
 * Использует:
 *   - OkHttp для HTTP-запросов,
 *   - kotlinx.serialization для сериализации/десериализации JSON,
 *   - Kotlin Result для безопасной обработки ошибок.
 */
object Api {
    const val TAG = "Contlog.Api"

    // ⚠️ ВАЖНО: IP-адрес и порт жёстко заданы — это плохо для поддержки.
    // Лучше вынести в конфигурацию или BuildConfig.
    const val API_DOMAIN = "89.189.173.36:800"
    const val API_ENDPOINT = "http://$API_DOMAIN"

    // Единый экземпляр HTTP-клиента для всего приложения
    private val client = OkHttpClient()

    // Настройка JSON-парсера: игнорировать неизвестные поля (защита от изменений API)
    private val jsonCoder = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Группа методов для аутентификации по SMS.
     */
    object Auth {
        /**
         * Отправляет запрос на получение SMS с кодом подтверждения.
         *
         * @param phoneNumber — номер телефона в формате "7XXXXXXXXXX"
         * @return Result<Unit> — успех без данных или ошибка
         */
        suspend fun getSms(
            phoneNumber: String
        ): Result<Unit> {
            try {
                // Формируем тело запроса: сериализуем параметры в JSON и оборачиваем в form-data
                val payload = jsonCoder.encodeToString(AuthGetSmsParams(phoneNumber))

                val body = FormBody.Builder()
                    .add("data", payload)
                    .build()

                // Формируем POST-запрос
                val request = Request.Builder()
                    .url("$API_ENDPOINT/auth/get_sms/wh")
                    .post(body)
                    .build()

                // Выполняем запрос (await — расширение для OkHttp)
                val call = client.newCall(request)
                val response = call.await()

                if (response.isSuccessful) {
                    val responseText = response.body.string()
                    val data = jsonCoder.decodeFromString<AuthGetSmsResponse>(responseText)
                    Log.i(TAG, "getSms: message: ${data.message}")

                    // Если сервер сообщает об ошибке — возвращаем failure
                    if (data.error) {
                        return Result.failure(ApiRequestException(
                            "getSms",
                            "Запрос вернул ошибку: ${response.message}"
                        ))
                    }

                    return Result.success(Unit)
                }

                // HTTP-ошибка (4xx, 5xx)
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

        /**
         * Проверяет введённый SMS-код и возвращает данные авторизации.
         *
         * @param phoneNumber — номер телефона
         * @param code — SMS-код
         * @return Result<ApiAuthData> — данные авторизации или ошибка
         */
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
                    // ⚠️ Обратите внимание: GenericApiResponse<ApiAuthData?>
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
    /**
     * Методы для работы с данными пользователя.
     */
    object User {
        suspend fun getUserData(
            apiAuthData: ApiAuthData,
        ): Result<UserData> {
            try {
                // ⚠️ НЕБЕЗОПАСНО: передача api_key в URL
                // Лучше передавать в заголовке: .addHeader("X-API-Key", apiAuthData.apiKey)
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

    /**
     * Методы для работы с подразделениями и продуктами.
     */
    object Division {
        suspend fun getDivisions(
            apiAuthData: ApiAuthData,
        ): Result<List<ru.contlog.mobile.helper.model.Division>> {
            try {
                // ⚠️ НЕБЕЗОПАСНО: api_key в URL
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

        /**
         * Вложенный объект для методов, связанных с продуктами.
         */
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
/*// Пакет репозитория для работы с сетевыми запросами
package ru.contlog.mobile.helper.repo

// Импорты
import android.util.Log
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.contlog.mobile.helper.exceptions.ApiRequestException
import ru.contlog.mobile.helper.model.*

// Расширение для OkHttp, позволяющее использовать suspend-функции
import ru.contlog.mobile.helper.utils.await

/**
 * Единый точка доступа ко всем API-методам приложения.
 *
 * Реализует:
 *   - отправку SMS для входа,
 *   - проверку SMS-кода,
 *   - получение данных пользователя,
 *   - получение списка подразделений,
 *   - получение информации о продукте по штрихкоду.
 *
 * Использует:
 *   - OkHttp для HTTP-запросов,
 *   - kotlinx.serialization для сериализации/десериализации JSON,
 *   - Kotlin Result для безопасной обработки ошибок.
 */
object Api {
    const val TAG = "Contlog.Api"

    // ⚠️ ВАЖНО: IP-адрес и порт жёстко заданы — это плохо для поддержки.
    // Лучше вынести в конфигурацию или BuildConfig.
    const val API_DOMAIN = "89.189.173.36:800"
    const val API_ENDPOINT = "http://$API_DOMAIN"

    // Единый экземпляр HTTP-клиента для всего приложения
    private val client = OkHttpClient()

    // Настройка JSON-парсера: игнорировать неизвестные поля (защита от изменений API)
    private val jsonCoder = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Группа методов для аутентификации по SMS.
     */
    object Auth {

        /**
         * Отправляет запрос на получение SMS с кодом подтверждения.
         *
         * @param phoneNumber — номер телефона в формате "7XXXXXXXXXX"
         * @return Result<Unit> — успех без данных или ошибка
         */
        suspend fun getSms(phoneNumber: String): Result<Unit> {
            try {
                // Формируем тело запроса: сериализуем параметры в JSON и оборачиваем в form-data
                val payload = jsonCoder.encodeToString(AuthGetSmsParams(phoneNumber))
                val body = FormBody.Builder()
                    .add("data", payload)
                    .build()

                // Формируем POST-запрос
                val request = Request.Builder()
                    .url("$API_ENDPOINT/auth/get_sms/wh")
                    .post(body)
                    .build()

                // Выполняем запрос (await — расширение для OkHttp)
                val response = client.newCall(request).await()

                if (response.isSuccessful) {
                    val responseText = response.body.string()
                    val data = jsonCoder.decodeFromString<AuthGetSmsResponse>(responseText)
                    Log.i(TAG, "getSms: message: ${data.message}")

                    // Если сервер сообщает об ошибке — возвращаем failure
                    if (data.error) {
                        return Result.failure(ApiRequestException(
                            "getSms",
                            "Сервер вернул ошибку: ${data.message}" // ⚠️ Было response.message — исправлено!
                        ))
                    }

                    return Result.success(Unit)
                }

                // HTTP-ошибка (4xx, 5xx)
                return Result.failure(ApiRequestException(
                    "getSms",
                    "HTTP ${response.code}: ${response.message}"
                ))
            } catch (e: Exception) {
                Log.e(TAG, "getSms: Error sending request", e)
                return Result.failure(ApiRequestException(
                    "getSms",
                    "Ошибка сети или парсинга: ${e.message ?: "неизвестная ошибка"}"
                ))
            }
        }

        /**
         * Проверяет введённый SMS-код и возвращает данные авторизации.
         *
         * @param phoneNumber — номер телефона
         * @param code — SMS-код
         * @return Result<ApiAuthData> — данные авторизации или ошибка
         */
        suspend fun checkSms(phoneNumber: String, code: String): Result<ApiAuthData> {
            try {
                val payload = jsonCoder.encodeToString(AuthCheckSmsParams(phoneNumber, code))
                val body = FormBody.Builder()
                    .add("data", payload)
                    .build()

                val request = Request.Builder()
                    .url("$API_ENDPOINT/auth/check_sms/wh")
                    .post(body)
                    .build()

                val response = client.newCall(request).await()

                if (response.isSuccessful) {
                    val responseText = response.body.string()
                    // ⚠️ Обратите внимание: GenericApiResponse<ApiAuthData?>
                    val data = jsonCoder.decodeFromString<GenericApiResponse<ApiAuthData?>>(responseText)

                    if (data.error) {
                        return Result.failure(ApiRequestException(
                            "checkSms",
                            "Сервер вернул ошибку: ${data.message}" // ⚠️ Исправлено!
                        ))
                    }

                    if (data.data == null) {
                        return Result.failure(ApiRequestException(
                            "checkSms",
                            "Сервер не вернул данные авторизации"
                        ))
                    }

                    return Result.success(data.data)
                }

                return Result.failure(ApiRequestException(
                    "checkSms",
                    "HTTP ${response.code}: ${response.message}"
                ))
            } catch (e: Exception) {
                Log.e(TAG, "checkSms: Error sending request", e) // ✅ Исправлено имя метода
                return Result.failure(ApiRequestException(
                    "checkSms",
                    "Ошибка сети или парсинга: ${e.message ?: "неизвестная ошибка"}"
                ))
            }
        }
    }

    /**
     * Методы для работы с данными пользователя.
     */
    object User {
        suspend fun getUserData(apiAuthData: ApiAuthData): Result<UserData> {
            try {
                // ⚠️ НЕБЕЗОПАСНО: передача api_key в URL
                // Лучше передавать в заголовке: .addHeader("X-API-Key", apiAuthData.apiKey)
                val url = "$API_ENDPOINT/wh/get_user_data/${apiAuthData.uid}?api_key=${apiAuthData.apiKey}".toHttpUrl()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).await()

                if (response.isSuccessful) {
                    val responseText = response.body.string()
                    val data = jsonCoder.decodeFromString<GenericApiResponse<UserData?>>(responseText)

                    if (data.error) {
                        return Result.failure(ApiRequestException("getUserData", "Сервер: ${data.message}"))
                    }
                    if (data.data == null) {
                        return Result.failure(ApiRequestException("getUserData", "Нет данных пользователя"))
                    }

                    return Result.success(data.data)
                }

                return Result.failure(ApiRequestException(
                    "getUserData",
                    "HTTP ${response.code}: ${response.message}"
                ))
            } catch (e: Exception) {
                Log.e(TAG, "getUserData: Error", e) // ✅ Исправлено имя метода
                return Result.failure(ApiRequestException(
                    "getUserData",
                    "Ошибка: ${e.message ?: "неизвестная"}"
                ))
            }
        }
    }

    /**
     * Методы для работы с подразделениями и продуктами.
     */
    object Division {
        suspend fun getDivisions(apiAuthData: ApiAuthData): Result<List<Division>> {
            try {
                // ⚠️ НЕБЕЗОПАСНО: api_key в URL
                val url = "$API_ENDPOINT/wh/get_divisions?api_key=${apiAuthData.apiKey}".toHttpUrl()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).await()

                if (response.isSuccessful) {
                    val responseText = response.body.string()
                    val data = jsonCoder.decodeFromString<GenericApiResponse<List<Division>?>>(responseText)

                    if (data.error) {
                        return Result.failure(ApiRequestException("getDivisions", "Сервер: ${data.message}"))
                    }
                    if (data.data == null) {
                        return Result.failure(ApiRequestException("getDivisions", "Пустой список подразделений"))
                    }

                    return Result.success(data.data)
                }

                return Result.failure(ApiRequestException(
                    "getDivisions",
                    "HTTP ${response.code}: ${response.message}"
                ))
            } catch (e: Exception) {
                Log.e(TAG, "getDivisions: Error", e) // ✅ Исправлено имя метода
                return Result.failure(ApiRequestException(
                    "getDivisions",
                    "Ошибка: ${e.message ?: "неизвестная"}"
                ))
            }
        }

        /**
         * Вложенный объект для методов, связанных с продуктами.
         */
        object Product {
            suspend fun getProductInfo(
                apiAuthData: ApiAuthData,
                productInfoParams: ProductInfoParams
            ): Result<List<Product>> {
                try {
                    // ⚠️ НЕБЕЗОПАСНО: api_key в URL
                    val url = "$API_ENDPOINT/api/post_contlog/Wh.ПолучитьИнфоТовараПоШК?api_key=${apiAuthData.apiKey}".toHttpUrl()
                    val payload = jsonCoder.encodeToString(productInfoParams)
                    val body = FormBody.Builder()
                        .add("data", payload)
                        .build()

                    val request = Request.Builder()
                        .url(url)
                        .post(body)
                        .build()

                    val response = client.newCall(request).await()

                    if (response.isSuccessful) {
                        val responseText = response.body.string()
                        val data = jsonCoder.decodeFromString<GenericApiResponse<List<Product>?>>(responseText)

                        if (data.error) {
                            return Result.failure(ApiRequestException(
                                "getProductInfo", // ⚠️ Было "getDivisions" — исправлено!
                                "Сервер: ${data.message}"
                            ))
                        }

                        if (data.data == null) {
                            return Result.failure(ApiRequestException(
                                "getProductInfo", // ⚠️ Было "getDivisions"
                                "Нет данных о продукте"
                            ))
                        }

                        return Result.success(data.data)
                    }

                    return Result.failure(ApiRequestException(
                        "getProductInfo", // ⚠️ Было "getDivisions"
                        "HTTP ${response.code}: ${response.message}"
                    ))
                } catch (e: Exception) {
                    Log.e(TAG, "getProductInfo: Error", e) // ✅ Исправлено имя метода
                    return Result.failure(ApiRequestException(
                        "getProductInfo", // ⚠️ Было "getDivisions"
                        "Ошибка: ${e.message ?: "неизвестная"}"
                    ))
                }
            }
        }
    }
}*/