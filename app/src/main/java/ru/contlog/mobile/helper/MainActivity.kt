package ru.contlog.mobile.helper

// Импорты системных и вспомогательных классов Android
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.ConnectivityManager // Импорт класса ConnectivityManager из пакета android.net. ConnectivityManager - это системный сервис Android, который предоставляет информацию о состоянии подключения к сети (Wi-Fi, мобильный интернет и т.д.) и управляет этими подключениями.
import android.net.Network          // Импорт класса Network из пакета android.net. Network представляет собой конкретное сетевое подключение на устройстве. Он используется, например, в колбэках ConnectivityManager.NetworkCallback для получения информации о конкретной сети, которая изменила состояние.
import android.net.NetworkCapabilities  // Импорт класса NetworkCapabilities из пакета android.net. NetworkCapabilities содержит информацию о возможностях (capabilities) и свойствах конкретного сетевого подключения (Network). Позволяет проверить, например, поддерживает ли сеть интернет (NET_CAPABILITY_INTERNET) или является VPN (NET_CAPABILITY_VPN).
import android.net.NetworkRequest // Импорт класса NetworkRequest из пакета android.net. NetworkRequest используется для описания требований к сетевому подключению, которое нужно отслеживать или запрашивать. Создается с помощью NetworkRequest.Builder. Например, можно запросить подключение, которое поддерживает интернет и использует Wi-Fi.
import android.os.Build                    // Для проверки версии Android API
import android.os.Bundle                     // Для работы с состоянием активности
import android.telecom.ConnectionService
import android.util.Log     // Импорт класса Log из пакета android.util. Класс Log используется для вывода сообщений в лог-систему Android. Это полезно для отладки приложений, отслеживания ошибок и получения информации о выполнении кода. Сообщения могут быть разного уровня (verbose, debug, info, warn, error).
import android.view.View                     // Базовый класс UI-элемента
import android.view.WindowInsets             // Для работы с системными вставками (status bar, navigation bar)
import android.widget.LinearLayout
// Включает edge-to-edge режим (полноэкранный интерфейс)
import androidx.activity.enableEdgeToEdge
// Делегат для получения ViewModel, привязанной к активности
import androidx.activity.viewModels
// Базовый класс активности с поддержкой Material Design
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
// Область корутин, привязанная к жизненному циклу активности
import androidx.lifecycle.lifecycleScope
// Утилиты для навигации между фрагментами
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
// Корутины
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
// ViewBinding для активности
import ru.contlog.mobile.helper.databinding.ActivityMainBinding
import ru.contlog.mobile.helper.repo.Api
// Репозиторий для работы с настройками приложения
import ru.contlog.mobile.helper.repo.AppPreferencesRepository
// ViewModel приложения
import ru.contlog.mobile.helper.vm.AppViewModel
// Фабрика для создания ViewModel с зависимостями
import ru.contlog.mobile.helper.vm.factories.AppViewModelFactory

// Основная активность приложения, управляющая навигацией и UI
class MainActivity : AppCompatActivity() {
    // ViewBinding для доступа к UI-элементам активности
    private lateinit var binding: ActivityMainBinding

    // ViewModel, привязанная к активности, создаваемая через фабрику с репозиторием настроек
    private val viewModel: AppViewModel by viewModels {
        AppViewModelFactory(AppPreferencesRepository(this))
    }

    private val connectivityManager: ConnectivityManager by lazy {
        getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    val networkQueryCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            super.onLost(network)

            binding.root.post {
                viewModel.setInternetAvailableState(available = false)
            }
        }

        override fun onAvailable(network: Network) {
            super.onAvailable(network)

            binding.root.post {
                viewModel.setInternetAvailableState(available = true)
            }
        }

        override fun onUnavailable() {
            super.onUnavailable()

            binding.root.post {
                viewModel.setInternetAvailableState(available = false)
            }
        }
    }

    // Метод вызывается при создании активности
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Включаем edge-to-edge режим (контент под статус-баром и навигационной панелью)
        enableEdgeToEdge()
        // Создаём ViewBinding из layout-файла
        binding = ActivityMainBinding.inflate(layoutInflater)
        // Устанавливаем корневое представление
        setContentView(binding.root)

        // Настройка системных вставок (status bar и navigation bar) в зависимости от версии Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // Для Android 14+ (API 34+) используем новый API WindowInsets
            window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                // Получаем вставки для статус-бара и навигационной панели
                val statusBarInsets = insets.getInsets(WindowInsets.Type.statusBars())
                val navigationBarInsets = insets.getInsets(WindowInsets.Type.navigationBars())
                // Устанавливаем цвет фона статус-бара
                view.setBackgroundColor(getColor(R.color.statusBarColor))
                // Добавляем отступы, чтобы контент не перекрывался системными элементами
                view.setPadding(0, statusBarInsets.top, 0, navigationBarInsets.bottom)
                // Возвращаем неизменённые insets
                insets
            }
        } else {
            // Для старых версий Android устанавливаем цвет статус-бара напрямую
            window.statusBarColor = getColor(R.color.statusBarColor)
            // Включаем автоматическую настройку отступов для системных вставок
            binding.root.fitsSystemWindows = true
        }

        // Откладываем получение NavController до полной инициализации FragmentContainerView
        // Это предотвращает ошибку инициализации FragmentContainerView
        binding.fragmentContainerView.post {
            // Получаем NavController через extension функцию на View.
            val navController = binding.fragmentContainerView.findNavController()

            // Отключаем автоповот
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            // Если пользователь уже авторизован (apiAuthData не null) — перенаправляем на экран рабочих площадок
            if (viewModel.apiAuthData != null) {
                val currentDestination = navController.currentDestination?.id
                // Используем безопасную навигацию в зависимости от текущего фрагмента
                when (currentDestination) {
                    R.id.loginFragment -> {
                        navController.navigate(R.id.action_loginFragment_to_workSitesFragment)
                    }
                    R.id.productInfoFragment -> {
                        navController.navigate(R.id.action_productInfoFragment_to_workSitesFragment)
                    }
                    R.id.profileFragment -> {
                        navController.navigate(R.id.action_profileFragment_to_workSitesFragment)
                    }
                    R.id.workSitesFragment -> {
                        // Уже на нужном экране, ничего не делаем
                    }
                    else -> {
                        // Для неизвестных фрагментов не выполняем навигацию
                        // Пользователь уже авторизован и может сам перейти на нужный экран
                        Log.d(TAG, "Пользователь авторизован, но текущий destination ($currentDestination) не требует автоматической навигации")
                    }
                }
            }

            // Настраиваем обработчик нажатий на элементы нижней навигационной панели
            binding.bottomNavigation.setOnItemSelectedListener { mi ->
                when (mi.itemId) {
                    // При выборе "Главная"
                    R.id.mHome -> {
                        // Если текущий экран — не workSitesFragment, переходим к нему
                        if ((navController.currentDestination?.id ?: false) != R.id.workSitesFragment) {
                            navController.navigate(R.id.action_profileFragment_to_workSitesFragment)
                        }
                    }
                    // При выборе "Профиль"
                    R.id.mProfile -> {
                        // Если текущий экран — не profileFragment, переходим к нему
                        if ((navController.currentDestination?.id ?: false) != R.id.profileFragment) {
                            navController.navigate(R.id.action_workSitesFragment_to_profileFragment)
                        }
                    }
                }
                // Возвращаем true, чтобы подтвердить обработку нажатия
                true
            }

            // Наблюдаем за изменениями в стеке навигации для управления видимостью нижней панели
            lifecycleScope.launch(Dispatchers.IO) {
                navController.currentBackStackEntryFlow.collect { entry ->
                    // Определяем, должна ли быть видна нижняя панель
                    val navbarVisibility = when (entry.destination.id) {
                        R.id.workSitesFragment, R.id.profileFragment -> View.VISIBLE
                        else -> View.GONE
                    }
                    // Обновляем UI на главном потоке
                    launch(Dispatchers.Main) {
                        binding.bottomNavigation.visibility = navbarVisibility
                        // Синхронизируем выделенный элемент навигационной панели с текущим экраном
                        when (entry.destination.id) {
                            R.id.workSitesFragment -> binding.bottomNavigation.selectedItemId = R.id.mHome
                            R.id.profileFragment -> binding.bottomNavigation.selectedItemId = R.id.mProfile
                        }
                    }
                }
            }
        }

        // Закомментированная строка: отображение версии приложения (возможно, для отладки)
        // binding.versionLabel.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)

        val networkQueryRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        connectivityManager.registerNetworkCallback(networkQueryRequest, networkQueryCallback)

        viewModel.internetAvailable.observe(this) { internetAvailable ->
            binding.noInternetOverlay.visibility = if (internetAvailable) View.GONE else View.VISIBLE
        }
        val hash = WH_SUPPLIER_CONTINENT.getSignature(this)
        Log.i(TAG, "App hash: $hash")
    }

    override fun onDestroy() {
        connectivityManager.unregisterNetworkCallback(networkQueryCallback)

        super.onDestroy()
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

    // Сопутствующий объект с константами класса
    companion object {
        const val TAG = "Contlog.MainActivity" // Тег для логирования
    }
}