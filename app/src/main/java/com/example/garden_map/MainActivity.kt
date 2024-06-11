package com.example.garden_map

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.garden_map.databinding.ActivityMainBinding
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.MarkerOptions
import android.widget.Button
import androidx.fragment.app.FragmentTransaction
import android.view.View
import android.content.Context
import android.util.Log
import com.example.garden_map.ui.dashboard.DashboardFragment
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class MainActivity : AppCompatActivity(), OnMapReadyCallback, DashboardFragment.OnButtonClickListener {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var binding: ActivityMainBinding
    private lateinit var mapFragment: SupportMapFragment
    private var mGoogleMap:GoogleMap? = null
    private val markerPositions: MutableList<LatLng> = mutableListOf()
    private var showAddButton = 0
    val borderMarkerAlpha = 0.2
    data class BorderMarkersStructure(var uniqueId: Int, var borderId: Int, var borderMarkerId: Int, var markerName: String, var latitude: Double, var longitude: Double)
    data class TreeMarkersStructure(var uniqueId: Int, var borderId: Int, var name: String, var date: String, var latitude: Double, var longitude: Double)
    var BorderMarkers = mutableListOf<BorderMarkersStructure>()
    var drawingNewBorder = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Initialize mapFragment
        mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment

        // Listen for destination changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Show or hide the map fragment based on the current destination
            if (destination.id == R.id.navigation_home) {
                showMapFragment()
                if(showAddButton == 1)
                    showWaypointButton()

            } else {
                hideMapFragment()
                hideWaypointButton()
            }
        }

        navView.setupWithNavController(navController)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        /* LOKALIZACJA */
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check for location permission
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            // Permission already granted, get location
            getLastLocation()
        }

        // MARKER LOKALIZACJI
        val addWaypointButton: Button = findViewById(R.id.addWaypointButton)
        addWaypointButton.setOnClickListener {
            // Get the current location
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(location.latitude, location.longitude)

                    addNewBorderMarker(BorderMarkers, currentLatLng.latitude, currentLatLng.longitude)
                    saveList(this, "BorderMarkers", BorderMarkers)
                    printBorderMarkers(BorderMarkers)

                    // Add a marker to the map at the current location
                    val mapMarker = mGoogleMap?.addMarker(
                        MarkerOptions()
                            .position(currentLatLng)
                            .title(BorderMarkers.last().markerName)
                            .visible(true)
                            .draggable(true)
                            .alpha(borderMarkerAlpha.toFloat())
                    )
                    mapMarker?.tag = BorderMarkers.last().uniqueId

                    mGoogleMap?.clear()
                    showAllBorderMarkers(BorderMarkers)
                    connectAllMarkersWithoutBorder(BorderMarkers, getMaxBorderId(BorderMarkers))
                    connectMarkersWithoutClosingLoop(BorderMarkers, getMaxBorderId(BorderMarkers))
                }
            }
        }

    }

    private fun addNewBorderMarker(borderMarkersList: List<BorderMarkersStructure>, latitude: Double, longitude: Double){
        var newBorder = getMaxBorderId(borderMarkersList);

        if(drawingNewBorder) {
            newBorder += 1
            drawingNewBorder = false
        }

        val newId = getMaxBorderMarkerIdForBorderId(borderMarkersList,newBorder)+1
        val markerName1 = "Border "
        val markerName2 = " marker "
        val newMarkerName = markerName1.plus(newBorder.toString()).plus(markerName2).plus(newId.toString())
        addBorderMarkerPosition(newBorder, newId, newMarkerName, latitude, longitude, getMaxUniqueId(BorderMarkers)+1)
    }
    private fun getMaxBorderMarkerIdForBorderId(borderMarkersList: List<BorderMarkersStructure>, borderId: Int): Int {
        // Filter the list to include only the elements with the specified borderId
        val filteredList = borderMarkersList.filter { it.borderId == borderId }

        // Find the maximum borderMarkerId within the filtered list
        return filteredList.maxByOrNull { it.borderMarkerId }?.borderMarkerId ?: -1
    }

    private fun getMaxBorderId(borderMarkersList: List<BorderMarkersStructure>): Int {
        return borderMarkersList.maxByOrNull { it.borderId }?.borderId ?: 0
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap

        // Enable the My Location layer if the permission is granted
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mGoogleMap?.isMyLocationEnabled = true

            // Get the last known location and move the camera to that location
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mGoogleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM))
                }
            }
        }

        // Set the listener for marker drag end
        mGoogleMap?.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {
                // Handle the event when the drag starts
            }

            override fun onMarkerDrag(marker: Marker) {
                // Handle the event while the marker is being dragged
            }

            override fun onMarkerDragEnd(marker: Marker) {
                if(showAddButton == 1)
                {
                    val position = marker.position
                    val uniqueId = marker.tag as? Int
                    uniqueId?.let {
                        updateMarkerPosition(BorderMarkers, uniqueId, position.latitude, position.longitude)
                    }
                    val myBorder = findBorderIdByUniqueId(BorderMarkers, marker.tag)
                    if(myBorder >= 0){
                        mGoogleMap?.clear()
                        connectAllMarkersWithoutBorder(BorderMarkers, myBorder)
                        connectMarkersWithoutClosingLoop(BorderMarkers, myBorder)
                        showAllBorderMarkers(BorderMarkers)
                    }
                }
                else
                {
                    //todo przesuwanie drzew
                }

            }
        })

//        mGoogleMap?.setOnMarkerClickListener {  } todo: do drzew

        BorderMarkers = getList(this, "BorderMarkers")
//        showAllBorderMarkers(BorderMarkers)
        connectAllMarkers(BorderMarkers)

        printBorderMarkers(BorderMarkers)
    }

    override fun onButton1Click() {
        eraseMarkers()
    }

    override fun onButton2Click() {
        val addWaypointButton: Button = findViewById(R.id.addWaypointButton)

        if (showAddButton == 0) {
            showAddButton = 1
            drawingNewBorder = true
            showAllBorderMarkers(BorderMarkers)
        } else {
//            connectFirstAndLastMarker(BorderMarkers, getMaxBorderId(BorderMarkers))
            mGoogleMap?.clear()
            connectAllMarkers(BorderMarkers)

            showAddButton = 0
            drawingNewBorder = false
        }
    }

    private fun eraseMarkers() {
        mGoogleMap?.clear() // This clears all markers from the map
        BorderMarkers.clear()
        saveList(this, "BorderMarkers", BorderMarkers) // Save the cleared state to SharedPreferences

        Log.d("DEV", "------------------- Markers erased...")
    }

    private fun getLastLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                // Got last known location. In some rare situations, this can be null.
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mGoogleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM))
                }
            }
    }

    // Handle permission request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get location
                getLastLocation()
            } else {
                // Permission denied, handle accordingly (e.g., show explanation or disable functionality)
            }
        }
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
        private const val DEFAULT_ZOOM = 15f
    }

    private fun showMapFragment() {
        // Show the map fragment
        val transaction = supportFragmentManager.beginTransaction()
        transaction.show(mapFragment)
        transaction.commit()
    }

    private fun hideMapFragment() {
        // Hide the map fragment
        val transaction = supportFragmentManager.beginTransaction()
        transaction.hide(mapFragment)
        transaction.commit()
    }

    private fun showWaypointButton() {
        val addWaypointButton: Button = findViewById(R.id.addWaypointButton)
        addWaypointButton.visibility = View.VISIBLE
    }

    private fun hideWaypointButton() {
        val addWaypointButton: Button = findViewById(R.id.addWaypointButton)
        addWaypointButton.visibility = View.GONE
    }

    // Function to retrieve a list of BorderMarkersStructure from SharedPreferences
    private fun getList(context: Context, key: String): MutableList<BorderMarkersStructure> {
        val gson = Gson()
        val prefs = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString(key, null)
        val type = object : TypeToken<MutableList<BorderMarkersStructure>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    private fun saveList(context: Context, key: String, list: MutableList<BorderMarkersStructure>) {
        val gson = Gson()
        val json = gson.toJson(list)
        val prefs = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(key, json).apply()
    }

    private fun addBorderMarkerPosition(borderID: Int, markerID: Int, name: String, lat: Double, long: Double, id: Int) {
        val newMarker = BorderMarkersStructure(id, borderID, markerID, name, lat, long)
        BorderMarkers.add(newMarker)
    }

    private fun connectAllMarkers(borderMarkersList: List<BorderMarkersStructure>) {
        // Extract all unique borderId values
        val uniqueBorderIds = borderMarkersList.map { it.borderId }.distinct()

        // Iterate through each unique borderId and connect markers
        for (borderId in uniqueBorderIds) {
            connectMarkersWithSameBorderId(borderMarkersList, borderId)
        }
    }

    private fun connectAllMarkersWithoutBorder(borderMarkersList: MutableList<BorderMarkersStructure>, borderIdToRemove: Int){
        var uniqueBorderIds = borderMarkersList.map { it.borderId }.distinct()
        println("Unique Border IDs before filtering: $uniqueBorderIds")
        uniqueBorderIds = uniqueBorderIds.filter { it != borderIdToRemove }.toMutableList()
        println("Unique Border IDs after filtering: $uniqueBorderIds")
        for (borderId in uniqueBorderIds) {
            connectMarkersWithSameBorderId(borderMarkersList, borderId)
        }
    }


    private fun getMaxUniqueId(borderMarkersList: List<BorderMarkersStructure>): Int {
        return borderMarkersList.maxByOrNull { it.uniqueId }?.uniqueId ?: 0
    }


    private fun connectMarkersWithSameBorderId(borderMarkersList: List<BorderMarkersStructure>, borderId: Int) {
        val googleMap = mGoogleMap ?: return
        val polylineOptions = PolylineOptions()

        // Filter the list to include only markers with the specified borderId
        var filteredList = borderMarkersList.filter { it.borderId == borderId }
        filteredList = filteredList.sortedBy { it.borderMarkerId }

        if (filteredList.isNotEmpty()) {
            // Connect markers in a circular manner
            for (i in filteredList.indices) {
                val currentBorderMarker = filteredList[i]
                val nextBorderMarker = filteredList[(i + 1) % filteredList.size]
                val currentLatLng = LatLng(currentBorderMarker.latitude, currentBorderMarker.longitude)
                val nextLatLng = LatLng(nextBorderMarker.latitude, nextBorderMarker.longitude)
                polylineOptions.add(currentLatLng, nextLatLng)
            }

            // Connect the last marker back to the first marker
            val firstBorderMarker = filteredList.first()
            val lastBorderMarker = filteredList.last()
            val firstLatLng = LatLng(firstBorderMarker.latitude, firstBorderMarker.longitude)
            val lastLatLng = LatLng(lastBorderMarker.latitude, lastBorderMarker.longitude)
            polylineOptions.add(lastLatLng, firstLatLng)

            // Add the polyline to the map
            googleMap.addPolyline(polylineOptions)
        }
    }

    private fun connectFirstAndLastMarker(borderMarkersList: List<BorderMarkersStructure>, borderId: Int) {
        val googleMap = mGoogleMap ?: return
        val polylineOptions = PolylineOptions()

        // Filter the list to include only markers with the specified borderId
        val filteredList = borderMarkersList.filter { it.borderId == borderId }

        if (filteredList.isNotEmpty()) {
            // Connect the last marker back to the first marker
            val firstBorderMarker = filteredList.first()
            val lastBorderMarker = filteredList.last()
            val firstLatLng = LatLng(firstBorderMarker.latitude, firstBorderMarker.longitude)
            val lastLatLng = LatLng(lastBorderMarker.latitude, lastBorderMarker.longitude)
            polylineOptions.add(lastLatLng, firstLatLng)

            // Add the polyline to the map
            googleMap.addPolyline(polylineOptions)
        }
    }

    private fun connectMarkersWithoutClosingLoop(borderMarkersList: List<BorderMarkersStructure>, borderId: Int) {
        val googleMap = mGoogleMap ?: return
        val polylineOptions = PolylineOptions()

        // Filter the list to include only markers with the specified borderId
        val filteredList = borderMarkersList.filter { it.borderId == borderId }

        if (filteredList.size > 1) {
            // Connect markers except for the last one
            for (i in 0 until filteredList.size - 1) {
                val currentBorderMarker = filteredList[i]
                val nextBorderMarker = filteredList[i + 1]
                val currentLatLng = LatLng(currentBorderMarker.latitude, currentBorderMarker.longitude)
                val nextLatLng = LatLng(nextBorderMarker.latitude, nextBorderMarker.longitude)
                polylineOptions.add(currentLatLng, nextLatLng)
            }

            // Add the polyline to the map
            googleMap.addPolyline(polylineOptions)
        }
    }

    private fun updateMarkerPosition(
        borderMarkersList: MutableList<BorderMarkersStructure>,
        uniqueId: Int,
        newLatitude: Double,
        newLongitude: Double
    ) {
        borderMarkersList.find { it.uniqueId == uniqueId }?.let { marker ->
            marker.latitude = newLatitude
            marker.longitude = newLongitude
            println("Updated Marker with uniqueId $uniqueId to new position: ($newLatitude, $newLongitude)")
        }

        saveList(this, "BorderMarkers", borderMarkersList)

//        val googleMap = mGoogleMap ?: return
//        googleMap.clear()
//
//        showAllBorderMarkers(borderMarkersList)
//        connectAllMarkers(borderMarkersList)

        printBorderMarkers(borderMarkersList)
    }


    private fun showAllBorderMarkers(borderMarkersList: List<BorderMarkersStructure>) {
        val googleMap = mGoogleMap ?: return

        borderMarkersList.forEach { marker ->
            val position = LatLng(marker.latitude, marker.longitude)
            val mapMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(marker.markerName)
                    .visible(true)
                    .draggable(true)
                    .alpha(borderMarkerAlpha.toFloat())
            )
            mapMarker?.tag = marker.uniqueId
        }
    }

    private fun printBorderMarkers(borderMarkersList: MutableList<BorderMarkersStructure>) {
        borderMarkersList.forEach { marker ->
            println("Border ID: ${marker.borderId}, Border Marker ID: ${marker.borderMarkerId}, Marker Name: ${marker.markerName}, Latitude: ${marker.latitude}, Longitude: ${marker.longitude}")
        }
        if(borderMarkersList.isEmpty()){
            println("PUSTOO")
        }
    }

    private fun findBorderIdByUniqueId(borderMarkersList: List<BorderMarkersStructure>, uniqueId: Any?): Int {
        val borderMarker = borderMarkersList.find { it.uniqueId == uniqueId }
        return borderMarker?.borderId ?: -1 // Return a default value if borderMarker is null
    }


    /*************************************  ************************************/
    /*****************************************************************************/


}