package com.example

import kotlinx.coroutines.flow.Flow

class StockRepository(private val stockDao: StockDao) {
    val allProducts: Flow<List<ProductEntity>> = stockDao.getAllProducts()

    suspend fun getProductByBarcode(barcode: String): ProductEntity? {
        return stockDao.getProductByBarcode(barcode)
    }

    fun searchProducts(query: String): Flow<List<ProductEntity>> {
        return stockDao.searchProducts(query)
    }

    suspend fun saveOrUpdateProduct(product: ProductEntity) {
        val existing = stockDao.getProductByBarcode(product.barcode)
        if (existing != null) {
            stockDao.updateProduct(product.copy(id = existing.id))
        } else {
            stockDao.insertProduct(product)
        }
    }

    suspend fun recordTransaction(transaction: StockTransactionEntity) {
        stockDao.insertTransaction(transaction)
    }
}
