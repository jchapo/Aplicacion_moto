package com.example.moto_version

import android.util.Log
import java.util.regex.Pattern
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.net.URLEncoder

object CoordenadasExtractor {
    private val client = OkHttpClient()

    private val latPattern = Pattern.compile("(-1[1-2]\\.\\d+)")
    private val lonPattern = Pattern.compile("(-7[6-7]\\.\\d+)")
    private val atCoordsPattern = Pattern.compile("@(-1[1-2]\\.\\d+),(-7[6-7]\\.\\d+)")
    private val lat3dPattern = Pattern.compile("!3d(-1[1-2]\\.\\d+)")
    private val lon4dPattern = Pattern.compile("!4d(-7[6-7]\\.\\d+)")

    fun extraerCoordenadas(texto: String): Pair<Double, Double>? {
        val decodedTexto = URLDecoder.decode(texto, "UTF-8")
        Log.d("TAG", "Texto decodificado: $decodedTexto")

        // 1. Buscar patrón @latitud,longitud en URL
        val atMatch = atCoordsPattern.matcher(decodedTexto)
        if (atMatch.find()) {
            Log.d("TAG", "Coordenadas encontradas con @lat,lng: ${atMatch.group(1)}, ${atMatch.group(2)}")
            return Pair(atMatch.group(1).toDouble(), atMatch.group(2).toDouble())
        }

        // 2. Buscar coordenadas en cualquier parte del texto
        val latMatches = latPattern.matcher(decodedTexto)
        val lonMatches = lonPattern.matcher(decodedTexto)

        val latitudes = mutableListOf<String>()
        val longitudes = mutableListOf<String>()

        while (latMatches.find()) {
            latitudes.add(latMatches.group(1))
        }
        while (lonMatches.find()) {
            longitudes.add(lonMatches.group(1))
        }

        Log.d("TAG", "Latitudes encontradas: $latitudes")
        Log.d("TAG", "Longitudes encontradas: $longitudes")

        if (latitudes.isNotEmpty() && longitudes.isNotEmpty()) {
            for (lat in latitudes) {
                for (lon in longitudes) {
                    try {
                        val latDouble = lat.toDouble()
                        val lonDouble = lon.toDouble()

                        if (latDouble in -13.0..-11.0 && lonDouble in -78.0..-76.0) {
                            Log.d("TAG", "Coordenadas válidas: $latDouble, $lonDouble")
                            return Pair(latDouble, lonDouble)
                        }
                    } catch (e: NumberFormatException) {
                        Log.e("TAG", "Error al convertir coordenadas: ${e.message}")
                    }
                }
            }
        }

        // 3. Buscar coordenadas en formato !3d{lat}!4d{lon}
        val lat3dMatch = lat3dPattern.matcher(decodedTexto)
        val lon4dMatch = lon4dPattern.matcher(decodedTexto)
        if (lat3dMatch.find() && lon4dMatch.find()) {
            Log.d("TAG", "Coordenadas encontradas con !3d{lat}!4d{lon}: ${lat3dMatch.group(1)}, ${lon4dMatch.group(1)}")
            return Pair(lat3dMatch.group(1).toDouble(), lon4dMatch.group(1).toDouble())
        }

        Log.d("TAG", "No se encontraron coordenadas.")
        return null
    }

    fun expandirUrl(url: String): String {
        Log.d("TAG", "Expandiendo URL: $url")
        return try {
            val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
            client.newCall(request).execute().use { response ->
                val expandedUrl = response.request.url.toString()
                Log.d("TAG", "URL expandida: $expandedUrl")
                expandedUrl
            }
        } catch (e: Exception) {
            Log.e("TAG", "Error al expandir URL: ${e.message}")
            url
        }
    }

    fun extraerNombreLugar(url: String): String? {
        val decodedUrl = URLDecoder.decode(url, "UTF-8")
        Log.d("TAG", "URL decodificada: $decodedUrl")

        val placeMatch = Regex("/place/([^/]+)").find(decodedUrl)
        val nombreLugar = placeMatch?.groupValues?.get(1)?.replace("+", " ")?.replace("%20", " ")?.replace("%2C", ",")
        Log.d("TAG", "Nombre del lugar extraído: $nombreLugar")

        return nombreLugar
    }

    fun obtenerCoordenadas(url: String): Pair<Double, Double>? {
        val expandedUrl = expandirUrl(url)
        extraerCoordenadas(expandedUrl)?.let {
            Log.d("TAG", "Coordenadas extraídas de la URL: $it")
            return it
        }

        val nombreLugar = extraerNombreLugar(expandedUrl)
        return nombreLugar?.let {
            Log.d("TAG", "Buscando coordenadas con Geocoding para: $it")
            obtenerCoordenadasGeocoding(it)
        }
    }

    fun obtenerCoordenadasGeocoding(texto: String): Pair<Double, Double>? {
        val apiKey = BuildConfig.GEO_API_KEY
        Log.d("TAG", "API Key utilizada: $apiKey")

        val textoFormateado = URLEncoder.encode(texto, "UTF-8")
        val url = "https://maps.googleapis.com/maps/api/geocode/json?address=$textoFormateado&key=$apiKey"
        Log.d("TAG", "URL de Geocoding: $url")

        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                val json = response.body?.string()
                Log.d("TAG", "Respuesta JSON: $json")

                val latMatch = Regex("\"lat\"\\s*:\\s*(-?\\d+\\.\\d+)").find(json ?: "")
                val lonMatch = Regex("\"lng\"\\s*:\\s*(-?\\d+\\.\\d+)").find(json ?: "")

                if (latMatch != null && lonMatch != null) {
                    val lat = latMatch.groupValues[1].toDouble()
                    val lon = lonMatch.groupValues[1].toDouble()

                    if (lat in -13.0..-11.0 && lon in -78.0..-76.0) {
                        Log.d("TAG", "Coordenadas obtenidas por Geocoding: $lat, $lon")
                        return Pair(lat, lon)
                    }
                }
                Log.d("TAG", "No se encontraron coordenadas en la respuesta JSON.")
                null
            }
        } catch (e: Exception) {
            Log.e("TAG", "Error en la solicitud a la API de Geocoding: ${e.message}")
            null
        }
    }
}
