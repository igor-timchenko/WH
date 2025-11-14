// Объявление пакета, к которому принадлежит класс
package ru.contlog.mobile.helper;

// Импорт необходимых классов и библиотек Android
import android.content.Context;                 // Контекст приложения
import android.content.pm.PackageManager;       // Менеджер пакетов для получения информации о приложении
import android.content.pm.Signature;           // Класс, представляющий подпись приложения
import android.util.Base64;             // Утилита для кодирования/декодирования Base64
import android.util.Log;                // Класс для вывода логов

// Импорт стандартных Java-библиотек
import java.io.UnsupportedEncodingException;        // Исключение, если кодировка не поддерживается
import java.security.MessageDigest;                     // Класс для вычисления хеш-функций
import java.security.NoSuchAlgorithmException;      // Исключение, если алгоритм хеширования не найден
import java.util.Arrays;                // Утилиты для работы с массивами

/**
 * Это вспомогательный класс для генерации хеша вашего сообщения, который должен быть включен в SMS-сообщение.
 * <p>
 * Без правильного хеша ваше приложение не получит обратный вызов сообщения. Это нужно генерировать
 * только один раз для приложения и сохранить. Затем вы можете удалить этот вспомогательный класс из кода.
 */
public class WH_SUPPLIER_CONTINENT {        // Имя класса (ваше, нестандартное)

    // Публичная статическая константа TAG для использования в логах
    public static final String TAG = ru.contlog.mobile.helper.WH_SUPPLIER_CONTINENT.class.getSimpleName();

    // Приватные статические константы, определяющие параметры хеширования
    private static final String HASH_TYPE = "SHA-256";          // Используемый алгоритм хеширования
    private static final int NUM_HASHED_BYTES = 9;              // Количество байт, которое нужно взять из полного хеша
    private static final int NUM_BASE64_CHAR = 11;              // Количество символов Base64 для итогового хеша (9 байт -> 12 Base64, но обрезаем до 11)

    /**
     * Получить все подписи приложения для текущего пакета
     *
     * @param context Контекст приложения
     * @return Хеш подписи приложения (строка из 11 символов) или пустую строку в случае ошибки
     */
    public static String getSignature(Context context) {

        try {
            // Получение имени пакета текущего приложения
            String packageName = context.getPackageName();

            // Получение экземпляра PackageManager
            PackageManager packageManager = context.getPackageManager();

            // Получение информации о пакете, включая его подписи (GET_SIGNATURES)
            Signature[] signatures = packageManager.getPackageInfo(packageName,
                    PackageManager.GET_SIGNATURES).signatures;

            // Цикл по всем подписям (обычно одна, но может быть несколько)
            for (Signature signature : signatures) {

                // Вызов внутреннего метода hash для создания хеша для текущей подписи
                String hash = hash(packageName, signature.toCharsString());

                // Если хеш успешно создан (не null)
                if (hash != null) {

                    // Вернуть этот хеш (обычно прервет цикл после первой итерации)
                    return String.format("%s", hash);       // Форматирование строки (избыточно, можно просто return hash;)
                }
            }
        } catch (PackageManager.NameNotFoundException e) {

            // Логирование ошибки, если пакет не найден (теоретически не должно произойти)
            Log.e(TAG, "Unable to find package to obtain hash.", e);
        }
        // Возврат пустой строки, если хеш не удалось получить
        return "";
    }

    /**
     * Внутренний метод для вычисления хеша подписи
     *
     * @param packageName Имя пакета приложения
     * @param signature   Строковое представление подписи
     * @return Хеш подписи (строка из 11 символов) или null в случае ошибки
     */
    private static String hash(String packageName, String signature) {

        // Создание строки, которая будет хешироваться: "имя_пакета подпись_в_виде_строки"
        String appInfo = packageName + " " + signature;
        try {
            // Получение экземпляра MessageDigest для алгоритма SHA-256
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_TYPE);

            // Обновление MessageDigest байтами строки appInfo в кодировке UTF-8
            messageDigest.update(appInfo.getBytes("UTF-8"));

            // Вычисление финального хеша
            byte[] hashSignature = messageDigest.digest();

            // Обрезание полученного массива байт до первых NUM_HASHED_BYTES (9 байт)
            hashSignature = Arrays.copyOfRange(hashSignature, 0, NUM_HASHED_BYTES);

            // Кодирование обрезанного массива байт в строку Base64 без дополнительных символов (NO_PADDING, NO_WRAP)
            String base64Hash = Base64.encodeToString(hashSignature, Base64.NO_PADDING | Base64.NO_WRAP);

            // Обрезание строки Base64 до NUM_BASE64_CHAR (11 символов)
            base64Hash = base64Hash.substring(0, NUM_BASE64_CHAR);

            // Логирование имени пакета и вычисленного хеша
            Log.d(TAG, String.format("pkg: %s -- hash: %s", packageName, base64Hash));

            // Возврат итогового хеша
            return base64Hash;
        } catch (UnsupportedEncodingException e) {

            // Логирование ошибки, если кодировка UTF-8 не поддерживается (маловероятно)
            Log.e(TAG, "hash:UnsupportedEncoding", e);
        } catch (NoSuchAlgorithmException e) {

            // Логирование ошибки, если алгоритм SHA-256 не найден (маловероятно)
            Log.e(TAG, "hash:NoSuchAlgorithm", e);
        }
        // Возврат null в случае ошибки
        return null;
    }
}

