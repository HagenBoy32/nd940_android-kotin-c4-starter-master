package com.udacity.project4.locationreminders.data.local

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {
//    TODO: Add testing implementation to the RemindersDao.kt

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var application: Application
    private lateinit var remindersDatabase: RemindersDatabase

    @Before
    fun setupDatabase() {
        application = ApplicationProvider.getApplicationContext()
        remindersDatabase = Room.inMemoryDatabaseBuilder(application, RemindersDatabase::class.java)
            .allowMainThreadQueries().build()
    }

   @After
   fun closeDb() = remindersDatabase.close()


    @Test
    fun insertEqualsRetrieve() = runBlocking {
        val reminder = ReminderDTO("Title", "Description", "Location", 19.0, 20.0)

        remindersDatabase.reminderDao().saveReminder(reminder)
        val reminder2: ReminderDTO? = remindersDatabase.reminderDao().getReminderById(reminder.id)

        assertThat(reminder2, `is`(reminder))
    }

    @Test
    fun noReminderForDeleted() = runBlocking {
        val reminder = ReminderDTO("Title", "Description", "Location", 19.0, 20.20)
        val id = reminder.id
        remindersDatabase.reminderDao().saveReminder(reminder)
        remindersDatabase.reminderDao().deleteAllReminders()

        val result = remindersDatabase.reminderDao().getReminderById(id)

        assertThat(result, `is`(CoreMatchers.nullValue()))
    }

}