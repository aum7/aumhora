package com.aum.aumhora

import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.serialization.json.*

data class Hora(
    val start: String,
    val end: String,
    val ruler: String,
    val subhoras: List<SubHora> = emptyList()
)

data class SubHora(
    val start: String,
    val end: String,
    val ruler: String
)

fun generateSubhoras(
    hora: Hora,
    sdf: SimpleDateFormat,
    rulers: List<String>
): List<SubHora> {
    val startDate = sdf.parse(hora.start)!!
    val endDate = sdf.parse(hora.end)!!
    val duration = endDate.time - startDate.time
    val subDuration = duration / 15

    val baseIndex = rulers.indexOf(hora.ruler)
    val list = mutableListOf<SubHora>()
    for (i in 0 until 15) {
        val s = Date(startDate.time + i * subDuration)
        val e = Date(startDate.time + (i + 1) * subDuration)
        val r = rulers[(baseIndex + i) % 7]
        list.add(
            SubHora(
                sdf.format(s), sdf.format(e),
                r
            )
        )
    }
    return list
}

fun getHoras(
    json: JsonObject,
    selectedDate: String
): List<Hora> {
    // load json
    val dataArray = json["data"]!!.jsonArray
    // find date
    var sunriseStr = ""
    var sunsetStr = ""
    var nextSunriseStr = ""
//    var weekday = ""
    for (i in dataArray.indices) {
        val obj = dataArray[i].jsonObject
        val date = obj["date"]!!.jsonPrimitive.content
        if (date == selectedDate) {
            sunriseStr = obj["sunrise"]!!.jsonPrimitive.content
            sunsetStr = obj["sunset"]!!.jsonPrimitive.content
//            weekday = obj["weekday"]!!.jsonPrimitive.content
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
    val rulers = listOf("su", "ve", "me", "mo", "sa", "ju", "ma")
//    val horaList = mutableListOf<Hora>()
    // find 1st ruler of the day
    val weekday =
        dataArray.first {
            it.jsonObject["date"]!!
                .jsonPrimitive.content == selectedDate
        }
            .jsonObject["weekday"]!!.jsonPrimitive.content.lowercase()
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
        val startTime = Date(
            sunrise.time + i * dayDuration / 12
        )
        val endTime = Date(
            sunrise.time + (i + 1) * dayDuration / 12
        )
        val rulerIndex = (
                rulers.indexOf(firstRuler) + i) % 7
        val hora = Hora(
            sdf.format(startTime),
            sdf.format(endTime),
            rulers[rulerIndex]
        )
        horaList.add(
            hora.copy(
                subhoras = generateSubhoras(
                    hora, sdf, rulers
                )
            )
        )
    }
    // night horas
    val firstNightRulerIndex = (
            rulers.indexOf(firstRuler) + 12) % 7
    for (i in 0 until 12) {
        val startTime = Date(
            sunset.time + i * nightDuration / 12
        )
        val endTime = Date(
            sunset.time + (i + 1) * nightDuration / 12
        )
        val rulerIndex = (
                firstNightRulerIndex + i) % 7
        val hora = Hora(
            sdf.format(startTime),
            sdf.format(endTime),
            rulers[rulerIndex]
        )
        horaList.add(
            hora.copy(
                subhoras = generateSubhoras(
                    hora, sdf,
                    rulers
                )
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
        for ((j, sh) in h.subhoras.withIndex()) {
            println("   ${i + 1}.${j + 1} : ${sh.start} - ${sh.end}  ${sh.ruler}")
        }
    }
}