package ru.contlog.mobile.helper.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels // Общий ViewModel для всей активности
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.contlog.mobile.helper.R
import ru.contlog.mobile.helper.utils.TextInputListener
import ru.contlog.mobile.helper.databinding.FragmentLoginBinding
import ru.contlog.mobile.helper.repo.Api
import ru.contlog.mobile.helper.vm.AppViewModel

class LoginFragment : Fragment() {
    private lateinit var binding: FragmentLoginBinding

    // Используется activity-scoped ViewModel, чтобы данные (логин, токен) сохранялись
    // при переходе между фрагментами в рамках одной активности.
    private val viewModel: AppViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Инициализация ViewBinding — современный способ доступа к UI-элементам без findViewById.
        binding = FragmentLoginBinding.inflate(inflater)

        // Настройка слушателей и начального состояния UI.
        bind()

        return binding.root
    }

    private fun bind() {
        // Слушатель изменения номера телефона: включает кнопку "Получить код",
        // только если введено ровно 10 цифр.
        binding.phoneInput.addTextChangedListener(TextInputListener { s ->
            val phoneOk = s != null && Regex("\\d{10}").matches(s)
            binding.getCodeButton.isEnabled = phoneOk
        })

        // Слушатель ввода кода: включает кнопку "Проверить", если поле не пустое.
        binding.codeInput.addTextChangedListener(TextInputListener { s ->
            binding.checkCodeButton.isEnabled = !s.isNullOrBlank()
        })

        // Обработка нажатия "Получить код"
        binding.getCodeButton.setOnClickListener {
            getCode()
        }

        // Обработка нажатия "Проверить код"
        binding.checkCodeButton.setOnClickListener {
            checkCode()
        }

        // Подгружаем сохранённый номер из ViewModel (на случай поворота экрана или возврата).
        binding.phoneInput.setText(viewModel.login)
    }

    private fun getCode() {
        // Скрываем сообщение "Код отправлен", если оно было показано ранее.
        binding.codeSentMessage.visibility = View.INVISIBLE

        val phoneNumber = binding.phoneInput.text.toString().trim()

        // Проверка длины номера (должно быть 10 цифр).
        if (phoneNumber.length != 10) {
            Toast.makeText(requireContext(), "Введите 10 цифр номера телефона", Toast.LENGTH_SHORT).show()
            return
        }

        // Запуск сетевого запроса в фоновом потоке (IO)
        lifecycleScope.launch(Dispatchers.IO) {
            val result = Api.Auth.getSms("7$phoneNumber") // Добавляем код страны "7" (Россия/Казахстан)

            // Возвращаемся в главный поток для обновления UI
            launch(Dispatchers.Main) {
                result.fold(
                    onSuccess = {
                        // Успешная отправка SMS → показываем сообщение
                        binding.codeSentMessage.visibility = View.VISIBLE
                    },
                    onFailure = { throwable ->
                        // Ошибка → показываем диалог с сообщением
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.errorSendingSMS))
                            .setMessage(throwable.message ?: "Неизвестная ошибка")
                            .setPositiveButton(getString(R.string.ok), null)
                            .show()
                    }
                )
            }
        }
    }

    private fun checkCode() {
        val phoneNumber = binding.phoneInput.text.toString().trim()
        val code = binding.codeInput.text.toString().trim()

        // Валидация ввода
        if (phoneNumber.length != 10) {
            Toast.makeText(requireContext(), "Введите 10 цифр номера телефона", Toast.LENGTH_SHORT).show()
            return
        }
        if (code.isEmpty()) {
            Toast.makeText(requireContext(), "Введите код", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверка кода на сервере
        lifecycleScope.launch(Dispatchers.IO) {
            val result = Api.Auth.checkSms("7$phoneNumber", code)

            launch(Dispatchers.Main) {
                result.fold(
                    onSuccess = { apiAuthData ->
                        // Сохраняем данные авторизации в ViewModel (доступны всей активности)
                        viewModel.login = phoneNumber
                        viewModel.apiAuthData = apiAuthData

                        // Переход к следующему экрану (очистка стека сделана в nav_graph.xml)
                        findNavController().navigate(R.id.action_loginFragment_to_workSitesFragment)
                    },
                    onFailure = { throwable ->
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.errorCheckingSMS))
                            .setMessage(throwable.message ?: "Неверный код или ошибка сети")
                            .setPositiveButton(getString(R.string.ok), null)
                            .show()
                    }
                )
            }
        }
    }
}