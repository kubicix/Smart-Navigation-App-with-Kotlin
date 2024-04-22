package com.kubicix.smartnavigation

import com.google.android.gms.maps.model.LatLng
import org.json.JSONException
import org.json.JSONObject

class DirectionsJSONParser {

    fun parse(jsonObject: JSONObject): Pair<List<List<HashMap<String, String>>>, List<String>> {
        val routes: MutableList<List<HashMap<String, String>>> = ArrayList()
        val maneuvers: MutableList<String> = ArrayList()

        val routesArray = jsonObject.getJSONArray("routes")
        for (i in 0 until routesArray.length()) {
            val route = routesArray.getJSONObject(i)
            val legs = route.getJSONArray("legs")
            val path = ArrayList<HashMap<String, String>>()

            for (j in 0 until legs.length()) {
                val leg = legs.getJSONObject(j)
                val steps = leg.getJSONArray("steps")

                for (k in 0 until steps.length()) {
                    val step = steps.getJSONObject(k)
                    val points = step.getJSONObject("polyline")
                    val encodedPoints = points.getString("points")
                    val decodedPoints = decodePoly(encodedPoints)

                    for (l in decodedPoints.indices) {
                        val position: HashMap<String, String> = HashMap()
                        position["lat"] = decodedPoints[l].latitude.toString()
                        position["lng"] = decodedPoints[l].longitude.toString()
                        path.add(position)
                    }

                    // Manevraları al ve listeye ekle
                    val maneuver = step.optString("html_instructions")
                    val distanceValue = step.optJSONObject("distance")?.optInt("value") // Adım mesafesini metre cinsinden al
                    if (distanceValue != null && maneuver.isNotEmpty()) {
                        val cleanManeuver = maneuver.replace(Regex("<[^>]*>"), "") // HTML etiketlerini kaldırır
                        val stepWithDistance = cleanManeuver.replace("ilerleyin", "") + " (ve $distanceValue metre ilerleyin)" // Adım mesafesini adım talimatına ekler
                        maneuvers.add(stepWithDistance)
                    }





                }
                routes.add(path)
            }
        }

        return Pair(routes, maneuvers)
    }


    fun parseManeuvers(jsonObject: JSONObject): List<String> {
        val maneuvers: MutableList<String> = mutableListOf()

        val routesArray = jsonObject.getJSONArray("routes")
        for (i in 0 until routesArray.length()) {
            val route = routesArray.getJSONObject(i)
            val legs = route.getJSONArray("legs")
            val path = ArrayList<HashMap<String, String>>()

            for (j in 0 until legs.length()) {
                val leg = legs.getJSONObject(j)
                val steps = leg.getJSONArray("steps")

                for (k in 0 until steps.length()) {
                    val step = steps.getJSONObject(k)
                    if (step.has("maneuver")) {
                        val maneuver = step.getString("maneuver")
                        maneuvers.add(maneuver)
                    } else if (step.has("html_instructions")) {
                        val maneuver = step.getString("html_instructions")
                        maneuvers.add(maneuver)
                    }
                }
            }
        }

        return maneuvers
    }

    private fun decodePoly(encoded: String): List<LatLng> {
        val poly: MutableList<LatLng> = ArrayList()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }

        return poly
    }




}