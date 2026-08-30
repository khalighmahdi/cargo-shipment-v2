package com.example.cargo.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShipmentDao {

    @Query("SELECT * FROM shipments ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Shipment>>

    @Query("SELECT * FROM shipments WHERE id = :id")
    fun getById(id: Int): Flow<Shipment?>

    @Query("SELECT * FROM shipments WHERE jalaliYear = :year ORDER BY createdAt DESC")
    fun getByYear(year: Int): Flow<List<Shipment>>

    @Query("SELECT * FROM shipments WHERE jalaliYear = :year AND jalaliMonth = :month ORDER BY createdAt DESC")
    fun getByMonth(year: Int, month: Int): Flow<List<Shipment>>

    @Query("SELECT * FROM shipments WHERE jalaliYear = :year AND jalaliMonth = :month AND jalaliDay = :day ORDER BY createdAt DESC")
    fun getByDay(year: Int, month: Int, day: Int): Flow<List<Shipment>>

    @Query("SELECT * FROM shipments WHERE status = :status ORDER BY createdAt DESC")
    fun getByStatus(status: String): Flow<List<Shipment>>

    @Query("SELECT * FROM shipments WHERE cargoDescription LIKE :q OR senderName LIKE :q OR receiverName LIKE :q OR destination LIKE :q OR notes LIKE :q ORDER BY createdAt DESC")
    fun search(q: String): Flow<List<Shipment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shipment: Shipment): Long

    @Update
    suspend fun update(shipment: Shipment)

    @Delete
    suspend fun delete(shipment: Shipment)

    @Query("DELETE FROM shipments WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT COUNT(*) FROM shipments")
    fun count(): Flow<Int>

    @Query("SELECT COUNT(*) FROM shipments WHERE status = :status")
    fun countByStatus(status: String): Flow<Int>
}
