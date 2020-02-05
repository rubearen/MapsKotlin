package com.example.mapskotlin

import com.google.android.gms.maps.model.LatLng

import com.google.maps.android.clustering.ClusterItem


class MyItem : ClusterItem {
    val mPosition: LatLng
    val mTitle: String
    val mDistanceBat: String?
    val mMarca: String
    val mModelo: String?
    val mPrice: String?
    val mDistanceFromUser: String?


    constructor(
        lat: Double,
        lng: Double,
        title: String,
        distance: String?,
        marca: String,
        modelo: String?,
        price: String?,
        distanceFromUser: String?
    ) {
        mPosition = LatLng(lat, lng)
        mTitle = title
        mDistanceBat = distance
        mMarca = marca
        mModelo = modelo
        mPrice = price
        mDistanceFromUser = distanceFromUser
    }

    override fun getPosition(): LatLng {
        return mPosition
    }


    override fun getTitle(): String {
        return mTitle
    }

    override fun getSnippet(): String {
        return ""
    }
}