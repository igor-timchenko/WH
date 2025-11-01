package ru.contlog.mobile.helper.fragments

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.contlog.mobile.helper.R
import ru.contlog.mobile.helper.databinding.FragmentLoginBinding
import ru.contlog.mobile.helper.repo.Api
import ru.contlog.mobile.helper.vm.AppViewModel

class LoginFragment : Fragment() {
    private lateinit var binding: FragmentLoginBinding
    private val viewModel: AppViewModel by activityViewModels()

    // Флаг: SMS уже запрашивался (чтобы не отправлять 10 раз при вводе)
    private var smsRequested = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater)
        bind()
        return binding.root
    }

    private fun bind() {
        // Поле кода изначально заблокировано
        binding.codeInput.isEnabled = false

        // Слушатель номера телефона
        binding.phoneInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val phone = s?.toString()?.trim() ?: ""
                val isPhoneValid = phone.length == 10 && phone.all { it.isDigit() }


                if (isPhoneValid && !smsRequested) {
                    smsRequested = true
                    requestSmsCode(phone)
                }

                // Если пользователь удаляет цифры — сбрасываем состояние
                if (phone.length < 10 && smsRequested) {
                    resetState()
                }
            }
        })

        // Слушатель кода
        binding.codeInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val code = s?.toString()?.trim() ?: ""
                if (code.length == 4 && code.all { it.isDigit() }) {
                    verifyCode(code)
                }
            }
        })
    }

    private fun requestSmsCode(phoneNumber: String) {
        binding.codeSentMessage.visibility = View.INVISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            val result = Api.Auth.getSms("7$phoneNumber")

            launch(Dispatchers.Main) {
                result.fold(
                    onSuccess = {
                        val formattedPhone = formatPhoneNumber("7$phoneNumber")
                        binding.codeSentMessage.text = getString(R.string.smsSentTo, formattedPhone)
                        binding.codeSentMessage.visibility = View.VISIBLE
                        binding.codeInput.isEnabled = true
                        binding.codeInput.requestFocus()
                    },
                    onFailure = { throwable ->
                        // Показываем ошибку В ТОМ ЖЕ TextView, что и сообщение об отправке
                        binding.codeSentMessage.text = "Номер телефона не заригистрирован в компании, обратить в отдел персонала"
                        binding.codeSentMessage.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
                        binding.codeSentMessage.visibility = View.VISIBLE

                        binding.codeInput.setText("")
                        binding.codeInput.requestFocus()
                    }
                )
            }
        }
    }

    private fun verifyCode(code: String) {
        val phoneNumber = binding.phoneInput.text.toString().trim()
        if (phoneNumber.length != 10 || !phoneNumber.all { it.isDigit() }) return

        lifecycleScope.launch(Dispatchers.IO) {
            val result = Api.Auth.checkSms("7$phoneNumber", code)

            launch(Dispatchers.Main) {
                result.fold(
                    onSuccess = { apiAuthData ->
                        viewModel.login = phoneNumber
                        viewModel.apiAuthData = apiAuthData
                        findNavController().navigate(R.id.action_loginFragment_to_workSitesFragment)
                    },
                    onFailure = { throwable ->
                        // Ошибка кода — очищаем поле и даём ввести заново
                        binding.codeInput.setText("")
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

    private fun resetState() {
        smsRequested = false
        binding.codeSentMessage.visibility = View.INVISIBLE
        binding.codeInput.isEnabled = false
        binding.codeInput.setText("")
    }

    // ✅ ИСПРАВЛЕННАЯ МАСКА: +7 (XXX) XXX-XX-XX
    private fun formatPhoneNumber(raw: String): String {
        val digits = raw.replace(Regex("\\D"), "")
        val trimmed = if (digits.length > 11) digits.substring(0, 11) else digits

        return when (trimmed.length) {
            0 -> ""
            1 -> "+$trimmed"
            2 -> "+$trimmed "
            3 -> "+${trimmed.take(1)} (${trimmed.substring(1)}"
            4 -> "+${trimmed.take(1)} (${trimmed.substring(1, 4)}"
            5 -> "+${trimmed.take(1)} (${trimmed.substring(1, 4)}) ${trimmed[4]}"
            6 -> "+${trimmed.take(1)} (${trimmed.substring(1, 4)}) ${trimmed.substring(4, 7)}"
            7 -> "+${trimmed.take(1)} (${trimmed.substring(1, 4)}) ${trimmed.substring(4, 7)}-${trimmed[7]}"
            8 -> "+${trimmed.take(1)} (${trimmed.substring(1, 4)}) ${trimmed.substring(4, 7)}-${trimmed.substring(7, 9)}"
            9 -> "+${trimmed.take(1)} (${trimmed.substring(1, 4)}) ${trimmed.substring(4, 7)}-${trimmed.substring(7, 9)}-${trimmed[9]}"
            10 -> "+${trimmed.take(1)} (${trimmed.substring(1, 4)}) ${trimmed.substring(4, 7)}-${trimmed.substring(7, 9)}-${trimmed.substring(9, 11)}"
            11 -> "+${trimmed.take(1)} (${trimmed.substring(1, 4)}) ${trimmed.substring(4, 7)}-${trimmed.substring(7, 9)}-${trimmed.substring(9, 11)}"
            else -> trimmed
        }
    }
}