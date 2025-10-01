// cosmicjoke
// mainactivity.kt
package com.aum.aumhora

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aum.aumhora.ui.theme.AumhoraTheme
import com.aum.aumhora.ui.theme.SettingsPanel
import com.aum.aumhora.ui.theme.defaultHoraColor
import com.aum.aumhora.ui.theme.horaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class) // topappbar & modaldrawersheet
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AumhoraTheme {
                // states for user selection
                val sunriseFile = remember {
                    mutableStateOf<File?>(null)
                }
                val sunriseFileName = remember {
                    mutableStateOf<String?>(null)
                }
                val selectedDate = remember {
                    mutableStateOf(Calendar.getInstance())
                }
                // parsed json object from the file
                var jsonObject by remember {
                    mutableStateOf<JsonObject?>(null)
                }
                var cityName by remember {
                    mutableStateOf<String?>(null)
                }
                // calculated horas
                var horasList by remember {
                    mutableStateOf<List<Hora>>(emptyList())
                }
                // flat list of all subhoras for the day
                var dailySubHoras by remember {
                    mutableStateOf<List<CalendarSubHora>>(emptyList())
                }
                // hora with calendar start / end & subhoras
                var calendarHorasList by remember {
                    mutableStateOf<List<CalendarHora>>(emptyList())
                }
                // toggle settings panel
                val leftDrawerState = rememberDrawerState(
                    initialValue = DrawerValue.Closed
                )
                val scope = rememberCoroutineScope()
                // need current time
                var currentTime by remember {
                    mutableStateOf(Calendar.getInstance())
                }
                LaunchedEffect(Unit) {
                    while (true) {
                        currentTime = Calendar.getInstance()
                        delay(1000)
                    }
                }
                // parse .json when changed
                LaunchedEffect(sunriseFile.value) {
                    sunriseFile.value?.let { file ->
                        try {
                            val jsonString = file.readText()
                            val parsedJson = Json.parseToJsonElement(jsonString).jsonObject
                            jsonObject = parsedJson
                            cityName = parsedJson["city"]?.jsonPrimitive?.content
                        } catch (e: Exception) {
                            e.printStackTrace()
                            jsonObject = null // handle error : toast notify
                            cityName = null
                        }
                    } ?: run {
                        jsonObject = null // clear jsonobject if file is null
                        cityName = null
                    }
                }
                // recalculate horas on user input / change
                LaunchedEffect(jsonObject, selectedDate.value) {
                    val currentJsonObject = jsonObject
                    val currentDate = selectedDate.value

                    if (currentJsonObject != null) {
                        // format selected date
                        val sdfOutput = SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault()
                        )
                        val formattedDateStr = sdfOutput.format(currentDate.time)
//                        println(
//                            "mainactivity : passing data to gethoras.kt" +
//                                    "\ncurrjsonobj : $currentJsonObject" +
//                                    "\nformatdatestr : $formattedDateStr"
//                        )
                        horasList = getHoras(currentJsonObject, formattedDateStr)
                        val tempHoras = mutableListOf<CalendarHora>()
                        val tempSubHoras = mutableListOf<CalendarSubHora>()

                        horasList.forEach { rawHora ->
                            val horaStartTimeCal = parseTimeStringToCalendar(
                                rawHora.start, currentDate
                            )
                            val horaEndTimeCal = parseTimeStringToCalendar(
                                rawHora.end, currentDate
                            )
                            // rollover to next day
                            if (horaEndTimeCal.before(horaStartTimeCal)) {
                                horaEndTimeCal.add(Calendar.DAY_OF_YEAR, 1)
                            }
                            val currentSubHoras = rawHora.subhoras.map { rawSubHora ->
                                val subStartTimeCal = parseTimeStringToCalendar(
                                    rawSubHora.start, currentDate
                                )
                                val subEndTimeCal = parseTimeStringToCalendar(
                                    rawSubHora.end, currentDate
                                )
                                // adjust for overnight subhoras
                                if (subEndTimeCal.before(subStartTimeCal)) {
                                    subEndTimeCal.add(Calendar.DAY_OF_YEAR, 1)
                                }
                                if (horaEndTimeCal.get(Calendar.DAY_OF_YEAR) >
                                    horaStartTimeCal.get(Calendar.DAY_OF_YEAR) &&
                                    subStartTimeCal.get(Calendar.DAY_OF_YEAR) ==
                                    horaStartTimeCal.get(Calendar.DAY_OF_YEAR) &&
                                    subStartTimeCal.before(horaStartTimeCal)
                                ) {
                                    subStartTimeCal.add(Calendar.DAY_OF_YEAR, 1)
                                    subEndTimeCal.add(Calendar.DAY_OF_YEAR, 1)
                                }
                                CalendarSubHora(
                                    ruler = rawSubHora.ruler,
                                    startTimeCal = subStartTimeCal,
                                    endTimeCal = subEndTimeCal,
                                    originalSubHora = rawSubHora
                                ).also { tempSubHoras.add(it) }
                            }
                            tempHoras.add(
                                CalendarHora(
                                    ruler = rawHora.ruler,
                                    startTimeCal = horaStartTimeCal,
                                    endTimeCal = horaEndTimeCal,
                                    processedSubHoras = currentSubHoras,
                                    originalHora = rawHora
                                )
                            )
                        }
                        dailySubHoras = tempSubHoras.sortedBy { it.startTimeCal }
                        calendarHorasList = tempHoras.sortedBy { it.startTimeCal }

                        Log.d("mainactivity", "processed ${dailySubHoras.size} subhoras")
                        dailySubHoras.take(5).forEach {
                            Log.d(
                                "mainactivity", "subhora : ${it.ruler} start : ${
                                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                        .format(it.startTimeCal.time)
                                } end : ${
                                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                        .format(it.endTimeCal.time)
                                }"
                            )
                        }
                    } else {
                        horasList = emptyList() // clear if no .json
                        dailySubHoras = emptyList()
                        calendarHorasList = emptyList()
                    }
                }
                ModalNavigationDrawer(
                    drawerState = leftDrawerState,
                    gesturesEnabled = leftDrawerState.isOpen,
                    drawerContent = {
                        ModalDrawerSheet {
                            SettingsPanel(
                                sunriseFile = sunriseFile,
                                sunriseFileName = sunriseFileName,
                                selectedDate = selectedDate,
                                horasList = horasList,
                                jsonObject = jsonObject,
                                cityName = cityName,
                                onDismissRequest = {
                                    scope.launch { leftDrawerState.close() }
                                }
                            )
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = { Text("focus") },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        scope.launch {
                                            if (leftDrawerState.isClosed)
                                                leftDrawerState.open()
                                            else leftDrawerState.close()
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Filled.Settings,
                                            contentDescription = "settings"
                                        )
                                    }
                                }
                            )
                        },
                        content = { innerPadding ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .padding(16.dp),// overall padding of main content
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // main content
                                val nowCal = currentTime
                                val currentHora = calendarHorasList.find {
                                    !nowCal.before(it.startTimeCal) &&
                                            nowCal.before(it.endTimeCal)
                                }
                                val currentSubHora = dailySubHoras.find {
                                    !nowCal.before(it.startTimeCal) &&
                                            nowCal.before(it.endTimeCal)
                                }
                                val nextCalendarSubHora = dailySubHoras.find {
                                    it.startTimeCal.after(nowCal)
                                }
                                // in last subhora of the day > next would be null todo
                                val horaLordColor = horaColors[
                                    currentHora?.ruler] ?: defaultHoraColor
                                val subHoraLordColor = horaColors[
                                    currentSubHora?.ruler] ?: MaterialTheme.colorScheme.onBackground
                                val nextSubHoraColor = horaColors[nextCalendarSubHora?.ruler]
                                    ?: MaterialTheme.colorScheme.onBackground

                                AnalogClockAum(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .aspectRatio(1f),
                                    time = nowCal,
                                    hourHandColor = horaLordColor,
                                    minuteHandColor = subHoraLordColor,
                                    secondHandColor = nextSubHoraColor,
                                    nextSubHoraTime = nextCalendarSubHora?.startTimeCal,
                                    secondsBeforeNotification = 60,
                                    // todo match next subhora lord color
                                    notificationMarkerColor = nextSubHoraColor.copy(alpha = 0.9f)
                                )
                                // debug current hora & subhora
//                                Spacer(Modifier.height(10.dp))
//                                Text(
//                                    "hora : ${currentHora?.ruler ?: "na"} (${
//                                        currentHora?.originalHora?.start
//                                    } - ${
//                                        currentHora?.originalHora?.end
//                                    })"
//                                )
//                                Text(
//                                    "sub : ${currentSubHora?.ruler ?: "na"} (${
//                                        currentSubHora?.originalSubHora?.start
//                                    } - ${
//                                        currentSubHora?.originalSubHora?.end
//                                    })"
//                                )
//                                nextCalendarSubHora?.let {
//                                    val sdfDisplay =
//                                        SimpleDateFormat("HH:mm:ss", Locale.getDefault())
//                                    Text(
//                                        "next sub at : ${
//                                            sdfDisplay.format(
//                                                it.startTimeCal.time
//                                            )
//                                        } (${it.ruler})"
//                                    )
//                                }
                            }
                        }
                    )
                }
            }
        }
    }
}