package com.example.cargo.data

import kotlinx.coroutines.flow.Flow

class ShipmentRepository(private val dao: ShipmentDao) {

    val allShipments: Flow<List<Shipment>> = dao.getAll()

    fun getById(id: Int): Flow<Shipment?> = dao.getById(id)

    fun getByYear(year: Int): Flow<List<Shipment>> = dao.getByYear(year)

    fun getByMonth(year: Int, month: Int): Flow<List<Shipment>> = dao.getByMonth(year, month)

    fun getByDay(year: Int, month: Int, day: Int): Flow<List<Shipment>> = dao.getByDay(year, month, day)

    fun getByStatus(status: String): Flow<List<Shipment>> = dao.getByStatus(status)

    fun search(query: String): Flow<List<Shipment>> = dao.search("%$query%")

    fun count(): Flow<Int> = dao.count()

    fun countByStatus(status: String): Flow<Int> = dao.countByStatus(status)

    suspend fun insert(shipment: Shipment): Long = dao.insert(shipment)

    suspend fun update(shipment: Shipment) = dao.update(shipment)

    suspend fun delete(shipment: Shipment) = dao.delete(shipment)

    suspend fun deleteById(id: Int) = dao.deleteById(id)
}
