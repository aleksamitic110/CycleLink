package com.example.cyclelink.screens

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.location.Location
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.cyclelink.R
import com.example.cyclelink.ThemeSetting
import com.example.cyclelink.data.models.Ride
import com.example.cyclelink.data.models.User
import com.example.cyclelink.data.models.Waypoint
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.android.compose.*
import com.google.maps.model.TravelMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun CreateRideScreen(navController: NavController, themeSetting: ThemeSetting) {


    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firestore = Firebase.firestore
    val currentUser = Firebase.auth.currentUser
    val isSystemDark = isSystemInDarkTheme()

    var isCalculatingRoute by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("Srednja") }
    val waypoints = remember { mutableStateListOf<Waypoint>() }
    var isLoading by remember { mutableStateOf(false) }
    var totalDistance by remember { mutableStateOf(0.0) }
    var calculatedPolyline by remember { mutableStateOf("") }
    val calendar = Calendar.getInstance()
    var rideDate by remember { mutableStateOf<Date>(calendar.time) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedWaypointIndex by remember { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LatLng(44.787197, 20.457273), 12f) }
    val apiKey = com.example.cyclelink.BuildConfig.MAPS_API_KEY
    val geoApiContext = remember { GeoApiContext.Builder().apiKey(apiKey).build() }
    val isMapDark = when (themeSetting) {
        ThemeSetting.System -> isSystemDark
        ThemeSetting.Light -> false
        ThemeSetting.Dark -> true
    }
    val mapStyleOptions = if (isMapDark) MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark) else null
    var currentUserData by remember { mutableStateOf<User?>(null) }

    // Efekti
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        currentUserData = document.toObject<User>()
                    }
                }
        }
    }
    LaunchedEffect(Unit) {
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng, 15f)
                    }
                }
        } catch (_: SecurityException) {}
    }

    // Funkcije
    fun calculateDirections() {
        if (waypoints.size < 2) return
        isCalculatingRoute = true
        totalDistance = 0.0
        scope.launch(Dispatchers.IO) {
            try {
                val origin = waypoints.first().location.run { "$latitude,$longitude" }
                val destination = waypoints.last().location.run { "$latitude,$longitude" }
                val intermediateWaypoints = if (waypoints.size > 2) {
                    waypoints.subList(1, waypoints.size - 1).joinToString("|") { "${it.location.latitude},${it.location.longitude}" }
                } else ""
                val request = DirectionsApi.newRequest(geoApiContext).origin(origin).destination(destination).mode(TravelMode.DRIVING)
                if (intermediateWaypoints.isNotEmpty()) request.waypoints(intermediateWaypoints)
                val directionsResult = request.await()
                withContext(Dispatchers.Main) {
                    val route = directionsResult.routes.firstOrNull()
                    if (route != null) {
                        val distanceInMeters = route.legs.sumOf { it.distance.inMeters }
                        totalDistance = distanceInMeters / 1000.0
                        calculatedPolyline = route.overviewPolyline.encodedPath
                    } else {
                        Toast.makeText(context, "Nije pronađena ruta.", Toast.LENGTH_SHORT).show()
                    }
                    isCalculatingRoute = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Greška pri računanju rute: ${e.message}", Toast.LENGTH_LONG).show()
                    isCalculatingRoute = false
                }
            }
        }
    }

    fun handleSaveRide() {
        if (currentUserData == null) { Toast.makeText(context, "Greška: Podaci o korisniku nisu učitani.", Toast.LENGTH_SHORT).show(); return }
        if (title.isBlank()) { Toast.makeText(context, "Naziv vožnje je obavezan.", Toast.LENGTH_SHORT).show(); return }
        if (waypoints.size < 2) { Toast.makeText(context, "Morate dodati bar 2 tačke za rutu.", Toast.LENGTH_SHORT).show(); return }
        isLoading = true
        val waypointTitlesMap = waypoints.mapIndexed { index, waypoint -> "point_$index" to waypoint.title }.toMap()
        val newRide = Ride(
            title = title, description = description, authorId = currentUser!!.uid, authorUsername = currentUserData!!.username,
            authorImageUrl = currentUserData!!.profilePictureUrl, startTime = Timestamp(rideDate),
            waypoints = waypoints.map { GeoPoint(it.location.latitude, it.location.longitude) }, participants = listOf(currentUser.uid),
            difficulty = difficulty, distance = totalDistance, waypointTitles = waypointTitlesMap, overviewPolyline = calculatedPolyline
        )
        firestore.collection("rides").add(newRide)
            .addOnSuccessListener {
                Toast.makeText(context, "Vožnja uspešno kreirana!", Toast.LENGTH_SHORT).show()
                val userRef = firestore.collection("users").document(currentUser!!.uid)
                userRef.update("points", FieldValue.increment(15))
                isLoading = false
                scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) { showBottomSheet = false } }
                navController.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Greška pri čuvanju: ${e.message}", Toast.LENGTH_LONG).show()
                isLoading = false
            }
    }

    // UI
    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true, mapStyleOptions = mapStyleOptions),
            uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false),
            onMapClick = { latLng -> waypoints.add(Waypoint(location = latLng, title = "Tačka ${waypoints.size + 1}")) }
        ) {
            waypoints.forEachIndexed { index, waypoint ->
                val icon = when (index) {
                    0 -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    waypoints.lastIndex -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    else -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                }
                Marker(
                    state = MarkerState(position = waypoint.location), title = waypoint.title, icon = icon,
                    onClick = { selectedWaypointIndex = index; showEditDialog = true; true }
                )
            }
        }
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
        }
        if (waypoints.size >= 2) {
            FloatingActionButton(
                onClick = { calculateDirections(); showBottomSheet = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Nastavi sa detaljima")
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(onDismissRequest = { showBottomSheet = false }, sheetState = sheetState) {
                val focusManager = LocalFocusManager.current
                val descriptionFocusRequester = remember { FocusRequester() }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Text("Detalji Vožnje", style = MaterialTheme.typography.headlineSmall) }
                    itemsIndexed(waypoints) { index, waypoint ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${index + 1}. ${waypoint.title}")
                            IconButton(onClick = { waypoints.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Obriši tačku")
                            }
                        }
                        HorizontalDivider()
                    }
                    item {
                        if (isCalculatingRoute) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Računam najbolju rutu...")
                            }
                        } else {
                            Text(text = "Ukupna dužina: %.2f km".format(totalDistance), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = title, onValueChange = { title = it }, label = { Text("Naziv vožnje") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { descriptionFocusRequester.requestFocus() })
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = description, onValueChange = { description = it }, label = { Text("Opis (opciono)") },
                            modifier = Modifier.fillMaxWidth().focusRequester(descriptionFocusRequester),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DifficultySelector(selectedDifficulty = difficulty, onDifficultySelected = { difficulty = it }, modifier = Modifier.weight(1f))
                            DateTimePicker(selectedDate = rideDate, onDateSelected = { rideDate = it }, modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Button(onClick = { handleSaveRide() }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading) {
                            if (isLoading) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary) }
                            else { Text("Sačuvaj Vožnju") }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }

        if (showEditDialog && selectedWaypointIndex != null) {
            val waypointToEdit = waypoints[selectedWaypointIndex!!]
            var tempTitle by remember { mutableStateOf(waypointToEdit.title) }
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Uredi Tačku") },
                text = { OutlinedTextField(value = tempTitle, onValueChange = { tempTitle = it }, label = { Text("Naziv tačke") }) },
                confirmButton = {
                    Button(onClick = {
                        waypoints[selectedWaypointIndex!!] = waypointToEdit.copy(title = tempTitle)
                        showEditDialog = false
                    }) { Text("Sačuvaj") }
                },
                dismissButton = { OutlinedButton(onClick = { showEditDialog = false }) { Text("Otkaži") } }
            )
        }
    }
}




// -----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DifficultySelector(selectedDifficulty: String, onDifficultySelected: (String) -> Unit, modifier: Modifier = Modifier) {
    val difficulties = listOf("Laka", "Srednja", "Teška")
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = selectedDifficulty, onValueChange = {}, readOnly = true,
            label = { Text("Težina") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            difficulties.forEach { difficulty ->
                DropdownMenuItem(text = { Text(difficulty) }, onClick = {
                    onDifficultySelected(difficulty)
                    expanded = false
                })
            }
        }
    }
}

@Composable
fun DateTimePicker(selectedDate: Date, onDateSelected: (Date) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { time = selectedDate }
    val datePickerDialog = DatePickerDialog(
        context, { _, year, month, dayOfMonth ->
            TimePickerDialog(
                context, { _, hourOfDay, minute ->
                    calendar.set(year, month, dayOfMonth, hourOfDay, minute)
                    onDateSelected(calendar.time)
                },
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
            ).show()
        },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )
    Button(onClick = { datePickerDialog.show() }, modifier = modifier) {
        Text(SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()).format(selectedDate))
    }
}