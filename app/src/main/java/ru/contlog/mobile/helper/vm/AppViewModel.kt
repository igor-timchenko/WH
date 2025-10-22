package ru.contlog.mobile.helper.vm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.contlog.mobile.helper.model.ApiAuthData
import ru.contlog.mobile.helper.model.Division
import ru.contlog.mobile.helper.model.UserData
import ru.contlog.mobile.helper.repo.Api
import ru.contlog.mobile.helper.repo.AppPreferencesRepository

class AppViewModel(private val appPreferencesRepository: AppPreferencesRepository) : ViewModel() {
    var login = appPreferencesRepository.getString("app_login", "")
        set(value) {
            appPreferencesRepository.saveString("app_login", value)
            field = value
        }

    var apiAuthData: ApiAuthData? = appPreferencesRepository.getApiAuthData("app_apiAuthData")
        set(value) {
            appPreferencesRepository.saveApiAuthData("app_apiAuthData", value)
            field = value
        }

    private val _userData: MutableLiveData<UserData?> by lazy {
        MutableLiveData<UserData?>(null)
    }
    val userData: LiveData<UserData?> = _userData

    private val _division: MutableLiveData<List<Division>> by lazy {
        MutableLiveData<List<Division>>(emptyList())
    }
    val division: LiveData<List<Division>> = _division

    private val _errors: MutableLiveData<MutableList<Throwable>> by lazy {
        MutableLiveData<MutableList<Throwable>>(mutableListOf())
    }
    val errors: LiveData<MutableList<Throwable>> = _errors

    fun clearErrors() {
        _errors.value = mutableListOf()
    }

    suspend fun fetchUserData() {
        val result = Api.User.getUserData(apiAuthData!!)
        result.fold(
            { userData ->
                viewModelScope.launch(Dispatchers.Main) {
                    this@AppViewModel._userData.value = userData
                }
            },
            { error ->
                viewModelScope.launch(Dispatchers.Main) {
                    _errors.value = (_errors.value ?: mutableListOf()).let {
                        it.add(error)

                        it
                    }
                }
            }
        )
    }

    suspend fun fetchDivisions() {
        val result = Api.Division.getDivisions(apiAuthData!!)
        result.fold(
            { divisions ->
                viewModelScope.launch(Dispatchers.Main) {
                    this@AppViewModel._division.value = divisions
                }
            },
            { error ->
                viewModelScope.launch(Dispatchers.Main) {
                    _errors.value = (_errors.value ?: mutableListOf()).let {
                        it.add(error)

                        it
                    }
                }
            }
        )
    }

    fun logout() {
        appPreferencesRepository.remove("app_login") // чтобы удалить логин из памяти приложения
        appPreferencesRepository.saveApiAuthData("app_apiAuthData", null)
        apiAuthData = null
    }
}