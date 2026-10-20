package com.example.data.db

import androidx.room.*
import com.example.data.model.PlantProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantProfileDao {
    @Query("SELECT * FROM plant_profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<PlantProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: PlantProfile)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultProfiles(profiles: List<PlantProfile>)

    @Delete
    suspend fun deleteProfile(profile: PlantProfile)

    @Query("SELECT * FROM plant_profiles WHERE name = :name LIMIT 1")
    suspend fun getProfileByName(name: String): PlantProfile?
}
