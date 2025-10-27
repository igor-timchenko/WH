// Пакет фрагмента, отвечающего за сканирование штрихкода и отображение информации о продукте
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

/**
 * Фрагмент для:
 * - сканирования штрихкода (QR-кода или другого),
 * - загрузки данных о продукте по этому коду,
 * - отображения списка найденных продуктов (например, мест хранения).
 *
 * Принимает в аргументах объект подразделения (Division), чтобы фильтровать данные по нему.
 */
class ProductInfoFragment : Fragment() {

    // ViewBinding для удобной работы с UI
    private lateinit var binding: FragmentProductInfoBinding

    // Общий ViewModel приложения — содержит данные авторизации (apiAuthData)
    private val viewModel: AppViewModel by viewModels {
        AppViewModelFactory(AppPreferencesRepository(this.requireContext()))
    }

    // Специализированный ViewModel для логики этого экрана
    private val productViewModel: ProductInfoViewModel by viewModels()

    // Регистрация Activity Result API для сканирования штрихкода
    private val barcodeLauncher = registerForActivityResult<ScanOptions?, ScanIntentResult?>(
        ScanContract(),
        ActivityResultCallback { result: ScanIntentResult? ->
            Log.i("ScanIntentResult", "$result")
            // Проверяем, что сканирование прошло успешно и есть результат
            if (result?.contents != null) {
                val code = result.contents
                // Сохраняем отсканированный код в ViewModel
                productViewModel.setScannedCode(code)
                // Загружаем данные по этому коду
                loadData(code)
            }
        }
    )

    /**
     * Создаёт корневой View фрагмента.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProductInfoBinding.inflate(inflater)
        return binding.root
    }

    /**
     * Настраивает UI и подписывается на данные из ViewModel.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // === Получение подразделения из аргументов фрагмента ===
        // Начиная с Android 13 (TIRAMISU), getSerializable требует указания типа
        val division = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getSerializable("division", Division::class.java)
        } else {
            requireArguments().getSerializable("division") as Division
        }!!

        productViewModel.setDivision(division)

        // === Обработка нажатия на кнопку сканирования ===
        binding.scan.setOnClickListener { doScan() }

        // === Настройка RecyclerView ===
        // Используется кастомный LayoutManager, возможно, с возможностью блокировки прокрутки
        binding.productsList.layoutManager = CustomLinearLayoutManager(requireContext())

        // Адаптер для списка продуктов
        val adapter = ProductsRVAdapter { enable ->
            // Позволяет включать/отключать прокрутку списка изнутри элементов (например, при свайпе)
            (binding.productsList.layoutManager as CustomLinearLayoutManager).isScrollEnabled = enable
        }

        // Подписка на изменения списка продуктов
        productViewModel.products.observe(viewLifecycleOwner) { products ->
            if (products == null) {
                adapter.setData(emptyList())
                binding.productsListEmptyInfo.visibility = View.GONE
                return@observe
            }

            adapter.setData(products)
            // Показываем/скрываем надпись "Нет данных"
            binding.productsListEmptyInfo.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
        }

        // Подписка на отсканированный код
        productViewModel.scannedCode.observe(viewLifecycleOwner) { scannedCode ->
            if (scannedCode == null) {
                binding.scannedLabel.visibility = View.GONE
                binding.scannedLabel.text = ""
            } else {
                // Отображаем отсканированный код в формате: "Отсканировано: 123456"
                binding.scannedLabel.text = getString(R.string.scanned_label, scannedCode)
                binding.scannedLabel.visibility = View.VISIBLE
            }
        }

        binding.productsList.adapter = adapter
    }

    /**
     * Запускает сканер штрихкодов с настройками.
     */
    private fun doScan() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES) // Поддержка всех типов штрихкодов
        options.setPrompt("Scan a barcode") // Подсказка в интерфейсе сканера
        options.setCameraId(0) // Использовать основную камеру
        options.setBeepEnabled(false) // Отключить звуковой сигнал
        options.setBarcodeImageEnabled(true) // Сохранять изображение штрихкода (если нужно)
        barcodeLauncher.launch(options)
    }

    /**
     * Загружает данные о продукте по отсканированному коду.
     */
    private fun loadData(code: String) {
        // Очищаем предыдущие данные и показываем индикатор загрузки
        productViewModel.setProducts(null)
        binding.progress.visibility = View.VISIBLE

        // Выполняем запрос в фоновом потоке
        lifecycleScope.launch(Dispatchers.IO) {
            // Передаём данные авторизации и код продукта
            productViewModel.fetchUserData(viewModel.apiAuthData!!, code)

            // Скрываем прогресс в основном потоке
            launch(Dispatchers.Main) {
                binding.progress.visibility = View.INVISIBLE
            }
        }
    }
}