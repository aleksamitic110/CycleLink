package com.example.cyclelink.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.cyclelink.R
import com.example.cyclelink.data.models.User
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObjects
import com.google.firebase.Firebase

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(navController: NavController) {
    val firestore = Firebase.firestore
    var userList by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        firestore.collection("users")
            //.whereEqualTo("isVerified", true) // Prikazujemo samo verifikovane
            .orderBy("points", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { result ->
                userList = result.toObjects<User>()
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    // Delim korisnike na prva tri i ostatak
    val topThree = userList.take(3)
    val restOfUsers = if (userList.size > 3) userList.subList(3, userList.size) else emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rang Lista") },
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
        } else if (userList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Rang lista je trenutno prazna.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Prikaz podijuma za top 3
                if (topThree.isNotEmpty()) {
                    item {
                        Text(
                            text = "Top 3 Korisnika",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TopThreePodium(users = topThree)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Ostatak liste
                if (restOfUsers.isNotEmpty()) {
                    stickyHeader {
                        ListHeader()
                    }
                    itemsIndexed(restOfUsers, key = { _, user -> user.uid }) { index, user ->
                        LeaderboardListItem(user = user, rank = index + 4) // Počinjemo od 4. mesta
                    }
                }
            }
        }
    }
}

@Composable
fun TopThreePodium(users: List<User>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // 2. Mesto
        if (users.size >= 2) {
            PodiumItem(user = users[1], rank = 2, color = Color(0xFFC0C0C0), modifier = Modifier.weight(1f))
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // 1. Mesto
        if (users.isNotEmpty()) {
            PodiumItem(user = users[0], rank = 1, icon = Icons.Default.WorkspacePremium, color = Color(0xFFFFD700), modifier = Modifier.weight(1.2f).padding(bottom = 24.dp))
        }

        // 3. Mesto
        if (users.size >= 3) {
            PodiumItem(user = users[2], rank = 3, color = Color(0xFFCD7F32), modifier = Modifier.weight(1f))
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun PodiumItem(user: User, rank: Int, modifier: Modifier = Modifier, icon: ImageVector? = null, color: Color) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = "Prvo mesto", tint = color, modifier = Modifier.size(32.dp))
        }
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current)
                    .data(user.profilePictureUrl.takeIf { it.isNotBlank() } ?: R.drawable.ic_launcher_foreground)
                    .build()
            ),
            contentDescription = user.username,
            modifier = Modifier
                .size(if (rank == 1) 80.dp else 60.dp)
                .clip(CircleShape)
                .border(if (rank == 1) 3.dp else 2.dp, color, CircleShape),
            contentScale = ContentScale.Crop
        )
        Text(text = user.username, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Text(text = "${user.points} poena", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ListHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Rank", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(50.dp))
        Text(text = "Korisnik", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = "Poeni", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun LeaderboardListItem(user: User, rank: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(50.dp)
        )
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current)
                    .data(user.profilePictureUrl.takeIf { it.isNotBlank() } ?: R.drawable.ic_launcher_foreground)
                    .build()
            ),
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = user.username, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = "${user.points}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}