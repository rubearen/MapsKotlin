package com.example.mapskotlin

import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer


class OwnIconRendered(
    private val context: Context,
    map: GoogleMap,
    clusterManager: ClusterManager<MyItem>


) : DefaultClusterRenderer<MyItem>(context, map, clusterManager) {

    /*
    override fun onBeforeClusterRendered(cluster: Cluster<MyItem>?, markerOptions: MarkerOptions?) {
        super.onBeforeClusterRendered(cluster, markerOptions)
        markerOptions?.icon(BitmapDescriptorFactory.fromResource(R.drawable.scout_round_bmp))


    }
*/
    fun render(){

    }
    override fun onBeforeClusterItemRendered(item: MyItem?, markerOptions: MarkerOptions?) {
        super.onBeforeClusterItemRendered(item, markerOptions)
        if (item?.mMarca.equals("Scoot")) {

            //val bMap = BitmapFactory.decodeResource(Resources.getSystem(),R.drawable.scout_round_bmp75)


           markerOptions?.icon(BitmapDescriptorFactory.fromResource(R.drawable.scout_icon_round25))

        } else if (item?.mMarca.equals("Ecooltra")) {
            markerOptions?.icon(BitmapDescriptorFactory.fromResource(R.drawable.ecooltra_icon_round_25))

        } else if (item?.mMarca.equals("Acciona")) {
            markerOptions?.icon(BitmapDescriptorFactory.fromResource(R.drawable.acciona_icon_round_25))

        }
    }
}


