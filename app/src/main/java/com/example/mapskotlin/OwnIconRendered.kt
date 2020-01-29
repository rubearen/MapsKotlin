package com.example.mapskotlin

import android.content.Context

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
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
    override fun onBeforeClusterItemRendered(item: MyItem?, markerOptions: MarkerOptions?) {
        super.onBeforeClusterItemRendered(item, markerOptions)
        if (item?.getMarca().equals("scout")) {
            markerOptions?.icon(BitmapDescriptorFactory.fromResource(R.drawable.scout_icon_round25))
        }
    }
}


