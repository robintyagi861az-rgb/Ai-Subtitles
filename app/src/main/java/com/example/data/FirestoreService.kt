package com.example.data

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FirestoreService {
    private const val TAG = "FirestoreService"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Firestore is not initialized. Ensure google-services.json is present.", e)
            null
        }
    }

    fun isAvailable(): Boolean {
        return firestore != null
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: RuntimeException("Firebase task failed"))
            }
        }
    }

    suspend fun saveUserProfile(user: User): Boolean = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext false
        try {
            val data = mapOf(
                "username" to user.username,
                "passwordHash" to user.passwordHash,
                "role" to user.role,
                "email" to user.email
            )
            db.collection("users").document(user.username).set(data).awaitTask()
            Log.d(TAG, "User profile saved to Firestore: ${user.username}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user profile to Firestore", e)
            false
        }
    }

    suspend fun fetchUserProfile(username: String): User? = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext null
        try {
            val doc = db.collection("users").document(username).get().awaitTask()
            if (doc.exists()) {
                val pwdHash = doc.getString("passwordHash") ?: ""
                val role = doc.getString("role") ?: "USER"
                val email = doc.getString("email")
                User(username = username, passwordHash = pwdHash, role = role, email = email)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch user profile from Firestore for: $username", e)
            null
        }
    }

    suspend fun fetchUserProfileByEmail(email: String): User? = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext null
        try {
            val querySnapshot = db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .awaitTask()
            
            if (!querySnapshot.isEmpty) {
                val doc = querySnapshot.documents[0]
                val username = doc.getString("username") ?: ""
                val pwdHash = doc.getString("passwordHash") ?: ""
                val role = doc.getString("role") ?: "USER"
                User(username = username, passwordHash = pwdHash, role = role, email = email)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch user profile by email from Firestore: $email", e)
            null
        }
    }

    suspend fun saveSubtitleProject(
        project: SubtitleProject,
        lines: List<SubtitleLine>,
        ownerUsername: String
    ): Boolean = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext false
        try {
            val linesData = lines.map {
                mapOf(
                    "startMs" to it.startMs,
                    "endMs" to it.endMs,
                    "text" to it.text
                )
            }
            val data = mapOf(
                "id" to project.id,
                "title" to project.title,
                "videoUri" to project.videoUri,
                "durationMs" to project.durationMs,
                "selectedPresetId" to project.selectedPresetId,
                "targetLanguage" to project.targetLanguage,
                "creationTime" to project.creationTime,
                "ownerUsername" to ownerUsername,
                "lines" to linesData
            )
            // Use ownerUsername + "_" + project.id as the firestore document ID to avoid conflicts
            val docId = "${ownerUsername}_${project.id}"
            db.collection("projects").document(docId).set(data).awaitTask()
            Log.d(TAG, "Subtitle project saved to Firestore: $docId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save project to Firestore", e)
            false
        }
    }

    suspend fun deleteSubtitleProject(projectId: Long, ownerUsername: String): Boolean = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext false
        try {
            val docId = "${ownerUsername}_$projectId"
            db.collection("projects").document(docId).delete().awaitTask()
            Log.d(TAG, "Subtitle project deleted from Firestore: $docId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete project from Firestore", e)
            false
        }
    }

    suspend fun fetchProjectsForUser(ownerUsername: String): List<Pair<SubtitleProject, List<SubtitleLine>>> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext emptyList()
        try {
            val querySnapshot = db.collection("projects")
                .whereEqualTo("ownerUsername", ownerUsername)
                .get()
                .awaitTask()
            
            val resultList = mutableListOf<Pair<SubtitleProject, List<SubtitleLine>>>()
            for (doc in querySnapshot.documents) {
                val id = doc.getLong("id") ?: 0L
                val title = doc.getString("title") ?: "Untitled"
                val videoUri = doc.getString("videoUri") ?: ""
                val durationMs = doc.getLong("durationMs") ?: 0L
                val selectedPresetId = doc.getString("selectedPresetId") ?: "default"
                val targetLanguage = doc.getString("targetLanguage") ?: "English"
                val creationTime = doc.getLong("creationTime") ?: System.currentTimeMillis()

                val proj = SubtitleProject(
                    id = id,
                    title = title,
                    videoUri = videoUri,
                    durationMs = durationMs,
                    selectedPresetId = selectedPresetId,
                    targetLanguage = targetLanguage,
                    creationTime = creationTime
                )

                val linesList = mutableListOf<SubtitleLine>()
                val linesRaw = doc.get("lines") as? List<Map<String, Any>>
                linesRaw?.forEach { lineMap ->
                    val startMs = (lineMap["startMs"] as? Number)?.toLong() ?: 0L
                    val endMs = (lineMap["endMs"] as? Number)?.toLong() ?: 0L
                    val text = lineMap["text"] as? String ?: ""
                    linesList.add(SubtitleLine(projectId = id, startMs = startMs, endMs = endMs, text = text))
                }

                resultList.add(proj to linesList)
            }
            resultList
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch projects from Firestore for user: $ownerUsername", e)
            emptyList()
        }
    }
}
