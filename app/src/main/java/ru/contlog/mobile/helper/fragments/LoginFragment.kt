package ru.contlog.mobile.helper.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
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

    @SuppressLint("SetTextI18n")
    private fun bind() {
        binding.TextCodeInput.visibility = View.GONE
        binding.CodeInput.visibility = View.GONE
        binding.CodeSentMessage.visibility = View.GONE
        binding.CodeInput.isEnabled = false

        binding.PhoneInput.addTextChangedListener(object : android.text.TextWatcher {
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
                binding.PhoneInput.setSelection(formatted.length)
                isFormatting = false

                val isPhoneValid = clean.length == 10
                binding.CodeInput.isEnabled = isPhoneValid

                if (isPhoneValid && !smsRequested) {
                    smsRequested = true
                    requestSmsCode(clean)
                }

                if (clean.length < 10 && smsRequested) {
                    resetState()
                }
            }
        })

        binding.CodeInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val code = s?.toString()?.trim() ?: ""
                if (code.length > 5) {
                    s?.delete(5, code.length)
                    return
                }
                if (code.length == 5 && code.all { it.isDigit() }) {
                    binding.CodeSentMessage.visibility = View.INVISIBLE
                    verifyCode(code)
                } else if (code.isNotEmpty()) {
                    binding.CodeSentMessage.visibility = View.VISIBLE
                }
            }
        })

        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            val version = packageInfo.versionName ?: "неизвестна"
            binding.appVersionText.text = "Версия: $version"
        } catch (e: Exception) {
            binding.appVersionText.text = "Версия: неизвестна"
        }
    }

    private fun requestSmsCode(phoneNumber: String) {
        binding.PhoneSentMessage.visibility = View.INVISIBLE

        // 🔌 Проверка интернета
        if (!isNetworkAvailable()) {
            binding.PhoneSentMessage.text = "Проверьте интернет соединение"
            binding.PhoneSentMessage.setTextColor(
                ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
            )
            binding.PhoneSentMessage.visibility = View.VISIBLE
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val result = Api.Auth.getSms("7$phoneNumber")

            launch(Dispatchers.Main) {
                if (!isAdded || _binding == null) return@launch

                result.fold(
                    onSuccess = {
                        val formattedPhone = formatPhoneNumber(phoneNumber)
                        binding.PhoneSentMessage.text = getString(R.string.smsSentTo, formattedPhone)
                        binding.PhoneSentMessage.setTextColor(
                            ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
                        )
                        binding.PhoneSentMessage.visibility = View.VISIBLE

                        binding.TextCodeInput.visibility = View.VISIBLE
                        binding.CodeInput.visibility = View.VISIBLE
                        binding.CodeSentMessage.visibility = View.GONE

                        binding.CodeInput.isEnabled = true
                        binding.CodeInput.setText("")
                        binding.CodeInput.requestFocus()

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
                        val formattedPhone = formatPhoneNumber(phoneNumber)
                        binding.PhoneSentMessage.text = getString(R.string.error_user_not_found, formattedPhone)
                        binding.PhoneSentMessage.setTextColor(
                            ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                        )
                        binding.PhoneSentMessage.visibility = View.VISIBLE

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

    private fun verifyCode(code: String) {
        // 🔌 Проверка интернета при отправке кода
        if (!isNetworkAvailable()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Нет соединения")
                .setMessage("Проверьте интернет соединение")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val rawInput = binding.PhoneInput.text?.toString() ?: ""
        val phoneNumber = rawInput.replace(Regex("\\D"), "")
        if (phoneNumber.length != 10 || !phoneNumber.all { it.isDigit() }) return

        lifecycleScope.launch(Dispatchers.IO) {
            val result = Api.Auth.checkSms("7$phoneNumber", code)

            launch(Dispatchers.Main) {
                if (!isAdded || _binding == null) return@launch

                result.fold(
                    onSuccess = { apiAuthData ->
                        viewModel.login = phoneNumber
                        viewModel.apiAuthData = apiAuthData
                        binding.PhoneSentMessage.visibility = View.INVISIBLE
                        binding.CodeSentMessage.visibility = View.INVISIBLE
                        findNavController().navigate(R.id.action_loginFragment_to_workSitesFragment)
                    },
                    onFailure = { _ ->
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

    private fun resetState() {
        smsRequested = false
        binding.PhoneSentMessage.visibility = View.INVISIBLE
        binding.CodeSentMessage.visibility = View.GONE
        binding.TextCodeInput.visibility = View.GONE
        binding.CodeInput.visibility = View.GONE
        binding.CodeInput.isEnabled = false
        binding.CodeInput.setText("")
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

    // 🔍 Проверка наличия интернет-соединения
    @SuppressLint("MissingPermission", "ObsoleteSdkInt")
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected ?: false
        }
    }
}