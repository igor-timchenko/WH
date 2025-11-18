package ru.contlog.mobile.helper.fragments

// Импорты необходимых классов и библиотек Android и Kotlin
import android.animation.Animator               // Импорт необходимого класса для анимации (ViewAnimationUtils)
import android.annotation.SuppressLint          // Импорт аннотации для подавления предупреждений компилятора Android
import android.content.Context                  // Импорт класса Context для доступа к ресурсам и сервисам приложения
import android.content.IntentFilter         // Импорт класса IntentFilter для фильтрации Broadcast-сообщений
import android.net.ConnectivityManager          // Импорт класса ConnectivityManager для проверки состояния подключения к сети
import android.net.NetworkCapabilities      // Импорт класса NetworkCapabilities для определения возможностей сети
import android.os.Build         // Импорт класса Build для получения информации о версии Android
import android.os.Bundle                // Импорт класса Bundle для передачи данных между компонентами Android
import android.text.Editable        // Импорт интерфейса Editable для работы с редактируемым текстом
import android.util.Log         // Импорт класса Log для вывода логов
import android.view.LayoutInflater          // Импорт класса LayoutInflater для инфляции (создания) View из XML
import android.view.View            // Импорт базового класса View для элементов пользовательского интерфейса
import android.view.ViewAnimationUtils          // Импорт класса ViewAnimationUtils для создания анимаций (например, кругового развертывания)
import android.view.ViewGroup           // Импорт класса ViewGroup для контейнеров View
import android.widget.Toast         // Импорт класса Toast для показа коротких всплывающих сообщений
import androidx.core.animation.doOnEnd              // Импорт расширения для корутин (doOnEnd) из библиотеки androidx.core
import androidx.core.animation.doOnStart       // Импорт расширения для корутин (doOnStart) из библиотеки androidx.core
import androidx.core.content.ContextCompat       // Импорт класса ContextCompat для безопасного получения ресурсов и цветов
import androidx.core.view.isInvisible   // Импорт расширения для проверки видимости (isInvisible) из библиотеки androidx.core
import androidx.core.view.postDelayed       // Импорт расширения для задержки (postDelayed) из библиотеки androidx.core
import androidx.fragment.app.Fragment     // Импорт базового класса Fragment для создания фрагментов
import androidx.fragment.app.activityViewModels     // Импорт делегата для получения ViewModel, привязанной к активности
import androidx.lifecycle.lifecycleScope    // Импорт области видимости жизненного цикла для корутин
import androidx.navigation.fragment.findNavController       // Импорт утилиты для навигации между фрагментами
import com.google.android.gms.auth.api.phone.SmsRetriever       // Импорт класса SmsRetriever из Google Play Services для получения SMS
import com.google.android.gms.tasks.OnFailureListener   // Импорт интерфейса OnFailureListener для обработки неудачных задач Google API
import com.google.android.gms.tasks.OnSuccessListener       // Импорт интерфейса OnSuccessListener для обработки успешных задач Google API
import com.google.android.material.dialog.MaterialAlertDialogBuilder   // Импорт билдера для создания Material Design диалогов
import kotlinx.coroutines.Dispatchers       // Импорт диспетчеров корутин (Dispatchers)
import kotlinx.coroutines.launch        // Импорт функции launch для запуска корутин
import ru.contlog.mobile.helper.R   // Импорт сгенерированного класса R для доступа к ресурсам приложения
import ru.contlog.mobile.helper.SMSRetrieverBroadcastReceiver       // Импорт класса SMSRetrieverBroadcastReceiver, определенного в вашем проекте
import ru.contlog.mobile.helper.databinding.FragmentLoginBinding        // Импорт ViewBinding для безопасного доступа к View
import ru.contlog.mobile.helper.repo.Api    // Импорт класса Api, определенного в вашем проекте для сетевых запросов
import ru.contlog.mobile.helper.vm.AppViewModel         // Импорт класса AppViewModel, определенного в вашем проекте
import kotlin.math.hypot        // Импорт функции hypot из библиотеки


// Класс фрагмента экрана авторизации
class LoginFragment : Fragment() {

    // Безопасная реализация ViewBinding с защитой от утечек памяти
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // Получение ViewModel, общего для всей активности (данные сохраняются при навигации)
    private val viewModel: AppViewModel by activityViewModels()
    // Флаг для предотвращения повторной отправки SMS при вводе номера
    private var smsRequested = false

    private var smsRetrieverBroadcastReceiver: SMSRetrieverBroadcastReceiver? = null

    // Создание корневого представления фрагмента из layout-файла
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Настройка UI после создания представления
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind() // Инициализация слушателей и начального состояния
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "LoginFragment onStart - Регистрация BroadcastReceiver")

        val filter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        smsRetrieverBroadcastReceiver = SMSRetrieverBroadcastReceiver(::onSmsReceived)
        ContextCompat.registerReceiver(
            requireContext(),
            smsRetrieverBroadcastReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        Log.d(TAG, "SMSRetrieverBroadcastReceiver зарегистрирован")
    }

    override fun onStop() {
        Log.d(TAG, "LoginFragment onStop - Отмена регистрации BroadcastReceiver")
        smsRetrieverBroadcastReceiver?.let {
            requireContext().unregisterReceiver(it)
            Log.d(TAG, "SMSRetrieverBroadcastReceiver отменена регистрация")
        }
        super.onStop()
    }

    // Освобождение ресурсов при уничтожении View для предотвращения утечек
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Метод инициализации UI-элементов и слушателей
    @SuppressLint("SetTextI18n") // Подавление предупреждения о конкатенации строк для text
    private fun bind() {
        // Изначально скрываем все элементы, связанные с вводом кода подтверждения
        binding.TextCodeInput.visibility = View.GONE
        binding.CodeInput.visibility = View.GONE
        binding.CodeSentMessage.visibility = View.GONE
        binding.getAuthCode.visibility = View.GONE
        // Деактивируем поле ввода кода (нельзя ввести код до получения SMS)
        binding.CodeInput.isEnabled = false

        // Слушатель изменений в поле ввода номера телефона с автоматическим форматированием
        binding.PhoneInput.addTextChangedListener(object : android.text.TextWatcher {
            // Флаг для предотвращения зацикливания при программном изменении текста
            private var isFormatting = false

            // Вызывается до изменения текста (не используется)
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            // Вызывается во время изменения текста (не используется)
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            // Основная логика форматирования номера после изменения текста
            override fun afterTextChanged(s: Editable?) {
                // Защита от рекурсии и null-значений
                if (isFormatting || s == null) return

                // Оставляем только цифры из введённого текста
                val digitsOnly = s.toString().replace(Regex("\\D"), "")
                // Ограничиваем длину до 10 цифр (номер без кода страны)
                val clean = if (digitsOnly.length > 10) digitsOnly.substring(0, 10) else digitsOnly
                // Форматируем номер по маске
                val formatted = formatPhoneNumber(clean)

                // Устанавливаем отформатированный текст обратно в поле ввода
                isFormatting = true
                s.replace(0, s.length, formatted)
                // Устанавливаем курсор в конец текста
                binding.PhoneInput.setSelection(formatted.length)
                isFormatting = false

                // Проверка валидности номера (ровно 10 цифр)
                val isPhoneValid = clean.length == 10
                // Активируем поле ввода кода только при валидном номере
                binding.CodeInput.isEnabled = isPhoneValid

                // --- Изменение: Кнопка отображается ТОЛЬКО если номер валидный И SMS НЕ запрашивался ---
                revealGetAuthCodeButton(show = isPhoneValid && !smsRequested)
                // --- Изменение ---

                // Если длина номера стала меньше 10 и SMS уже запрашивался — сбрасываем состояние
                if (clean.length < 10 && smsRequested) {
                    resetState()
                }
                // 🔹 Больше не отключаем поле при вводе 10 цифр — отключаем только после отправки SMS
                // binding.PhoneInput.isEnabled = !isPhoneValid ← УДАЛЕНО

                if (clean.length < 10 && smsRequested) {
                    resetState()
                }
            }
        })

        // Слушатель изменений в поле ввода кода подтверждения
        binding.CodeInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                // Получаем введённый код, удаляя пробелы по краям
                val code = s?.toString()?.trim() ?: ""
                // Ограничиваем длину кода до 5 символов
                if (code.length > 5) {
                    s?.delete(5, code.length)
                    return
                }
                // Если введено ровно 5 цифр — запускаем проверку кода
                if (code.length == 5 && code.all { it.isDigit() }) {
                    // Скрываем подсказку "Код должен содержать 5 символов"
                    binding.CodeSentMessage.visibility = View.INVISIBLE
                    verifyCode(code)
                } else if (code.isNotEmpty()) {
                    // Если введено 1–4 символа — показываем подсказку
                    binding.CodeSentMessage.visibility = View.VISIBLE
                }
            }
        })

        // Отображение версии приложения в футере
        try {
            // Получаем информацию о пакете (включая версию)
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            val version = packageInfo.versionName ?: "неизвестна"
            binding.appVersionText.text = "Версия: $version"
        } catch (e: Exception) {
            // На случай ошибки (например, пакет удалён) — показываем заглушку
            binding.appVersionText.text = "Версия: неизвестна"
        }

        // Установка слушателя кликов (OnClickListener) на элемент интерфейса с ID 'getAuthCode' (кнопка "Получить код")
        binding.getAuthCode.setOnClickListener {
            // Получение текста из поля ввода номера телефона (binding.PhoneInput)
            // Преобразование его в строку (.toString())
            // Удаление всех символов, которые не являются цифрами (\D - любые не-цифры), с помощью регулярного выражения
            val digitsOnly = binding.PhoneInput.text.toString().replace(Regex("\\D"), "")
            // Проверка двух условий:
            // 1. Длина строки из цифр (digitsOnly) равна 10 (валидный номер)
            // 2. Флаг smsRequested равен false (SMS еще не запрашивался для текущего номера)
            if (digitsOnly.length == 10 && !smsRequested) {
                // Установка флага smsRequested в true, чтобы предотвратить повторную отправку SMS
                // при повторном нажатии кнопки до завершения текущего цикла (ввода кода и проверки)
                smsRequested = true
                // Вызов метода requestSmsCode, передав ему строку из 10 цифр (номер телефона без форматирования)
                requestSmsCode(digitsOnly)
                // Вызов метода startSmsRetriever для инициализации прослушивания SMS-сообщений
                startSmsRetriever()
            }
        }
    }

    // Метод отправки запроса на SMS с кодом подтверждения
    @SuppressLint("InlinedApi")
    private fun requestSmsCode(phoneNumber: String) {
        // Скрываем предыдущее сообщение об отправке/ошибке
        binding.PhoneSentMessage.visibility = View.INVISIBLE

        // Проверка наличия интернет-соединения перед отправкой запроса
        if (!isNetworkAvailable()) {
            binding.PhoneSentMessage.text = "Проверьте интернет соединение"
            binding.PhoneSentMessage.setTextColor(
                ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
            )
            binding.PhoneSentMessage.visibility = View.VISIBLE
            return
        }

        // Запуск сетевого запроса в фоновом потоке
        lifecycleScope.launch(Dispatchers.IO) {
            val result = Api.Auth.getSms("7$phoneNumber") // Добавляем код страны "7"

            // Обновление UI в главном потоке
            launch(Dispatchers.Main) {
                // Защита от вызова после уничтожения фрагмента
                if (!isAdded || _binding == null) return@launch

                result.fold(
                    onSuccess = {
                        // Форматируем номер для отображения
                        val formattedPhone = formatPhoneNumber(phoneNumber)
                        // Устанавливаем текст сообщения "Код отправлен на ..."
                        binding.PhoneSentMessage.text = getString(R.string.smsSentTo, formattedPhone)
                        // Задаём зелёный цвет (успех)
                        binding.PhoneSentMessage.setTextColor(
                            ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
                        )
                        binding.PhoneSentMessage.visibility = View.VISIBLE

                        // 🔹 Отключаем поле ввода номера после отправки SMS
                        binding.PhoneInput.isEnabled = false

                        // Показываем элементы, связанные с вводом кода
                        binding.TextCodeInput.visibility = View.VISIBLE
                        binding.CodeInput.visibility = View.VISIBLE
                        binding.CodeSentMessage.visibility = View.GONE // Подсказка скрыта изначально

                        // Активируем поле, очищаем его и устанавливаем фокус
                        binding.CodeInput.isEnabled = true
                        binding.CodeInput.setText("")
                        binding.CodeInput.requestFocus()
                        binding.getAuthCode.visibility = View.GONE

                        // Показываем подсказку через 1 секунду, если поле осталось пустым
                        binding.CodeInput.postDelayed({
                            if (isAdded && _binding != null) {
                                val currentCode = binding.CodeInput.text?.toString()?.trim()
                                if (currentCode.isNullOrEmpty()) {
                                    binding.CodeSentMessage.visibility = View.VISIBLE
                                }
                            }
                        }, 1000)
                    },
                    onFailure = { _ ->
                        // При ошибке (номер не зарегистрирован) показываем красное сообщение
                        val formattedPhone = formatPhoneNumber(phoneNumber)
                        binding.PhoneSentMessage.text = getString(R.string.error_user_not_found, formattedPhone)
                        binding.PhoneSentMessage.setTextColor(
                            ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                        )
                        binding.PhoneSentMessage.visibility = View.VISIBLE

                        // Скрываем всё, что связано с вводом кода
                        binding.TextCodeInput.visibility = View.GONE
                        binding.CodeInput.visibility = View.GONE
                        binding.CodeSentMessage.visibility = View.GONE
                        binding.CodeInput.isEnabled = false
                        binding.CodeInput.setText("")
                    }
                )
            }
        }
    }

    // Метод проверки введённого кода подтверждения
    private fun verifyCode(code: String) {
        // Проверка интернета перед отправкой кода
        if (!isNetworkAvailable()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Нет соединения")
                .setMessage("Проверьте интернет соединение")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // Получаем сырой ввод из поля телефона и оставляем только цифры
        val rawInput = binding.PhoneInput.text?.toString() ?: ""
        val phoneNumber = rawInput.replace(Regex("\\D"), "") // только цифры
        // Дополнительная проверка длины и содержимого (защита от гонок)
        if (phoneNumber.length != 10 || !phoneNumber.all { it.isDigit() }) return

        // Запуск проверки кода в фоновом потоке
        lifecycleScope.launch(Dispatchers.IO) {
            val result = Api.Auth.checkSms("7$phoneNumber", code)

            // Обновление UI в главном потоке
            launch(Dispatchers.Main) {
                // Защита от вызова после уничтожения фрагмента
                if (!isAdded || _binding == null) return@launch

                result.fold(
                    onSuccess = { apiAuthData ->
                        // Сохраняем данные авторизации в ViewModel
                        viewModel.login = phoneNumber
                        viewModel.apiAuthData = apiAuthData
                        // Скрываем все сообщения
                        binding.PhoneSentMessage.visibility = View.INVISIBLE
                        binding.CodeSentMessage.visibility = View.INVISIBLE
                        // Переход к следующему экрану
                        findNavController().navigate(R.id.action_loginFragment_to_workSitesFragment)
                        // Показ Toast-уведомления об успешной авторизации (выполняется после корутины)
                        Toast.makeText(requireContext(), "Авторизация успешна!!!", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { _ ->
                        // При неверном коде очищаем поле и показываем диалог
                        binding.CodeInput.setText("")
                        binding.CodeSentMessage.visibility = View.VISIBLE
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Неверный код")
                            .setMessage("Код неправильный, повторите")
                            .setPositiveButton("OK") { _, _ ->
                                binding.CodeInput.requestFocus()
                            }
                            .show()
                    }
                )
            }
        }
    }

    // Метод сброса состояния (при удалении цифр из номера)
    private fun resetState() {
        smsRequested = false
        binding.PhoneSentMessage.visibility = View.INVISIBLE
        binding.CodeSentMessage.visibility = View.GONE
        binding.TextCodeInput.visibility = View.GONE
        binding.CodeInput.visibility = View.GONE
        binding.CodeInput.isEnabled = false
        binding.CodeInput.setText("")

        revealGetAuthCodeButton(show=false)

        // 🔹 Возвращаем активность полю при сбросе
        binding.PhoneInput.isEnabled = true
    }

    private var nextViewState: Int? = null
    private var animation: Animator? = null
    private fun revealGetAuthCodeButton(show: Boolean) {
        if (binding.getAuthCode.isInvisible && !show && nextViewState != View.INVISIBLE) {
            return
        }

        val cx = binding.getAuthCode.width / 2
        val cy = binding.getAuthCode.height / 2
        val circumcircleRadius = hypot(cx.toDouble(), cy.toDouble()).toFloat()
        val zeroRadius = 0f

        val starting = if (show) zeroRadius else circumcircleRadius
        val ending = if (show) circumcircleRadius else zeroRadius

        nextViewState = if (show) View.VISIBLE else View.INVISIBLE

        animation?.cancel()
        animation = ViewAnimationUtils.createCircularReveal(
            binding.getAuthCode,
            cx, cy,
            starting, ending
        )
        animation!!.doOnStart {
            binding.getAuthCode.visibility = View.VISIBLE
        }
        animation!!.doOnEnd {
            binding.getAuthCode.visibility = nextViewState ?: (if (show) View.VISIBLE else View.INVISIBLE)
            animation = null
            nextViewState = null
        }
        animation!!.start()
    }

    // Форматирование номера по маске: " (XXX) XXX-XX-XX"
    private fun formatPhoneNumber(digits: String): String {
        val clean = digits.take(10)
        return when (clean.length) {
            0 -> " " // Пробел вместо пустой строки — для стабильности UI
            1 -> " (${clean}"
            2 -> " (${clean}"
            3 -> " (${clean}"
            4 -> " (${clean.substring(0, 3)}) ${clean[3]}"
            5 -> " (${clean.substring(0, 3)}) ${clean.substring(3, 5)}"
            6 -> " (${clean.substring(0, 3)}) ${clean.substring(3, 6)}"
            7 -> " (${clean.substring(0, 3)}) ${clean.substring(3, 6)}-${clean[6]}"
            8 -> " (${clean.substring(0, 3)}) ${clean.substring(3, 6)}-${clean.substring(6, 8)}"
            9 -> " (${clean.substring(0, 3)}) ${clean.substring(3, 6)}-${clean.substring(6, 8)}-${clean[8]}"
            10 -> " (${clean.substring(0, 3)}) ${clean.substring(3, 6)}-${clean.substring(6, 8)}-${clean.substring(8, 10)}"
            else -> " (${clean.substring(0, 3)}) ${clean.substring(3, 6)}-${clean.substring(6, 8)}-${clean.substring(8, 10)}"
        }
    }

    // Проверка наличия активного интернет-соединения
    @SuppressLint("MissingPermission", "ObsoleteSdkInt") // Подавление предупреждений для старых API
    private fun isNetworkAvailable(): Boolean {
        // Получаем системный сервис управления подключениями
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Для Android 6.0+ используем NetworkCapabilities
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            // Для старых версий используем устаревший метод (с подавлением предупреждения)
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected ?: false
        }
    }

    private fun startSmsRetriever() {
        Log.d(TAG, "Вызов startSmsRetriever")
        val client = SmsRetriever.getClient(requireContext())
        val task = client.startSmsRetriever()

        task.addOnSuccessListener {
            Log.i(TAG, "startSmsRetriever: Удалось подписаться на получение СМС, ждём Broadcast...")
        }

        task.addOnFailureListener { e ->
            Log.e(TAG, "startSmsRetriever: Не удалось подписаться на получение СМС", e)
        }
    }

    private fun onSmsReceived(sender: String, code: String?) {
        lifecycleScope.launch(Dispatchers.Main) {
            Log.d(TAG, "Получено SMS: '$code'") // <-- Добавьте это

            val realCode = code?.let {
                try {
                    val codePattern = Regex("""\[#]\s*Ваш\s*код\s*подтверждения:\s+(\d{5})""", RegexOption.DOT_MATCHES_ALL)
                    val matchResult = codePattern.find(it)
                    Log.d(TAG, "Результат поиска по регулярному выражению: $matchResult") // <-- Добавьте это
                    matchResult?.groupValues?.get(1)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка в регулярном выражении", e) // <-- Добавьте это
                    null
                }
            }
            Log.d(TAG, "Извлеченный код: $realCode") // <-- Добавьте это

            if (realCode != null && isAdded && _binding != null) {
                binding.CodeInput.setText(realCode)
                binding.CodeInput.setSelection(realCode.length)
                binding.CodeSentMessage.visibility = View.INVISIBLE

                binding.root.postDelayed({
                    verifyCode(realCode)
                }, 10)
            } else {
                Log.w(TAG, "Не удалось извлечь код или фрагмент еще не готов. Код был: $code")
            }
        }
    }
    companion object {
        const val TAG = "Contlog.LoginFragment"
    }
}