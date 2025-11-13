package ru.contlog.mobile.helper.utils

// Импорт функции suspendCancellableCoroutine из kotlinx.coroutines —
// позволяет преобразовать колбэк-ориентированный API в suspend-функцию с поддержкой отмены
import kotlinx.coroutines.suspendCancellableCoroutine

// Импорты классов из OkHttp для работы с HTTP-запросами
import okhttp3.Call          // Представляет один HTTP-запрос (вызов)
import okhttp3.Callback     // Интерфейс для получения результата асинхронного запроса
import okhttp3.Response     // Ответ от сервера
import okio.IOException     // Исключение, возникающее при ошибках ввода-вывода (например, нет сети)

// Импорты функций для возобновления корутины
import kotlin.coroutines.resume            // Возобновляет корутину с результатом
import kotlin.coroutines.resumeWithException // Возобновляет корутину с исключением

// Расширение-функция для класса okhttp3.Call
// Превращает асинхронный колбэк-вызов в suspend-функцию, которую можно использовать в корутинах
suspend fun Call.await(): Response {
    // Используем suspendCancellableCoroutine, чтобы приостановить корутину
    // и дождаться завершения HTTP-запроса
    return suspendCancellableCoroutine { cont ->
        // Запускаем асинхронный HTTP-запрос с помощью enqueue
        enqueue(object : Callback {
            // Вызывается, если запрос завершился ошибкой (например, таймаут, нет соединения)
            override fun onFailure(call: Call, e: IOException) {
                // Возобновляем корутину с исключением — вызовет throw e в месте вызова await()
                cont.resumeWithException(e)
            }

            // Вызывается при успешном получении ответа от сервера (даже если статус 4xx/5xx)
            override fun onResponse(call: Call, response: Response) {
                // Возобновляем корутину, возвращая объект Response
                cont.resume(response)
            }
        })

        // Регистрируем обработчик отмены: если корутина будет отменена (например, при выходе из экрана),
        // автоматически отменяем HTTP-запрос, чтобы избежать утечек и ненужной работы
        cont.invokeOnCancellation {
            cancel()
        }
    }
}