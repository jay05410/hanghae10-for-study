package io.hhplus.ecommerce.inventory.domain.repository

import io.hhplus.ecommerce.inventory.domain.entity.Inventory

interface InventoryRepository {
    fun save(inventory: Inventory): Inventory
    fun findById(id: Long): Inventory?
    fun findByProductId(productId: Long): Inventory?
    fun findByProductIdWithLock(productId: Long): Inventory?
    fun findAll(): List<Inventory>
    fun delete(inventory: Inventory)
}