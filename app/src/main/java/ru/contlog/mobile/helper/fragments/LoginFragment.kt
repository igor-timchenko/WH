package ru.contlog.mobile.helper.fragments

// Импорты необходимых классов и библиотек Android и Kotlin
import android.annotation.SuppressLint                    // Для подавления предупреждений компилятора
import android.content.Context                          // Контекст приложения/активности
import android.net.ConnectivityManager                  // Системный сервис для проверки подключения к сети
import android.net.NetworkCapabilities                 // Возможности текущей сети (доступ в интернет и т.д.)
import android.os.Build                                 // Информация о версии Android
import android.os.Bundle                                // Контейнер для передачи данных между компонентами
import android.text.Editable                            // Тип для изменяемого текста (используется в TextWatcher)
import android.view.LayoutInflater                      // Создание View из XML-разметки
import android.view.View                                // Базовый класс UI-элемента
import android.view.ViewGroup                           // Контейнер для View
import android.widget.Toast                             // Всплывающее уведомление
import androidx.core.content.ContextCompat             // Безопасное получение ресурсов и цветов
import androidx.fragment.app.Fragment                   // Базовый класс фрагмента
import androidx.fragment.app.activityViewModels        // Делегат для получения ViewModel, привязанной к активности
import androidx.lifecycle.lifecycleScope               // Область корутин, привязанная к жизненному циклу
import androidx.navigation.fragment.findNavController  // Утилита для навигации между фрагментами
import com.google.android.material.dialog.MaterialAlertDialogBuilder // Диалоги Material Design
import kotlinx.coroutines.Dispatchers                   // Диспетчеры корутин (Main, IO и т.д.)
import kotlinx.coroutines.launch                        // Запуск корутины
import ru.contlog.mobile.helper.R                       // Сгенерированный класс ресурсов
import ru.contlog.mobile.helper.databinding.FragmentLoginBinding // ViewBinding для этого фрагмента
import ru.contlog.mobile.helper.repo.Api                // Объект для выполнения сетевых запросов
import ru.contlog.mobile.helper.vm.AppViewModel         // Общий ViewModel для хранения состояния приложения

// Класс фрагмента экрана авторизации
class LoginFragment : Fragment() {

    // Безопасная реализация ViewBinding с защитой от утечек памяти
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // Получение ViewModel, общего для всей активности (данные сохраняются при навигации)
    private val viewModel: AppViewModel by activityViewModels()
    // Флаг для предотвращения повторной отправки SMS при вводе номера
    private var smsRequested = false

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

                // Если номер валиден и SMS ещё не запрашивался — отправляем запрос
                if (isPhoneValid && !smsRequested) {
                    smsRequested = true
                    requestSmsCode(clean)
                }

                // Если длина номера стала меньше 10 и SMS уже запрашивался — сбрасываем состояние
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
    }

    // Метод отправки запроса на SMS с кодом подтверждения
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

                        // Показываем элементы, связанные с вводом кода
                        binding.TextCodeInput.visibility = View.VISIBLE
                        binding.CodeInput.visibility = View.VISIBLE
                        binding.CodeSentMessage.visibility = View.GONE // Подсказка скрыта изначально

                        // Активируем поле, очищаем его и устанавливаем фокус
                        binding.CodeInput.isEnabled = true
                        binding.CodeInput.setText("")
                        binding.CodeInput.requestFocus()

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
        // Показ Toast-уведомления об успешной авторизации (выполняется после корутины)
        Toast.makeText(requireContext(), "Авторизация успешна!!!", Toast.LENGTH_SHORT).show()
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
}