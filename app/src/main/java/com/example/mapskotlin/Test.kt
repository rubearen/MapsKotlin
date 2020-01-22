package com.example.mapskotlin

import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import java.io.FileReader

fun sacarArrayTest() {


    val gson = Gson()

    val tutorial_1: ScoutMotos =
        gson.fromJson(FileReader("vehicles.json"), ScoutMotos::class.java)

    println(tutorial_1.vehicles[0].toString())
    val motos: Array<MotoScout> = tutorial_1.vehicles


    for (moto in motos) {
        //  placeMarkerOnMap(LatLng(moto.latitude.toDouble(), moto.longitude.toDouble()))

    }}


//return motos