package com.example.cyclelink.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.cyclelink.R
import com.example.cyclelink.ThemeSetting
import com.example.cyclelink.components.CustomRiderMarker
import com.example.cyclelink.data.models.Ride
import com.example.cyclelink.data.models.User
import com.example.cyclelink.data.models.RideWithId
import com.example.cyclelink.services.LocationService
import com.example.cyclelink.utils.isLocationEnabled
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.Firebase
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.util.*
import java.text.SimpleDateFormat
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun HomeScreen(
    navController: NavController,
    currentTheme: ThemeSetting,
    toggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firestore = Firebase.firestore
    val currentUserAuth = Firebase.auth.currentUser


    var currentUserData by remember { mutableStateOf<User?>(null) }
    var isRiding by remember { mutableStateOf(false) }
    val isSystemDark = isSystemInDarkTheme()
    val allRides = remember { mutableStateListOf<RideWithId>() }
    val filteredRides = remember { mutableStateListOf<RideWithId>() }
    var selectedRide by remember { mutableStateOf<RideWithId?>(null) }
    var hasLocationPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    var locationRequested by remember { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LatLng(44.787197, 20.457273), 10f) }
    val participantsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showParticipantsSheet by remember { mutableStateOf(false) }
    val participantsList = remember { mutableStateListOf<User>() }
    val activeRiders = remember { mutableStateListOf<User>() }
    var isGpsEnabled by remember { mutableStateOf(isLocationEnabled(context)) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var isUpdating by remember { mutableStateOf(false) }


    // State za filtere
    var showFilterSheet by remember { mutableStateOf(false) }
    var difficultyFilter by remember { mutableStateOf<String?>(null) }
    var radiusFilter by remember { mutableStateOf<Int?>(null) }
    var startDateFilter by remember { mutableStateOf<Date?>(null) }
    var endDateFilter by remember { mutableStateOf<Date?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val isMapDark = when (currentTheme) {
        ThemeSetting.System -> isSystemDark
        ThemeSetting.Light -> false
        ThemeSetting.Dark -> true
    }
    val mapStyleOptions = if (isMapDark) MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark) else null

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        }
    )

    val lifecycleOwner = LocalLifecycleOwner.current

    // Efekti
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGpsEnabled = isLocationEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.POST_NOTIFICATIONS))
    }

    LaunchedEffect(currentUserAuth) {
        if (currentUserAuth != null) {
            firestore.collection("users").document(currentUserAuth.uid).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject<User>()
                    currentUserData = user
                    val remoteIsRiding = user?.isRiding ?: false

                    if (isUpdating) {
                        if (remoteIsRiding == isRiding) {
                            isUpdating = false
                        }
                    } else {
                        isRiding = remoteIsRiding
                    }
                }
            }
        }
        firestore.collection("rides").whereEqualTo("status", "planned").addSnapshotListener { snapshots, _ ->
            if (snapshots != null) {
                allRides.clear()
                allRides.addAll(snapshots.map { doc -> RideWithId(id = doc.id, ride = doc.toObject()) })
                applyFilters(allRides, difficultyFilter, radiusFilter, userLocation, startDateFilter, endDateFilter, searchQuery, filteredRides)
            }
        }
        firestore.collection("users").whereEqualTo("isRiding", true).addSnapshotListener { snapshots, _ ->
            if (snapshots != null) {
                activeRiders.clear()
                val riders = snapshots.map { doc -> doc.toObject<User>() }
                activeRiders.addAll(riders)
                com.example.cyclelink.utils.ActiveRidersRepo.activeRiders = riders
            }
        }
    }

    LaunchedEffect(searchQuery, difficultyFilter, radiusFilter, startDateFilter, endDateFilter, userLocation) {
        applyFilters(allRides, difficultyFilter, radiusFilter, userLocation, startDateFilter, endDateFilter, searchQuery, filteredRides)
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission && !locationRequested) {
            locationRequested = true
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        task.result?.let { location ->
                            val loc = LatLng(location.latitude, location.longitude)
                            userLocation = loc
                            scope.launch { cameraPositionState.animate(update = CameraUpdateFactory.newLatLngZoom(loc, 15f), durationMs = 1500) }
                        }
                    }
                }
        }
    }
    // UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Pretraži po autoru...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pretraga") }
                )},
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }

                    IconButton(onClick = { navController.navigate("leaderboard_screen") }) {
                        Icon(Icons.Default.Leaderboard, contentDescription = "Rang Lista")
                    }

                    IconButton(onClick = { navController.navigate("profile_screen") }) {
                        if (currentUserData != null && currentUserData!!.profilePictureUrl.isNotBlank()) {
                            Image(
                                painter = rememberAsyncImagePainter(ImageRequest.Builder(LocalContext.current).data(currentUserData!!.profilePictureUrl).crossfade(true).placeholder(R.drawable.ic_launcher_foreground).error(R.drawable.ic_launcher_foreground).build()),
                                contentDescription = "Profil",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Person, contentDescription = "Profil")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedRide == null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        FloatingActionButton(
                            onClick = { toggleTheme() },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(imageVector = if (isMapDark) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Promeni temu")
                        }
                        FloatingActionButton(
                            onClick = { navController.navigate("create_ride_screen") },
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Add, "Kreiraj novu vožnju")
                        }
                    }
                    ExtendedFloatingActionButton(
                        onClick = {

                            val userId = currentUserAuth?.uid ?: return@ExtendedFloatingActionButton
                            isUpdating = true
                            Intent(context, LocationService::class.java).also {
                                it.action = if (isRiding) LocationService.ACTION_STOP else LocationService.ACTION_START
                                context.startService(it)
                            }
                            isRiding = !isRiding

                            // Upis u Firestore
                            firestore.collection("users").document(userId)
                                .update("isRiding", isRiding)
                                .addOnFailureListener {
                                    isUpdating = false
                                    isRiding = !isRiding
                                }
                        },
                        containerColor = if (isRiding) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        text = { Text(if (isRiding) "Zaustavi Deljenje" else "Deli Lokaciju") },
                        icon = { Icon(if (isRiding) Icons.Default.Stop else Icons.Default.DirectionsBike, contentDescription = null) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            if (hasLocationPermission) {
                if (isGpsEnabled) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(isMyLocationEnabled = true, mapStyleOptions = mapStyleOptions),
                        uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        if (selectedRide == null) {
                            filteredRides.forEach { rideWithId ->
                                rideWithId.ride.waypoints.firstOrNull()?.let { startPoint ->
                                    val startLatLng = LatLng(startPoint.latitude, startPoint.longitude)
                                    Marker(state = MarkerState(position = startLatLng), title = rideWithId.ride.title, snippet = "Klikni da vidiš detalje", onClick = { selectedRide = rideWithId; true })
                                }
                            }
                        }
                        selectedRide?.let { rideWithId ->
                            val ride = rideWithId.ride
                            if (ride.overviewPolyline.isNotBlank()) {
                                val routeCoordinates = PolyUtil.decode(ride.overviewPolyline)
                                Polyline(points = routeCoordinates, color = MaterialTheme.colorScheme.primary, width = 15f)
                            } else {
                                val routeCoordinates = ride.waypoints.map { LatLng(it.latitude, it.longitude) }
                                if (routeCoordinates.size > 1) Polyline(points = routeCoordinates, color = MaterialTheme.colorScheme.primary, width = 15f)
                            }
                            ride.waypoints.forEachIndexed { index, geoPoint ->
                                val waypointLatLng = LatLng(geoPoint.latitude, geoPoint.longitude)
                                val title = ride.waypointTitles["point_$index"] ?: "Tačka ${index + 1}"
                                val icon = when (index) {
                                    0 -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                                    ride.waypoints.lastIndex -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                                    else -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                                }
                                Marker(state = MarkerState(position = waypointLatLng), title = title, icon = icon)
                            }
                        }
                        activeRiders.forEach { rider ->
                            if (rider.uid != currentUserAuth?.uid && rider.currentLocation != null) {
                                CustomRiderMarker(
                                    context = context,
                                    position = LatLng(rider.currentLocation.latitude, rider.currentLocation.longitude),
                                    title = rider.username,
                                    imageUrl = rider.profilePictureUrl
                                )
                            }
                        }
                    }
                } else {
                    LocationDisabledScreen { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                }
            } else {
                PermissionNotGrantedScreen { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
            }
            if (selectedRide != null) {
                RideInfoCard(
                    rideWithId = selectedRide!!,
                    currentUserUid = currentUserAuth?.uid ?: "",
                    onClose = { selectedRide = null },
                    onJoin = { rideIdToJoin ->
                        val userId = currentUserAuth?.uid ?: return@RideInfoCard
                        firestore.collection("rides").document(rideIdToJoin).update("participants", FieldValue.arrayUnion(userId))
                            .addOnSuccessListener {
                                Toast.makeText(context, "Uspešno ste se pridružili vožnji!", Toast.LENGTH_SHORT).show()
                                val userRef = firestore.collection("users").document(userId)
                                userRef.update("points", FieldValue.increment(5))
                                val updatedRide = selectedRide?.ride?.copy(participants = selectedRide!!.ride.participants + userId)
                                selectedRide = selectedRide?.copy(ride = updatedRide!!)
                            }
                            .addOnFailureListener { Toast.makeText(context, "Došlo je do greške.", Toast.LENGTH_SHORT).show() }
                    },
                    onLeave = { rideIdToLeave ->
                        val userId = currentUserAuth?.uid ?: return@RideInfoCard
                        firestore.collection("rides").document(rideIdToLeave).update("participants", FieldValue.arrayRemove(userId))
                            .addOnSuccessListener {
                                Toast.makeText(context, "Napustili ste vožnju.", Toast.LENGTH_SHORT).show()
                                val updatedParticipants = selectedRide!!.ride.participants.toMutableList().apply { remove(userId) }
                                val updatedRide = selectedRide?.ride?.copy(participants = updatedParticipants)
                                selectedRide = selectedRide?.copy(ride = updatedRide!!)
                            }
                            .addOnFailureListener { Toast.makeText(context, "Došlo je do greške.", Toast.LENGTH_SHORT).show() }
                    },
                    onParticipantsClick = { ride ->
                        participantsList.clear()
                        showParticipantsSheet = true
                        if (ride.participants.isNotEmpty()) {
                            firestore.collection("users").whereIn("uid", ride.participants).get()
                                .addOnSuccessListener { result -> participantsList.addAll(result.map { doc -> doc.toObject() }) }
                        }
                    },
                    onDelete = { rideIdToDelete ->
                        firestore.collection("rides").document(rideIdToDelete).delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Vožnja uspešno obrisana.", Toast.LENGTH_SHORT).show()
                                selectedRide = null
                            }
                            .addOnFailureListener { Toast.makeText(context, "Greška pri brisanju vožnje.", Toast.LENGTH_SHORT).show() }
                    }
                )
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            onDismiss = { showFilterSheet = false },
            onApplyFilters = { difficulty, radius, startDate, endDate ->
                difficultyFilter = difficulty
                radiusFilter = radius
                startDateFilter = startDate
                endDateFilter = endDate
                applyFilters(allRides, difficultyFilter, radiusFilter, userLocation, startDateFilter, endDateFilter, searchQuery, filteredRides)
            }
        )
    }

    if (showParticipantsSheet) {
        ModalBottomSheet(onDismissRequest = { showParticipantsSheet = false }, sheetState = participantsSheetState) {
            LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                item { Text("Učesnici vožnje", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp)) }
                if (participantsList.isEmpty() && selectedRide?.ride?.participants?.isNotEmpty() == true) {
                    item { Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                } else if (participantsList.isEmpty()) {
                    item { Text("Nema prijavljenih učesnika.") }
                } else {
                    items(participantsList) { user ->
                        ParticipantRow(user = user) {
                            showParticipantsSheet = false
                            navController.navigate("user_profile_screen/${user.uid}")
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

// -------

private fun applyFilters(
    allRides: List<RideWithId>,
    difficulty: String?,
    radiusKm: Int?,
    userLocation: LatLng?,
    startDate: Date?,
    endDate: Date?,
    searchQuery: String,
    filteredRides: SnapshotStateList<RideWithId>
) {

    var tempRides: List<RideWithId> = allRides

    if (searchQuery.isNotBlank()) {
        tempRides = tempRides.filter {
            it.ride.authorUsername.contains(searchQuery, ignoreCase = true)
        }
    }

    if (difficulty != null) {
        tempRides = tempRides.filter { it.ride.difficulty == difficulty }
    }
    if (radiusKm != null && userLocation != null) {
        val radiusMeters = radiusKm * 1000f
        tempRides = tempRides.filter { rideWithId ->
            rideWithId.ride.waypoints.firstOrNull()?.let { startPoint ->
                val rideLocation = LatLng(startPoint.latitude, startPoint.longitude)
                val distanceResults = FloatArray(1)
                Location.distanceBetween(userLocation.latitude, userLocation.longitude, rideLocation.latitude, rideLocation.longitude, distanceResults)
                distanceResults[0] <= radiusMeters
            } ?: false
        }
    }
    if (startDate != null) {
        val start = startDate.onlyDate()
        tempRides = tempRides.filter { ride ->
            ride.ride.startTime.toDate().onlyDate() >= start
        }
    }

    if (endDate != null) {
        val end = endDate.onlyDate()
        tempRides = tempRides.filter { ride ->
            ride.ride.startTime.toDate().onlyDate() <= end
        }
    }

    filteredRides.clear()
    filteredRides.addAll(tempRides)
}

@Composable
fun LocationDisabledScreen(onEnableClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("GPS je isključen.")
        Spacer(modifier = Modifier.height(8.dp))
        Text("Molimo vas, uključite lokaciju u podešavanjima da biste koristili mapu.")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onEnableClick) { Text("Otvori podešavanja") }
    }
}

@Composable
fun PermissionNotGrantedScreen(onPermissionClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Potrebna nam je vaša dozvola za lokaciju.")
        Button(onClick = onPermissionClick) { Text("Zatraži dozvolu") }
    }
}

@Composable
fun RideInfoCard(
    rideWithId: RideWithId,
    currentUserUid: String,
    onClose: () -> Unit,
    onJoin: (String) -> Unit,
    onLeave: (String) -> Unit,
    onParticipantsClick: (Ride) -> Unit,
    onDelete: (String) -> Unit
) {
    val ride = rideWithId.ride
    val formattedDate = remember(ride.startTime) { SimpleDateFormat("dd.MM.yyyy 'u' HH:mm", Locale.getDefault()).format(ride.startTime.toDate()) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Card(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(ride.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Zatvori") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Image(painter = rememberAsyncImagePainter(ImageRequest.Builder(LocalContext.current).data(ride.authorImageUrl).placeholder(R.drawable.ic_launcher_foreground).error(R.drawable.ic_launcher_foreground).build()), contentDescription = "Slika autora", modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape), contentScale = ContentScale.Crop)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Organizator: ${ride.authorUsername}", style = MaterialTheme.typography.bodyLarge)
                }
                Text("Polazak: $formattedDate", style = MaterialTheme.typography.bodyMedium)
                Text("Težina: ${ride.difficulty}", style = MaterialTheme.typography.bodyMedium)
                Text("Dužina: %.2f km".format(ride.distance), style = MaterialTheme.typography.bodyMedium)
                Row(modifier = Modifier
                    .clickable { onParticipantsClick(ride) }
                    .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.People, contentDescription = "Učesnici", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Učesnici: ${ride.participants.size}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(16.dp))
                when {
                    ride.authorId == currentUserUid -> {
                        Button(onClick = { onDelete(rideWithId.id) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Obriši vožnju") }
                    }
                    ride.participants.contains(currentUserUid) -> {
                        Button(onClick = { onLeave(rideWithId.id) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Napusti vožnju") }
                    }
                    else -> {
                        Button(onClick = { onJoin(rideWithId.id) }, modifier = Modifier.fillMaxWidth()) { Text("Pridruži se") }
                    }
                }
            }
        }
    }
}

@Composable
fun ParticipantRow(user: User, onClick: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }
        .padding(vertical = 12.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = rememberAsyncImagePainter(ImageRequest.Builder(LocalContext.current).data(user.profilePictureUrl.takeIf { it.isNotBlank() } ?: R.drawable.ic_launcher_foreground).crossfade(true).build()),
            contentDescription = "Slika učesnika",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(user.username, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    onDismiss: () -> Unit,
    onApplyFilters: (difficulty: String?, radius: Int?, startDate: Date?, endDate: Date?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val difficulties = listOf("Sve", "Laka", "Srednja", "Teška")
    var selectedDifficulty by remember { mutableStateOf(difficulties[0]) }
    val radii = listOf("Sve", "5 km", "10 km", "20 km")
    var selectedRadius by remember { mutableStateOf(radii[0]) }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }

    ModalBottomSheet(onDismissRequest = { onDismiss() }, sheetState = sheetState) {
        Column(modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()) {
            Text("Filteri", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
            Text("Težina vožnje:", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                difficulties.forEach { difficulty ->
                    FilterChip(selected = (difficulty == selectedDifficulty), onClick = { selectedDifficulty = difficulty }, label = { Text(difficulty) })
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Udaljenost od vas:", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                radii.forEach { radiusText ->
                    FilterChip(selected = (radiusText == selectedRadius), onClick = { selectedRadius = radiusText }, label = { Text(radiusText) })
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            Text("Datum polaska:", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                DatePickerButton(date = startDate, label = "Od", onDateSelected = { startDate = it }, modifier = Modifier.weight(1f))
                DatePickerButton(date = endDate, label = "Do", onDateSelected = { endDate = it }, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val difficultyFilter = if (selectedDifficulty == "Sve") null else selectedDifficulty
                    val radiusFilter = when (selectedRadius) {
                        "5 km" -> 5; "10 km" -> 10; "20 km" -> 20; else -> null
                    }

                    onApplyFilters(difficultyFilter, radiusFilter, startDate, endDate)

                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Primeni Filtere")
            }
        }
    }
}

@Composable
fun DatePickerButton(date: Date?, label: String, onDateSelected: (Date) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    date?.let { calendar.time = it }
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth); onDateSelected(calendar.time)

        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    OutlinedButton(onClick = { datePickerDialog.show() }, modifier = modifier) {
        Text(date?.let { SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(it) } ?: label)
    }
}

fun Date.onlyDate(): Date {
    return Calendar.getInstance().apply {
        time = this@onlyDate
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time
}