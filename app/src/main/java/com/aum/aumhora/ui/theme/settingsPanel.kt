// settingsPanel.kt
package com.aum.aumhora.ui.theme

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aum.aumhora.Hora
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// helper : parse date string into calendar object
fun parseDateString(dateStr: String): Calendar? {
    if (dateStr.isBlank()) {
        println("settingspanel : empty parse date string")
        return null
    }
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        Calendar.getInstance().apply { time = sdf.parse(dateStr)!! }
    } catch (e: java.text.ParseException) {
        println("settingspanel : invalid date format : $dateStr\ncause : ${e.message}")
        null
    } catch (e: Exception) {
        println(
            "settingspanel : unexpected parse date error : $dateStr\ncause : $e"
        )
        null
    }
}

// helper : get date range from .json object / file
fun getJsonDateRange(jsonObject: JsonObject?): Pair<Calendar?, Calendar?> {
    if (jsonObject == null) {
        println("settingspanel : json is null")
        return null to null
    }
    val dataArray = jsonObject["data"]!!.jsonArray
    if (dataArray.isEmpty()) {
        println("settingspanel : json data array is empty")
        return null to null
    }
    val startDateStr = dataArray.first().jsonObject["date"]?.jsonPrimitive?.content
    val endDateStr = dataArray.last().jsonObject["date"]?.jsonPrimitive?.content
    val startCal: Calendar? = startDateStr?.let {
        parseDateString(it)
    } ?: parseDateString("")
    val endCal: Calendar? = endDateStr?.let {
        parseDateString(it)
    } ?: parseDateString("")
    if (startDateStr != null && startCal == null) {
        println("settingspanel : start date exists but failed to parse")
    }
    if (endDateStr != null && endCal == null) {
        println("settingspanel : end date exists but failed to parse")
    }
    return Pair(startCal, endCal)
}

// copy uri content to cache file
fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("sunrise", ".json", context.cacheDir)
        inputStream?.use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// display hora item
@Composable
fun HoraListItem(hora: Hora, mainHoraIndex: Int) {
    var isExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) { // main hora row : clickable
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
//                .padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${
                    String.format(
                        Locale.ROOT,
                        "%02d",
                        mainHoraIndex
                    )
                } : ${hora.start} - ${hora.end}"
            )
            Text(
                text = hora.ruler,
                color = horaColors[hora.ruler] ?: MaterialTheme.colorScheme.onSurface
            )
        }
        // subhoras
//        Row(verticalAlignment = Alignment.CenterVertically) {
//            Text(
//                text = hora.ruler,
//                color = horaColors[hora.ruler] ?: MaterialTheme.colorScheme.onSurface,
//                modifier = Modifier.padding(end = 4.dp)
//            )
//            // optional expansion indicator icon
//            if (hora.subhoras.isNotEmpty()) {
//                val rotationAngle by animateFloatAsState(
//                    targetValue = if (isExpanded) 180f else 0f,
//                    label = "arrowRotation"
//                )
//                Icon(
//                    imageVector = Icons.Filled.ArrowDropDown,
//                    contentDescription = if (isExpanded) "collapse" else "expand",
//                    modifier = Modifier.rotate(rotationAngle)
//                )
//            }
//        }
    }
    // animated subhoras display
//    AnimatedVisibility(visible = isExpanded && hora.subhoras.isNotEmpty()) {
    if (isExpanded && hora.subhoras.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp)
        ) {
            hora.subhoras.forEachIndexed { index, subHora ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${String.format(Locale.ROOT, "%02d", mainHoraIndex)}.${
                            String.format(
                                Locale.ROOT,
                                "%02d",
                                index + 1
                            )
                        } : ${subHora.start} - ${subHora.end}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = subHora.ruler,
                        color = horaColors[subHora.ruler] ?: defaultHoraColor
                    )
                }
            }
        }
    }
}


@Composable
fun SettingsPanel(
    sunriseFile: MutableState<File?>,
    sunriseFileName: MutableState<String?>,
    selectedDate: MutableState<Calendar>,
    horasList: List<Hora>,
    jsonObject: JsonObject?,
    cityName: String?,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val (minDate, maxDate) = remember(jsonObject) {
        getJsonDateRange(jsonObject)
    }
    // check if current selected date is valid by range
    LaunchedEffect(minDate, maxDate, selectedDate.value) {
        if (minDate != null && selectedDate.value.before(minDate)) {
            selectedDate.value = minDate
        } else if (maxDate != null && selectedDate.value.after(maxDate)) {
            selectedDate.value = maxDate
        }
    }
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // selected file name
        Text("sunrise file : ${sunriseFileName.value ?: "no file selected"}")
        // file picker
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri: Uri? ->
                uri?.let {
                    // get original file name
                    val cursor = context.contentResolver.query(it, null, null, null, null)
                    cursor?.use { c ->
                        if (c.moveToFirst()) {
                            val displayNameIndex = c.getColumnIndex(
                                OpenableColumns.DISPLAY_NAME
                            )
                            if (displayNameIndex != -1) {
                                sunriseFileName.value = c.getString(displayNameIndex)
                            }
                        }
                    }
                    val file = uriToFile(context, it)
                    if (file != null) sunriseFile.value = file
                    else {
                        sunriseFileName.value = null
                        Toast.makeText(
                            context, "cannot access file",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
        Button(
            onClick = { launcher.launch(arrayOf("application/json")) },
            modifier = Modifier.align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("select file")
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 10.dp),
            thickness = 2.dp,
            color = Color.LightGray
        )
        // selected date
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val formattedDayOfWeek = dayOfWeekFormat.format(selectedDate.value.time)
        val dayOfWeek = formattedDayOfWeek.lowercase(Locale.getDefault())
        Text(
            "selected date : ${dateFormat.format(selectedDate.value.time)}" +
                    " ${dayOfWeek.format(selectedDate.value.time)}"
        )
        // date picker
        val calendar = selectedDate.value
        val datePickerDialog = remember(calendar, minDate, maxDate) {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val newCalendar = Calendar.getInstance().apply {
                        timeInMillis = calendar.timeInMillis
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    }
                    // safety clamp
                    if (minDate != null && newCalendar.before(minDate)) {
                        selectedDate.value = minDate
                    } else if (maxDate != null && newCalendar.after(maxDate)) {
                        selectedDate.value = maxDate
                    } else selectedDate.value = newCalendar
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.minDate = minDate?.timeInMillis ?: Long.MIN_VALUE
                datePicker.maxDate = maxDate?.timeInMillis ?: Long.MAX_VALUE
            }
        }
        Button(
            onClick = { datePickerDialog.show() },
            modifier = Modifier.align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("select date")
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 10.dp),
            thickness = 2.dp,
            color = Color.LightGray
        )
        // parsed file
//        Text("parsed .json : ${if (jsonObject != null) "loaded" else "not loaded"}")
//        if (horasList.isNotEmpty()) {
//            Text(
//                "first hora : ${horasList.first().start} - ${horasList.first().end}" +
//                        "  ${horasList.first().ruler}"
//            )
//        }
//        HorizontalDivider(
//            modifier = Modifier.padding(vertical = 16.dp),
//            thickness = 2.dp,
//            color = Color.LightGray
//        )
        // resulting hora & subhoras
        Text("horas list ${cityName?.let { "for $it" } ?: ""}")
        if (horasList.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 700.dp)
            ) {
                itemsIndexed(
                    items = horasList,
                    key = { _, hora: Hora -> hora.start + hora.end + hora.ruler }
                ) { index, horaItem: Hora ->
                    HoraListItem(
                        hora = horaItem,
                        mainHoraIndex = index + 1
                    )
                }
            }
        } else {
            Text(
                "horas not calculated or not available",
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterHorizontally)
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 10.dp),
            thickness = 2.dp,
            color = Color.LightGray
        )
        Button(
            onClick = onDismissRequest,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(4.dp),
        ) {
            Text("done")
        }
    }
}