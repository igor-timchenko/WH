package ru.contlog.mobile.helper.fragments

// Импорты системных и сторонних библиотек
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build                   // Для проверки версии Android API
import android.os.Bundle                    // Для передачи данных между компонентами
import android.util.Log                     // Для логирования отладочной информации
import android.view.LayoutInflater          // Для создания UI из XML-разметки
import android.view.View                    // Базовый класс представления
import android.view.ViewGroup               // Контейнер для View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback // Обратный вызов результата активности
import androidx.fragment.app.Fragment       // Базовый класс фрагмента
import androidx.fragment.app.viewModels     // Делегат для получения ViewModel, привязанной к фрагменту
import androidx.lifecycle.lifecycleScope   // Область корутин, привязанная к жизненному циклу
import androidx.navigation.fragment.findNavController // Утилита для навигации между фрагментами
import com.journeyapps.barcodescanner.ScanContract // Контракт для сканирования штрихкода (библиотека ZXing)
import com.journeyapps.barcodescanner.ScanIntentResult // Результат сканирования
import com.journeyapps.barcodescanner.ScanOptions // Настройки сканера
import kotlinx.coroutines.Dispatchers        // Диспетчеры корутин (IO, Main и т.д.)
import kotlinx.coroutines.launch            // Запуск корутины
import kotlinx.datetime.LocalDateTime       // Модель даты и времени (kotlinx-datetime)
import ru.contlog.mobile.helper.R           // Сгенерированный класс ресурсов
import ru.contlog.mobile.helper.databinding.FragmentProductInfoBinding // ViewBinding для этого фрагмента
import ru.contlog.mobile.helper.model.Division // Модель подразделения
import ru.contlog.mobile.helper.model.Product // Модель продукта
import ru.contlog.mobile.helper.model.ProductPlace // Модель места продукта
import ru.contlog.mobile.helper.repo.AppPreferencesRepository // Репозиторий для хранения настроек/токенов
import ru.contlog.mobile.helper.rvadapters.ProductsRVAdapter // Адаптер для RecyclerView с продуктами
import ru.contlog.mobile.helper.utils.CustomLinearLayoutManager // Кастомный LayoutManager (с возможностью блокировки прокрутки)
import ru.contlog.mobile.helper.vm.AppViewModel // Основной ViewModel с авторизационными данными
import ru.contlog.mobile.helper.vm.ProductInfoViewModel // ViewModel для логики этого экрана
import ru.contlog.mobile.helper.vm.factories.AppViewModelFactory // Фабрика для создания AppViewModel

// Фрагмент отображения информации о продукте после сканирования штрихкода
class ProductInfoFragment : Fragment() {
    // ViewBinding для безопасного доступа к UI-элементам
    private lateinit var binding: FragmentProductInfoBinding

    // Основной ViewModel с данными авторизации (получается через фабрику с репозиторием)
    private val viewModel: AppViewModel by viewModels {
        AppViewModelFactory(AppPreferencesRepository(this.requireContext()))
    }

    // Специализированный ViewModel для логики этого экрана
    private val productViewModel: ProductInfoViewModel by viewModels()

    // Регистрация лаунчера для сканирования штрихкода
    private val barcodeLauncher = registerForActivityResult<ScanOptions?, ScanIntentResult?>(
        ScanContract(), // Используем контракт из библиотеки ZXing
        ActivityResultCallback { result: ScanIntentResult? ->
            // Логируем результат сканирования для отладки
            Log.i("ScanIntentResult", "$result")
            // Если сканирование прошло успешно и есть содержимое — обрабатываем код
            if (result!!.contents != null) {
                val code = result.contents
                // Сохраняем отсканированный код в ViewModel
                productViewModel.setScannedCode(code)
                // Загружаем данные по этому коду
                loadData(code)
            }
        })

    // Создание корневого представления из layout-файла
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProductInfoBinding.inflate(inflater)
        return binding.root
    }

    // Настройка UI после создания View
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получаем объект Division из аргументов фрагмента
        // Используем безопасное получение для API 33+ (TIRAMISU)
        productViewModel.setDivision(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireArguments().getSerializable("division", Division::class.java)
            } else {
                requireArguments().getSerializable("division") as Division
            }!!
        )

        // Устанавливаем название тулбара как имя подразделения
        binding.productInfoToolbar.title = productViewModel.division.value!!.name
        // Обрабатываем нажатие на кнопку "назад" в тулбаре
        binding.productInfoToolbar.setNavigationOnClickListener {
            findNavController().navigate(R.id.action_productInfoFragment_to_workSitesFragment)
        }

        // 🔹 ОБНОВЛЁННЫЙ ОБРАБОТЧИК: проверка интернета перед сканированием
        binding.scan.setOnClickListener {
            if (!isOnline()) {
                Toast.makeText(requireContext(), "Включите интернет", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            doScan()
        }

        // Устанавливаем кастомный LayoutManager, который позволяет блокировать прокрутку
        binding.productsList.layoutManager = CustomLinearLayoutManager(
            requireContext()
        )

        // Создаём адаптер RecyclerView с коллбэком для включения/отключения прокрутки
        val adapter = ProductsRVAdapter { enable ->
            (binding.productsList.layoutManager as CustomLinearLayoutManager).isScrollEnabled = enable
        }

        // Подписываемся на изменения списка продуктов в ViewModel
        productViewModel.products.observe(viewLifecycleOwner) { products ->
            // Если данных ещё нет — показываем пустой список
            if (products == null) {
                adapter.setData(emptyList())
                binding.productsListEmptyInfo.visibility = View.GONE
                return@observe
            }
            // Обновляем данные в адаптере
            adapter.setData(products)
            // Показываем/скрываем сообщение "список пуст"
            binding.productsListEmptyInfo.visibility = if (products.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        // Подписываемся на изменения отсканированного кода
        productViewModel.scannedCode.observe(viewLifecycleOwner) { code ->
            // Устанавливаем подзаголовок тулбара в формате "Отсканировано: XXX"
            binding.productInfoToolbar.subtitle = code?.let { getString(R.string.scanned_label, it) }
        }

        // Привязываем адаптер к RecyclerView
        binding.productsList.adapter = adapter

        // Долгое нажатие на кнопку сканирования — для тестирования (загрузка мок-данных)
        binding.scan.setOnLongClickListener {
            val data = mock()
            adapter.setData(data)
            true // потребляется событие
        }
    }

    // Метод запуска сканирования штрихкода
    private fun doScan() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES) // Поддерживаем все типы штрихкодов
        options.setPrompt("Scan a barcode") // Текст-подсказка на экране сканера
        options.setCameraId(0) // Используем основную камеру
        options.setBeepEnabled(false) // Отключаем звук при сканировании
        options.setBarcodeImageEnabled(true) // Сохраняем изображение штрихкода (если нужно)
        barcodeLauncher.launch(options) // Запускаем активность сканера
    }

    // Метод загрузки данных по отсканированному коду
    private fun loadData(code: String) {
        // Сбрасываем предыдущие данные
        productViewModel.setProducts(null)
        // Показываем индикатор загрузки
        binding.progress.visibility = View.VISIBLE
        // Запускаем загрузку в фоновом потоке
        lifecycleScope.launch(Dispatchers.IO) {
            // Выполняем сетевой запрос с авторизационными данными и кодом
            productViewModel.fetchUserData(viewModel.apiAuthData!!, code)
            // Скрываем индикатор загрузки на главном потоке
            launch(Dispatchers.Main) {
                binding.progress.visibility = View.INVISIBLE
            }
        }
    }

    // Метод генерации мок-данных для тестирования без сканирования
    private fun mock(): List<Product> {
        val data = mutableListOf<Product>()
        // Создаём 10 продуктов
        for (i in 0 until 10) {
            val places = mutableListOf<ProductPlace>()
            // Каждый продукт содержит 100 мест
            for (j in 0 until 100) {
                val pp = ProductPlace(
                    "asd", 1, 1, LocalDateTime(1, 1, 1, 1, 1, 1, 1), LocalDateTime(1, 1, 1, 1, 1, 1, 1),
                    "asd", false, 1, 1, "asd", "asd", "asd", false, false, false, "asd", "asd", "asd"
                )
                places.add(pp)
            }
            val p = Product(
                "asd", "asd", 1, "asd", "asd", "asd", "asd", LocalDateTime(1, 1, 1, 1, 1, 1, 1),
                "asd", 1, places, "asd"
            )
            data.add(p)
        }
        return data
    }

    // 🔹 МЕТОД ПРОВЕРКИ ДОСТУПА В ИНТЕРНЕТ
    private fun isOnline(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected == true
        }
    }
}