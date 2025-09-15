package com.example.cyclelink.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.cyclelink.data.models.User
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(navController: NavController) {

    var isLoginScreen by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val firebaseAuth = Firebase.auth

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isLoginScreen) "Dobrodošli Nazad!" else "Kreirajte Nalog",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isLoginScreen) "Prijavite se da nastavite" else "Popunite podatke za registraciju",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!isLoginScreen) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Korisničko ime") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Šifra") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = if (passwordVisible) "Sakrij šifru" else "Prikaži šifru")
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!isLoginScreen) {
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Potvrdi šifru") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        if (isLoginScreen) {
                            // Logika za prijavu
                            if (email.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Email i šifra su obavezni.", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            firebaseAuth.signInWithEmailAndPassword(email.trim(), password.trim())
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val user = task.result?.user
                                        if (user != null && user.isEmailVerified) {
                                            Toast.makeText(context, "Prijava uspešna!", Toast.LENGTH_SHORT).show()
                                            Firebase.firestore.collection("users").document(user.uid)
                                                .update("isVerified", true)
                                            navController.navigate("home_screen") { popUpTo("auth_screen") { inclusive = true } }
                                        } else {
                                            Toast.makeText(context, "Molimo vas, verifikujte vaš email pre prijave.", Toast.LENGTH_LONG).show()
                                            firebaseAuth.signOut()
                                        }
                                    } else {
                                        val errorMessage = task.exception?.message ?: "Greška pri prijavi."
                                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                    }
                                }
                        } else {
                            // Logika za registraciju
                            if (email.isBlank() || password.isBlank() || username.isBlank() || confirmPassword.isBlank()) { Toast.makeText(context, "Sva polja su obavezna.", Toast.LENGTH_SHORT).show(); return@launch }
                            if (password != confirmPassword) { Toast.makeText(context, "Šifre se ne poklapaju.", Toast.LENGTH_SHORT).show(); return@launch }
                            if (password.length < 6) { Toast.makeText(context, "Šifra mora imati najmanje 6 karaktera.", Toast.LENGTH_SHORT).show(); return@launch }

                            // Provera formata email adrese
                            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                                Toast.makeText(context, "Molimo vas, unesite ispravnu email adresu.", Toast.LENGTH_SHORT).show()
                                return@launch
                            }

                            firebaseAuth.createUserWithEmailAndPassword(email.trim(), password.trim())
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val firebaseUser = task.result?.user
                                        if (firebaseUser != null) {
                                            val newUser = User(uid = firebaseUser.uid, username = username.trim(), email = email.trim(), profilePictureUrl = "", firstName = "", lastName = "", bio = "", points = 0,  isVerified = firebaseUser.isEmailVerified)
                                            Firebase.firestore.collection("users").document(firebaseUser.uid)
                                                .set(newUser)
                                                .addOnSuccessListener {
                                                    firebaseUser.sendEmailVerification()
                                                        .addOnCompleteListener { verificationTask ->
                                                            if (verificationTask.isSuccessful) {
                                                                Toast.makeText(context, "Registracija uspešna! Proverite email.", Toast.LENGTH_LONG).show()
                                                            }
                                                        }
                                                }
                                                .addOnFailureListener { Toast.makeText(context, "Greška pri čuvanju podataka.", Toast.LENGTH_SHORT).show() }
                                        }
                                        isLoginScreen = true
                                    } else {
                                        val errorMessage = task.exception?.message ?: "Greška pri registraciji."
                                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                    }
                                }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (isLoginScreen) "Prijavi se" else "Registruj se")
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (isLoginScreen) "Nemaš nalog? Registruj se" else "Već imaš nalog? Prijavi se",
                modifier = Modifier.clickable { isLoginScreen = !isLoginScreen },
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}