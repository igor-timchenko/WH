package ru.contlog.mobile.helper.fragments

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import ru.contlog.mobile.helper.R
import ru.contlog.mobile.helper.databinding.FragmentDivisionsListBinding
import ru.contlog.mobile.helper.databinding.FragmentProfileBinding
import ru.contlog.mobile.helper.repo.AppPreferencesRepository
import ru.contlog.mobile.helper.rvadapters.DivisionsRVAdapter
import ru.contlog.mobile.helper.vm.AppViewModel
import ru.contlog.mobile.helper.vm.factories.AppViewModelFactory
import kotlin.getValue

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding

    private val viewModel: AppViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.userData.observe(viewLifecycleOwner) { userData ->
            if (userData == null) {
                binding.userInfoCard.visibility = View.GONE
                return@observe
            }

            binding.fullName.text = userData.name
            binding.position.text = userData.position

            lifecycleScope.launch(Dispatchers.Default) {
                val bytes = Base64.decode(userData.photo, Base64.DEFAULT)
                val decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                launch(Dispatchers.Main) {
                    binding.userAvatar.setImageBitmap(decodedBitmap)
                }
            }

            binding.userInfoCard.visibility = View.VISIBLE
        }

        binding.refresh.setOnRefreshListener {
            getData()
        }

        binding.logout.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
            viewModel.logout()
        }
    }

    private fun getData() {
        viewModel.clearErrors()
        lifecycleScope.launch(Dispatchers.IO) {
            launch(Dispatchers.Main) {
                binding.refresh.isRefreshing = true
            }
            awaitAll(
                async {
                    viewModel.fetchUserData()
                },
                async {
                    viewModel.fetchDivisions()
                }
            )
            launch(Dispatchers.Main) {
                binding.refresh.isRefreshing = false
            }
        }
    }
}