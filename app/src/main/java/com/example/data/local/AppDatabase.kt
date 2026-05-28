package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vip_users")
data class VipUserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isVip: Boolean,
    val requestedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_configs")
data class AppConfigEntity(
    @PrimaryKey val configKey: String,
    val configValue: String
)

@Dao
interface AdminDao {
    @Query("SELECT * FROM vip_users ORDER BY requestedAt DESC")
    fun getAllVipUsersFlow(): Flow<List<VipUserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVipUser(user: VipUserEntity)

    @Query("DELETE FROM vip_users WHERE id = :id")
    suspend fun deleteVipUser(id: String)

    @Query("SELECT * FROM vip_users WHERE id = :id LIMIT 1")
    suspend fun getVipUserById(id: String): VipUserEntity?

    @Query("SELECT * FROM app_configs WHERE configKey = :key LIMIT 1")
    suspend fun getConfigByKey(key: String): AppConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AppConfigEntity)
}

@Database(entities = [VipUserEntity::class, AppConfigEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract val adminDao: AdminDao
}
