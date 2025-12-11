package ru.contlog.mobile.helper.vm

// Импорты архитектурных компонентов Android для работы с ViewModel и LiveData
import androidx.lifecycle.LiveData         // Наблюдаемый поток данных (только для чтения)
import androidx.lifecycle.MutableLiveData  // Изменяемая версия LiveData (внутреннее состояние)
import androidx.lifecycle.ViewModel        // Базовый класс ViewModel
import androidx.lifecycle.viewModelScope   // Область корутин, привязанная к жизненному циклу ViewModel
// Импорты диспетчеров и корутин из kotlinx
import kotlinx.coroutines.Dispatchers       // Диспетчеры потоков (Main, IO и др.)
import kotlinx.coroutines.launch           // Запуск корутины
// Модели данных приложения
import ru.contlog.mobile.helper.model.ApiAuthData
import ru.contlog.mobile.helper.model.Division
import ru.contlog.mobile.helper.model.Product
import ru.contlog.mobile.helper.model.ProductInfoParams
// Репозиторий для работы с сетевыми запросами
import ru.contlog.mobile.helper.repo.Api

// ViewModel, управляющий состоянием экрана информации о продукте (после сканирования штрихкода)
class ProductInfoViewModel() : ViewModel() {
    // Внутреннее изменяемое состояние для хранения отсканированного кода
    private val _scannedCode: MutableLiveData<String?> by lazy {
        MutableLiveData(null) // Изначально — null
    }
    // Публичный LiveData для наблюдения за отсканированным кодом (только чтение)
    val scannedCode: LiveData<String?> = _scannedCode
    // Метод для установки значения отсканированного кода извне
    fun setScannedCode(scannedCode: String) {
        _scannedCode.value = scannedCode
    }

    // Внутреннее изменяемое состояние для хранения выбранного подразделения
    private val _division: MutableLiveData<Division> by lazy {
        MutableLiveData(null) // Изначально — null
    }
    // Публичный LiveData для наблюдения за подразделением
    val division: LiveData<Division> = _division
    // Метод для установки подразделения (обычно вызывается при старте фрагмента)
    fun setDivision(division: Division) {
        _division.value = division
    }

    // Внутреннее изменяемое состояние для хранения списка продуктов, полученных по штрихкоду
    private val _products: MutableLiveData<List<Product>?> by lazy {
        MutableLiveData(null) // Изначально — null (данные ещё не загружены)
    }
    // Публичный LiveData для наблюдения за списком продуктов
    val products: LiveData<List<Product>?> = _products
    // Метод для установки списка продуктов (например, после загрузки или для мок-данных)
    fun setProducts(products: List<Product>?) {
        _products.value = products
    }

    // Внутреннее изменяемое состояние для хранения ошибок
    private val _errors: MutableLiveData<MutableList<Throwable>> by lazy {
        MutableLiveData(mutableListOf())
    }
    // Публичный LiveData для наблюдения за ошибками
    val errors: LiveData<MutableList<Throwable>> = _errors
    // Метод для очистки ошибок
    fun clearErrors() {
        _errors.value = mutableListOf()
    }

    // Suspend-функция для загрузки данных о продукте по отсканированному коду
    suspend fun fetchUserData(apiAuthData: ApiAuthData, scannedCode: String) {
        // Получаем текущее подразделение из LiveData (гарантированно не null благодаря !!)
        val division = division.value!!
        // Формируем параметры запроса на основе подразделения и штрихкода
        val params = ProductInfoParams(
            division.uid,          // UID подразделения
            division.divisionCode, // Числовой код подразделения
            scannedCode,           // Отсканированный штрихкод
        )
        // Выполняем сетевой запрос через репозиторий Api
        val result = Api.Division.Product.getProductInfo(apiAuthData, params)
        result.fold(
            // В случае успеха (получен список Product)
            { productsData ->
                // Фильтруем продукты: исключаем те, у которых отсутствует массив "Данные" (places пустой)
                val filteredProducts = productsData.filter { it.places.isNotEmpty() }
                // Обновляем LiveData на главном (UI) потоке
                viewModelScope.launch(Dispatchers.Main) {
                    setProducts(filteredProducts)
                }
            },
            // В случае ошибки (например, сетевая ошибка или 404)
            { error ->
                // Обрабатываем ошибку на главном потоке
                viewModelScope.launch(Dispatchers.Main) {
                    // Сохраняем ошибку в LiveData для отображения пользователю
                    val currentErrors = _errors.value ?: mutableListOf()
                    currentErrors.add(error)
                    _errors.value = currentErrors
                    // Устанавливаем пустой список продуктов при ошибке
                    setProducts(emptyList())
                }
            }
        )
    }
}