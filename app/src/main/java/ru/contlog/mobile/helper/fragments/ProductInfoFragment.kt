package ru.contlog.mobile.helper.fragments

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import ru.contlog.mobile.helper.R
import ru.contlog.mobile.helper.databinding.FragmentProductInfoBinding
import ru.contlog.mobile.helper.model.Division
import ru.contlog.mobile.helper.model.Product
import ru.contlog.mobile.helper.model.ProductPlace
import ru.contlog.mobile.helper.repo.AppPreferencesRepository
import ru.contlog.mobile.helper.rvadapters.ProductsRVAdapter
import ru.contlog.mobile.helper.utils.CustomLinearLayoutManager
import ru.contlog.mobile.helper.vm.AppViewModel
import ru.contlog.mobile.helper.vm.ProductInfoViewModel
import ru.contlog.mobile.helper.vm.factories.AppViewModelFactory


class ProductInfoFragment : Fragment() {
    private lateinit var binding: FragmentProductInfoBinding

    private val viewModel: AppViewModel by viewModels {
        AppViewModelFactory(AppPreferencesRepository(this.requireContext()))
    }

    private val productViewModel: ProductInfoViewModel by viewModels()

    private val barcodeLauncher = registerForActivityResult<ScanOptions?, ScanIntentResult?>(
        ScanContract(),
        ActivityResultCallback { result: ScanIntentResult? ->
            Log.i("ScanIntentResult", "$result")
            if (result!!.contents != null) {
                val code = result.contents
                productViewModel.setScannedCode(code)
                loadData(code)
            }
        })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProductInfoBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        productViewModel.setDivision(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireArguments().getSerializable("division", Division::class.java)
            } else {
                requireArguments().getSerializable("division") as Division
            }!!
        )

        binding.scan.setOnClickListener { doScan() }

        binding.productsList.layoutManager = CustomLinearLayoutManager(
            requireContext()
        )

        val adapter = ProductsRVAdapter { enable ->
            (binding.productsList.layoutManager as CustomLinearLayoutManager).isScrollEnabled = enable
        }
        productViewModel.products.observe(viewLifecycleOwner) { products ->
            if (products == null) {
                adapter.setData(emptyList())
                binding.productsListEmptyInfo.visibility = View.GONE
                return@observe
            }
            adapter.setData(products)
            binding.productsListEmptyInfo.visibility = if (products.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        productViewModel.scannedCode.observe(viewLifecycleOwner) { scannedCode ->
            if (scannedCode == null) {
                binding.scannedLabel.text = ""
                binding.scannedLabel.visibility = View.GONE
            } else {
                binding.scannedLabel.text = getString(R.string.scanned_label, scannedCode)
                binding.scannedLabel.visibility = View.VISIBLE
            }
        }

        binding.productsList.adapter = adapter


    }

    private fun doScan() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
        options.setPrompt("Scan a barcode")
        options.setCameraId(0)
        options.setBeepEnabled(false)
        options.setBarcodeImageEnabled(true)
        barcodeLauncher.launch(options)
    }

    private fun loadData(code: String) {
        productViewModel.setProducts(null)
        binding.progress.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            productViewModel.fetchUserData(viewModel.apiAuthData!!, code)
            launch(Dispatchers.Main) {
                binding.progress.visibility = View.INVISIBLE
            }
        }
    }

}