package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.launch

class SaveReminderViewModel(val app: Application, val dataSource: ReminderDataSource) :
    BaseViewModel(app) {

    val reminderTitle = MutableLiveData<String>()
    val reminderDescription = MutableLiveData<String>()
    val reminderSelectedLocationStr = MutableLiveData<String>()
    private val selectedPOI = MutableLiveData<PointOfInterest>()
    val latitude = MutableLiveData<Double>()
    val longitude = MutableLiveData<Double>()

    val selectedPlaceOfInterestName = Transformations.map(selectedPOI
        ) {
        if (it == null) {
            return@map app.getString(R.string.select_location)
        }

        if (it.name.isNullOrBlank()) {
            return@map "Lat: ${it.latLng.latitude} Lon: ${it.latLng.longitude}"
        }
        it.name.replace("\n", "").trim()
    }

    fun onClear() {
        reminderTitle.value = null
        reminderDescription.value = null
        selectedPOI.value = null
        latitude.value = null
        longitude.value = null

    }


    fun validateAndSaveReminder(reminderData: ReminderDataItem):Boolean {
       if (validateEnteredData(reminderData)) {
            saveReminder(reminderData)
            return true
        }
        return  false
    }


    fun saveReminder(reminderData: ReminderDataItem) {
        showLoading.value = true
        viewModelScope.launch {
            dataSource.saveReminder(
                ReminderDTO(
                    reminderData.title,
                    reminderData.description,
                    reminderData.location,
                    reminderData.latitude,
                    reminderData.longitude,
                    reminderData.id
                )
            )

            showLoading.value = false
            showToast.value = app.getString(R.string.reminder_saved)
            navigationCommand.value = NavigationCommand.Back
        }
    }

    fun validateEnteredData(reminderData: ReminderDataItem): Boolean {
        Log.d("SaveReminderViewModel", " in validateEnteredData: ")
        if (reminderData.title.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_enter_title
            return false
        }

        if (reminderData.latitude == null || reminderData.longitude == null) {
            showSnackBarInt.value = R.string.err_select_location
            return false
        }

            return true
    }

}

