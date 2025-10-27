// Пакет вспомогательных утилит
package ru.contlog.mobile.helper.utils

// Импорты для работы с корутинами и OkHttp
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okio.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Расширение-функция для [okhttp3.Call], превращающая callback-стиль OkHttp
 * в suspend-функцию, совместимую с Kotlin Coroutines.
 *
 * Позволяет писать асинхронные сетевые запросы в императивном стиле:
 *
 * Пример использования:
 * ```kotlin
 * val request = Request.Builder().url("https://api.example.com").build()
 * val call = client.newCall(request)
 * val response = call.await() // ← без колбэков!
 * ```
 *
 * Особенности:
 *   - Поддерживает отмену (cancellation): если корутина отменяется, запрос прерывается.
 *   - Безопасно обрабатывает ошибки сети (IOException).
 *   - Возвращает [Response] напрямую — клиент сам должен проверить.isSuccessful и закрыть тело.
 */
suspend fun Call.await(): Response {
    // suspendCancellableCoroutine — основной способ обернуть callback-API в suspend-функцию
    return suspendCancellableCoroutine { cont ->
        // Регистрируем колбэк у OkHttp
        enqueue(object : Callback {
            /**
             * Вызывается при сетевой ошибке (нет интернета, таймаут и т.д.)
             */
            override fun onFailure(call: Call, e: IOException) {
                // Передаём исключение в корутину → Result.failure() или throw
                cont.resumeWithException(e)
            }

            /**
             * Вызывается при получении HTTP-ответа (даже если код 4xx/5xx!)
             */
            override fun onResponse(call: Call, response: Response) {
                // Успешно завершаем корутину с объектом Response
                cont.resume(response)
            }
        })

        // Если корутина будет отменена (например, пользователь вышел из экрана),
        // автоматически отменяем сетевой запрос — экономим трафик и ресурсы
        cont.invokeOnCancellation {
            cancel()
        }
    }
}