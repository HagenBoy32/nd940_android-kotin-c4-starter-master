package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {

//    TODO: test the navigation of the fragments.
//    TODO: test the displayed data on the UI.
//    TODO: add testing for the error messages.

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var application: Application
    private lateinit var repository: ReminderDataSource
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun initRepository() {
        stopKoin() // stop the original app koin
        application = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    application,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    application,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(application) }
        }
        // declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        // Get our real repository
        repository = GlobalContext.get().koin.get()

        // clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @After
    fun stopKoinAfterTest() = stopKoin()

    @Test
    fun clickOnFAB_navigatesToSaveReminder() {
        // GIVEN - on ReminderList
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // WHEN - click on addReminderFAB
        onView(withId(R.id.addReminderFAB)).perform(click())

        // THEN verify that we navigate to SaveReminder
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }


    @Test
    fun twoRemindersInDB_UIshowsTwo() {
        runBlocking {
            repository.saveReminder(ReminderDTO("Title1", "Description1", "Location1", 1.0, 2.0))
            repository.saveReminder(ReminderDTO("Title2", "Description2", "Location2", 5.0, 9.0))
        }

        // GIVEN - on ReminderList with two Reminders
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // THEN - UI shows both Reminders
        onView(ViewMatchers.withText("Title1")).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        onView(ViewMatchers.withText("Title1")).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }


    @Test
    fun emptyDB_noDataShows() {
        runBlocking {
            repository.deleteAllReminders()
        }

        // GIVEN - ReminderList without Reminders
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // THEN - UI shows noDataSymbol
        onView(withId(R.id.noDataTextView)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }



}