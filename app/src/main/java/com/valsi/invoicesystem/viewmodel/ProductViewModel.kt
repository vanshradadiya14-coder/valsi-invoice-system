package com.valsi.invoicesystem.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valsi.invoicesystem.data.entity.Product
import com.valsi.invoicesystem.data.repository.ProductRepository
import com.valsi.invoicesystem.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProductViewModel @Inject constructor(
    private val repository: ProductRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val products: StateFlow<List<Product>> = _query
        .flatMapLatest { repository.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currencySymbol: StateFlow<String> = settingsRepository.observe()
        .map { it.currencySymbol }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "£")

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun updatePrice(productId: Long, price: Double) {
        viewModelScope.launch { repository.updatePrice(productId, price) }
    }

    fun setActive(productId: Long, active: Boolean) {
        viewModelScope.launch { repository.setActive(productId, active) }
    }
}
