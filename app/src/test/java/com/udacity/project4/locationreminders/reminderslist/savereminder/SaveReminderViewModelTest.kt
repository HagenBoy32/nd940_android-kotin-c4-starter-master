package com.udacity.project4.locationreminders.reminderslist.savereminder

import android.app.Application
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.O])
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {
    //TODO: provide testing to the SaveReminderView and its live data objects

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var application: Application
    private lateinit var dataSource: FakeDataSource
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    @Before
    fun setupViewModel() {
        stopKoin()
        application = ApplicationProvider.getApplicationContext()
        FirebaseApp.initializeApp(application)
        dataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(application, dataSource)
    }

    @Test
    fun check_loading() = mainCoroutineRule.runBlockingTest {

        val reminder = ReminderDataItem("Title", "Description", "Location", 19.0, 20.0)

        mainCoroutineRule.pauseDispatcher()

        saveReminderViewModel.validateAndSaveReminder(reminder)

        MatcherAssert.assertThat(
            saveReminderViewModel.showLoading.getOrAwaitValue(),
            Matchers.`is`(true)
        )

        mainCoroutineRule.resumeDispatcher()

        MatcherAssert.assertThat(
            saveReminderViewModel.showLoading.getOrAwaitValue(),
            Matchers.`is`(false)
        )
    }

    @Test
    fun shouldReturnError() = mainCoroutineRule.runBlockingTest {

        val reminderNoTitle = ReminderDataItem(null, "Description", "Location", 19.0, 20.2)
        saveReminderViewModel.validateAndSaveReminder(reminderNoTitle)

        MatcherAssert.assertThat(
            saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            Matchers.`is`(Matchers.notNullValue())
        )
    }



}