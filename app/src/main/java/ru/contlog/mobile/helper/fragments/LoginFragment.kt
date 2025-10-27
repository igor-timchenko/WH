// Пакет, в котором находится фрагмент входа в приложение
package ru.contlog.mobile.helper.fragments

// Стандартные импорты Android и библиотек
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.contlog.mobile.helper.BuildConfig
import ru.contlog.mobile.helper.R
import ru.contlog.mobile.helper.TextInputListener
import ru.contlog.mobile.helper.databinding.FragmentLoginBinding
import ru.contlog.mobile.helper.repo.Api
import ru.contlog.mobile.helper.repo.AppPreferencesRepository
import ru.contlog.mobile.helper.vm.AppViewModel
import ru.contlog.mobile.helper.vm.factories.AppViewModelFactory

/**
 * Фрагмент экрана входа по SMS:
 * - Пользователь вводит номер телефона (10 цифр),
 * - Нажимает "Получить код" → отправляется SMS,
 * - Вводит полученный код → происходит аутентификация,
 * - При успехе — переход на следующий экран.
 */
class LoginFragment : Fragment() {

    // ViewBinding для удобной работы с UI-элементами
    private lateinit var binding: FragmentLoginBinding

    // ViewModel для хранения состояния (например, номера телефона между пересозданиями)
    private val viewModel: AppViewModel by viewModels {
        AppViewModelFactory(AppPreferencesRepository(this.requireContext()))
    }

    /**
     * Создаёт корневой View фрагмента и инициализирует привязку.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(inflater)

        bind()          // Настраиваем обработчики событий и начальное состояние

        return binding.root
    }

    /**
     * Настраивает взаимодействие с элементами интерфейса.
     */
    private fun bind() {

        // === Валидация номера телефона ===
        binding.phoneInput.addTextChangedListener(TextInputListener { s ->
            var phoneOk = true

            // Проверяем, что строка не null и соответствует формату 10 цифр
            if (s == null) {
                phoneOk = false
            } else {
                val phonePattern = Regex("\\d{10}")
                if (!phonePattern.matches(s)) {
                    phoneOk = false
                }
            }

            // Кнопка "Получить код" активна только при корректном номере
            binding.getCodeButton.isEnabled = phoneOk
        })

        // === Валидация SMS-кода ===
        binding.codeInput.addTextChangedListener(TextInputListener { s ->
            // Кнопка проверки кода активна, если поле не пустое
            binding.checkCodeButton.isEnabled = s != null
        })

        // === Обработка нажатия "Получить код" ===
        binding.getCodeButton.setOnClickListener {
            getCode()
        }

        // === Обработка нажатия "Проверить код" ===
        binding.checkCodeButton.setOnClickListener {
            checkCode()
        }

        // === Восстановление сохранённого номера (если был) ===
        binding.phoneInput.setText(viewModel.login)

        // === Отображение версии приложения ===
        binding.versionLabel.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)
    }

    /**
     * Отправка запроса на получение SMS-кода.
     */
    private fun getCode() {
        // Скрываем сообщение "Код отправлен", если оно было показано ранее
        binding.codeSentMessage.visibility = View.INVISIBLE
        val phoneNumber = binding.phoneInput.text.toString().trim()

        // Дополнительная проверка длины (на случай, если валидация сбилась)
        if (phoneNumber.length != 10) {
            Toast.makeText(requireContext(), "Введите 10 цифр номера телефона", Toast.LENGTH_SHORT).show()
            return
        }

        // Выполняем сетевой запрос в фоновом потоке
        lifecycleScope.launch(Dispatchers.IO) {

            // Формируем полный номер: +7 + 10 цифр → "7" + phoneNumber
            val result = Api.Auth.getSms("7${phoneNumber}")

            // Обрабатываем результат в основном потоке (UI)
            launch(Dispatchers.Main) {
                result.fold( // Успех: SMS отправлена
                    {
                        binding.codeSentMessage.visibility = View.VISIBLE
                    },
                    { tr ->     // Ошибка
                        MaterialAlertDialogBuilder(this@LoginFragment.requireContext())
                            .setTitle(getString(R.string.errorSendingSMS))
                            .setMessage(tr.message)
                            .setPositiveButton(getString(R.string.ok), null)
                            .show()
                    }
                )
            }
        }
    }

    /**
     * Проверка введённого SMS-кода.
     */
    private fun checkCode() {
        val phoneNumber = binding.phoneInput.text.toString().trim()
        val code = binding.codeInput.text.toString().trim()

        // Валидация на клиенте
        if (phoneNumber.length != 10) {
            Toast.makeText(requireContext(), "Введите 10 цифр номера телефона", Toast.LENGTH_SHORT).show()
            return
        }

        if (code.isEmpty()) {
            Toast.makeText(requireContext(), "Введите код", Toast.LENGTH_SHORT).show()
            return
        }

        // Выполняем запрос проверки кода в фоне
        lifecycleScope.launch(Dispatchers.IO) {
            val result = Api.Auth.checkSms("7${phoneNumber}", code)
            launch(Dispatchers.Main) {
                result.fold(
                    { apiAuthData ->     // Успешная аутентификация
                        // Сохраняем номер и данные авторизации в ViewModel
                        viewModel.login = phoneNumber
                        viewModel.apiAuthData = apiAuthData

                        // Переход к следующему экрану (списку подразделений)
                        findNavController().navigate(R.id.action_loginFragment_to_workSitesFragment)
                    },
                    { tr ->    // Ошибка проверки кода
                        MaterialAlertDialogBuilder(this@LoginFragment.requireContext())
                            .setTitle(getString(R.string.errorCheckingSMS))
                            .setMessage(tr.message)
                            .setPositiveButton(getString(R.string.ok), null)
                            .show()
                    }
                )
            }
        }
    }
}