package ru.contlog.mobile.helper.vm.factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.contlog.mobile.helper.repo.AppPreferencesRepository
import ru.contlog.mobile.helper.vm.AppViewModel

class AppViewModelFactory(private val appPreferencesRepository: AppPreferencesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(appPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}