package com.example.mapskotlin

//classes para sacar las motos del json de scout
class MotoScout (
    val id: String,
    val latitude: String,
    val longitude: String,
    var marca: String)
    {

        override fun toString(): String {
            return "Moto(id='$id', latitude='$latitude', longitude='$longitude', marca='$marca')"
        }
    }

class ScoutMotos (
    val vehicles: Array<MotoScout>)