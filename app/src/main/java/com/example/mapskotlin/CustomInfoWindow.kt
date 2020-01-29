package com.example.mapskotlin

import android.app.Activity
import android.content.Context
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import kotlinx.android.synthetic.main.info_view_constraint.view.*


class CustomInfoWindow(val context: Context) : GoogleMap.InfoWindowAdapter, MapsActivity() {

    override fun getInfoContents(marker: Marker): View {


        var mInfoView =
            (context as Activity).layoutInflater.inflate(R.layout.info_view_constraint, null)

        mInfoView.tvMarca.setText(clickedClusterItem?.getMarca())

        if (clickedClusterItem?.getModelo().equals("SCOOT_EBIKE")) {
            mInfoView.pictureMoto.setImageResource(R.drawable.ebike)
        } else if (clickedClusterItem?.getModelo().equals("SILENCE")) {
            mInfoView.pictureMoto.setImageResource(R.drawable.silence)
        }

        return mInfoView
    }

    override fun getInfoWindow(marker: Marker?): View? {
        if (marker != null && marker.isInfoWindowShown()) {
            marker.hideInfoWindow();
            marker.showInfoWindow();
        }


        return null
    }

}






