// Объявление пакета, к которому принадлежит класс
package ru.contlog.mobile.helper

// Импорт необходимых классов и библиотек Android и Google Play Services
import android.content.BroadcastReceiver            /** Базовый класс для получения broadcast-сообщений */
import android.content.Context                      /** Контекст приложения */
import android.content.Intent                       /** Объект, содержащий информацию о намерении */
import android.util.Log                     /** Класс для вывода логов */
import com.google.android.gms.auth.api.phone.SmsRetriever           /** Класс для работы с SMS Retriever API */
import com.google.android.gms.common.api.CommonStatusCodes          /** Класс с общими кодами статусов Google API */
import com.google.android.gms.common.api.Status             /** Класс для представления статуса Google API */

/**
 * Объявление класса SMSRetrieverBroadcastReceiver, наследующего от BroadcastReceiver
 * Конструктор принимает лямбду onSmsReceived, которая будет вызвана при получении SMS
 */
class SMSRetrieverBroadcastReceiver(private val onSmsReceived: (String, String?) -> Unit) : BroadcastReceiver() {

    /**
     * Переопределение метода onReceive, который вызывается системой
     * при получении broadcast-сообщения
     */
    override fun onReceive(context: Context?, intent: Intent?) {

        /**
         *  Проверка, не равен
         *  ли пришедший Intent null
         */
        if (intent == null) {

            // Если Intent null, логируем ошибку и выходим из метода
            Log.e(TAG, "onReceive: Получили Broadcast, но intent == null!")
            return
        }

        // Получение объекта Bundle с дополнительными данными из Intent
        val extras = intent.extras
        // Проверка, не равны ли extras null
        if (extras == null) {

            // Если extras null, логируем ошибку и выходим из метода
            Log.e(TAG, "onReceive: Получили Broadcast с нормальным intent, но extras == null!")
            return
        }

        // Извлечение объекта Status из extras по ключу SmsRetriever.EXTRA_STATUS
        val status = extras.get(SmsRetriever.EXTRA_STATUS) as Status?

        // Проверка, не равен ли объект status null
        if (status == null) {

            // Если status null, логируем ошибку и выходим из метода
            Log.e(
                TAG,
                "onReceive: Получили Broadcast с нормальным intent и extras, но SmsRetriever.EXTRA_STATUS даёт null!"
            )
            return
        }

        // Использование when для обработки различных значений кода статуса
        when (status.statusCode) {

            // Случай, когда операция прошла успешно (обычно это означает, что SMS получено)
            CommonStatusCodes.SUCCESS -> {

                // Извлечение адреса отправителя SMS из extras. Если отсутствует, используется "N/A"
                val senderAddress = extras.getString(SmsRetriever.EXTRA_SMS_ORIGINATING_ADDRESS, "N/A") // N/A = NO ACKNOWLEDGMENT

                // Извлечение текста SMS из extras
                val message = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE)

                // Вызов переданной в конструкторе лямбды onSmsReceived, передавая ей адрес отправителя и текст сообщения
                onSmsReceived(senderAddress, message)
            }

            // Случай, когда истекло время }ожидания получения SMS
            CommonStatusCodes.TIMEOUT -> {

                // Логирование ошибки таймаута
                Log.e(
                    TAG,
                    "onReceive: SMSRetrieverBroadcastReceiver получил сообщение, что произошёл таймаут ожидания СМС!"
                )
            }
        }
    }

    // Объявление companion object для хранения констант, связанных с классом
    companion object {

        // Константа TAG для использования в логах
        const val TAG = "Contlog.SMSRetrieverBroadcastReceiver"
    }
}