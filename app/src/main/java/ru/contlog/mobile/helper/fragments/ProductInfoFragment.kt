package ru.contlog.mobile.helper.fragments

// Импорты системных и сторонних библиотек
import android.R.attr.endY
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Camera
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build                   // Для проверки версии Android API
import android.os.Bundle                    // Для передачи данных между компонентами
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log                     // Для логирования отладочной информации
import android.view.LayoutInflater          // Для создания UI из XML-разметки
import android.view.KeyEvent
import android.view.View                    // Базовый класс представления
import android.view.ViewGroup               // Контейнер для View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback // Обратный вызов результата активности
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment       // Базовый класс фрагмента
import androidx.fragment.app.viewModels     // Делегат для получения ViewModel, привязанной к фрагменту
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope   // Область корутин, привязанная к жизненному циклу
import androidx.navigation.fragment.findNavController // Утилита для навигации между фрагментами
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.journeyapps.barcodescanner.ScanContract // Контракт для сканирования штрихкода (библиотека ZXing)
import com.journeyapps.barcodescanner.ScanIntentResult // Результат сканирования
import com.journeyapps.barcodescanner.ScanOptions // Настройки сканера
import kotlinx.coroutines.Dispatchers        // Диспетчеры корутин (IO, Main и т.д.)
import kotlinx.coroutines.launch            // Запуск корутины
import kotlinx.coroutines.withContext      // Переключение контекста корутины
import kotlinx.datetime.LocalDateTime       // Модель даты и времени (kotlinx-datetime)
import ru.contlog.mobile.helper.R           // Сгенерированный класс ресурсов
import ru.contlog.mobile.helper.databinding.FragmentProductInfoBinding // ViewBinding для этого фрагмента
import ru.contlog.mobile.helper.exceptions.ApiRequestException // Исключения API
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

    // В начале класса, после binding
    private lateinit var searchInput: TextInputEditText
    private lateinit var searchButton: MaterialButton

    private lateinit var scannerContainer: LinearLayout
    private lateinit var scannerLine: View
    private lateinit var scanTitle: TextView
    private lateinit var cameraIcon: ImageView



    // Флаг для отслеживания первой загрузки данных
    private var isFirstLoad = true

    // Аниматор для пульсации индикатора загрузки
    private var loadingIndicatorAnimator: android.animation.Animator? = null

    // Константы для анимации
    private companion object {
        const val ANIMATION_DURATION = 300L // Длительность анимации в миллисекундах
        const val PULSE_DURATION = 1000L // Длительность пульсации в миллисекундах
    }

    // Основной ViewModel с данными авторизации (получается через фабрику с репозиторием)
    private val viewModel: AppViewModel by viewModels {
        AppViewModelFactory(AppPreferencesRepository(this.requireContext()))
    }

    // Специализированный ViewModel для логики этого экрана
    private val productViewModel: ProductInfoViewModel by viewModels()

    // Регистрация лаунчера для сканирования штрихкода
    private val barcodeLauncher = registerForActivityResult<ScanOptions?, ScanIntentResult?>(
        ScanContract(),
        ActivityResultCallback { result: ScanIntentResult? ->
            Log.i("ScanIntentResult", "$result")

            if (result != null && result.contents != null) {
                val code = result.contents

                // 🔹 Остановить анимацию
                stopScannerAnimation()

                // 🔹 Скрыть блок сканера после сканирования
                scannerContainer.visibility = View.GONE

                productViewModel.setScannedCode(code)
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

        applyHintFloatingPosition(binding.search1)

        // Обработчик клика вне поля поиска для скрытия клавиатуры
        binding.root.setOnClickListener {
            hideKeyboard()
        }
        
        // Исключаем контейнер поиска из обработки клика корневого layout
        binding.searchContainer.setOnClickListener {
            // Не скрываем клавиатуру при клике на контейнер поиска
        }

        binding.root.post {
            startScannerAnimation()
        }


        // Получаем объект Division из аргументов фрагмента
        // Используем безопасное получение для API 33+ (TIRAMISU)
        productViewModel.setDivision(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireArguments().getSerializable("division", Division::class.java)
            } else {
                @Suppress("DEPRECATION")
                requireArguments().getSerializable("division") as Division
            }!!
        )

        scannerContainer = binding.scannerContainer
        scannerLine = binding.scannerLine
        scanTitle = binding.scanTitle
        cameraIcon = binding.camera


        // Устанавливаем название тулбара как имя подразделения
        binding.productInfoToolbar.title = productViewModel.division.value!!.name
        // Обрабатываем нажатие на кнопку "назад" в тулбаре
        binding.productInfoToolbar.setNavigationOnClickListener {
            findNavController().navigate(R.id.action_productInfoFragment_to_workSitesFragment)
        }

        // 🔹 ИНИЦИАЛИЗАЦИЯ ЭЛЕМЕНТОВ ПОИСКА
        searchInput = binding.searchInput
        searchButton = binding.searchButton

        // 🔹 ПЕРЕХВАТ ВВОДА ОТ BLUETOOTH-СКАНЕРА НА КОРНЕВОМ VIEW
        // Делаем корневой view способным получать фокус для перехвата событий клавиатуры
        binding.root.isFocusableInTouchMode = true
        
        // Перехватываем события клавиатуры на корневом view
        binding.root.setOnKeyListener { _, keyCode, event ->
            // Обрабатываем только нажатия клавиш (не отпускания)
            if (event.action == KeyEvent.ACTION_DOWN) {
                // Если поле поиска не в фокусе, устанавливаем фокус и перенаправляем ввод
                if (!searchInput.hasFocus()) {
                    // Устанавливаем фокус на поле поиска
                    searchInput.requestFocus()
                    // Если это не служебная клавиша (Enter, Back и т.д.), перенаправляем событие в поле поиска
                    if (keyCode != KeyEvent.KEYCODE_ENTER && 
                        keyCode != KeyEvent.KEYCODE_BACK && 
                        keyCode != KeyEvent.KEYCODE_DEL) {
                        // Перенаправляем событие в поле поиска
                        searchInput.dispatchKeyEvent(event)
                        return@setOnKeyListener true
                    }
                }
            }
            false // Пропускаем событие дальше
        }
        
        // 🔹 ОТСЛЕЖИВАНИЕ НАЧАЛА ВВОДА В ПОЛЕ ПОИСКА
        // Если ввод начинается, когда поле не в фокусе, устанавливаем фокус
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Не используется
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Если текст начал появляться, а поле не в фокусе - устанавливаем фокус
                if (s != null && s.isNotEmpty() && !searchInput.hasFocus()) {
                    searchInput.requestFocus()
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Не используется
            }
        })

        // 🔹 ОБРАБОТЧИК НАЖАТИЯ КНОПКИ "ПОИСК"
        searchButton.setOnClickListener {

            searchInput.clearFocus()
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                if (!isOnline()) {
                    Toast.makeText(requireContext(), "Ошибка соединения, проверьте подключение!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                performSearch(query)
            } else {
                Toast.makeText(requireContext(), "Введите запрос для поиска", Toast.LENGTH_SHORT).show()
            }
        }

        // 🔹 ОБРАБОТЧИК НАЖАТИЯ ENTER НА КЛАВИАТУРЕ
        searchInput.setOnEditorActionListener { view, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Скрываем клавиатуру перед выполнением поиска
                hideKeyboard()
                // Имитируем клик по кнопке
                searchButton.performClick()
                true
            } else {
                false
            }
        }

        // 🔹 ОБНОВЛЁННЫЙ ОБРАБОТЧИК: проверка интернета перед сканированием
        binding.scan.setOnClickListener {
            // Скрываем клавиатуру при клике на кнопку сканирования
            hideKeyboard()

            if (!isOnline()) {
                Toast.makeText(requireContext(), "Ошибка соединения, проверьте подключение!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔹 Показываем UI сканера
            scannerContainer.visibility = View.VISIBLE

            // 🔹 Запускаем сканирование
            doScan()
        }

        // Устанавливаем кастомный LayoutManager, который позволяет блокировать прокрутку
        binding.productsList.layoutManager = CustomLinearLayoutManager(
            requireContext()
        )
        
        // Обработчик касания на список продуктов для скрытия клавиатуры при клике на пустую область
        binding.productsList.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                hideKeyboard()
            }
            false // Не перехватываем событие, позволяем прокрутке работать
        }

        // Создаём адаптер RecyclerView с коллбэком для включения/отключения прокрутки
        val adapter = ProductsRVAdapter { enable ->
            (binding.productsList.layoutManager as CustomLinearLayoutManager).isScrollEnabled = enable
        }

        // Подписываемся на ошибки из ViewModel
        productViewModel.errors.observe(viewLifecycleOwner) { errors ->
            if (errors.isNotEmpty()) {
                // Получаем последнюю ошибку
                val lastError = errors.last()
                // Формируем сообщение для пользователя
                val errorMessage = if (lastError is ApiRequestException) {
                    lastError.humanMessage
                } else {
                    "Ошибка при получении данных. Проверьте подключение к интернету."
                }
                // Показываем сообщение об ошибке пользователю
                Toast.makeText(
                    requireContext(),
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
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
            // Анимация появления списка продуктов после первой загрузки
            if (products.isNotEmpty() && isFirstLoad) {
                animateProductsListAppearance()
                isFirstLoad = false
            }
        }

        // Подписываемся на изменения отсканированного кода
        productViewModel.scannedCode.observe(viewLifecycleOwner) { code ->
            // Устанавливаем подзаголовок тулбара в формате "Отсканировано: XXX" белым цветом
            binding.productInfoToolbar.subtitle = code?.let {
                val text = getString(R.string.scanned_label, it)
                SpannableString(text).apply {
                    setSpan(ForegroundColorSpan(Color.WHITE), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
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
    @SuppressLint("UseKtx")
    private fun loadData(code: String) {
        // Сбрасываем предыдущие данные и флаг первой загрузки для новой анимации
        productViewModel.setProducts(null)
        // Очищаем предыдущие ошибки перед новой загрузкой
        productViewModel.clearErrors()
        isFirstLoad = true
        // Показываем overlay загрузки с анимацией
        showLoadingOverlay()
        // Запускаем загрузку в фоновом потоке
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Выполняем сетевой запрос с авторизационными данными и кодом
                productViewModel.fetchUserData(viewModel.apiAuthData!!, code)
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Обрабатываем отмену корутины (например, при уничтожении фрагмента)
                Log.d("ProductInfoFragment", "Загрузка данных отменена")
                // Гарантируем закрытие overlay даже при отмене
                withContext(Dispatchers.Main) {
                    hideLoadingOverlay()
                }
                throw e // Пробрасываем исключение отмены дальше
            } catch (e: Exception) {
                // Логируем неожиданную ошибку для отладки
                Log.e("ProductInfoFragment", "Неожиданная ошибка при загрузке данных: ${e.message}", e)
                // Устанавливаем пустой список продуктов при ошибке
                productViewModel.setProducts(emptyList())
            } finally {
                // Гарантируем закрытие overlay загрузки в любом случае (успех или ошибка)
                // Это предотвращает блокировку экрана при любых ошибках
                // Используем withContext вместо launch для гарантированного выполнения
                withContext(Dispatchers.Main) {
                    if (binding.loadingOverlay.visibility == View.VISIBLE) {
                        hideLoadingOverlay()
                    }
                }
            }
        }
    }

    // Метод показа overlay загрузки с анимацией появления
    private fun showLoadingOverlay() {
        // Устанавливаем текст загрузки
        binding.loadingText.text = getString(R.string.label_processing_barcode)
        // Начинаем с прозрачного состояния
        binding.loadingOverlay.alpha = 0f
        binding.loadingOverlay.visibility = View.VISIBLE
        // Анимация появления overlay с затемнением
        binding.loadingOverlay.animate()
            .alpha(1f)
            .setDuration(ANIMATION_DURATION)
            .setListener(null)
        // Анимация пульсации индикатора загрузки
        animateLoadingIndicator()
    }

    // Метод скрытия overlay загрузки с анимацией исчезновения
    private fun hideLoadingOverlay() {
        // Останавливаем анимацию пульсации
        loadingIndicatorAnimator?.cancel()
        loadingIndicatorAnimator = null
        // Сбрасываем масштаб индикатора
        binding.loadingProgressIndicator.scaleX = 1f
        binding.loadingProgressIndicator.scaleY = 1f
        // Анимация исчезновения overlay
        binding.loadingOverlay.animate()
            .alpha(0f)
            .setDuration(ANIMATION_DURATION)
            .setListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    binding.loadingOverlay.visibility = View.GONE
                }
            })
    }

    // Метод анимации пульсации индикатора загрузки
    private fun animateLoadingIndicator() {
        // Останавливаем предыдущую анимацию, если она есть
        loadingIndicatorAnimator?.cancel()
        // Создаем новую анимацию пульсации
        val scaleUpX = android.animation.ObjectAnimator.ofFloat(
            binding.loadingProgressIndicator,
            "scaleX",
            1f, 1.15f
        ).apply {
            duration = PULSE_DURATION / 2
        }
        val scaleUpY = android.animation.ObjectAnimator.ofFloat(
            binding.loadingProgressIndicator,
            "scaleY",
            1f, 1.15f
        ).apply {
            duration = PULSE_DURATION / 2
        }
        val scaleDownX = android.animation.ObjectAnimator.ofFloat(
            binding.loadingProgressIndicator,
            "scaleX",
            1.15f, 1f
        ).apply {
            duration = PULSE_DURATION / 2
        }
        val scaleDownY = android.animation.ObjectAnimator.ofFloat(
            binding.loadingProgressIndicator,
            "scaleY",
            1.15f, 1f
        ).apply {
            duration = PULSE_DURATION / 2
        }

        val scaleUpSet = android.animation.AnimatorSet().apply {
            playTogether(scaleUpX, scaleUpY)
        }
        val scaleDownSet = android.animation.AnimatorSet().apply {
            playTogether(scaleDownX, scaleDownY)
        }

        val animatorSet = android.animation.AnimatorSet().apply {
            playSequentially(scaleUpSet, scaleDownSet)
            addListener(object : android.animation.AnimatorListenerAdapter() {
                @SuppressLint("UseKtx")
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    // Повторяем анимацию, если overlay все еще виден
                    if (binding.loadingOverlay.visibility == View.VISIBLE) {
                        animateLoadingIndicator()
                    }
                }
            })
        }
        loadingIndicatorAnimator = animatorSet
        animatorSet.start()
    }

    // Метод анимации появления списка продуктов
    private fun animateProductsListAppearance() {
        binding.productsList.alpha = 0f
        binding.productsList.animate()
            .alpha(1f)
            .setDuration(ANIMATION_DURATION)
            .setListener(null)
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
    @SuppressLint("ObsoleteSdkInt")
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

    // Освобождение ресурсов при уничтожении View для предотвращения утечек
    @SuppressLint("UseKtx")
    override fun onDestroyView() {
        super.onDestroyView()

        // Останавливаем анимацию загрузки при уничтожении View
        loadingIndicatorAnimator?.cancel()
        loadingIndicatorAnimator = null
        // Гарантируем закрытие overlay загрузки при уничтожении View
        // Это предотвращает блокировку экрана, если фрагмент был уничтожен во время загрузки
        if (::binding.isInitialized && binding.loadingOverlay.visibility == View.VISIBLE) {
            hideLoadingOverlay()
        }
    }

    /**
     * Выполняет поиск по введенному запросу.
     * @param query Строка поиска (артикул, штрих-код и т.д.)
     */
    private fun performSearch(query: String) {
        // Логируем запрос для отладки
        Log.i("ProductInfoFragment", "Выполняется поиск по запросу: $query")

        // 🔹 Остановить анимацию
        stopScannerAnimation()

        scannerLine.visibility = View.GONE
        scanTitle.visibility = View.GONE
        cameraIcon.visibility = View.GONE


        // Устанавливаем запрос в ViewModel (если нужно для истории)
        productViewModel.setScannedCode(query)

        // Загружаем данные по этому запросу
        loadData(query)

        // Очищаем поле ввода после запуска поиска (опционально)
        searchInput.text?.clear()
    }

    private var scannerLineAnimator: ValueAnimator? = null

    /**
     * Запускает анимацию движения красной линии вверх-вниз.
     */
    private fun startScannerAnimation() {

        scannerLine.visibility = View.VISIBLE

        // Начальная позиция: чуть ниже верхней границы Титла
        val startY = scanTitle.y + 1f

        // Конечная позиция: до самого низа Титла
        val endY = scanTitle.y + scanTitle.height - scannerLine.height

        // Создаём аниматор
        scannerLineAnimator = ValueAnimator.ofFloat(startY, endY).apply {
            duration = 1500L         // 1.5 секунды на один цикл
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                scannerLine.y = value
            }
        }
        scannerLineAnimator?.start()
    }

    /**
     * Останавливает анимацию и скрывает линию.
     */
    private fun stopScannerAnimation() {
        scannerLineAnimator?.cancel()
        scannerLineAnimator = null
        scannerLine.visibility = View.GONE
    }

    private fun applyHintFloatingPosition(til: TextInputLayout) {
        til.post {
            try {
                val helperField = TextInputLayout::class.java.getDeclaredField("collapsingTextHelper")
                helperField.isAccessible = true
                val collapsingHelper = helperField[til] as Any

                val setCollapsedBoundsMethod = collapsingHelper.javaClass.getDeclaredMethod(
                    "setCollapsedBounds", Int::class.java, Int::class.java,
                    Int::class.java, Int::class.java
                )
                setCollapsedBoundsMethod.isAccessible = true

                val width = til.width
                val height = 0
                setCollapsedBoundsMethod.invoke(collapsingHelper, 0, height, width, height + 40)
            } catch (e: Exception) {
                Log.e("TAG", "onCreate: fuck", e)
            }
        }
    }

    /**
     * Скрывает клавиатуру и убирает фокус с поля поиска
     */
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        // Используем windowToken из searchInput, если он инициализирован и в фокусе
        val viewToHide = if (::searchInput.isInitialized && searchInput.hasFocus()) {
            searchInput
        } else {
            requireActivity().currentFocus
        }
        if (viewToHide != null && viewToHide.windowToken != null) {
            imm.hideSoftInputFromWindow(viewToHide.windowToken, 0)
        }
        if (::searchInput.isInitialized) {
            searchInput.clearFocus()
        }
    }
}