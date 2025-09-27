package com.aum.aumhora

import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

//import android.content.Context
//import android.os.Bundle
//import android.util.Log
//import androidx.appcompat.app.AppCompatActivity
//import java.io.InputStream

data class Hora(
    val start: String,
    val end: String,
    val ruler: String
)

fun getHoras(
    json: JSONObject,
    selectedDate: String
): List<Hora> {
    // load json
    var dataArray = json.getJSONArray("data")
    // find date
    var sunriseStr = ""
    var sunsetStr = ""
    var nextSunriseStr = ""
    var weekday = ""
    for (i in 0 until dataArray.length()) {
        val obj = dataArray.getJSONObject(i)
        val date = obj.getString("date")
        if (date == selectedDate) {
            sunriseStr = obj.getString("sunrise")
            sunsetStr = obj.getString("sunset")
            weekday = obj.getString("weekday")
            if (i + 1 < dataArray.length()) {
                nextSunriseStr = dataArray.getJSONObject(i + 1).getString("sunrise")
            }
            break
        }
    }
    if (sunriseStr.isEmpty() || sunsetStr.isEmpty() || nextSunriseStr.isEmpty()) return emptyList()
    // parse times
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val sunrise = sdf.parse(sunriseStr)!!
    val sunset = sdf.parse(sunsetStr)!!
    val nextSunrise = sdf.parse(nextSunriseStr)!!
    // calculate day / night duration in millis
    val dayDuration = sunset.time - sunrise.time
    val nightDuration = nextSunrise.time - sunset.time
    // hora rulers
    var rulers = listOf("su", "mo", "ma", "me", "ju", "ve", "sa")
    val weekdayRulerMap = mapOf(
        "sun" to "su",
        "mon" to "mo",
        "tue" to "ma",
        "wed" to "me",
        "thu" to "ju",
        "fri" to "ve",
        "sat" to "sa"
    )
    val firstRuler = weekdayRulerMap[weekday.lowercase()] ?: "su"
    val horaList = mutableListOf<Hora>()
    // calculate day horas
    for (i in 0 until 12) {
        val startTime = Date(sunrise.time + i * dayDuration / 12)
        val endTime = Date(sunrise.time + (i + 1) * dayDuration / 12)
        val rulerIndex = (rulers.indexOf(firstRuler) + 1) % 7
        horaList.add(
            Hora(
                sdf.format(startTime),
                sdf.format(endTime),
                rulers[rulerIndex]
            )
        )
    }
    // night horas
    val firstNightRulerIndex = (rulers.indexOf(firstRuler) + 12) % 7
    for (i in 0 until 12) {
        val startTime = Date(sunset.time + i * nightDuration / 12)
        val endTime = Date(sunset.time + (i + 1) * nightDuration / 12)
        val rulerIndex = (firstNightRulerIndex + i) % 7
        horaList.add(
            Hora(
                sdf.format(startTime),
                sdf.format(endTime),
                rulers[rulerIndex]
            )
        )
    }
    return horaList
}

fun main() {
    val input = context.assets.open("slo_ljubljana_2025.json")
    val jsonString = input.bufferReader().use { it.readText() }
    val json = JSONObject(jsonString)
    val selectedDate = "2025-01-01"
    val horas = getHoras(json, selectedDate)
    println("horas for $selectedDate : ")
    for ((i, h) in horas.withIndex()) {
        println("${i + 1} : ${h.start} - ${h.end}  ${h.ruler}")
    }
}

//    val dataArray = json.getJSONArray("data")
//    var sunriseStr = ""
//    var sunsetStr = ""
//    var nextSunriseStr = ""
//    var weekday = ""
//    for (i in 0 until dataArray.length()) {
//        val obj = dataArray.getJSONObject(i)
//        if (obj.getString("date") == selectedDate) {
//            sunriseStr = obj.getString("sunrise")
//            sunsetStr = obj.getString("sunset")
//            weekday = obj.getString("weekday")
//            if (i + 1 < dataArray.length()) {
//                nextSunriseStr = dataArray.getJSONObject(i + 1).getString("sunrise")
//            }
//            break
//        }
//    }
//    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
//    val sunrise = sdf.parse(sunriseStr)!!
//    val sunset = sdf.parse(sunsetStr)!!
//    val nextSunrise = sdf.parse(nextSunriseStr)!!
//
//    val dayDuration = sunset.time - sunrise.time
//    val nightDuration = nextSunrise.time - sunset.time
//
//    println("day duration : ${dayDuration / 60000} min")
//    println("night duration : ${nightDuration / 60000} min")
//    println("horas for $selectedDate :")
//    for ((i, h) in horas.withIndex()) {
//        println("${i + 1} : ${h.start} - ${h.end} ${h.ruler}")
//    }
