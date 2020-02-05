package com.example.mapskotlin


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager

import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*

import com.google.gson.Gson
import com.google.maps.android.clustering.ClusterManager


import java.io.IOException
import java.io.InputStream
import com.google.android.gms.maps.model.Marker

import android.view.View
import android.widget.Button
import com.google.android.gms.maps.*
import com.google.maps.android.clustering.Cluster


open class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    lateinit var btnReservar: Button

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

    //variables estatidas, son comunes a todos los objetos
    companion object {
        //propiedades para actualizar la localización del usuario en tiempo real
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2
        //ultimo marker que ha apretado el usuario, lo guardamos aquí para poder acceder desde custominfowindow
        var clickedClusterItem: MyItem? = null
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment


        mapFragment.getMapAsync(this)
        onMapReadyCallback()
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
        listener()
        map.uiSettings.isMapToolbarEnabled=false
        map.uiSettings.isZoomControlsEnabled = true //habilitamos los controles del ZOOM
        map.isMyLocationEnabled = true      //habilitamos los controles del boton para ubicar en mi posicion
        map.setOnMarkerClickListener(mClusterManager)

        sacarArrayScout()
        sacarArrayEcooltra()
        setUpMap()
        seUptInfoWindow()


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
       // map.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            // Got last known location. In some rare situations this can be null.
            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                placeMarkerOnMap(currentLatLng) // marca en el punto en el que se encuentra el usuario al abrir al aplicación
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        currentLatLng,
                        15f
                    ),4000,null
                ) //mueve la camara al punto en el que se encuentra el usuario (position, zoom, tiempo animación, null)
            }
        }
    }




    fun onMapReadyCallback() { // Registrar escucha onMapReadyCallback
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
        // map.setOnMarkerClickListener(mClusterManager)


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
            moto.marca = "Scoot"
            val item =
                MyItem(
                    moto.latitude.toDouble(),
                    moto.longitude.toDouble(),
                    moto.id,
                    null,
                    "Scoot", moto.vehicle_type, null, null
                )
            placeMotoOnMapClustering(item)
        }
    }

    fun sacarArrayEcooltra() {

        val gson = Gson()
        var json = ""

        try {
            val inputStream: InputStream = assets.open("ecooltra.json")
            json = inputStream.bufferedReader().use { it.readText() }

        } catch (e: IOException) {
            e.printStackTrace()
        }
        val arrayEcooltras: EcooltraMotos =
            gson.fromJson(json, EcooltraMotos::class.java)


        val motos: Array<EcooltraMotosInferior> = arrayEcooltras.vehicles

        for (moto in motos) {
            moto.vehicles[0].marca = "Ecooltra"
            println(moto.vehicles[0].toString())
        }


        for (motoSup in motos) {

            var moto = motoSup.vehicles[0]
            val item =
                MyItem(
                    moto.position.latitude.toDouble(),
                    moto.position.longitude.toDouble(),
                    moto.id, moto.distance,
                    "Ecooltra", moto.vehicleType, moto.pricePerMinute, moto.currentDistance
                )
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

                    BitmapFactory.decodeResource(resources, R.drawable.scout_icon_round25)
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


    private fun seUptInfoWindow() {

        var customInfoWindow = CustomInfoWindow(this)
        map.setInfoWindowAdapter(mClusterManager.markerManager)
        mClusterManager.markerCollection.setOnInfoWindowAdapter(customInfoWindow)

    }


    fun listener() {


        //mClusterManager?.setOnClusterItemClickListener(this)

        mClusterManager//listener para cada MARKER
            .setOnClusterItemClickListener { item ->
                clickedClusterItem = item




                Toast.makeText(
                    getBaseContext(),
                    "you click marker" + item.mMarca,
                    Toast.LENGTH_SHORT
                ).show()
                false
            }

        // mClusterManager?.setOnClusterClickListener(this)

        mClusterManager //listener para cada CLUSTER
            .setOnClusterClickListener { item ->


                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        item.position,map.cameraPosition.zoom+2

                    ), 600, null
                )
                // si devuelves false, no funciona el map animate camera
                true
            }

/*
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
                  // marker.hideInfoWindow()


                    startActivity( Intent(Intent.ACTION_VIEW, Uri.parse("ecooltra://reserve?vehicle_id=ES-B-A00990&referrer_id=RaccTrips")));


               }*/
    }


}
