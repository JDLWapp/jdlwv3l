package com.example.jdlw1.presentation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jdlw1.presentation.theme.JDLW1Theme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

@Composable
fun ThemePreference(): Boolean {
    val context = LocalContext.current
    val preferences = context.getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
    return preferences.getBoolean("isDarkTheme", false)
}

@Composable
fun SaveThemePreference(isDarkTheme: Boolean) {
    val context = LocalContext.current
    val preferences = context.getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
    preferences.edit().putBoolean("isDarkTheme", isDarkTheme).apply()
}


/**
 * Data class para la información de perfil del usuario.
 */
data class UserProfileData(
    val name: String = "",
    val email: String = "",
    val gender: String = "",
    val age: String = "",
    val weight: String = "",
    val height: String = "",
    val photoUrl: String = ""
)

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JDLW1Theme {
                ProfileScreen()
            }
        }
    }
}

/**
 * Pantalla de perfil con TopAppBar y BottomNavBar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    Scaffold(
        /*topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "Profile", fontWeight = FontWeight.Bold, fontSize = 8.sp)
                },

                )
        },*/
        bottomBar = { BottomNavBar(currentScreen = Icons.Default.Home) }

    ) { innerPadding ->
        ProfileContent(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFBAD0E7)) // Establece el fondo
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

    }
}

/**
 * Contenido principal del perfil.
 * Se muestra la foto (local o remota), los datos del usuario y se permite editar campos.
 */
@Composable
fun ProfileContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    val uid = currentUser?.uid

    var userData by remember { mutableStateOf(UserProfileData()) }
    LaunchedEffect(uid) {
        if (uid != null) {
            Firebase.firestore.collection("users").document(uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val map = snapshot.data
                        userData = UserProfileData(
                            name = map?.get("name") as? String ?: "",
                            email = map?.get("email") as? String ?: "",
                            gender = map?.get("gender") as? String ?: "",
                            age = map?.get("age") as? String ?: "",
                            weight = map?.get("weight") as? String ?: "",
                            height = map?.get("height") as? String ?: "",
                            photoUrl = map?.get("photoUrl") as? String ?: ""
                        )
                    }
                }
        }
    }

    var gender by remember { mutableStateOf(userData.gender) }
    var age by remember { mutableStateOf(userData.age) }
    var weight by remember { mutableStateOf(userData.weight) }
    var height by remember { mutableStateOf(userData.height) }
    LaunchedEffect(userData) {
        gender = userData.gender
        age = userData.age
        weight = userData.weight
        height = userData.height
    }

    var localPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            localPhotoUri = it
            uploadProfileImageAndSaveUrl(context, uid, it) { newUrl ->
                if (uid != null && newUrl != null) {
                    Firebase.firestore.collection("users").document(uid)
                        .update("photoUrl", newUrl)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Imagen actualizada", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error al actualizar Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }

    LaunchedEffect(userData.photoUrl) {
        if (userData.photoUrl.isNotBlank()) {
            localPhotoUri = null
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ProfilePhotoSection(
                photoUrl = userData.photoUrl,
                localPhotoUri = localPhotoUri,
                onClickChangePhoto = { launcher.launch("image/*") }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = userData.name.ifBlank { "Usuario" }, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.Black, textAlign = TextAlign.Center)
            Text(text = userData.email.ifBlank { "correo@ejemplo.com" }, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            listOf(
                "Género" to gender,
                "Edad" to age,
                "Peso" to weight,
                "Altura" to height
            ).forEachIndexed { index, (label, value) ->
                OutlinedTextField(
                    value = value,
                    onValueChange = { newValue ->
                        when (label) {
                            "Género" -> gender = newValue
                            "Edad" -> age = newValue
                            "Peso" -> weight = newValue
                            "Altura" -> height = newValue
                        }
                    },
                    label = { Text(label) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (index < 3) Spacer(modifier = Modifier.height(12.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            Button(
                onClick = { /* Guardar cambios */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar cambios")
            }
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    context.startActivity(Intent(context, LoginActivity::class.java))
                    (context as? Activity)?.finish()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Log Out",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Log Out", color = Color.White)
            }
        }
    }
}


/**
 * Sección de foto de perfil.
 * Muestra la imagen de la URI local (si hay) o la URL remota.
 */
@Composable
fun ProfilePhotoSection(
    photoUrl: String,
    localPhotoUri: Uri?,
    onClickChangePhoto: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(Color.LightGray)
            .clickable { onClickChangePhoto() },
        contentAlignment = Alignment.Center
    ) {
        when {
            localPhotoUri != null -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(localPhotoUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto de perfil local",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            photoUrl.isNotBlank() -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto de perfil remota",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Text(
                    text = "Cambiar foto",
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Barra de navegación inferior con Home, Calendar, Search, More y Profile.
 */

@Composable
fun BottomNavBarProfile(currentScreen: ImageVector) {

    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    val items = listOf(
        Icons.Default.Home to {
            context.startActivity(Intent(context, MainActivity::class.java))
            expanded = false // Cierra el menú después de la acción
        },
        Icons.Default.DateRange to {
            context.startActivity(Intent(context, CalendarActivity::class.java))
            expanded = false
        },
        Icons.Default.Cloud to {
            context.startActivity(Intent(context, WeatherActivity::class.java))
            expanded = false
        },
        Icons.Default.Person to {
            context.startActivity(Intent(context, ProfileActivity::class.java))
            expanded = false
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color(0xFFBAD0E7)),
        contentAlignment = Alignment.Center
    ) {
        if (expanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items.filter { it.first != currentScreen }.forEach { (icon, action) ->
                    IconButton(onClick = action) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = { expanded = !expanded }, // Alterna entre expandir y contraer
            modifier = Modifier
                .size(50.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { expanded = true },
                        onTap = { expanded = !expanded } // Cambia el estado con un toque
                    )
                }
        ) {
            Icon(
                imageVector = currentScreen,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

/**
 * Función para subir la imagen a Firebase Storage y obtener la URL de descarga.
 */
fun uploadProfileImageAndSaveUrl(context: Context, uid: String?, uri: Uri, onResult: (String?) -> Unit) {
    if (uid == null) {
        onResult(null)
        return
    }
    val storageRef = Firebase.storage.reference.child("profileImages/$uid.jpg")
    storageRef.putFile(uri)
        .addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                onResult(downloadUri.toString())
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Error al obtener URL: ${e.message}", Toast.LENGTH_SHORT).show()
                onResult(null)
            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error al subir imagen: ${e.message}", Toast.LENGTH_SHORT).show()
            onResult(null)
        }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    JDLW1Theme {
        ProfileScreen()
    }
}
