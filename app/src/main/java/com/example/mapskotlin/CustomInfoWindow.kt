package com.example.mapskotlin

import android.app.Activity
import android.content.Context
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import kotlinx.android.synthetic.main.info_view_constraint.view.*


class CustomInfoWindow(val context: Context) : GoogleMap.InfoWindowAdapter, MapsActivity() {

    val precioScootBici: String = "0.15"
    val precioScootMoto: String = "0.28"
    val precioEooltra: String = "0.26"

    //usar geinfowindow para que el fondo  se ajuste a los bordes redondeados

    override fun getInfoContents(marker: Marker): View? {
        return null

    }

    override fun getInfoWindow(marker: Marker?): View? {


        var mInfoView =
            (context as Activity).layoutInflater.inflate(R.layout.info_view_constraint, null)


        //atributos comunes para todas las marcas, comprobamos si es null para darle valor
        if (clickedClusterItem?.mDistanceFromUser == null) {
            mInfoView.tv_distance.setText("?")
        } else {
            mInfoView.tv_distance.setText(clickedClusterItem?.mDistanceFromUser + " Km")
        }
        mInfoView.tv_batery.setText(clickedClusterItem?.mDistanceBat)

        //atributos específicos para cada marca
        if (clickedClusterItem?.mMarca.equals("Scoot")) {
            scootInfoview(mInfoView)

        } else if (clickedClusterItem?.mMarca.equals("Ecooltra")) {

            ecooltraInfoview(mInfoView)
           
        } else if (clickedClusterItem?.mMarca.equals("Acciona")) {

            accionaInfoView(mInfoView)
        }

        return mInfoView
    }


    fun scootInfoview(mInfoView: View) {

        mInfoView.letrasMarca.setImageResource(R.drawable.scoot_logo_letras)
        if (clickedClusterItem?.mModelo.equals("SCOOT_EBIKE")) {
            mInfoView.tv_precio.setText(precioScootBici)
            mInfoView.pictureMoto.setImageResource(R.drawable.ebike)

        } else if (clickedClusterItem?.mModelo.equals("SILENCE")) {
            mInfoView.tv_precio.setText(precioScootBici)
            mInfoView.pictureMoto.setImageResource(R.drawable.silence)

        }

    }

    fun ecooltraInfoview(mInfoView: View) {
        mInfoView.letrasMarca.setImageResource(R.drawable.ecooltra_logo_letras)
        mInfoView.pictureMoto.setImageResource(R.drawable.ecooltra_moto)
        mInfoView.tv_precio.setText(clickedClusterItem?.mPrice)


    }

    fun accionaInfoView(mInfoView: View) {
        mInfoView.letrasMarca.setImageResource(R.drawable.acciona_letras)
        mInfoView.pictureMoto.setImageResource(R.drawable.acciona_moto)
        mInfoView.tv_precio.setText(clickedClusterItem?.mPrice)


    }
}




