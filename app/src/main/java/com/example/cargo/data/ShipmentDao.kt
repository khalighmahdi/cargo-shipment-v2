package com.example.cargo.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

interface ShipmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shipment: Shipment): Long

    @Update
    suspend fun update(shipment: Shipment)

    @Delete
    suspend fun delete(shipment: Shipment)

    @Query("DELETE FROM shipments WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM shipments ORDER BY id DESC")
    fun getAll(): Flow<List<Shipment>>

    @Query("SELECT * FROM shipments WHERE id = :id")
    fun getById(id: Int): Flow<Shipment?>

    @Query("SELECT * FROM shipments WHERE status = :status ORDER BY id DESC")
    fun getByStatus(status: String): Flow<List<Shipment>>

    @Query("SELECT * FROM shipments WHERE jalaliYear = :year ORDER BY jalaliDay DESC")
    fun getByYear(year: Int): Flow<List<Shipment>>

    @Query("SELECT * FROM shipments WHERE jalaliYear = :year AND jalaliMonth = :month ORDER BY jalaliDay DESC")
    fun getByMonth(year: Int, month: Int): Flow<List<Shipment>>

    @Query("SELECT * FROM shipments WHERE jalaliYear = :year AND jalaliMonth = :month AND jalaliDay = :day ORDER BY id DESC")
    fun getByDay(year: Int, month: Int, day: Int): Flow<List<Shipment>>

    @Query("""SELECT * FROM shipments WHERE 
              cargoDescription LIKE '%' || :query || '%' OR
              senderName LIKE '%' || :query || '%' OR
              receiverName LIKE '%' || :query || '%' OR
              destination LIKE '%' || :query || '%' OR
              senderPhone LIKE '%' || :query || '%' OR
              receiverPhone LIKE '%' || :query || '%' OR
              notes LIKE '%' || :query || '%'
              ORDER BY id DESC""")
    fun search(query: String): Flow<List<Shipment>>

    @Query("SELECT COUNT(*) FROM shipments WHERE status = :status")
    fun countByStatus(status: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM shipments")
    fun count(): Flow<Int>
}

interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: Contact): Long

    @Update
    suspend fun update(contact: Contact)

    @Delete
    suspend fun delete(contact: Contact)

    @Query("SELECT * FROM contacts ORDER BY name COLLATE NOCASE")
    fun getAll(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE")
    fun search(query: String): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE id = :id")
    fun getById(id: Int): Flow<Contact?>

    @Query("SELECT * FROM contacts WHERE phone = :phone LIMIT 1")
    suspend fun getByPhone(phone: String): Contact?
}
