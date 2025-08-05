package com.example.aiassistant.data.local

import androidx.room.*
import com.example.aiassistant.data.models.LLMConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface LLMConfigDao {
    @Query("SELECT * FROM llm_configs")
    fun getAllConfigs(): Flow<List<LLMConfig>>

    @Query("SELECT * FROM llm_configs WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveConfig(): LLMConfig?

    @Query("SELECT * FROM llm_configs WHERE id = :id")
    suspend fun getConfigById(id: String): LLMConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: LLMConfig)

    @Update
    suspend fun updateConfig(config: LLMConfig)

    @Delete
    suspend fun deleteConfig(config: LLMConfig)

    @Query("UPDATE llm_configs SET isActive = 0")
    suspend fun deactivateAllConfigs()

    @Query("UPDATE llm_configs SET isActive = 1 WHERE id = :configId")
    suspend fun activateConfig(configId: String)
}

