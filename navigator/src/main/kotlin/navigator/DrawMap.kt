package navigator

import com.opencsv.CSVReader
import io.jenetics.jpx.GPX
import java.io.FileReader
import java.nio.file.Paths


fun main(args: Array<String>) {

    val pivotPoint = LatLonPosition(lat = Latitude(49.336916), lng = Longitude(7.82547))

    val csvReader = CSVReader(FileReader(Paths.get("data/db/betriebsstellen.csv").toFile()))
    val csvLines = csvReader.readAll().drop(1)
    val result: GPX.Builder = csvLines.map(::csvLineTo)
            .filter { "Bf" == it.stellen_ART }
            .filter { calculateDistanceInKilometer(it.latLon(), pivotPoint).value < 50 }
            .fold(GPX.builder()) { acc, next -> addLocationAsWayPointTo(acc, next) }
    GPX.write(result.build(), Paths.get("data/db/stations.gpx"))
}


data class NameAndGeoLocation(val bezeichnung: String, val stellen_ART: String, val geoGr_BREITE: String, val geoGR_LAENGE: String) {
    fun latLon(): LatLonPosition {
        return LatLonPosition(lat = Latitude(geoGr_BREITE.toDouble()), lng = Longitude(geoGR_LAENGE.toDouble()))
    }
}

fun csvLineTo(parts: Array<String>): NameAndGeoLocation {
    return NameAndGeoLocation(bezeichnung = parts[4], stellen_ART = parts[5], geoGr_BREITE = parts[9], geoGR_LAENGE = parts[10])
}

fun addLocationAsWayPointTo(gpx: GPX.Builder, location: NameAndGeoLocation): GPX.Builder {
    gpx.addWayPoint { wayPoint ->
        wayPoint.name(location.bezeichnung)
        wayPoint.lat(location.geoGr_BREITE.toDouble())
        wayPoint.lon(location.geoGR_LAENGE.toDouble())
    }
    return gpx;
}

data class Latitude(val value: Double)
data class Longitude(val value: Double)
data class Kilometers(val value: Double)

data class LatLonPosition(val lat: Latitude, val lng: Longitude)


val AVERAGE_RADIUS_OF_EARTH_KM = Kilometers(6371.0)

fun calculateDistanceInKilometer(user: LatLonPosition, pivot: LatLonPosition): Kilometers {

    val latDistance = Math.toRadians(user.lat.value - pivot.lat.value)
    val lngDistance = Math.toRadians(user.lng.value - pivot.lng.value)

    val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + (Math.cos(Math.toRadians(user.lat.value)) * Math.cos(Math.toRadians(pivot.lat.value))
            * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2))

    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    val kilometers = AVERAGE_RADIUS_OF_EARTH_KM.value * c
    return Kilometers(kilometers)
}