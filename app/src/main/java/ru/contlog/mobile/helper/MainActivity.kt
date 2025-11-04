package ru.contlog.mobile.helper

// Импорты системных и вспомогательных классов Android
import android.os.Build                    // Для проверки версии Android API
import android.os.Bundle                     // Для работы с состоянием активности
import android.util.Log                      // Для логирования (в коде не используется напрямую, но импортирован)
import android.view.View                     // Базовый класс UI-элемента
import android.view.WindowInsets             // Для работы с системными вставками (status bar, navigation bar)
// Включает edge-to-edge режим (полноэкранный интерфейс)
import androidx.activity.enableEdgeToEdge
// Делегат для получения ViewModel, привязанной к активности
import androidx.activity.viewModels
// Базовый класс активности с поддержкой Material Design
import androidx.appcompat.app.AppCompatActivity
// Расширения для работы с совместимостью View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
// Область корутин, привязанная к жизненному циклу активности
import androidx.lifecycle.lifecycleScope
// Утилиты для навигации между фрагментами
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
// Корутины
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
// ViewBinding для активности
import ru.contlog.mobile.helper.databinding.ActivityMainBinding
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

        // Получаем NavHostFragment из контейнера по ID
        val navHostFragment = supportFragmentManager.findFragmentById(binding.fragmentContainerView.id) as NavHostFragment
        // Получаем NavController для управления навигацией
        val navController = navHostFragment.navController

        // Если пользователь уже авторизован (apiAuthData не null) — перенаправляем на экран рабочих площадок
        if (viewModel.apiAuthData != null) {
            navController.navigate(R.id.action_loginFragment_to_workSitesFragment)
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

        // Закомментированная строка: отображение версии приложения (возможно, для отладки)
        // binding.versionLabel.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)
    }

    // Сопутствующий объект с константами класса
    companion object {
        const val TAG = "Contlog.MainActivity" // Тег для логирования
    }
}