package com.udacity.project4.locationreminders.data.local

import android.app.Application
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {
//    TODO: Add testing implementation to the RemindersLocalRepository.kt
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var application: Application
    private lateinit var remindersDatabase: RemindersDatabase
    private lateinit var remindersLocalRepository: RemindersLocalRepository

    @Before
    fun setupRepository() {
        application = ApplicationProvider.getApplicationContext()
        remindersDatabase = Room.inMemoryDatabaseBuilder(application, RemindersDatabase::class.java)
            .allowMainThreadQueries().build()

        remindersLocalRepository = RemindersLocalRepository(remindersDatabase.reminderDao(), Dispatchers.Main)
    }

    @Test
    fun insertEqualsRetrieve() = runBlocking {
        val reminder = ReminderDTO("Title", "Description", "Location", 19.0, 20.2)

        remindersLocalRepository.saveReminder(reminder)
        val reminder2: Result.Success<ReminderDTO> = remindersLocalRepository.getReminder(reminder.id) as Result.Success

        assertThat(reminder2.data, `is`(reminder))
    }

    @Test
    fun noReminderError() = runBlocking {
        val reminder = ReminderDTO("Title", "Description", "Location", 19.0, 20.2)
        val id = reminder.id
        Log.d("<<1>>", "noReminderError: " + id  + reminder)
        remindersLocalRepository.saveReminder(reminder)
        remindersLocalRepository.deleteAllReminders()

        val result = remindersLocalRepository.getReminder(id) as Result.Error
        Log.d("<<2..>>", "noReminderError: " + result.message)


        assertThat(result.message, `is`("java.lang.Exception: XX"))

    }


}