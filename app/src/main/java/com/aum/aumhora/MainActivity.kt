// cosmicjoke
// mainactivity.kt
package com.aum.aumhora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

import com.aum.aumhora.ui.theme.AumhoraTheme
import com.aum.aumhora.ui.theme.SettingsPanel
import com.aum.aumhora.ui.theme.horaColors
import com.aum.aumhora.ui.theme.defaultHoraColor
import kotlinx.serialization.json.jsonPrimitive


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class) // topappbar & modaldrawersheet
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AumhoraTheme {
                // states for user selection
                val sunriseFile = remember { mutableStateOf<File?>(null) }
                val sunriseFileName = remember { mutableStateOf<String?>(null) }
                val selectedDate = remember { mutableStateOf(Calendar.getInstance()) }
                // parsed json object from the file
                var jsonObject by remember { mutableStateOf<JsonObject?>(null) }
                var cityName by remember { mutableStateOf<String?>(null) }
                // calculated horas
                var horasList by remember { mutableStateOf<List<Hora>>(emptyList()) }
                // toggle settings panel
                val leftDrawerState = rememberDrawerState(
                    initialValue = DrawerValue.Closed
                )
                val scope = rememberCoroutineScope()
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
                        println(
                            "mainactivity : passing data to gethoras.kt" +
                                    "\ncurrjsonobj : $currentJsonObject" +
                                    "\nformatdatestr : $formattedDateStr"
                        )

                        horasList = getHoras(currentJsonObject, formattedDateStr)
                    } else {
                        horasList = emptyList() // clear if no .json
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
                                HoraCircle(
//                                    modifier = Modifier.weight(1f),
                                    horaColor = horaColors[horasList.firstOrNull()?.ruler]
                                        ?: defaultHoraColor
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HoraCircle(
//    modifier: Modifier = Modifier,
    horaColor: Color = Color.LightGray
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 4f
            drawCircle(
                color = horaColor,
                radius = radius,
                center = center
            )
        }
    }
}