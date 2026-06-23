package com.example.data

import kotlinx.coroutines.flow.Flow

class SubtitleRepository(private val db: AppDatabase) {

    val allConfigs: Flow<List<AppConfig>> = db.appConfigDao().getAllConfigFlow()
    val allProjects: Flow<List<SubtitleProject>> = db.subtitleProjectDao().getAllProjectsFlow()
    val allLogs: Flow<List<TranscribeLog>> = db.transcribeLogDao().getAllLogsFlow()

    suspend fun getUserByUsername(username: String): User? {
        return db.userDao().getUserByUsername(username)
    }

    suspend fun getUserByEmail(email: String): User? {
        return db.userDao().getUserByEmail(email)
    }

    suspend fun insertUser(user: User) {
        db.userDao().insertUser(user)
    }

    suspend fun deleteUserByUsername(username: String) {
        db.userDao().deleteUserByUsername(username)
    }

    suspend fun setConfig(key: String, value: String) {
        db.appConfigDao().setConfig(AppConfig(key, value))
    }

    suspend fun getConfigValue(key: String): String? {
        return db.appConfigDao().getConfigByKey(key)?.value
    }

    suspend fun createProject(project: SubtitleProject): Long {
        return db.subtitleProjectDao().insertProject(project)
    }

    suspend fun deleteProject(project: SubtitleProject) {
        // Delete lines and project
        db.subtitleLineDao().deleteLinesForProject(project.id)
        db.subtitleProjectDao().deleteProject(project)
    }

    suspend fun getProjectById(id: Long): SubtitleProject? {
        return db.subtitleProjectDao().getProjectById(id)
    }

    fun getLinesForProjectFlow(projectId: Long): Flow<List<SubtitleLine>> {
        return db.subtitleLineDao().getLinesForProjectFlow(projectId)
    }

    suspend fun getLinesForProject(projectId: Long): List<SubtitleLine> {
        return db.subtitleLineDao().getLinesForProject(projectId)
    }

    suspend fun saveSubtitleLines(projectId: Long, lines: List<SubtitleLine>) {
        db.subtitleLineDao().deleteLinesForProject(projectId)
        db.subtitleLineDao().insertLines(lines)
    }

    suspend fun updateSubtitleLine(line: SubtitleLine) {
        db.subtitleLineDao().updateLine(line)
    }

    suspend fun deleteSubtitleLine(line: SubtitleLine) {
        db.subtitleLineDao().deleteLine(line)
    }

    suspend fun insertLog(log: TranscribeLog) {
        db.transcribeLogDao().insertLog(log)
    }

    suspend fun clearLogs() {
        db.transcribeLogDao().clearLogs()
    }
}
