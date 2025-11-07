package ru.contlog.mobile.helper.vm

// Импорты стандартных и вспомогательных классов
import androidx.lifecycle.LiveData         // Наблюдаемый поток данных, доступный только для чтения
import androidx.lifecycle.MutableLiveData  // Изменяемая версия LiveData
import androidx.lifecycle.ViewModel        // Базовый класс ViewModel из архитектурных компонентов Android
import androidx.lifecycle.viewModelScope   // Область корутин, привязанная к жизненному циклу ViewModel
import kotlinx.coroutines.Dispatchers       // Диспетчеры корутин (Main, IO и др.)
import kotlinx.coroutines.launch           // Запуск корутины
// Модели данных
import ru.contlog.mobile.helper.model.ApiAuthData
import ru.contlog.mobile.helper.model.Division
import ru.contlog.mobile.helper.model.UserData
// Репозитории для работы с API и настройками
import ru.contlog.mobile.helper.repo.Api
import ru.contlog.mobile.helper.repo.AppPreferencesRepository

// Основной ViewModel приложения, управляющий состоянием авторизации, пользователя и подразделений
class AppViewModel(private val appPreferencesRepository: AppPreferencesRepository) : ViewModel() {
    // Свойство login: при чтении возвращает сохранённое значение, при записи — сохраняет в настройки
    var login = appPreferencesRepository.getString("app_login", "")
        set(value) {
            // Сохраняем новое значение в репозиторий настроек
            appPreferencesRepository.saveString("app_login", value)
            // Обновляем внутреннее значение поля
            field = value
        }

    // Свойство apiAuthData: хранит данные авторизации (uid, apiKey)
    // При чтении загружает из настроек, при записи — сохраняет
    var apiAuthData: ApiAuthData? = appPreferencesRepository.getApiAuthData("app_apiAuthData")
        set(value) {
            // Сохраняем в репозиторий (включая null)
            appPreferencesRepository.saveApiAuthData("app_apiAuthData", value)
            // Обновляем внутреннее значение
            field = value
        }

    // MutableLiveData для данных пользователя (внутреннее изменяемое состояние)
    // Инициализируется лениво при первом доступе
    private val _userData: MutableLiveData<UserData?> by lazy {
        MutableLiveData<UserData?>(null)
    }
    // Публичный LiveData (только для чтения), предоставляемый внешним наблюдателям
    val userData: LiveData<UserData?> = _userData

    // MutableLiveData для списка подразделений (внутреннее изменяемое состояние)
    private val _division: MutableLiveData<List<Division>> by lazy {
        MutableLiveData<List<Division>>(appPreferencesRepository.getDivisionsList("app_divisionsList") ?: mutableListOf())
    }
    // Публичный LiveData для списка подразделений
    val division: LiveData<List<Division>> = _division

    // MutableLiveData для списка ошибок (внутреннее изменяемое состояние)
    private val _errors: MutableLiveData<MutableList<Throwable>> by lazy {
        MutableLiveData<MutableList<Throwable>>(mutableListOf())
    }
    // Публичный LiveData для ошибок
    val errors: LiveData<MutableList<Throwable>> = _errors

    // Метод для очистки списка ошибок
    fun clearErrors() {
        _errors.value = mutableListOf()
    }

    private val _internetAvailable: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>(true)
    }
    val internetAvailable: LiveData<Boolean> = _internetAvailable
    fun setInternetAvailableState(available: Boolean) {
        _internetAvailable.value = available
    }

    // Suspend-функция для загрузки данных пользователя с сервера
    suspend fun fetchUserData() : Boolean {
        // Выполняем запрос к API с использованием сохранённых данных авторизации
        val result = Api.User.getUserData(apiAuthData!!)
        return result.fold(
            // В случае успеха (получен UserData)
            { userData ->
                // Обновляем LiveData на главном потоке (UI-поток)
                viewModelScope.launch(Dispatchers.Main) {
                    this@AppViewModel._userData.value = userData
                }

                true
            },
            // В случае ошибки
            { error ->
                // Добавляем ошибку в список ошибок на главном потоке
                viewModelScope.launch(Dispatchers.Main) {
                    _errors.value = (_errors.value ?: mutableListOf()).let {
                        it.add(error)
                        it
                    }
                }

                false
            }
        )
    }

    // Suspend-функция для загрузки списка подразделений с сервера
    suspend fun fetchDivisions() {
        // Выполняем запрос к API с использованием данных авторизации
        val result = Api.Division.getDivisions(apiAuthData!!)
        result.fold(
            // В случае успеха (получен список Division)
            { divisions ->
                appPreferencesRepository.saveDivisionsList("app_divisionsList", divisions)
                // Обновляем LiveData на главном потоке
                viewModelScope.launch(Dispatchers.Main) {
                    this@AppViewModel._division.value = divisions
                }
            },
            // В случае ошибки
            { error ->
                // Добавляем ошибку в список ошибок на главном потоке
                viewModelScope.launch(Dispatchers.Main) {
                    _errors.value = (_errors.value ?: mutableListOf()).let {
                        it.add(error)
                        it
                    }
                }
            }
        )
    }

    // Метод для выхода из системы (logout)
    fun logout() {
        // Удаляем сохранённый логин из настроек
        appPreferencesRepository.remove("app_login")
        // Сохраняем null вместо данных авторизации
        appPreferencesRepository.saveApiAuthData("app_apiAuthData", null)
        // Обнуляем локальное поле
        apiAuthData = null
    }
}