package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ItemType
import com.example.data.LauncherDatabase
import com.example.data.LauncherItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LauncherPersistenceTest {

    private lateinit var database: LauncherDatabase

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LauncherDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAndRetrieveItem() = runBlocking {
        val item = LauncherItem(
            type = ItemType.APP,
            screenIndex = 0,
            cellX = 1,
            cellY = 1,
            label = "Test App",
            packageName = "com.test.app"
        )
        
        database.launcherDao().insertItem(item)
        
        val items = database.launcherDao().getAllItems().first()
        assertEquals(1, items.size)
        assertEquals("Test App", items[0].label)
    }

    @Test
    fun testDeleteItem() = runBlocking {
        val item = LauncherItem(
            id = 1,
            type = ItemType.APP,
            screenIndex = 0,
            cellX = 1,
            cellY = 1,
            label = "Test App",
            packageName = "com.test.app"
        )
        
        database.launcherDao().insertItem(item)
        database.launcherDao().deleteItemById(1)
        
        val items = database.launcherDao().getAllItems().first()
        assertEquals(0, items.size)
    }
}
