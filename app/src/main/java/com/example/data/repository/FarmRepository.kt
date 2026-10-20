package com.example.data.repository

import com.example.data.db.PlantProfileDao
import com.example.data.db.SensorLogDao
import com.example.data.model.PlantProfile
import com.example.data.model.SensorLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FarmRepository(
    private val plantProfileDao: PlantProfileDao,
    private val sensorLogDao: SensorLogDao
) {
    val allProfiles: Flow<List<PlantProfile>> = plantProfileDao.getAllProfiles()
    val allLogs: Flow<List<SensorLog>> = sensorLogDao.getAllLogsFlow()

    suspend fun checkAndPrepopulateDefaults() {
        val existing = plantProfileDao.getAllProfiles().first()
        if (existing.isEmpty()) {
            plantProfileDao.insertDefaultProfiles(PlantProfile.getDefaults())
        }
    }

    suspend fun insertProfile(profile: PlantProfile) {
        plantProfileDao.insertProfile(profile)
    }

    suspend fun deleteProfile(profile: PlantProfile) {
        plantProfileDao.deleteProfile(profile)
    }

    suspend fun getProfileByName(name: String): PlantProfile? {
        return plantProfileDao.getProfileByName(name)
    }

    fun getLogsForCellFlow(cellId: String): Flow<List<SensorLog>> {
        return sensorLogDao.getLogsForCellFlow(cellId)
    }

    suspend fun getLogsForCell(cellId: String): List<SensorLog> {
        return sensorLogDao.getLogsForCell(cellId)
    }

    fun getLatestLogForCellFlow(cellId: String): Flow<SensorLog?> {
        return sensorLogDao.getLatestLogForCellFlow(cellId)
    }

    suspend fun getLatestLogForCell(cellId: String): SensorLog? {
        return sensorLogDao.getLatestLogForCell(cellId)
    }

    suspend fun insertLog(log: SensorLog): Long {
        return sensorLogDao.insertLog(log)
    }

    suspend fun deleteLogById(id: Long) {
        sensorLogDao.deleteLogById(id)
    }

    suspend fun deleteLogsForCell(cellId: String) {
        sensorLogDao.deleteLogsForCell(cellId)
    }

    suspend fun getAllLogsList(): List<SensorLog> {
        return sensorLogDao.getAllLogs()
    }

    suspend fun clearAllLogs() {
        sensorLogDao.clearAllLogs()
    }
}
