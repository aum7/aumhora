package com.aum.aumhora

import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.serialization.json.*

data class Hora(
    val start: String,
    val end: String,
    val ruler: String
)

fun getHoras(
    json: JsonObject,
    selectedDate: String
): List<Hora> {
    // load json
    var dataArray = json["data"]!!.jsonArray
    // find date
    var sunriseStr = ""
    var sunsetStr = ""
    var nextSunriseStr = ""
    var weekday = ""
    for (i in dataArray.indices) {
        val obj = dataArray[i].jsonObject
        val date = obj["date"]!!.jsonPrimitive.content
        if (date == selectedDate) {
            sunriseStr = obj["sunrise"]!!.jsonPrimitive.content
            sunsetStr = obj["sunset"]!!.jsonPrimitive.content
            weekday = obj["weekday"]!!.jsonPrimitive.content
            if (i + 1 < dataArray.size) {
                nextSunriseStr = dataArray[i + 1].jsonObject["sunrise"]!!.jsonPrimitive.content
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
    val nextSunriseMillis =
        if (nextSunrise.time <= sunset.time) nextSunrise.time + 24 * 3600_000 else nextSunrise.time
    val nightDuration = nextSunriseMillis - sunset.time
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
        val rulerIndex = (rulers.indexOf(firstRuler) + i) % 7
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
    val file = File("slo_ljubljana_2025.json")
    val jsonString = file.readText()
    val json = Json.parseToJsonElement(jsonString).jsonObject
    val city = json["city"]?.jsonPrimitive?.content
    println("city : $city")
    val selectedDate = "2025-01-01"
    val horas = getHoras(json, selectedDate)
    // get weekday
    val dataArray = json["data"]!!.jsonArray
    val weekday =
        dataArray.first { it.jsonObject["date"]!!.jsonPrimitive.content == selectedDate }.jsonObject["weekday"]!!.jsonPrimitive.content.lowercase()
    println("horas for $weekday | $selectedDate : ")
    for ((i, h) in horas.withIndex()) {
        println("${i + 1} : ${h.start} - ${h.end}  ${h.ruler}")
    }
}