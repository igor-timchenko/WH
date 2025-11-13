package ru.contlog.mobile.helper.fragments

import android.Manifest
import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.contlog.mobile.helper.R
import ru.contlog.mobile.helper.databinding.FragmentLoginBinding
import ru.contlog.mobile.helper.repo.Api
import ru.contlog.mobile.helper.vm.AppViewModel
import kotlin.math.hypot

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppViewModel by activityViewModels()
    private var smsRequested = false
    private var smsRetrieved = false

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
        startSmsRetriever()

        // ✅ Запрашиваем разрешение на чтение SMS
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_SMS), 1001)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // ✅ Запускаем проверку SMS
            binding.CodeInput.postDelayed({
                checkSmsForCode()
            }, 2000)

            // Повторяем проверку каждые 3 секунды
            binding.CodeInput.postDelayed(object : Runnable {
                override fun run() {
                    if (!smsRetrieved) {
                        checkSmsForCode()
                        binding.CodeInput.postDelayed(this, 3000)
                    }
                }
            }, 5000)
        }
    }

    // ✅ Проверяем SMS вручную
    private fun checkSmsForCode() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            val uri = android.net.Uri.parse("content://sms/inbox")
            val contentResolver = requireContext().contentResolver
            val cursor = contentResolver.query(
                uri,
                arrayOf("address", "body", "date"),
                null, // фильтр по отправителю можно добавить
                null,
                "date DESC"
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val body = it.getString(it.getColumnIndexOrThrow("body"))
                    // Проверяем формат: [#] Ваш код подтверждения:\n12345\n/hexCode
                    if (body.startsWith("[#]")) {
                        val lines = body.split("\n")
                        if (lines.size >= 2) {
                            val codeLine = lines[1].trim() // "92406"
                            if (codeLine.length == 5 && codeLine.all { it.isDigit() }) {
                                onSmsCodeReceived(codeLine)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading SMS: ${e.message}", e)
        }
    }

    // ✅ Запуск SMS Retriever (для совместимости)
    private fun startSmsRetriever() {
        val client = SmsRetriever.getClient(requireActivity())
        val task = client.startSmsRetriever()

        task.addOnSuccessListener {
            Log.d(TAG, "SMS Retriever started successfully.")
        }

        task.addOnFailureListener { e ->
            Log.e(TAG, "Failed to start SMS Retriever: ${e.message}")
        }
    }

    // ✅ Метод для вызова из Activity при получении SMS
    fun onSmsCodeReceived(code: String) {
        if (smsRetrieved) return // Защита от повторного вызова
        smsRetrieved = true

        binding.CodeInput.setText(code) // ← Вот тут вставляется "92406"
        binding.CodeInput.setSelection(code.length) // Курсор в конец
        binding.CodeSentMessage.visibility = View.INVISIBLE
        verifyCode(code) // Автоматически проверить
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
                revealGetAuthCodeButton(show = isPhoneValid)

                if (clean.length < 10 && smsRequested) {
                    resetState()
                }
            }
        })

        binding.CodeInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                // ✅ Защита от ввода после получения SMS
                if (smsRetrieved) return

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

        binding.getAuthCode.setOnClickListener {
            val digitsOnly = binding.PhoneInput.text.toString().replace(Regex("\\D"), "")
            if (digitsOnly.length == 10 && !smsRequested) {
                smsRequested = true
                requestSmsCode(digitsOnly)
            }
        }
    }

    @SuppressLint("InlinedApi")
    private fun requestSmsCode(phoneNumber: String) {
        binding.PhoneSentMessage.visibility = View.INVISIBLE

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

                        binding.PhoneInput.isEnabled = false
                        binding.TextCodeInput.visibility = View.VISIBLE
                        binding.CodeInput.visibility = View.VISIBLE
                        binding.CodeSentMessage.visibility = View.GONE

                        binding.CodeInput.isEnabled = true
                        binding.CodeInput.setText("")
                        binding.CodeInput.requestFocus()

                        binding.CodeInput.postDelayed({
                            if (isAdded && _binding != null) {
                                val currentCode = binding.CodeInput.text?.toString()?.trim()
                                if (currentCode.isNullOrEmpty() && !smsRetrieved) {
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
                        binding.CodeInput.isEnabled = false
                        binding.CodeInput.setText("")
                        smsRequested = false
                        binding.PhoneInput.isEnabled = true
                    }
                )
            }
        }
    }

    private fun verifyCode(code: String) {
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
                        // ✅ Сбрасываем флаг после успешной проверки
                        smsRetrieved = false
                        viewModel.login = phoneNumber
                        viewModel.apiAuthData = apiAuthData
                        binding.PhoneSentMessage.visibility = View.INVISIBLE
                        binding.CodeSentMessage.visibility = View.INVISIBLE
                        findNavController().navigate(R.id.action_loginFragment_to_workSitesFragment)
                        Toast.makeText(requireContext(), "Авторизация успешна!!!", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { _ ->
                        // ✅ Сбрасываем флаг при ошибке
                        smsRetrieved = false
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
        binding.PhoneInput.isEnabled = true

        // ✅ Сброс флага при сбросе состояния
        smsRetrieved = false

        revealGetAuthCodeButton(show = false)
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

    companion object {
        const val TAG = "Contlog.LoginFragment"
    }
}
