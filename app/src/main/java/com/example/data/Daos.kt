package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("DELETE FROM users WHERE username = :username")
    suspend fun deleteUserByUsername(username: String)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config")
    fun getAllConfigFlow(): Flow<List<AppConfig>>

    @Query("SELECT * FROM app_config WHERE `key` = :key LIMIT 1")
    suspend fun getConfigByKey(key: String): AppConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setConfig(config: AppConfig)

    @Query("DELETE FROM app_config WHERE `key` = :key")
    suspend fun deleteConfig(key: String)
}

@Dao
interface SubtitleProjectDao {
    @Query("SELECT * FROM subtitle_projects ORDER BY creationTime DESC")
    fun getAllProjectsFlow(): Flow<List<SubtitleProject>>

    @Query("SELECT * FROM subtitle_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): SubtitleProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: SubtitleProject): Long

    @Delete
    suspend fun deleteProject(project: SubtitleProject)
}

@Dao
interface SubtitleLineDao {
    @Query("SELECT * FROM subtitle_lines WHERE projectId = :projectId ORDER BY startMs ASC")
    fun getLinesForProjectFlow(projectId: Long): Flow<List<SubtitleLine>>

    @Query("SELECT * FROM subtitle_lines WHERE projectId = :projectId ORDER BY startMs ASC")
    suspend fun getLinesForProject(projectId: Long): List<SubtitleLine>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<SubtitleLine>)

    @Query("DELETE FROM subtitle_lines WHERE projectId = :projectId")
    suspend fun deleteLinesForProject(projectId: Long)

    @Update
    suspend fun updateLine(line: SubtitleLine)

    @Delete
    suspend fun deleteLine(line: SubtitleLine)
}

@Dao
interface TranscribeLogDao {
    @Query("SELECT * FROM transcribe_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<TranscribeLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TranscribeLog)

    @Query("DELETE FROM transcribe_logs")
    suspend fun clearLogs()
}
