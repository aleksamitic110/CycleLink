package com.example.cyclelink.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.cyclelink.R
import com.example.cyclelink.data.models.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.Firebase
import com.google.firebase.storage.storage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val firestore = Firebase.firestore
    val currentUser = Firebase.auth.currentUser
    val context = LocalContext.current


    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }

    var username by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    val cropImageLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { uri ->
                uploadProfileImage(uri, { isSaving = it }) { newImageUrl ->
                    user = user?.copy(profilePictureUrl = newImageUrl)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val fetchedUser = document.toObject<User>()
                        user = fetchedUser
                        fetchedUser?.let {
                            username = it.username
                            firstName = it.firstName
                            lastName = it.lastName
                            bio = it.bio
                        }
                    }
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil i Podešavanja") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(user?.profilePictureUrl.takeIf { !it.isNullOrBlank() } ?: R.drawable.ic_launcher_foreground)
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = "Profilna slika",
                    modifier = Modifier.size(120.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable {
                            val cropOptions = CropImageContractOptions(
                                uri = null,
                                cropImageOptions = CropImageOptions(
                                    fixAspectRatio = true, aspectRatioX = 1, aspectRatioY = 1,
                                    cropShape = CropImageView.CropShape.OVAL, outputCompressQuality = 75,
                                    activityBackgroundColor = backgroundColor.toArgb(), activityTitle = "Iseci Sliku",
                                    toolbarColor = primaryColor.toArgb(), toolbarBackButtonColor = onPrimaryColor.toArgb(),
                                    toolbarTintColor = onPrimaryColor.toArgb(), activityMenuIconColor = onPrimaryColor.toArgb()
                                )
                            )
                            cropImageLauncher.launch(cropOptions)
                        },
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(user?.email ?: "Nema emaila", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Korisničko ime") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("Ime") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Prezime") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("O meni (bio)") }, modifier = Modifier.fillMaxWidth().height(120.dp))

                Spacer(modifier = Modifier.weight(1f, fill = false))

                Button(
                    onClick = {
                        isSaving = true
                        val updatedData = mapOf("username" to username.trim(), "firstName" to firstName.trim(), "lastName" to lastName.trim(), "bio" to bio.trim())
                        firestore.collection("users").document(currentUser!!.uid)
                            .update(updatedData)
                            .addOnSuccessListener { Toast.makeText(context, "Profil uspešno sačuvan!", Toast.LENGTH_SHORT).show(); isSaving = false }
                            .addOnFailureListener { Toast.makeText(context, "Greška pri čuvanju.", Toast.LENGTH_SHORT).show(); isSaving = false }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    if (isSaving) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary) }
                    else { Text("Sačuvaj promene") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text("Obriši Nalog")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { Firebase.auth.signOut(); navController.navigate("auth_screen") { popUpTo(0) } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Odjavi se")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Potvrda brisanja") },
            text = { Text("Da li ste sigurni da želite da obrišete nalog? Ova akcija je nepovratna.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        showReauthDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Obriši") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Otkaži") } }
        )
    }

    if (showReauthDialog) {
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReauthDialog = false },
            title = { Text("Potrebna je ponovna prijava") },
            text = {
                Column {
                    Text("Unesite vašu šifru da potvrdite brisanje naloga.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Šifra") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val userToDelete = Firebase.auth.currentUser
                        if (userToDelete != null && userToDelete.email != null) {
                            isSaving = true
                            showReauthDialog = false
                            val credential = EmailAuthProvider.getCredential(userToDelete.email!!, password)
                            userToDelete.reauthenticate(credential)
                                .addOnSuccessListener {
                                    firestore.collection("users").document(userToDelete.uid).delete()
                                        .addOnCompleteListener {
                                            userToDelete.delete().addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    Toast.makeText(context, "Nalog je obrisan.", Toast.LENGTH_SHORT).show()
                                                    navController.navigate("auth_screen") { popUpTo(0) }
                                                }
                                            }
                                        }
                                }
                                .addOnFailureListener { e ->
                                    isSaving = false
                                    Toast.makeText(context, "Greška: Pogrešna šifra.", Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Potvrdi i Obriši") }
            },
            dismissButton = { TextButton(onClick = { showReauthDialog = false }) { Text("Otkaži") } }
        )
    }
}

private fun uploadProfileImage(
    uri: Uri,
    setLoading: (Boolean) -> Unit,
    onSuccess: (String) -> Unit
) {
    val currentUser = Firebase.auth.currentUser ?: return
    val storageRef = Firebase.storage.reference.child("profile_images/${currentUser.uid}")
    setLoading(true)
    storageRef.putFile(uri)
        .addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                Firebase.firestore.collection("users").document(currentUser.uid)
                    .update("profilePictureUrl", downloadUrl.toString())
                    .addOnSuccessListener { onSuccess(downloadUrl.toString()); setLoading(false) }
                    .addOnFailureListener { setLoading(false) }
            }.addOnFailureListener { setLoading(false) }
        }
        .addOnFailureListener { setLoading(false) }
}