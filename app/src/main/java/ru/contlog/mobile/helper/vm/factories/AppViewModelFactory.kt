// Пакет фабрик ViewModel
package ru.contlog.mobile.helper.vm.factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.contlog.mobile.helper.repo.AppPreferencesRepository
import ru.contlog.mobile.helper.vm.AppViewModel

/**
 * Фабрика для создания экземпляра [AppViewModel] с необходимыми зависимостями.
 *
 * Назначение:
 *   - Внедрение зависимости [AppPreferencesRepository] в ViewModel,
 *   - Обеспечение совместимости с механизмом создания ViewModel в Android Architecture Components.
 *
 * Используется при инициализации ViewModel во фрагменте или активити:
 * ```kotlin
 * private val viewModel: AppViewModel by viewModels {
 *     AppViewModelFactory(AppPreferencesRepository(requireContext()))
 * }
 * ```
 */
class AppViewModelFactory(
    private val appPreferencesRepository: AppPreferencesRepository
) : ViewModelProvider.Factory {

    /**
     * Создаёт экземпляр ViewModel указанного класса.
     *
     * @param modelClass — класс запрашиваемого ViewModel
     * @return экземпляр ViewModel
     * @throws IllegalArgumentException если запрошен неизвестный класс ViewModel
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Проверяем, что запрашиваем именно AppViewModel (или его подкласс)
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Создаём ViewModel с внедрённой зависимостью
            return AppViewModel(appPreferencesRepository) as T
        }
        // Если запрошен другой ViewModel — ошибка
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}