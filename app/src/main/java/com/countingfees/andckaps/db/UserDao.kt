package com.countingfees.andckaps.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: User)

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUser(): User?

    @Query("DELETE FROM user_profile")
    suspend fun clearUser()
}
