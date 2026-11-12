package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LauncherItem::class, AppLaunch::class, AppInfoEntity::class, FolderEntity::class, ProfileEntity::class],
    version = 4,
    exportSchema = false
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun launcherDao(): LauncherDao
    abstract fun profileDao(): ProfileDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Check if the column already exists to avoid duplicate column error
                val cursor = db.query("PRAGMA table_info(`launcher_items`)", emptyArray())
                var hasColumn = false
                while (cursor.moveToNext()) {
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    if (name == "userSerialNumber") {
                        hasColumn = true
                        break
                    }
                }
                cursor.close()

                if (!hasColumn) {
                    db.execSQL("ALTER TABLE `launcher_items` ADD COLUMN `userSerialNumber` INTEGER")
                }
                
                db.execSQL("CREATE TABLE IF NOT EXISTS `app_launches` (`packageName` TEXT NOT NULL, `lastLaunchTimestamp` INTEGER NOT NULL, PRIMARY KEY(`packageName`))")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `app_info` (`packageName` TEXT NOT NULL, `className` TEXT NOT NULL, `label` TEXT NOT NULL, `isHidden` INTEGER NOT NULL, `customLabel` TEXT, `installTime` INTEGER NOT NULL, PRIMARY KEY(`packageName`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `folders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `screenIndex` INTEGER NOT NULL, `cellX` INTEGER NOT NULL, `cellY` INTEGER NOT NULL, `containerId` INTEGER NOT NULL)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `isEnabled` INTEGER NOT NULL)")
                db.execSQL("ALTER TABLE `launcher_items` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
