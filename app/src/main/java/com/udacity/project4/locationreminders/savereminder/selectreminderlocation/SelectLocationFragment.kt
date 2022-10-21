package com.udacity.project4.locationreminders.savereminder.selectreminderlocation
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.observe
import androidx.navigation.fragment.DialogFragmentNavigatorDestinationBuilder
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.geofence.GeofenceConstants
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.LocationUtils
import com.udacity.project4.utils.PermissionsResultEvent
import com.udacity.project4.utils.toLatLng
import kotlinx.android.synthetic.main.fragment_save_reminder.*
import kotlinx.android.synthetic.main.fragment_save_reminder.view.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*
import kotlin.math.log

private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29


class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    companion object {
        private const val TAG = "SaveReminderFragment"

        private const val FINE_LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val TURN_DEVICE_LOCATION_ON_REQUEST_CODE = 29

        private val DEFAULT_LAT_LNG = LatLng(-34.0, 151.0)  // Sydney
    }

    enum class MapZoomLevel(val level: Float) {
        World(1f),
        Landmass(5f),
        City(10f),
        Streets(15f),
        Buildings(20f)
    }

    //Use Koin to get the view model of the SaveReminder
    override val viewModel: SaveReminderViewModel by inject()
    private val selectLocationViewModel: SelectLocationViewModel by viewModel()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private var selectedLocationMarker: Marker?= null
    private lateinit var selectedLocationCircle: Circle
    private val REQUEST_LOCATION_PERMISSION = 1

    private val DEFAULT_ZOOM = 17f
    private val defaultLocation = LatLng(44.97974325243857, 10.709856046507827)
    private lateinit var lastKnownLocation: Location

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this


        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)


        checkDeviceLocationSettings()
        val mapFragment = childFragmentManager.findFragmentById(R.id.select_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.saveButton.setOnClickListener {


            onLocationSelected()
        }

        return binding.root
    }

    private fun onLocationSelected() {
        selectedLocationMarker.let {
        viewModel.latitude.value = it?.position?.latitude
        viewModel.longitude.value = it?.position?.longitude
        viewModel.reminderSelectedLocationStr.value = it?.title
        }

        findNavController().popBackStack()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        fun setMapType(mapType: Int): Boolean {
        map.mapType = mapType
        return true
    }

    return when (item.itemId) {
        R.id.normal_map -> setMapType(GoogleMap.MAP_TYPE_NORMAL)
        R.id.hybrid_map -> setMapType(GoogleMap.MAP_TYPE_HYBRID)
        R.id.terrain_map -> setMapType(GoogleMap.MAP_TYPE_TERRAIN)
        R.id.satellite_map -> setMapType(GoogleMap.MAP_TYPE_SATELLITE)

        else -> false
    }
  }

  override fun onMapReady(googleMap: GoogleMap) {
      map = googleMap

// map marker
      map.setOnMapLongClickListener { latLng ->
          addMapMarker(latLng)
          selectedLocationMarker!!.showInfoWindow()
      }

// poi marker
      map.setOnPoiClickListener { PointOfInterest ->
          addPoiMarker(PointOfInterest)
          selectedLocationMarker!!.showInfoWindow()
      }

// map style
      map.setMapStyle(
          MapStyleOptions.loadRawResourceStyle(
              requireContext(),
              R.raw.map_style
          )
      )

      if (ContextCompat.checkSelfPermission(
              requireContext(),
              Manifest.permission.ACCESS_FINE_LOCATION
          ) == PackageManager.PERMISSION_GRANTED
      ) {

          enableMapMyLocation()

      } else {

          requestPermissions(
              arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
              FINE_LOCATION_PERMISSION_REQUEST_CODE
          )
      }

  }

      private fun addPoiMarker(pointOfInterest: PointOfInterest) {
          selectedLocationMarker?.remove()
          selectedLocationMarker = map.addMarker(
              MarkerOptions()
                  .position(pointOfInterest.latLng)
                  .title(pointOfInterest.name)
                  .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
          )
      }

      private fun addMapMarker(latLng: LatLng) {
          // A Snippet is Additional text that's displayed below the title.
          val snippet = String.format(
              Locale.getDefault(),
              "Lat: %1$.5f, Long: %2$.5f",
              latLng.latitude,
              latLng.longitude
          )

          selectedLocationMarker?.remove()
          selectedLocationMarker = map.addMarker(MarkerOptions()
              .position(latLng)
              .title(getString(R.string.dropped_pin))
              .snippet(snippet)
              .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
          )
      }

     @SuppressLint("MissingPermission")
      fun enableMapMyLocation() {
         map.isMyLocationEnabled = true
         addLastLocationCallback()
     }

    /* This DOES NOT Work when device location is off*/
    @SuppressLint("MissingPermission")
    private fun addLastLocationCallback() {

        val fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())
        val lastLocationTask = fusedLocationProviderClient.lastLocation

        // On completion, zoom to the user location and add marker
        lastLocationTask.addOnCompleteListener(requireActivity()) { task ->

            if (task.isSuccessful) {
                val taskResult = task.result
                taskResult?.run {

                    val latLng = LatLng(latitude, longitude)
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            latLng,
                            MapZoomLevel.Streets.level
                        )
                    )

                    addMapMarker(latLng)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          getDeviceLocation()
        } else {
            Snackbar.make(
                binding.selectLocationFragment,
                R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
            ).setAction(android.R.string.ok) {
                requestPermissions(
                    arrayOf<String>(android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION
                )
            }.show()
        }
    }

    //******************************************************
    private fun getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */

        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        try {
            if (isPermissionGranted()) {
                map.isMyLocationEnabled = true
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        if (task.result != null) {
                            lastKnownLocation = task.result!!
                            map?.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        lastKnownLocation!!.latitude,
                                        lastKnownLocation!!.longitude
                                    ),
                                    DEFAULT_ZOOM.toFloat()
                                )
                            )
                        }
                    } else {
                        Log.d("<<SelectLocFragment>>", "Current location is null. Using defaults.")
                        Log.e("<<SelectLocFragment>>", "Exception: %s", task.exception)
                        map?.moveCamera(
                            CameraUpdateFactory
                                .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat())
                        )
                        map?.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            } else {
                requestPermissions(
                    arrayOf<String>(android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION
                )
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }



    private fun checkDeviceLocationSettings(resolve: Boolean = true) {

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireContext())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        activity,
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                }
            } else {
                Snackbar.make(
                    this.requireView(),
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettings()
                }.show()
            }
        }
    }


    private fun isPermissionGranted(): Boolean {
        return context?.let {
            ContextCompat.checkSelfPermission(
                it,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        } == PackageManager.PERMISSION_GRANTED
    }


}
