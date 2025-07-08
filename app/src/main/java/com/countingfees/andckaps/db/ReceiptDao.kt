package com.countingfees.andckaps.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update



@Dao
interface ReceiptDao {
    @Insert
    suspend fun insert(receipt: Receipt)

    @Update
    suspend fun update(receipt: Receipt)

    @Delete
    suspend fun delete(receipt: Receipt)

    @Query("SELECT * FROM receipts ORDER BY date DESC")
    suspend fun getAll(): List<Receipt>

    @Query("SELECT * FROM receipts WHERE category = :category")
    suspend fun getByCategory(category: String): List<Receipt>

    @Query("DELETE FROM receipts")
    suspend fun deleteAll()

}
