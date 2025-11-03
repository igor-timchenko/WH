package ru.contlog.mobile.helper.fragments

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        binding.codeInput.isEnabled = false

        // Слушатель номера телефона
        binding.phoneInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val phone = s?.toString()?.trim() ?: ""
                val isPhoneValid = phone.length == 10 && phone.all { it.isDigit() }

                // 🔑 Активируем поле кода ТОЛЬКО при валидном номере
                binding.codeInput.isEnabled = isPhoneValid

                if (isPhoneValid && !smsRequested) {
                    smsRequested = true
                    requestSmsCode(phone)
                }

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
                if (code.length > 4) {
                    s?.delete(4, code.length)
                    return
                }
                if (code.length == 4 && code.all { it.isDigit() }) {
                    binding.codeSentMessage2.visibility = View.INVISIBLE
                    verifyCode(code)
                } else if (code.isNotEmpty()) {
                    binding.codeSentMessage2.visibility = View.VISIBLE
                }
            }
        })

        // Версия приложения
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            binding.appVersionText.text = "Версия: ${packageInfo.versionName}"
        } catch (e: Exception) {
            binding.appVersionText.text = "Версия: неизвестна"
        }
    }

    private fun requestSmsCode(phoneNumber: String) {
        binding.codeSentMessage.visibility = View.INVISIBLE
        binding.codeSentMessage2.visibility = View.INVISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            val result = Api.Auth.getSms("7$phoneNumber")

            launch(Dispatchers.Main) {
                result.fold(
                    onSuccess = {
                        val formattedPhone = formatPhoneNumber(phoneNumber)
                        binding.codeSentMessage.text = getString(R.string.smsSentTo, formattedPhone)
                        binding.codeSentMessage.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
                        binding.codeSentMessage.visibility = View.VISIBLE
                        binding.codeSentMessage2.visibility = View.VISIBLE

                        binding.codeInput.isEnabled = true
                        binding.codeInput.setText("")
                        binding.codeInput.requestFocus() // ✅ Только при успехе
                    },
                    onFailure = { _ ->
                        binding.codeSentMessage.text = "Номер телефона не зарегистрирован в компании. Обратитесь в отдел персонала. Или попробуйте ещё раз!"
                        binding.codeSentMessage.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
                        binding.codeSentMessage.visibility = View.VISIBLE

                        // ❌ НЕ даём фокус и НЕ активируем ввод кода при ошибке номера
                        binding.codeSentMessage2.visibility = View.INVISIBLE
                        binding.codeInput.isEnabled = false // ← критически важно
                        binding.codeInput.setText("")
                        // НЕ вызываем requestFocus()
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
                        binding.codeSentMessage.visibility = View.INVISIBLE
                        binding.codeSentMessage2.visibility = View.INVISIBLE
                        findNavController().navigate(R.id.action_loginFragment_to_workSitesFragment)
                    },
                    onFailure = { _ ->
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

    private fun resetState() {
        smsRequested = false
        binding.codeSentMessage.visibility = View.INVISIBLE
        binding.codeSentMessage2.visibility = View.INVISIBLE
        binding.codeInput.isEnabled = false
        binding.codeInput.setText("")
    }

    private fun formatPhoneNumber(digits: String): String {
        val clean = digits.take(10)
        return when (clean.length) {
            0 -> ""
            1 -> "+7 ($clean"
            2 -> "+7 ($clean"
            3 -> "+7 ($clean"
            4 -> "+7 (${clean.substring(0, 3)}) ${clean[3]}"
            5 -> "+7 (${clean.substring(0, 3)}) ${clean.substring(3, 5)}"
            6 -> "+7 (${clean.substring(0, 3)}) ${clean.substring(3, 6)}"
            7 -> "+7 (${clean.substring(0, 3)}) ${clean.substring(3, 6)}-${clean[6]}"
            8 -> "+7 (${clean.substring(0, 3)}) ${clean.substring(3, 6)}-${clean.substring(6, 8)}"
            9 -> "+7 (${clean.substring(0, 3)}) ${clean.substring(3, 6)}-${clean.substring(6, 8)}-${clean[8]}"
            10 -> "+7 (${clean.substring(0, 3)}) ${clean.substring(3, 6)}-${clean.substring(6, 8)}-${clean.substring(8, 10)}"
            else -> "+7 (${clean.substring(0, 3)}) ${clean.substring(3, 6)}-${clean.substring(6, 8)}-${clean.substring(8, 10)}"
        }
    }
}