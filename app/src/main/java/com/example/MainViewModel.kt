package com.example

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProductFormState(
    val stockCode: String = "",
    val stockName: String = "",
    val unit: String = "",
    val currentQuantity: Int = 0,
    val brand: String = "",
    val barcode: String = "",
    val location: String = "",
    val stockLocation: String = "",
    val description: String = "",
    val transactionQuantity: String = "1",
    val transactionType: String = "IN", // "IN" or "OUT"
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: StockRepository
    
    val allProducts: StateFlow<List<ProductEntity>>
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    private val _searchResults = MutableStateFlow<List<ProductEntity>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _formState = MutableStateFlow(ProductFormState())
    val formState = _formState.asStateFlow()
    
    private val _isScanningPaused = MutableStateFlow(false)
    val isScanningPaused = _isScanningPaused.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = StockRepository(database.stockDao())
        
        allProducts = repository.allProducts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            repository.searchProducts(query).collect { results ->
                _searchResults.value = results
            }
        }
    }

    fun onBarcodeScanned(rawBarcode: String) {
        if (_isScanningPaused.value) return
        
        _isScanningPaused.value = true
        _formState.update { it.copy(errorMessage = null, successMessage = null) }
        
        val parsed = BarcodeParser.parse(rawBarcode)
        
        viewModelScope.launch {
            if (parsed.isFullData) {
                // Pre-fill form, maybe update DB immediately? Let's just fill form
                val existingProduct = repository.getProductByBarcode(parsed.barcode)
                
                _formState.update { 
                    it.copy(
                        stockCode = parsed.stockCode,
                        stockName = parsed.stockName,
                        unit = parsed.unit,
                        currentQuantity = existingProduct?.currentQuantity ?: parsed.quantity,
                        brand = parsed.brand,
                        barcode = parsed.barcode,
                        location = parsed.location,
                        stockLocation = parsed.stockLocation,
                        description = parsed.description
                    )
                }
            } else {
                // Search for it in DB
                val existingProduct = repository.getProductByBarcode(parsed.barcode)
                if (existingProduct != null) {
                    _formState.update {
                        it.copy(
                            stockCode = existingProduct.stockCode,
                            stockName = existingProduct.stockName,
                            unit = existingProduct.unit,
                            currentQuantity = existingProduct.currentQuantity,
                            brand = existingProduct.brand,
                            barcode = existingProduct.barcode,
                            location = existingProduct.location,
                            stockLocation = existingProduct.stockLocation,
                            description = existingProduct.description
                        )
                    }
                } else {
                    _formState.update {
                        ProductFormState(
                            barcode = parsed.barcode,
                            errorMessage = "Ürün veritabanında bulunamadı. Lütfen bilgileri manuel giriniz."
                        )
                    }
                }
            }
        }
    }

    fun resumeScanning() {
        _isScanningPaused.value = false
        _formState.value = ProductFormState()
    }

    fun updateFormState(update: (ProductFormState) -> ProductFormState) {
        _formState.update(update)
    }

    fun saveTransaction() {
        val currentState = _formState.value
        val transQty = currentState.transactionQuantity.toIntOrNull() ?: 0
        
        if (transQty <= 0) {
            _formState.update { it.copy(errorMessage = "Geçerli bir miktar giriniz!") }
            return
        }

        if (currentState.barcode.isBlank() || currentState.stockName.isBlank()) {
            _formState.update { it.copy(errorMessage = "Barkod ve Stok Adı zorunludur!") }
            return
        }

        val newQuantity = if (currentState.transactionType == "IN") {
            currentState.currentQuantity + transQty
        } else {
            if (currentState.currentQuantity < transQty) {
                _formState.update { it.copy(errorMessage = "Yetersiz Stok!") }
                return
            }
            currentState.currentQuantity - transQty
        }

        viewModelScope.launch {
            val product = ProductEntity(
                stockCode = currentState.stockCode,
                stockName = currentState.stockName,
                unit = currentState.unit,
                currentQuantity = newQuantity,
                brand = currentState.brand,
                barcode = currentState.barcode,
                location = currentState.location,
                stockLocation = currentState.stockLocation,
                description = currentState.description,
                lastUpdated = System.currentTimeMillis()
            )

            val transaction = StockTransactionEntity(
                barcode = currentState.barcode,
                transactionType = currentState.transactionType,
                quantity = transQty,
                timestamp = System.currentTimeMillis()
            )

            repository.saveOrUpdateProduct(product)
            repository.recordTransaction(transaction)

            _formState.update { 
                it.copy(
                    currentQuantity = newQuantity,
                    successMessage = "İşlem başarıyla kaydedildi!",
                    errorMessage = null
                )
            }
        }
    }

    fun exportToCsv(uri: Uri, context: Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        // UTF-8 BOM for Excel compatibility
                        outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                        
                        val writer = outputStream.bufferedWriter()
                        writer.write("Stok Kodu;Stok Adı;Birim;Miktar;Marka;Barkod;Konum;Stok Yeri;Açıklama\n")
                        
                        val products = allProducts.value
                        for (p in products) {
                            val row = listOf(
                                p.stockCode,
                                p.stockName,
                                p.unit,
                                p.currentQuantity.toString(),
                                p.brand,
                                p.barcode,
                                p.location,
                                p.stockLocation,
                                p.description
                            ).joinToString(";") { field ->
                                val escaped = field.replace("\"", "\"\"")
                                "\"$escaped\""
                            }
                            writer.write("$row\n")
                        }
                        writer.flush()
                    }
                }
                onResult(true, "Dışa aktarma başarılı!")
            } catch (e: Exception) {
                onResult(false, "Hata oluştu: ${e.message}")
            }
        }
    }
}
