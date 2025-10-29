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

class ProductInfoViewModel() : ViewModel() {
    private val _scannedCode: MutableLiveData<String?> by lazy {
        MutableLiveData(null)
    }
    val scannedCode: LiveData<String?> = _scannedCode
    fun setScannedCode(scannedCode: String) {
        _scannedCode.value = scannedCode
    }

    private val _division: MutableLiveData<Division> by lazy {
        MutableLiveData(null)
    }
    val division: LiveData<Division> = _division
    fun setDivision(division: Division) {
        _division.value = division
    }

    private val _products: MutableLiveData<List<Product>?> by lazy {
        MutableLiveData(null)
    }
    val products: LiveData<List<Product>?> = _products
    fun setProducts(products: List<Product>?) {
        _products.value = products
    }

    suspend fun fetchUserData(apiAuthData: ApiAuthData, scannedCode: String) {
        val division = division.value!!
        val params = ProductInfoParams(
            division.uid,
            division.divisionCode,
            scannedCode,
        )
        val result = Api.Division.Product.getProductInfo(apiAuthData, params)
        result.fold(
            { productsData ->
                viewModelScope.launch(Dispatchers.Main) {
                    setProducts(productsData)
                }
            },
            { error ->
                viewModelScope.launch(Dispatchers.Main) {
                    // TODO: implement
                }
            }
        )
    }
}