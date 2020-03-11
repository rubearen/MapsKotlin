package com.example.mapskotlin


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.gson.Gson
import com.google.maps.android.clustering.ClusterManager
import kotlinx.android.synthetic.main.activity_map_searchbar.*
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList


open class MapsActivity : AppCompatActivity(), OnMapReadyCallback, PlaceSelectionListener {
    private var jsonUpdate = true

    //necesitamos 2 arraylist para actualizar solo los datos nuevos o que hay que borrar, y mantener los que siguen
    protected var alItems = ArrayList<MyItem>()
    protected var alItemsActualizado = ArrayList<MyItem>()

    private var isFABOpen: Boolean = false //saber si el filtro esta desplegado o no
    //flag para saber cuando está activada o no una marca
    private var ecooltraCheck: Boolean = true
    private var scootCheck: Boolean = true
    private var accionaCheck: Boolean=true

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

    //variables para guardar los JSON con la info de las motos, tenemos 2 de cada para simular una api real que se actualiza
    var jsonScout = ""
    var jsonEcooltra = ""
    var jsonEcooltraBorrado = ""
    var jsonScoutBorrado = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_searchbar)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)

        onMapReadyCallback()

    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setUpMap()


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
        //si tiene permisos iniciamos la app, si los tiene que pedir, la iniciamos en onRequestPermissionsResult
        jsonFromApi()
        setUpClusterer()
        listener()
        setUpInfoWindow()
        //marcarMotosMapa()
        searchbarSetUp()


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted,
                    Toast.makeText(this, "permisos aceptados", Toast.LENGTH_LONG).show()

                    jsonFromApi()
                    setUpClusterer()
                    listener()
                    setUpInfoWindow()
                    // marcarMotosMapa()
                    searchbarSetUp()


                } else {
                    // permission denied
                    println("AAAAAAAAAAAAAAAAAAAAAAA///////////////////////////////////////////")
                    Toast.makeText(this, "APP CERRADA", Toast.LENGTH_LONG).show()
                    finishAffinity()


                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }


    fun onMapReadyCallback() { // Registrar escucha onMapReadyCallback
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                lastLocation =
                    p0.lastLocation //se va acutalizando la posición actual y se guarda en esta var
                //placeMarkerOnMap(LatLng(lastLocation.latitude, lastLocation.longitude))
            }
        }
        createLocationRequest()

    }

    private fun startLocationUpdates() {//funcion para actualizar la posicion en tiempo real

        //pedimos la localizacion del usuario
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

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                place.latLng, 15f

            ), 600, null
        )
    }

    //listener de la searchbar, cuando no elegimos o da error
    override fun onError(status: Status) {
        Log.i(TAG, "An error occurred: " + status)

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

        map.uiSettings.isMapToolbarEnabled = false
        map.uiSettings.isZoomControlsEnabled = true //habilitamos los controles del ZOOM
        map.isMyLocationEnabled =
            true      //habilitamos los controles del boton para ubicar en mi posicion
        map.setOnMarkerClickListener(mClusterManager)


    }

    fun actualizarMotos() {
        //funcion para actualizar los items conforme los vayamos descargando, borramos los que ya no están disponibles
        //y añadimos los nuevos , manteniendo los que sigan estando para que el usuario pueda seguir consultando las motos
        //y no se le cierre el infoview cada vez que actualizamos (solo se le cerraría si la  moto que está consultando
        // dejase de estar disponible, y en este caso se le muestra un toast al usuario informadole )

        //para simular el funcionamiento de una api real, hemos creado 2 json, para que lea las motos de uno y otro cada x tiempo
        //y actualice el mapa


        var alItemsborrar = ArrayList<MyItem>()
        for (item2 in alItemsActualizado) {
            var flag = false
            for (item in alItems) {
                if (item.mUrlReserva.equals(item2.mUrlReserva)) {
                    flag = true
                }
            }

            if (!flag) {
                if (clickedClusterItem?.mUrlReserva.equals(item2.mUrlReserva)) {
                    Toast.makeText(
                        this,
                        "Esta moto ha dejado de estar disponible",
                        Toast.LENGTH_LONG
                    ).show()
                }
                //alItemsActualizado.remove(item2)

                alItemsborrar.add(item2)
                mClusterManager.removeItem(item2)

            }
        }

        alItemsActualizado.removeAll(alItemsborrar)

        for (item in alItems) {
            var flag = false
            for (item2 in alItemsActualizado) {
                if (item.mUrlReserva.equals(item2.mUrlReserva)) {
                    flag = true
                }
            }
            if (!flag) {
                alItemsActualizado.add(item)
                mClusterManager.addItem(item)


            }
        }

        mClusterManager.cluster()

    }

    fun marcarMotosMapa() {

        Toast.makeText(this, alItems.size.toString(), Toast.LENGTH_LONG).show()
        mClusterManager.clearItems()


        mClusterManager.addItems(alItems)
        mClusterManager.cluster()

        /*
        for (item: MyItem in alItems) {
            placeMotoOnMapClustering(item)
        }*/

    }

    fun sacarArrayScout() {

        val gson = Gson()
        /* //codigo para sacar las motos desde un json local guardado en assets
        //var jsonScout = ""

        try {
            //val inputStream: InputStream = assets.open("vehicles.json")
            //jsonScout = inputStream.bufferedReader().use { it.readText() }


        } catch (e: IOException) {
            e.printStackTrace()
        }

 */
        var arrayScouts: ScoutMotos =
            gson.fromJson(jsonScout, ScoutMotos::class.java)
        if (!jsonUpdate) {
            arrayScouts = gson.fromJson(jsonScoutBorrado, ScoutMotos::class.java)
        }


        println(arrayScouts.vehicles[0].toString())
        val motos: Array<MotoScout> = arrayScouts.vehicles

        if (motos.size > 0) {
            for (moto in motos) {
                moto.marca = "Scoot"
                val item =
                    MyItem(
                        moto.latitude.toDouble(),
                        moto.longitude.toDouble(),
                        moto.id,
                        null,
                        "Scoot", moto.vehicle_type, null, null, "scout" + moto.latitude
                    )           //falta crear enlace para scout, nos sirve para diferenciarlas
                alItems.add(item)
                // placeMotoOnMapClustering(item)
            }
        }
    }

    fun sacarArrayEcooltra() {

        val gson = Gson()
        /* //codigo para sacar las motos desde un json local guardado en assets
        //jsonEcooltra = ""

        try {
            //  val inputStream: InputStream = assets.open("ecooltra.json")
            // jsonEcooltra = inputStream.bufferedReader().use { it.readText() }

            } catch (e: IOException) {
            e.printStackTrace()
        }



        val arrayEcooltras: EcooltraMotos =
            gson.fromJson(jsonEcooltra, EcooltraMotos::class.java)

            */


        val arrayEcooltras: EcooltraMotos
        if (jsonUpdate) {
            arrayEcooltras =
                gson.fromJson(jsonEcooltra, EcooltraMotos::class.java)
            jsonUpdate = false

        } else {
            arrayEcooltras =
                gson.fromJson(jsonEcooltraBorrado, EcooltraMotos::class.java)
            jsonUpdate = true
        }


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

//listener que recibe la ultima localización al abrir la app
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            //trabajamos con la ultima localización que tenemos
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
            } else {
                Toast.makeText(getBaseContext(), "LOCATION IS NULL", Toast.LENGTH_SHORT).show()


            }
        }


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

        fab1.setOnClickListener { v: View? ->
            if (isFABOpen) {
                if (!accionaCheck) {
                    fab1.setImageResource(R.mipmap.acciona_icon)
                    accionaCheck = true
                    for (item: MyItem in alItems) {
                        if (item.mMarca.equals("Acciona")) {
                            mClusterManager.addItem(item)
                        }
                    }

                } else {
                    fab1.setImageResource(R.mipmap.acciona_icon_grey)
                    accionaCheck = false
                    for (item: MyItem in alItems) {
                        if (item.mMarca.equals("Acciona")) {
                            mClusterManager.removeItem(item)
                        }
                    }
                }
            }
            mClusterManager.cluster()


        }

        fab3.setOnClickListener(View.OnClickListener { v: View? ->

            if (isFABOpen) {
                if (!ecooltraCheck) {
                    fab3.setImageResource(R.mipmap.ecooltra_icon)
                    ecooltraCheck = true
                    for (item: MyItem in alItems) {
                        if (item.mMarca.equals("Ecooltra")) {
                            mClusterManager.addItem(item)
                        }
                    }

                } else {
                    fab3.setImageResource(R.mipmap.ecooltra_grey)
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

        fab2.setOnClickListener(View.OnClickListener { v: View? ->

            if (isFABOpen) {
                if (!scootCheck) {
                    fab2.setImageResource(R.mipmap.scout_icon)
                    scootCheck = true

                    for (item: MyItem in alItems) {
                        if (item.mMarca.equals("Scoot")) {
                            mClusterManager.addItem(item)
                        }
                    }
                } else {
                    fab2.setImageResource(R.mipmap.scoot_grey)
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
        fab1.animate().translationY(-getResources().getDimension(R.dimen.standard_55))
        fab2.animate().translationY(-getResources().getDimension(R.dimen.standard_105))
        fab3.animate().translationY(-getResources().getDimension(R.dimen.standard_155))
    }

    fun closeFABMenu() {
        isFABOpen = false
        fab1.animate().translationY(0f)
        fab2.animate().translationY(0f)
        fab3.animate().translationY(0f)
    }

    fun jsonFromApi() {


        Thread {


            jsonScout = URL("https://api.myjson.com/bins/9akyq").readText()
            jsonScoutBorrado = URL("https://api.jsonbin.io/b/5e66351d1a3d6b7e7c050f1d").readText()

            jsonEcooltra = URL("https://api.jsonbin.io/b/5e662c3a1a3d6b7e7c0508c3").readText()
            jsonEcooltraBorrado =
                URL("https://api.jsonbin.io/b/5e662d80ffe2e77da21fbce8").readText()


            runOnUiThread {


                alItems.clear()
                sacarArrayScout()
                sacarArrayEcooltra()
                actualizarMotos()
                //marcarMotosMapa()
                Handler().postDelayed({
                    jsonFromApi()

                }, 10000)

            }
        }.start()

    }

}