package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders:MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {
//    DOne: Create a fake data source to act as a double to the real data source

    private var returnsError = false

    fun setReturnsError(value: Boolean) {
        returnsError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (returnsError) return Result.Error(Exception("ERROR").toString())
        else {
            reminders?.let { return Result.Success(ArrayList(it)) }
            return Result.Error(
                Exception(
                    "Reminders not found"
                ).toString()
            )
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
       // Done("return the reminder with the id")
        if (returnsError) return Result.Error(Exception("ERROR").toString())
        else {
            reminders?.let {
                for (reminder in it) {
                    if (reminder.id == id) return Result.Success(reminder)
                }
            }
            return Result.Error(
                Exception(
                    "Reminder not found"
                ).toString()
            )
        }
    }

    override suspend fun deleteAllReminders() {
        //Done("delete all the reminders")
        reminders?.clear()
    }


}