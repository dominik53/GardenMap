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

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var binding: ActivityMainBinding
    private lateinit var mapFragment: SupportMapFragment
    private var mGoogleMap:GoogleMap? = null
    private val markerPositions: MutableList<LatLng> = mutableListOf()
    val init = 1

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
                showWaypointButton()
            } else {
                hideMapFragment()
                hideWaypointButton()
            }
        }

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
            R.id.navigation_home, R.id.navigation_dashboard))
        setupActionBarWithNavController(navController, appBarConfiguration)
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

        // WAYPOINT (MARKER)
        val addWaypointButton: Button = findViewById(R.id.addWaypointButton)
        addWaypointButton.setOnClickListener {
            // Get the current location
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {

                    val currentLatLng = LatLng(location.latitude, location.longitude)

                    // Add a marker to the map at the current location
                    mGoogleMap?.addMarker(
                        MarkerOptions()
                            .position(currentLatLng)
                            .title("Waypoint")
                    )

                    // Add marker position to the list and save it
                    addMarkerPosition(currentLatLng)
                    saveMarkerPositions()
                    logSharedPreferencesData()

                }
            }
        }

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

        loadMarkers()
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

    // Function to save marker positions to SharedPreferences
    private fun saveMarkerPositions() {
        // Ensure proper reference to the Context class
        val sharedPreferences = this.getSharedPreferences("Markers", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear() // Clear existing markers before saving new ones
        markerPositions.forEachIndexed { index, latLng ->
            val latLngString = "${latLng.latitude},${latLng.longitude}"
            editor.putString("marker_$index", latLngString)
        }
        editor.putInt("marker_count", markerPositions.size)
        editor.apply()
    }

    // Function to load markers from SharedPreferences
    private fun loadMarkers() {
        // Ensure proper reference to the Context class
        val sharedPreferences = this.getSharedPreferences("Markers", Context.MODE_PRIVATE)
        val markerCount = sharedPreferences.getInt("marker_count", 0)
        val googleMap = mGoogleMap ?: return

        Log.d("SharedPreferences", "------------------- Marker count: $markerCount")

        for (i in 0 until markerCount) {
            val latLngString = sharedPreferences.getString("marker_$i", null) ?: continue
            val (lat, lng) = latLngString.split(",").map { it.toDouble() }
            val markerLatLng = LatLng(lat, lng)

            addMarkerPosition(markerLatLng)

            googleMap.addMarker(
                MarkerOptions()
                    .position(markerLatLng)
                    .title("Waypoint")
            )
        }
    }

    private fun addMarkerPosition(markerLatLng: LatLng) {
        markerPositions.add(markerLatLng)
    }

    private fun logSharedPreferencesData() {
        val sharedPreferences = this.getSharedPreferences("Markers", Context.MODE_PRIVATE)
        val markerCount = sharedPreferences.getInt("marker_count", 0)
        Log.d("SharedPreferences", "LOGS:")
        for (i in 0 until markerCount) {
            val latLngString = sharedPreferences.getString("marker_$i", null)
            Log.d("SharedPreferences", "Marker $i: $latLngString")
        }
    }
}