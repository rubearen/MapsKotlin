package com.example.mapskotlin


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager

import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.maps.android.clustering.Cluster
import java.util.*
import kotlin.collections.ArrayList


open class MapsActivity : AppCompatActivity(), OnMapReadyCallback, PlaceSelectionListener {


    protected var alItems = ArrayList<MyItem>()
    private lateinit var fab: FloatingActionButton
    private lateinit var fab1: FloatingActionButton
    private lateinit var fabScoot: FloatingActionButton
    private lateinit var fabEcooltra: FloatingActionButton
    private var isFABOpen: Boolean = false //saber si el filtro esta desplegado o no
    private var ecooltraCheck: Boolean = true
    private var scootCheck: Boolean = true
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
        private val TAG = "ClassName"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_searchbar)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)
        onMapReadyCallback()

        fab = findViewById(R.id.fab);
        fab1 = findViewById(R.id.fab1);
        fabScoot = findViewById(R.id.fab2);
        fabEcooltra = findViewById(R.id.fab3);
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
        map.uiSettings.isMapToolbarEnabled = false
        map.uiSettings.isZoomControlsEnabled = true //habilitamos los controles del ZOOM
        map.isMyLocationEnabled =
            true      //habilitamos los controles del boton para ubicar en mi posicion
        map.setOnMarkerClickListener(mClusterManager)

        sacarArrayScout()
        sacarArrayEcooltra()
        setUpMap()
        setUpInfoWindow()
        marcarMotosMapa()
        searchbarSetUp()

    }

    //This code checks if the app has been granted the ACCESS_FINE_LOCATION permission.
    // //If it hasn’t, then request it from the user.
    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }


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
                    ), 4000, null
                ) //mueve la camara al punto en el que se encuentra el usuario (position, zoom, tiempo animación, null)
            }
        }
    }


    fun onMapReadyCallback() { // Registrar escucha onMapReadyCallback
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                lastLocation = p0.lastLocation //se va acutalizando la posición actual y se guarda en esta var
                 //placeMarkerOnMap(LatLng(lastLocation.latitude, lastLocation.longitude))
            }
        }
        createLocationRequest()

    }

    private fun startLocationUpdates() {//funcion para actualizar la posicion en tiempo real
        //si no nos han dado permisos, los pedimos
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
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


    fun searchbarSetUp() {//funcion para activar la searchbar
        //usamos la api de google, hay que activar tanto maps, como places
        var apikey: String = getString(R.string.api_key)

        Places.initialize(applicationContext, apikey)

        var placesClient: PlacesClient = Places.createClient(this)


        var autocompleteFragment: AutocompleteSupportFragment =
            supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                    as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(
            Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG
            )
        ) //esta es la información que nos devolverá el objeto place de la searchbar
        autocompleteFragment.setOnPlaceSelectedListener(this) //listener


    }

    //listener de la searchbar, cuando elegimos un lugar
    override fun onPlaceSelected(place: Place) {


        Log.i(TAG, "Place: " + place.getName() + ", " + place.getId())
        Toast.makeText(
            getBaseContext(),
            "AAAAAAAAAAAAAAAAAAAAAAAA",
            Toast.LENGTH_SHORT
        ).show()

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                place.latLng, 15f

            ), 600, null
        )
    }

    //listener de la searchbar, cuando no elegimos o da error
    override fun onError(status: Status) {
        Log.i(TAG, "An error occurred: " + status)
        Toast.makeText(
            getBaseContext(),
            "EEEEEEEEEEEEEEEEEEEEEERRRROOR",
            Toast.LENGTH_SHORT
        ).show()
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
        mClusterManager.setRenderer(OwnIconRendered(this, map, mClusterManager)) //iconos
        // map.setOnMarkerClickListener(mClusterManager)


    }

    fun marcarMotosMapa() {


        mClusterManager.addItems(alItems)

        /*
        for (item: MyItem in alItems) {
            placeMotoOnMapClustering(item)
        }*/

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
                    "Scoot", moto.vehicle_type, null, null, null
                )
            alItems.add(item)
            // placeMotoOnMapClustering(item)
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
                    moto.id,
                    moto.distance,
                    "Ecooltra",
                    moto.vehicleType,
                    moto.pricePerMinute,
                    moto.currentDistance,
                    moto.urls?.reserveUrl
                )
            alItems.add(item)
            // placeMotoOnMapClustering(item)
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


    private fun setUpInfoWindow() {

        var customInfoWindow = CustomInfoWindow(this)
        map.setInfoWindowAdapter(mClusterManager.markerManager) //el cluster se ocupa del infoview
        mClusterManager.markerCollection.setOnInfoWindowAdapter(customInfoWindow) //ponemos el custom infoview
        map.setOnInfoWindowClickListener(mClusterManager)  //el cluster manager se ocupa del listener del infoview

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
                        item.position, map.cameraPosition.zoom + 2

                    ), 600, null
                )
                // si devuelves false, no funciona el map animate camera
                true
            }

        mClusterManager//listener para InfoView
            .setOnClusterItemInfoWindowClickListener { myItem ->

                Toast.makeText(getBaseContext(), "you click info", Toast.LENGTH_SHORT).show()
                // startActivity( Intent(Intent.ACTION_VIEW, Uri.parse("ecooltra://reserve?vehicle_id=ES-B-A00990&referrer_id=RaccTrips")));
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(myItem.mUrlReserva)
                    )
                )

            }

        fab.setOnClickListener(View.OnClickListener { v: View? ->
            if (!isFABOpen) {
                showFABMenu()
            } else {
                closeFABMenu()
            }


        })

        fabEcooltra.setOnClickListener(View.OnClickListener { v: View? ->

            if (isFABOpen) {
                if (!ecooltraCheck) {
                    fabEcooltra.setImageResource(R.mipmap.ecooltra_icon)
                    ecooltraCheck = true
                    for (item: MyItem in alItems) {
                        if (item.mMarca.equals("Ecooltra")) {
                            mClusterManager.addItem(item)
                        }
                    }

                } else {
                    fabEcooltra.setImageResource(R.mipmap.ecooltra_grey)
                    ecooltraCheck = false
                    for (item: MyItem in alItems) {
                        if (item.mMarca.equals("Ecooltra")) {
                            mClusterManager.removeItem(item)
                        }
                    }
                }
            }
            mClusterManager.cluster()

        })

        fabScoot.setOnClickListener(View.OnClickListener { v: View? ->

            if (isFABOpen) {
                if (!scootCheck) {
                    fabScoot.setImageResource(R.mipmap.scout_icon)
                    scootCheck = true

                    for (item: MyItem in alItems) {
                        if (item.mMarca.equals("Scoot")) {
                            mClusterManager.addItem(item)
                        }
                    }
                } else {
                    fabScoot.setImageResource(R.mipmap.scoot_grey)
                    scootCheck = false

                    for (item: MyItem in alItems) {
                        if (item.mMarca.equals("Scoot")) {
                            mClusterManager.removeItem(item)
                        }
                    }

                }
            }
            mClusterManager.cluster()

        })
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


                    startActivity( Intent(Intent.ACTION_VIEW, Uri.parse("")));


               }
*/


    }

    fun showFABMenu() {
        isFABOpen = true
        fab1.animate().translationY(-getResources().getDimension(R.dimen.standard_55));
        fabScoot.animate().translationY(-getResources().getDimension(R.dimen.standard_105));
        fabEcooltra.animate().translationY(-getResources().getDimension(R.dimen.standard_155));
    }

    fun closeFABMenu() {
        isFABOpen = false
        fab1.animate().translationY(0f)
        fabScoot.animate().translationY(0f)
        fabEcooltra.animate().translationY(0f)
    }


}
