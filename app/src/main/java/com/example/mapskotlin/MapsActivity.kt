package com.example.mapskotlin


import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

import com.google.gson.Gson
import com.google.maps.android.clustering.ClusterManager


import java.io.IOException
import java.io.InputStream
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T


open class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {


    override fun onMarkerClick(p0: Marker?) = false

    protected lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private val mapMarkers = null

    //propiedad para clustering en google map
    protected lateinit var mClusterManager: ClusterManager<MyItem>

    //propiedades para actualizar la localización del usuario en tiempo real
    private lateinit var locationCallback: LocationCallback


    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

        private const val REQUEST_CHECK_SETTINGS = 2
        //propiedades para actualizar la localización del usuario en tiempo real

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)

        // Registrar escucha onMapReadyCallback
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation
                // placeMarkerOnMap(LatLng(lastLocation.latitude, lastLocation.longitude))
            }
        }
        createLocationRequest()


    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setUpClusterer()
        sacarArrayScout()


        map.uiSettings.isZoomControlsEnabled = true //habilitamos los controles del ZOOM
        map.setOnMarkerClickListener(this)


        setUpMap()
        listener()


    }

    //This code checks if the app has been granted the ACCESS_FINE_LOCATION permission.
    // //If it hasn’t, then request it from the user.
    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        map.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            // Got last known location. In some rare situations this can be null.
            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                placeMarkerOnMap(currentLatLng) // marca en el punto en el que se encuentra el usuario al abrir al aplicación
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        currentLatLng,
                        12f
                    )
                ) //mueve la camara al punto en el que se encuentra el usuario
            }
        }
    }


    private fun startLocationUpdates() {//funcion para actualizar la posicion en tiempo real
        //si no nos han dado permisos, los pedimos
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        //si hay permisos,pedimos la localizacion
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null /* Looper */
        )
    }

    private fun createLocationRequest() {
        //creamos una instancia de location request, y la añadimos a LocationSettingsRequest.Builder,
        // recupera y gestiona los cambios que se realizaran segun el estado actual de la configuracion de ubicacion de usuario
        locationRequest = LocationRequest()

        locationRequest.interval = 10000 //intervalo con el que queremos que reciva la posicion
        locationRequest.fastestInterval =
            5000 //especificamos el intervalo maximo que la app puede pedir
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        // comprobamos el estado del user's location settings, creamos dos variales para comprobar
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        //success : todo está bien y podemos iniciar el location request
        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }
        //failure, hay algun problema, mostramos un cuadro para  activar el gps
        task.addOnFailureListener { e ->
            // 6
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(
                        this@MapsActivity,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }


    //  start the update request if it has a RESULT_OK result for a REQUEST_CHECK_SETTINGS
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                startLocationUpdates()
            }
        }
    }

    // parar location update request
    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // reiniciar  location update request
    public override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
    }


    //funciones para iniciar clustering

    private fun setUpClusterer() {

        mClusterManager = ClusterManager(this, map)
        map.setOnCameraIdleListener(mClusterManager)
        map.setOnMarkerClickListener(mClusterManager)

    }


    fun sacarArrayScout() {

        val gson = Gson()
        var json = ""

        try {
            val inputStream: InputStream = assets.open("vehicles.json")
            json = inputStream.bufferedReader().use { it.readText() }

        } catch (e: IOException) {
            e.printStackTrace()
        }
        val arrayScouts: ScoutMotos =
            gson.fromJson(json, ScoutMotos::class.java)

        println(arrayScouts.vehicles[0].toString())
        val motos: Array<MotoScout> = arrayScouts.vehicles

        for (moto in motos) {
            moto.marca = "scout"
            val item =
                MyItem(moto.latitude.toDouble(), moto.longitude.toDouble(), moto.id, "snippet")
            placeMotoOnMapClustering(item)
        }
    }


///funcion para poner marcas en el mapa

    fun placeMarkerOnMap(latLng: LatLng) {

        val markerOptions =
            MarkerOptions().position(latLng)
        markerOptions.icon(
            BitmapDescriptorFactory.fromBitmap(
                BitmapFactory.decodeResource(resources, R.mipmap.ic_user_location)
            )
        )
        map.addMarker(markerOptions)


    }

    //funcion para poner moto en el mapa segun la marca
    fun placeMotoOnMap(moto: MotoScout) {
        // 1

        val markerOptions =
            MarkerOptions().position(LatLng(moto.latitude.toDouble(), moto.longitude.toDouble()))

        if (moto.marca.equals("scout"))
            markerOptions.icon(
                BitmapDescriptorFactory.fromBitmap(

                    BitmapFactory.decodeResource(resources, R.drawable.scout_round_bmp)
                )
            )
        map.addMarker(markerOptions)

        //cluster


    }

    //funcion para poner moto en el mapa segun la marca con clustering
    fun placeMotoOnMapClustering(item: MyItem) {
        mClusterManager.setRenderer(OwnIconRendered(this, map, mClusterManager))
        mClusterManager.addItem(item)
    }


    fun listener() {
        //listener marker
        map.setOnMarkerClickListener(object : GoogleMap.OnMarkerClickListener {
            override fun onMarkerClick(marker: Marker): Boolean {
                Toast.makeText(getBaseContext(), "you click marker"+marker.id, Toast.LENGTH_SHORT).show();
                marker.showInfoWindow()

                return false
            }
        })

        //listener info window
        
        map.setOnInfoWindowClickListener { marker ->
            val ssid = marker.title
            Toast.makeText(getBaseContext(), "you click info", Toast.LENGTH_SHORT).show();
            marker.hideInfoWindow()
        }
    }


}
