package com.c242_ps246.mentalq.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NoteEntity::class, UserEntity::class, AnalysisEntity::class],
    version = 14,
    exportSchema = true
)
abstract class MentalQDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun userDao(): UserDao
    abstract fun analysisDao(): AnalysisDao
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS chat_room")
        db.execSQL("DROP TABLE IF EXISTS chat_message")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `note` ADD COLUMN `pendingAction` TEXT")
    }
}

// Versions 1-11 never exported schemas and only stored server-backed cache data.
// Rebuild those cache tables explicitly instead of enabling Room's global destructive fallback.
val LEGACY_CACHE_MIGRATIONS: List<Migration> = (1..11).map { oldVersion ->
    object : Migration(oldVersion, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf(
                "note",
                "user_data",
                "analysis",
                "chat_room",
                "chat_message",
                "remote_keys"
            ).forEach { table -> db.execSQL("DROP TABLE IF EXISTS `$table`") }

            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `note` (
                    `id` TEXT NOT NULL,
                    `title` TEXT,
                    `content` TEXT,
                    `contentNormalized` TEXT,
                    `predictedStatus` TEXT,
                    `confidenceScore` REAL,
                    `emotion` TEXT,
                    `updatedAt` TEXT,
                    `createdAt` TEXT,
                    `pendingAction` TEXT,
                    PRIMARY KEY(`id`)
                )""".trimIndent()
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `user_data` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `email` TEXT NOT NULL,
                    `birthday` TEXT,
                    `profilePhotoUrl` TEXT,
                    `role` TEXT,
                    PRIMARY KEY(`id`)
                )""".trimIndent()
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `analysis` (
                    `id` TEXT NOT NULL,
                    `noteId` TEXT,
                    `predictedStatus` TEXT NOT NULL,
                    `confidenceScore` REAL,
                    `updatedAt` TEXT,
                    `createdAt` TEXT,
                    PRIMARY KEY(`id`)
                )""".trimIndent()
            )
        }
    }
}
