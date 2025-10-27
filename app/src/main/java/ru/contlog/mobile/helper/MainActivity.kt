// Пакет главной активности приложения
package ru.contlog.mobile.helper

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import ru.contlog.mobile.helper.databinding.ActivityMainBinding
import ru.contlog.mobile.helper.repo.AppPreferencesRepository
import ru.contlog.mobile.helper.vm.AppViewModel
import ru.contlog.mobile.helper.vm.factories.AppViewModelFactory

/**
 * Главная (и, вероятно, единственная) активность приложения.
 *
 * Отвечает за:
 *   - настройку Edge-to-Edge (полноэкранный режим с учётом вырезов и системных панелей),
 *   - управление навигацией через Navigation Component,
 *   - автоматический переход на экран списка подразделений, если пользователь уже авторизован.
 */
class MainActivity : AppCompatActivity() {

    // ViewBinding для удобной работы с UI
    private lateinit var binding: ActivityMainBinding

    // Общий ViewModel приложения с данными авторизации и профиля
    private val viewModel: AppViewModel by viewModels {
        AppViewModelFactory(AppPreferencesRepository(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Включаем Edge-to-Edge: контент отображается под системными панелями
        enableEdgeToEdge()

        // Инициализируем ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настройка отступов под системные элементы (статус-бар, навигационная панель)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Получаем NavController для управления навигацией
        val navHostFragment = supportFragmentManager
            .findFragmentById(binding.fragmentContainerView.id) as NavHostFragment
        val navController = navHostFragment.navController

        // === Автоматический вход, если пользователь уже авторизован ===
        // Если в ViewModel есть данные авторизации (сохранённые в SharedPreferences),
        // сразу переходим на экран списка подразделений, минуя экран логина.
        if (viewModel.apiAuthData != null) {
            navController.navigate(R.id.action_loginFragment_to_workSitesFragment)
        }
    }
}