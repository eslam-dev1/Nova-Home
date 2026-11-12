package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LauncherDao {
    @Query("SELECT * FROM launcher_items")
    fun getAllItems(): Flow<List<LauncherItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: LauncherItem): Long

    @Update
    suspend fun updateItem(item: LauncherItem)

    @Delete
    suspend fun deleteItem(item: LauncherItem)

    @Query("DELETE FROM launcher_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM launcher_items WHERE containerId = :folderId")
    suspend fun deleteItemsInFolder(folderId: Long)

    @Query("DELETE FROM launcher_items")
    suspend fun deleteAllItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppLaunch(launch: AppLaunch)

    @Query("SELECT * FROM app_launches ORDER BY lastLaunchTimestamp DESC LIMIT :limit")
    fun getRecentlyUsedApps(limit: Int): Flow<List<AppLaunch>>

    @Query("SELECT * FROM launcher_items")
    suspend fun getAllItemsList(): List<LauncherItem>

    @Query("SELECT * FROM folders")
    suspend fun getAllFoldersList(): List<FolderEntity>

    // App Drawer State
    @Query("SELECT * FROM app_info")
    fun getAllDrawerApps(): Flow<List<AppInfoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrawerApp(app: AppInfoEntity)

    @Query("UPDATE app_info SET isHidden = :isHidden WHERE packageName = :packageName")
    suspend fun setAppHidden(packageName: String, isHidden: Boolean)

    // Folder Management
    @Query("SELECT * FROM folders")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)

    @Query("DELETE FROM folders")
    suspend fun deleteAllFolders()

    @Query("DELETE FROM launcher_items WHERE packageName = :packageName")
    suspend fun deleteItemsByPackageName(packageName: String)
}
