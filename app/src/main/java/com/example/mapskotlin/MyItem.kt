package com.example.mapskotlin

import com.google.android.gms.maps.model.LatLng

import com.google.maps.android.clustering.ClusterItem


class MyItem : ClusterItem {
    private val mPosition: LatLng
    private val mTitle: String
    private val mSnippet: String
    private val mMarca: String
    private val mModelo: String

    constructor(lat: Double, lng: Double, marca: String) {
        mPosition = LatLng(lat, lng)
        mTitle = ""
        mSnippet = ""
        mMarca = marca
        mModelo = ""


    }

    constructor(
        lat: Double,
        lng: Double,
        title: String,
        snippet: String,
        marca: String,
        modelo: String
    ) {
        mPosition = LatLng(lat, lng)
        mTitle = title
        mSnippet = snippet
        mMarca = marca
        mModelo = modelo
    }

    override fun getPosition(): LatLng {
        return mPosition
    }

    fun getMarca(): String {
        return mMarca
    }

    fun getModelo(): String {
        return mModelo
    }

    override fun getTitle(): String {
        return mTitle
    }

    override fun getSnippet(): String {
        return mSnippet
    }
}