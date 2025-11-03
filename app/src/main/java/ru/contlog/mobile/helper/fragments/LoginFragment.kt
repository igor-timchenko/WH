package ru.contlog.mobile.helper.fragments

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppViewModel by activityViewModels()
    private var smsRequested = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bind() {
        // Скрываем всё, что связано с кодом
        binding.textCodeInput.visibility = View.GONE
        binding.codeInput.visibility = View.GONE
        binding.codeSentMessage2.visibility = View.GONE
        binding.codeInput.isEnabled = false

        // === Слушатель номера телефона с маской (913) 440-89-04 ===
        binding.phoneInput.addTextChangedListener(object : android.text.TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return

                val digitsOnly = s.toString().replace(Regex("\\D"), "")
                val clean = if (digitsOnly.length > 10) digitsOnly.substring(0, 10) else digitsOnly
                val formatted = formatPhoneNumber(clean)

                isFormatting = true
                s.replace(0, s.length, formatted)
                binding.phoneInput.setSelection(formatted.length)
                isFormatting = false

                val isPhoneValid = clean.length == 10
                binding.codeInput.isEnabled = isPhoneValid

                if (isPhoneValid && !smsRequested) {
                    smsRequested = true
                    requestSmsCode(clean)
                }

                if (clean.length < 10 && smsRequested) {
                    resetState()
                }
            }
        })

        // === Слушатель кода подтверждения ===
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

        // === Версия приложения ===
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            val version = packageInfo.versionName ?: "неизвестна"
            binding.appVersionText.text = "Версия: $version"
        } catch (e: Exception) {
            binding.appVersionText.text = "Версия: неизвестна"
        }
    }

    private fun requestSmsCode(phoneNumber: String) {
        binding.codeSentMessage.visibility = View.INVISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            val result = Api.Auth.getSms("7$phoneNumber")

            launch(Dispatchers.Main) {
                if (!isAdded || _binding == null) return@launch

                result.fold(
                    onSuccess = {
                        val formattedPhone = formatPhoneNumber(phoneNumber)
                        binding.codeSentMessage.text = getString(R.string.smsSentTo, formattedPhone)
                        binding.codeSentMessage.setTextColor(
                            ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
                        )
                        binding.codeSentMessage.visibility = View.VISIBLE

                        // Показываем элементы кода
                        binding.textCodeInput.visibility = View.VISIBLE
                        binding.codeInput.visibility = View.VISIBLE
                        binding.codeSentMessage2.visibility = View.GONE // скрыто сначала

                        binding.codeInput.isEnabled = true
                        binding.codeInput.setText("")
                        binding.codeInput.requestFocus()

                        // Показываем подсказку через 1 секунду, если поле пустое
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
                        binding.codeSentMessage.text = "Номер телефона не зарегистрирован в компании. Обратитесь в отдел персонала. Или попробуйте ещё раз!"
                        binding.codeSentMessage.setTextColor(
                            ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                        )
                        binding.codeSentMessage.visibility = View.VISIBLE

                        // При ошибке — скрываем всё
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

    private fun verifyCode(code: String) {
        val rawInput = binding.phoneInput.text?.toString() ?: ""
        val phoneNumber = rawInput.replace(Regex("\\D"), "") // только цифры
        if (phoneNumber.length != 10 || !phoneNumber.all { it.isDigit() }) return

        lifecycleScope.launch(Dispatchers.IO) {
            val result = Api.Auth.checkSms("7$phoneNumber", code)

            launch(Dispatchers.Main) {
                if (!isAdded || _binding == null) return@launch

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
        binding.codeSentMessage2.visibility = View.GONE
        binding.textCodeInput.visibility = View.GONE
        binding.codeInput.visibility = View.GONE
        binding.codeInput.isEnabled = false
        binding.codeInput.setText("")
    }

    private fun formatPhoneNumber(digits: String): String {
        val clean = digits.take(10)
        return when (clean.length) {
            0 -> " "
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