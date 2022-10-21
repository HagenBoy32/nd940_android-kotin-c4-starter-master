package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.ACTION_GEOFENCE_EVENT
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
private const val FINE_AND_BACKGROUND_LOCATIONS_REQUEST_CODE = 33
private const val FINE_LOCATION_REQUEST_CODE  = 34
private const val FINE_LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
private const val ACTION_GEOFENCE_EVENT = "SaveReminderFragment.project4.action.ACTION_GEOFENCE_EVENT"
private const val TURN_DEVICE_LOCATION_ON_REQUEST_CODE = 29
const val GEOFENCE_RADIUS_IN_METERS = 100f
class SaveReminderFragment : BaseFragment() {


    override val viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    private lateinit var reminderDataItem : ReminderDataItem

    private lateinit var geofencingClient: GeofencingClient

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = GeofenceBroadcastReceiver.ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(
            requireContext(),
            0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT)
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = viewModel

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        return binding.root
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            viewModel.navigationCommand.value = NavigationCommand.To(
                SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        // save button
        binding.saveReminder.setOnClickListener {
            val title = viewModel.reminderTitle.value
            val description = viewModel.reminderDescription.value
            val location = viewModel.reminderSelectedLocationStr.value
            val latitude = viewModel.latitude.value
            val longitude = viewModel.longitude.value


            reminderDataItem = ReminderDataItem(title, description, location, latitude, longitude)

            if(viewModel.validateEnteredData(reminderDataItem)) {

                if (fineAndBackgroundLocationPermissionsApproved()) {
                    checkDeviceLocationSettingsAndStartGeofence()
                } else {
                    requestFineAndBackgroundLocationPermissions()
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        viewModel.onClear()
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestFineAndBackgroundLocationPermissions() {
        if (fineAndBackgroundLocationPermissionsApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val requestCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                FINE_AND_BACKGROUND_LOCATIONS_REQUEST_CODE
            }
            else -> FINE_LOCATION_REQUEST_CODE
        }

        requestPermissions(permissionsArray, requestCode)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun fineAndBackgroundLocationPermissionsApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_FINE_LOCATION))

        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                true
            }

        return foregroundLocationApproved && backgroundPermissionApproved
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantedResults: IntArray) {

        super.onRequestPermissionsResult(requestCode, permissions, grantedResults)
        if (grantedResults.isEmpty() ||
            grantedResults[FINE_LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == FINE_AND_BACKGROUND_LOCATIONS_REQUEST_CODE &&
                    grantedResults[BACKGROUND_LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED)
        ) {

            viewModel.showSnackBarInt.value = R.string.permission_denied_explanation

        } else {

            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve:Boolean = true) {

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                try {
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        TURN_DEVICE_LOCATION_ON_REQUEST_CODE,
                        null,
                        0,
                        0,
                        0,
                        null)

                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d("<<1>>", "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    requireView(),
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }

        locationSettingsResponseTask.addOnCompleteListener {
            if ( it.isSuccessful ) {
                startGeoFence()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startGeoFence() {
        val geofence = Geofence.Builder()
            .setRequestId(reminderDataItem.id)
            .setCircularRegion(
                reminderDataItem.latitude!!,
                reminderDataItem.longitude!!,
                GEOFENCE_RADIUS_IN_METERS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
            addOnSuccessListener {
                viewModel.saveReminder(reminderDataItem)
            }
            addOnFailureListener {
                viewModel.showSnackBarInt.value = R.string.error_adding_geofence
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TURN_DEVICE_LOCATION_ON_REQUEST_CODE) {
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }
}