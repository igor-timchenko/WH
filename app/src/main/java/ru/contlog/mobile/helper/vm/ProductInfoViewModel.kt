// Пакет ViewModel'ей приложения
package ru.contlog.mobile.helper.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.contlog.mobile.helper.model.ApiAuthData
import ru.contlog.mobile.helper.model.Division
import ru.contlog.mobile.helper.model.Product
import ru.contlog.mobile.helper.model.ProductInfoParams
import ru.contlog.mobile.helper.repo.Api

/**
 * ViewModel для экрана информации о продукте (после сканирования штрихкода).
 *
 * Управляет:
 *   - отсканированным кодом,
 *   - выбранным подразделением,
 *   - списком найденных продуктов.
 *
 * Отвечает за загрузку данных с сервера по отсканированному коду в контексте подразделения.
 */
class ProductInfoViewModel() : ViewModel() {

    // === Отсканированный штрихкод ===
    private val _scannedCode: MutableLiveData<String?> by lazy {
        MutableLiveData(null)
    }
    val scannedCode: LiveData<String?> = _scannedCode

    fun setScannedCode(scannedCode: String) {
        _scannedCode.value = scannedCode
    }

    // === Выбранное подразделение (передаётся из предыдущего экрана) ===
    private val _division: MutableLiveData<Division> by lazy {
        MutableLiveData(null)
    }
    val division: LiveData<Division> = _division

    fun setDivision(division: Division) {
        _division.value = division
    }

    // === Список продуктов, полученных по штрихкоду ===
    private val _products: MutableLiveData<List<Product>?> by lazy {
        MutableLiveData(null)
    }
    val products: LiveData<List<Product>?> = _products

    fun setProducts(products: List<Product>?) {
        _products.value = products
    }

    /**
     * Загружает информацию о продукте по отсканированному коду.
     *
     * ⚠️ Важно: функция suspend — вызывается из корутины (обычно из фрагмента).
     *
     * @param apiAuthData — данные авторизации (uid + api_key)
     * @param scannedCode — штрихкод, полученный от сканера
     */
    suspend fun fetchUserData(apiAuthData: ApiAuthData, scannedCode: String) {
        // ⚠️ Опасное использование !! — если division.value == null, будет краш
        val division = division.value!!

        val params = ProductInfoParams(
            divisionUID = division.uid,
            divisionCode = division.divisionCode,
            productCode = scannedCode, // ← здесь scannedCode используется как productCode
        )

        val result = Api.Division.Product.getProductInfo(apiAuthData, params)

        result.fold(
            { productsData ->
                // Успех: обновляем список продуктов в основном потоке
                viewModelScope.launch(Dispatchers.Main) {
                    setProducts(productsData)
                }
            },
            { error ->
                // Ошибка: пока не реализована обработка
                viewModelScope.launch(Dispatchers.Main) {
                    // TODO: implement
                    // Например: показать Snackbar, добавить в список ошибок и т.д.
                }
            }
        )
    }
}