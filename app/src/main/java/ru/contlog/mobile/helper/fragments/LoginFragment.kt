package ru.contlog.mobile.helper.fragments

// Импорты необходимых классов и библиотек
import android.annotation.SuppressLint
import android.os.Bundle                // Для работы с данными жизненного цикла
import android.text.Editable           // Для работы с изменяемым текстом (TextWatcher)
import android.view.LayoutInflater      // Для раздувания (inflation) layout-файлов
import android.view.View                // Базовый класс UI-элемента
import android.view.ViewGroup           // Контейнер для View
import androidx.core.content.ContextCompat // Безопасное получение цветов и ресурсов
import androidx.fragment.app.Fragment   // Базовый класс фрагмента
import androidx.fragment.app.activityViewModels // Делегат для получения ViewModel, привязанной к активности
import androidx.lifecycle.lifecycleScope // Область корутин, привязанная к жизненному циклу
import androidx.navigation.fragment.findNavController // Утилита для навигации между фрагментами
import com.google.android.material.dialog.MaterialAlertDialogBuilder // Диалоговые окна Material Design
import kotlinx.coroutines.Dispatchers    // Диспетчеры корутин (Main, IO и др.)
import kotlinx.coroutines.launch        // Запуск корутины
import ru.contlog.mobile.helper.R       // Сгенерированный класс ресурсов
import ru.contlog.mobile.helper.databinding.FragmentLoginBinding // ViewBinding для этого фрагмента
import ru.contlog.mobile.helper.repo.Api // Объект для работы с сетевыми запросами
import ru.contlog.mobile.helper.vm.AppViewModel // Общий ViewModel для хранения состояния

// Класс фрагмента экрана авторизации
class LoginFragment : Fragment() {

    // Безопасная реализация ViewBinding с защитой от утечек
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // Получаем ViewModel, общий для всей активности (чтобы данные сохранились при навигации)
    private val viewModel: AppViewModel by activityViewModels()
    // Флаг для предотвращения повторной отправки SMS
    private var smsRequested = false

    // Создаём корневое представление из layout-файла
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Вызывается после создания View — здесь происходит инициализация UI
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind() // Настраиваем слушатели и начальное состояние
    }

    // Освобождаем ViewBinding при уничтожении View для предотвращения утечек памяти
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Метод инициализации UI-элементов и слушателей
    @SuppressLint("SetTextI18n")
    private fun bind() {
        // Изначально скрываем все элементы, связанные с вводом кода
        binding.textCodeInput.visibility = View.GONE
        binding.codeInput.visibility = View.GONE
        binding.codeSentMessage2.visibility = View.GONE
        // Деактивируем поле ввода кода (нельзя ввести код до получения SMS)
        binding.codeInput.isEnabled = false

        // === Слушатель ввода номера телефона с автоматическим форматированием по маске (913) 440-89-04 ===
        binding.phoneInput.addTextChangedListener(object : android.text.TextWatcher {
            // Флаг для предотвращения зацикливания при программном изменении текста
            private var isFormatting = false

            // Вызывается до изменения текста (не используется)
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            // Вызывается во время изменения текста (не используется)
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            // Вызывается после изменения текста — основная логика форматирования
            override fun afterTextChanged(s: Editable?) {
                // Защита от рекурсии и null
                if (isFormatting || s == null) return

                // Оставляем только цифры из введённого текста
                val digitsOnly = s.toString().replace(Regex("\\D"), "")
                // Ограничиваем длину до 10 цифр (максимум для номера без кода страны)
                val clean = if (digitsOnly.length > 10) digitsOnly.substring(0, 10) else digitsOnly
                // Форматируем номер по маске
                val formatted = formatPhoneNumber(clean)

                // Устанавливаем отформатированный текст обратно в поле ввода
                isFormatting = true
                s.replace(0, s.length, formatted)
                // Устанавливаем курсор в конец текста
                binding.phoneInput.setSelection(formatted.length)
                isFormatting = false

                // Проверяем, является ли номер валидным (ровно 10 цифр)
                val isPhoneValid = clean.length == 10
                // Активируем поле ввода кода только при валидном номере
                binding.codeInput.isEnabled = isPhoneValid

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

        // === Слушатель ввода кода подтверждения ===
        binding.codeInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                // Получаем введённый код, убираем пробелы по краям
                val code = s?.toString()?.trim() ?: ""
                // Ограничиваем длину кода до 4 символов
                if (code.length > 4) {
                    s?.delete(4, code.length)
                    return
                }
                // Если введено ровно 4 цифры — запускаем проверку кода
                if (code.length == 4 && code.all { it.isDigit() }) {
                    // Скрываем подсказку "Код должен содержать 4 символа"
                    binding.codeSentMessage2.visibility = View.INVISIBLE
                    verifyCode(code)
                } else if (code.isNotEmpty()) {
                    // Если введено 1–3 символа — показываем подсказку
                    binding.codeSentMessage2.visibility = View.VISIBLE
                }
            }
        })

        // === Отображение версии приложения в футере ===
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
        binding.codeSentMessage.visibility = View.INVISIBLE

        // Запускаем сетевой запрос в фоновом потоке
        lifecycleScope.launch(Dispatchers.IO) {
            val result = Api.Auth.getSms("7$phoneNumber") // Добавляем код страны "7"

            // Возвращаемся в главный поток для обновления UI
            launch(Dispatchers.Main) {
                // Защита от вызова после уничтожения фрагмента
                if (!isAdded || _binding == null) return@launch

                result.fold(
                    onSuccess = {
                        // Форматируем номер для отображения (без +7)
                        val formattedPhone = formatPhoneNumber(phoneNumber)
                        // Устанавливаем текст сообщения "Код отправлен на ..."
                        binding.codeSentMessage.text = getString(R.string.smsSentTo, formattedPhone)
                        // Задаём зелёный цвет (успех)
                        binding.codeSentMessage.setTextColor(
                            ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
                        )
                        binding.codeSentMessage.visibility = View.VISIBLE

                        // Показываем элементы, связанные с вводом кода
                        binding.textCodeInput.visibility = View.VISIBLE
                        binding.codeInput.visibility = View.VISIBLE
                        binding.codeSentMessage2.visibility = View.GONE // Подсказка скрыта изначально

                        // Активируем поле, очищаем его и устанавливаем фокус
                        binding.codeInput.isEnabled = true
                        binding.codeInput.setText("")
                        binding.codeInput.requestFocus()

                        // Показываем подсказку "Код должен содержать 4 символа" через 1 секунду,
                        // но только если поле осталось пустым
                        binding.codeInput.postDelayed({
                            if (isAdded && _binding != null) {
                                val currentCode = binding.codeInput.text?.toString()?.trim()
                                if (currentCode.isNullOrEmpty()) {
                                    binding.codeSentMessage2.visibility = View.VISIBLE
                                }
                            }
                        }, 1000)
                    },
                    onFailure = { _ ->
                        // При ошибке (номер не зарегистрирован) показываем красное сообщение
                        binding.codeSentMessage.text = "Номер телефона не зарегистрирован в компании. Обратитесь в отдел персонала. Или попробуйте ещё раз!"
                        binding.codeSentMessage.setTextColor(
                            ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                        )
                        binding.codeSentMessage.visibility = View.VISIBLE

                        // Скрываем всё, что связано с вводом кода
                        binding.textCodeInput.visibility = View.GONE
                        binding.codeInput.visibility = View.GONE
                        binding.codeSentMessage2.visibility = View.GONE
                        binding.codeInput.isEnabled = false
                        binding.codeInput.setText("")
                    }
                )
            }
        }
    }

    // Метод проверки введённого кода подтверждения
    private fun verifyCode(code: String) {
        // Получаем сырой ввод из поля телефона и оставляем только цифры
        val rawInput = binding.phoneInput.text?.toString() ?: ""
        val phoneNumber = rawInput.replace(Regex("\\D"), "") // только цифры
        // Дополнительная проверка длины и содержимого (защита от гонок)
        if (phoneNumber.length != 10 || !phoneNumber.all { it.isDigit() }) return

        // Запускаем проверку кода в фоновом потоке
        lifecycleScope.launch(Dispatchers.IO) {
            val result = Api.Auth.checkSms("7$phoneNumber", code)

            launch(Dispatchers.Main) {
                // Защита от вызова после уничтожения фрагмента
                if (!isAdded || _binding == null) return@launch

                result.fold(
                    onSuccess = { apiAuthData ->
                        // Сохраняем данные авторизации в ViewModel
                        viewModel.login = phoneNumber
                        viewModel.apiAuthData = apiAuthData
                        // Скрываем все сообщения
                        binding.codeSentMessage.visibility = View.INVISIBLE
                        binding.codeSentMessage2.visibility = View.INVISIBLE
                        // Переход к следующему экрану
                        findNavController().navigate(R.id.action_loginFragment_to_workSitesFragment)
                    },
                    onFailure = { _ ->
                        // При неверном коде очищаем поле и показываем диалог
                        binding.codeInput.setText("")
                        binding.codeSentMessage2.visibility = View.VISIBLE
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Неверный код")
                            .setMessage("Код неправильный, повторите")
                            .setPositiveButton("OK") { _, _ ->
                                binding.codeInput.requestFocus()
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
        binding.codeSentMessage.visibility = View.INVISIBLE
        binding.codeSentMessage2.visibility = View.GONE
        binding.textCodeInput.visibility = View.GONE
        binding.codeInput.visibility = View.GONE
        binding.codeInput.isEnabled = false
        binding.codeInput.setText("")
    }

    // Форматирование номера по маске: " (XXX) XXX-XX-XX"
    // Обратите внимание: первая строка возвращает " " (пробел) для пустого ввода,
    // чтобы избежать полного исчезновения текста и "прыгания" UI.
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
}