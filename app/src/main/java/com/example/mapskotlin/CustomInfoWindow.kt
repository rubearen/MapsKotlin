package com.example.mapskotlin

import android.app.Activity
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.example.mapskotlin.MapsActivity.Companion.clickedClusterItem
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




        //atributos comunes para todas las marcas
        mInfoView.tv_distance.setText(clickedClusterItem?.mDistanceFromUser)
        mInfoView.tv_batery.setText(clickedClusterItem?.mDistanceBat)

        //atributos específicos para cada marca
        if (clickedClusterItem?.mMarca.equals("Scoot")) {
            scootInfoview(mInfoView)

        } else if (clickedClusterItem?.mMarca.equals("Ecooltra")) {

            ecooltraInfoview(mInfoView)
        }

        return mInfoView
    }

    fun test(view: View){

        Toast.makeText(
            getBaseContext(),
            "you click button",
            Toast.LENGTH_SHORT
        ).show()
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
}




