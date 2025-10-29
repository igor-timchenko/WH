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
import ru.contlog.mobile.helper.repo.AppPreferencesRepository
import ru.contlog.mobile.helper.rvadapters.DivisionsRVAdapter
import ru.contlog.mobile.helper.vm.AppViewModel
import ru.contlog.mobile.helper.vm.factories.AppViewModelFactory

class DivisionsListFragment : Fragment() {
    private lateinit var binding: FragmentDivisionsListBinding

    private val viewModel: AppViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDivisionsListBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.i("asdf1", "onViewCreated: ${viewModel.userData.value}")

        binding.bottomNavigation.setOnItemSelectedListener { mi ->
            when (mi.itemId) {
                R.id.mHome -> {}
                R.id.mProfile -> {
                    findNavController().navigate(R.id.action_workSitesFragment_to_profileFragment)
                }
            }

            true
        }

        viewModel.errors.observe(viewLifecycleOwner) { errors ->
            if (errors.isEmpty()) {
                binding.errorsCard.visibility = View.GONE
                binding.errorsText.text = ""
                return@observe
            }

            val errorMessage = errors.mapIndexed { i, e ->
                "${i+1}. ${e.message ?: e}"
            }.joinToString("\n")
            binding.errorsText.text = errorMessage
            binding.errorsCard.visibility = View.VISIBLE

            binding.divisionsList.visibility = View.GONE
            binding.divisionsListEmptyInfo.visibility = View.GONE
        }

        val adapter = DivisionsRVAdapter { division ->
            val bundle = bundleOf("division" to division)
            findNavController().navigate(R.id.action_workSitesFragment_to_productInfoFragment, bundle)
        }
        viewModel.division.observe(viewLifecycleOwner) { divisions ->
            adapter.setData(divisions)

            binding.divisionsListEmptyInfo.visibility = if (divisions.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
            binding.divisionsList.visibility = if (divisions.isNotEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        binding.divisionsList.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        binding.divisionsList.adapter = adapter

        binding.refresh.setOnRefreshListener {
            getData()
        }
        binding.root.post {
            getData()
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