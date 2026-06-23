package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        AppConfig::class,
        SubtitleProject::class,
        SubtitleLine::class,
        TranscribeLog::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun appConfigDao(): AppConfigDao
    abstract fun subtitleProjectDao(): SubtitleProjectDao
    abstract fun subtitleLineDao(): SubtitleLineDao
    abstract fun transcribeLogDao(): TranscribeLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "subtitle_generator_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database)
                }
            }
        }

        suspend fun populateDatabase(db: AppDatabase) {
            // Seed Admin User
            db.userDao().insertUser(
                User(username = "admin", passwordHash = "admin123", role = "ADMIN")
            )
            // Seed Standard User
            db.userDao().insertUser(
                User(username = "user", passwordHash = "user123", role = "USER")
            )

            // Seed Default Configurations
            db.appConfigDao().setConfig(AppConfig("preferred_ai", "Gemini 3.5 Flash"))
            db.appConfigDao().setConfig(AppConfig("openai_api_key", ""))
            db.appConfigDao().setConfig(AppConfig("gemini_api_key", ""))
        }
    }
}
