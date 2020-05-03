package com.example.mapskotlin

//classes para sacar las motos del jsonScout de scout


class MotoEcooltra(
    var id: String,
    var position: Position,
    var vehicleType: String?,
    var urls: Urls?,
    var distance: String?,
    var plate: String?,
    var pricePerMinute: String?,
    var currentDistance: String?

) {
    var marca: String = "Ecooltra"


    override fun toString(): String {
        return "MotoEcooltra(id='$id', position=$position, vehicleType='$vehicleType', urls=$urls, distance='$distance', plate='$plate', pricePerMinute='$pricePerMinute', marca=" + marca + ")"
    }


}


class EcooltraMotosInferior(
    var vehicles: Array<MotoEcooltra>
)


class EcooltraMotos(
    val vehicles: Array<EcooltraMotosInferior>
)

class Position(
    val latitude: String,
    val longitude: String
) {
    override fun toString(): String {
        return "(latitude='$latitude', longitude='$longitude')"
    }
}

class Urls(
    var reserveUrl: String?
) {
    override fun toString(): String {
        return "Urls(reserveUrl='$reserveUrl')"
    }
}
