package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val username: String,
    val passwordHash: String, // Plaintext or simple hash for secure/mock admin auth
    val role: String, // "ADMIN" or "USER"
    val email: String? = null
)

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "subtitle_projects")
data class SubtitleProject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val videoUri: String,
    val durationMs: Long,
    val selectedPresetId: String,
    val targetLanguage: String,
    val creationTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "subtitle_lines")
data class SubtitleLine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val startMs: Long,
    val endMs: Long,
    val text: String
)

@Entity(tableName = "transcribe_logs")
data class TranscribeLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val fileName: String,
    val language: String,
    val aiEngine: String,
    val status: String, // "SUCCESS", "FAILED"
    val errorMessage: String? = null
)
