package com.nickrankin.traktapp.dao.auth

import androidx.room.*
import com.nickrankin.traktapp.dao.auth.model.AuthUser
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthUserDao {
    @Transaction
    @Query("SELECT * FROM users WHERE slug = :slug")
    fun getUser(slug: String): Flow<AuthUser?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(authUser: AuthUser)

    @Update
    fun updateUser(authUser: AuthUser)

    @Delete
    fun deleteUser(authUser: AuthUser)
}