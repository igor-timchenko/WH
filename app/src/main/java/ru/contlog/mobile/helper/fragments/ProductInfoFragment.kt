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
import androidx.navigation.fragment.findNavController
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

        binding.productInfoToolbar.title = productViewModel.division.value!!.name
        binding.productInfoToolbar.setNavigationOnClickListener {
            findNavController().navigate(R.id.action_productInfoFragment_to_workSitesFragment)
        }

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

        productViewModel.scannedCode.observe(viewLifecycleOwner) { code ->
            binding.productInfoToolbar.subtitle = code?.let { getString(R.string.scanned_label, it) }
        }

        binding.productsList.adapter = adapter

        binding.scan.setOnLongClickListener {
            val data = mock()
            adapter.setData(data)

            true
        }
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

    private fun mock(): List<Product> {
        val data = mutableListOf<Product>()
        for (i in 0 until 10) {
            val places = mutableListOf<ProductPlace>()
            for (j in 0 until 100) {
                val pp =
                    ProductPlace(
                        "asd",
                        1,
                        1,
                        LocalDateTime(1, 1, 1, 1, 1, 1, 1),
                        LocalDateTime(1, 1, 1, 1, 1, 1, 1),
                        "asd",
                        false,
                        1,
                        1,
                        "asd",
                        "asd",
                        "asd",
                        false,
                        false,
                        false,
                        "asd",
                        "asd",
                        "asd"
                    )
                places.add(pp)
            }
            val p = Product(
                "asd",
                "asd",
                1,
                "asd",
                "asd",
                "asd",
                "asd",
                LocalDateTime(1, 1, 1, 1, 1, 1, 1),
                "asd",
                1,
                places,
                "asd"
            )
            data.add(p)
        }

        return data
    }
}