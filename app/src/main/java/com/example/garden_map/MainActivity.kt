package com.example.garden_map

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.gms.maps.model.Marker
import android.widget.TextView
import android.widget.Button


class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var map: GoogleMap
    private var selectedMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Ustawienie początkowej lokalizacji mapy
        val initialLocation = LatLng(52.2297, 21.0122) // Warszawa, Polska
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 10f))

        // Dodaj listener dla kliknięć na markery
        map.setOnMarkerClickListener(this)

        // Dodaj listener dla kliknięć na mapę
        map.setOnMapClickListener { latLng ->
            showAddMarkerDialog(latLng)
        }
    }


    override fun onMarkerClick(marker: Marker): Boolean {
        selectedMarker = marker
        showMarkerDetailsDialog(marker)
        return true
    }

    private fun showAddMarkerDialog(latLng: LatLng) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_marker, null)
        val nameEditText = dialogView.findViewById<TextInputEditText>(R.id.et_name)
        val datePlantedEditText = dialogView.findViewById<TextInputEditText>(R.id.et_date_planted)
        val dateHarvestedEditText = dialogView.findViewById<TextInputEditText>(R.id.et_date_harvested)

        MaterialAlertDialogBuilder(this)
            .setTitle("Dodaj Marker")
            .setView(dialogView)
            .setPositiveButton("Dodaj") { _, _ ->
                val name = nameEditText.text.toString()
                val datePlanted = datePlantedEditText.text.toString()
                val dateHarvested = dateHarvestedEditText.text.toString()

                val marker = map.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(name)
                        .snippet("Data zasadzenia: $datePlanted\nData zbioru: $dateHarvested")
                )
                marker?.showInfoWindow()
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun showMarkerInfoDialog(marker: Marker) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_marker_info, null)
        val nameEditText = dialogView.findViewById<TextInputEditText>(R.id.et_name)
        val datePlantedEditText = dialogView.findViewById<TextInputEditText>(R.id.et_date_planted)
        val dateHarvestedEditText = dialogView.findViewById<TextInputEditText>(R.id.et_date_harvested)

        nameEditText.setText(marker.title)
        val snippet = marker.snippet?.split("\n") ?: listOf("")
        if (snippet.size >= 2) {
            datePlantedEditText.setText(snippet[0].substringAfter(": "))
            dateHarvestedEditText.setText(snippet[1].substringAfter(": "))
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Edytuj Marker")
            .setView(dialogView)
            .setPositiveButton("Zapisz") { _, _ ->
                val name = nameEditText.text.toString()
                val datePlanted = datePlantedEditText.text.toString()
                val dateHarvested = dateHarvestedEditText.text.toString()

                marker.title = name
                marker.snippet = "Data zasadzenia: $datePlanted\nData zbioru: $dateHarvested"
                marker.showInfoWindow()
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun showMarkerDetailsDialog(marker: Marker) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_marker_details, null)
        val nameTextView = dialogView.findViewById<TextView>(R.id.tv_marker_name)
        val infoTextView = dialogView.findViewById<TextView>(R.id.tv_marker_info)
        val editButton = dialogView.findViewById<Button>(R.id.btn_edit_marker)
        val deleteButton = dialogView.findViewById<Button>(R.id.btn_delete_marker)

        nameTextView.text = marker.title
        infoTextView.text = marker.snippet

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .show()

        editButton.setOnClickListener {
            dialog.dismiss()
            showMarkerInfoDialog(marker)
        }

        deleteButton.setOnClickListener {
            marker.remove()
            dialog.dismiss()
        }
    }

}
