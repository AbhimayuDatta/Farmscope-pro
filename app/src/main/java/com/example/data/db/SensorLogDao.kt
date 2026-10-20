package com.example.data.db

import androidx.room.*
import com.example.data.model.SensorLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorLogDao {
    @Query("SELECT * FROM sensor_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<SensorLog>>

    @Query("SELECT * FROM sensor_logs ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<SensorLog>

    @Query("SELECT * FROM sensor_logs WHERE matrixCellId = :cellId ORDER BY timestamp DESC")
    fun getLogsForCellFlow(cellId: String): Flow<List<SensorLog>>

    @Query("SELECT * FROM sensor_logs WHERE matrixCellId = :cellId ORDER BY timestamp DESC")
    suspend fun getLogsForCell(cellId: String): List<SensorLog>

    @Query("SELECT * FROM sensor_logs WHERE matrixCellId = :cellId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestLogForCellFlow(cellId: String): Flow<SensorLog?>

    @Query("SELECT * FROM sensor_logs WHERE matrixCellId = :cellId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestLogForCell(cellId: String): SensorLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SensorLog): Long

    @Query("DELETE FROM sensor_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM sensor_logs WHERE matrixCellId = :cellId")
    suspend fun deleteLogsForCell(cellId: String)

    @Query("DELETE FROM sensor_logs")
    suspend fun clearAllLogs()
}
