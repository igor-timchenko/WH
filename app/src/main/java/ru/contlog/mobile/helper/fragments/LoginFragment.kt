package ru.contlog.mobile.helper.fragments

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
import ru.contlog.mobile.helper.R
import ru.contlog.mobile.helper.TextInputListener
import ru.contlog.mobile.helper.databinding.FragmentLoginBinding
import ru.contlog.mobile.helper.repo.Api
import ru.contlog.mobile.helper.repo.AppPreferencesRepository
import ru.contlog.mobile.helper.vm.AppViewModel
import ru.contlog.mobile.helper.vm.factories.AppViewModelFactory

class LoginFragment : Fragment() {
    private lateinit var binding: FragmentLoginBinding

    private val viewModel: AppViewModel by viewModels {
        AppViewModelFactory(AppPreferencesRepository(this.requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(inflater)

        bind()

        return binding.root
    }

    private fun bind() {
        binding.phoneInput.addTextChangedListener(TextInputListener { s ->
            var phoneOk = true

            if (s == null) {
                phoneOk = false
            } else {
                val phonePattern = Regex("\\d{10}")
                if (!phonePattern.matches(s)) {
                    phoneOk = false
                }
            }

            binding.getCodeButton.isEnabled = phoneOk
        })

        binding.codeInput.addTextChangedListener(TextInputListener { s ->
            binding.checkCodeButton.isEnabled = s != null
        })

        binding.getCodeButton.setOnClickListener {
            getCode()
        }

        binding.checkCodeButton.setOnClickListener {
            checkCode()
        }

        binding.phoneInput.setText(viewModel.login)
    }

    private fun getCode() {
        binding.codeSentMessage.visibility = View.INVISIBLE
        val phoneNumber = binding.phoneInput.text.toString().trim()

        if (phoneNumber.length != 10) {
            Toast.makeText(requireContext(), "Введите 10 цифр номера телефона", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val result = Api.Auth.getSms("7${phoneNumber}")
            launch(Dispatchers.Main) {
                result.fold(
                    {
                        binding.codeSentMessage.visibility = View.VISIBLE
                    },
                    { tr ->
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

    private fun checkCode() {
        val phoneNumber = binding.phoneInput.text.toString().trim()
        val code = binding.codeInput.text.toString().trim()

        if (phoneNumber.length != 10) {
            Toast.makeText(requireContext(), "Введите 10 цифр номера телефона", Toast.LENGTH_SHORT).show()
            return
        }

        if (code.isEmpty()) {
            Toast.makeText(requireContext(), "Введите код", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val result = Api.Auth.checkSms("7${phoneNumber}", code)
            launch(Dispatchers.Main) {
                result.fold(
                    { apiAuthData ->
                        viewModel.login = phoneNumber
                        viewModel.apiAuthData = apiAuthData
                        findNavController().navigate(R.id.action_loginFragment_to_workSitesFragment)
                    },
                    { tr ->
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