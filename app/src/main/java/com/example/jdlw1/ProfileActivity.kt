package com.example.jdlw1

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jdlw1.ui.theme.JDLW1Theme
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
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },

                )
        },
        bottomBar = { ProfileBottomNavBar() }
    ) { innerPadding ->
        ProfileContent(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFBAD0E7)) // Establece el fondo
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
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

    // Estado para los datos del usuario obtenidos de Firestore
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
                        val name = map?.get("name") as? String ?: ""
                        val email = map?.get("email") as? String ?: ""
                        val gender = map?.get("gender") as? String ?: ""
                        val age = map?.get("age") as? String ?: ""
                        val weight = map?.get("weight") as? String ?: ""
                        val height = map?.get("height") as? String ?: ""
                        val photoUrl = map?.get("photoUrl") as? String ?: ""
                        userData = UserProfileData(name, email, gender, age, weight, height, photoUrl)
                    }
                }
        }
    }

    // Estados para edición
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

    // Estado para foto local seleccionada (para mostrarla inmediatamente)
    var localPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher para seleccionar imagen de la galería
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Muestra la imagen seleccionada inmediatamente
            localPhotoUri = it
            // Sube la imagen a Firebase Storage y actualiza Firestore
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

    // Si Firestore tiene una photoUrl, usamos esa imagen y limpiamos la foto local
    LaunchedEffect(userData.photoUrl) {
        if (userData.photoUrl.isNotBlank()) {
            localPhotoUri = null
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProfilePhotoSection(
            photoUrl = userData.photoUrl,
            localPhotoUri = localPhotoUri,
            onClickChangePhoto = { launcher.launch("image/*") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (userData.name.isBlank()) "Usuario" else userData.name,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (userData.email.isBlank()) "correo@ejemplo.com" else userData.email,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = gender,
            onValueChange = { gender = it },
            label = { Text("Género") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = age,
            onValueChange = { age = it },
            label = { Text("Edad") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text("Peso") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = height,
            onValueChange = { height = it },
            label = { Text("Altura") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (uid != null) {
                    Firebase.firestore.collection("users").document(uid)
                        .update(
                            mapOf(
                                "gender" to gender,
                                "age" to age,
                                "weight" to weight,
                                "height" to height
                            )
                        )
                        .addOnSuccessListener {
                            Toast.makeText(context, "Datos guardados", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Guardar cambios")
        }
        Spacer(modifier = Modifier.weight(1f))
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileBottomNavBar() {
    val context = LocalContext.current
    NavigationBar(
        containerColor = Color(0xFFBAD0E7),
        contentColor = Color.Gray
    ) {
        NavigationBarItem(
            selected = false,
            onClick = {
                val intent = Intent(context, MainActivity::class.java)
                context.startActivity(intent)
            },
            icon = {
                Icon(imageVector = Icons.Default.Home, contentDescription = "Home")
            }
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                val intent = Intent(context, CalendarActivity::class.java)
                context.startActivity(intent)
            },
            icon = {
                Icon(imageVector = Icons.Default.DateRange, contentDescription = "Calendar")
            }
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                val intent = Intent(context, WeatherActivity::class.java)
                context.startActivity(intent)


                /* Acción para Weather */ },
            icon = {
                Icon(imageVector = Icons.Default.Cloud, contentDescription = "Search")
            }
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                val intent = Intent(context, IAActivity::class.java)
                context.startActivity(intent)

                /* Acción para More */ },
            icon = {
                Icon(imageVector = Icons.Default.Psychology, contentDescription = "More")
            }
        )
        NavigationBarItem(
            selected = true,
            onClick = { /* Ya estamos en Profile */ },
            icon = {
                Icon(imageVector = Icons.Default.Person, contentDescription = "Profile")
            }
        )
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
