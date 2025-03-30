package com.example.jdlw1

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jdlw1.ui.theme.JDLW1Theme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.sin
import kotlin.random.Random

// ---------- Funciones de simulación ECG ---------------------
fun simulateECGData(numPoints: Int): List<Float> =
    List(numPoints) { i ->
        (sin(i * 0.2) * 20 + Random.nextDouble(-2.0, 2.0)).toFloat()
    }

fun simulateFancyECGData(numPoints: Int, waveOffset: Float): List<Float> =
    List(numPoints) { i ->
        val base = sin(i * 0.2) * 20
        val noise = Random.nextDouble(-2.0, 2.0)
        val offset = sin(i * 0.3f + waveOffset) * 5f
        (base + noise + offset).toFloat()
    }

// --------------------- MainActivity ---------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JDLW1Theme {
                // Se obtienen los datos del usuario y niveles de estrés (o se usan valores por defecto para preview)
                val userName = remember { mutableStateOf("Usuario") }
                val stressLevels = remember { mutableStateListOf<Int>().apply {
                    addAll(listOf(50, 50, 50, 50, 50, 50, 50))
                } }

                // Recupera el nombre y niveles de estrés desde Firestore
                LaunchedEffect(Unit) {
                    FirebaseAuth.getInstance().currentUser?.let { user ->
                        val uid = user.uid
                        Firebase.firestore.collection("users").document(uid).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    document.getString("name")?.let { name ->
                                        userName.value = name
                                    }
                                    val storedLevels = document.get("stressLevels") as? List<Number>
                                    storedLevels?.let { levels ->
                                        stressLevels.clear()
                                        stressLevels.addAll(levels.map { it.toInt() })
                                    }
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this@MainActivity, "Error al cargar datos previos", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                MainScreen(userName = userName.value, stressLevels = stressLevels)
            }
        }
    }
}

// --------------------- MainScreen ---------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(userName: String = "Usuario", stressLevels: SnapshotStateList<Int>) {
    val days = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    val defaultECGData = simulateECGData(150)

    var ecgData by remember { mutableStateOf(defaultECGData) }
    var isMeasuring by remember { mutableStateOf(false) }
    var shouldStartMeasurement by remember { mutableStateOf(false) }
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    // Animación ECG
    val infiniteTransition = rememberInfiniteTransition()
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart)
    )

    // Eventos (primer evento)
    var firstEventText by remember { mutableStateOf("Sin eventos") }
    val currentUser = FirebaseAuth.getInstance().currentUser
    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            Firebase.firestore.collection("events")
                .whereEqualTo("userId", currentUser.uid)
                .addSnapshotListener { querySnapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (querySnapshot != null) {
                        val eventsList = querySnapshot.documents.mapNotNull { it.toObject(MyEvent::class.java) }
                        if (eventsList.isNotEmpty()) {
                            val sortedEvents = eventsList.sortedBy { it.date }
                            val event = sortedEvents.first()
                            firstEventText = "${event.title} - ${event.date}"
                        } else {
                            firstEventText = "Sin eventos"
                        }
                    }
                }
        }
    }

    // Lógica de ECG
    if (shouldStartMeasurement) {
        LaunchedEffect(shouldStartMeasurement) {
            isMeasuring = true
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 15_000L) {
                ecgData = simulateFancyECGData(150, waveOffset)
                delay(300L)
            }
            val newStress = Random.nextInt(0, 101)
            selectedDayIndex?.let { index ->
                stressLevels[index] = newStress
                FirebaseAuth.getInstance().currentUser?.let { user ->
                    val uid = user.uid
                    Firebase.firestore.collection("users").document(uid)
                        .update("stressLevels", stressLevels.toList())
                        .addOnFailureListener {
                            Toast.makeText(context, "Error al guardar la medición", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            isMeasuring = false
            shouldStartMeasurement = false
        }
    }

    Scaffold(
        bottomBar = { BottomNavBar() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .background(Color(0xFFBAD0E7))
        ) {
            HeaderSection(userName)
            RecommendationsSection() // Aquí se usa la nueva RecommendationCard
            StressSection(
                days = days,
                stressLevels = stressLevels,
                selectedDayIndex = selectedDayIndex,
                onDaySelected = { index -> selectedDayIndex = index }
            )
            ECGSection(
                ecgData = ecgData,
                isMeasuring = isMeasuring,
                waveOffset = if (!LocalInspectionMode.current) waveOffset else 0f,
                onStartMeasurement = {
                    if (selectedDayIndex == null) {
                        Toast.makeText(context, "Selecciona un día para la medición", Toast.LENGTH_SHORT).show()
                    } else {
                        shouldStartMeasurement = true
                    }
                }
            )
            ClimateSection()
            EventsSection(firstEventText = firstEventText)
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// --------------------- Data class para eventos ---------------------



// --------------------- HeaderSection (con botón LogOut) ---------------------
@Composable
fun HeaderSection(userName: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color(0xFFBAD0E7)),

        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Hola, $userName",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "¡Bienvenido de nuevo!",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Gray)
                .clickable {
                    FirebaseAuth.getInstance().signOut()
                    context.startActivity(Intent(context, LoginActivity::class.java))
                    (context as? Activity)?.finish()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = "LogOut",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// --------------------- RecommendationsSection ---------------------
@Composable
fun RecommendationsSection() {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFB3C1DC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Recomendaciones",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            val recommendations = listOf("Haz ejercicio", "Medita", "Lee", "Camina", "Desconéctate")
            LazyRow(
                contentPadding = PaddingValues(end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recommendations) { item ->
                    RecommendationCard(item)
                }
            }
        }
    }
}

/**
 * RecommendationCard modificada:
 * - Si el título es "Haz ejercicio" (sin distinción de mayúsculas/minúsculas),
 *   la card es clickeable y abre un navegador con la URL asignada.
 * - Se muestra una imagen de referencia dentro de la card.
 */
@Composable
fun RecommendationCard(title: String) {
    val context = LocalContext.current
    // Definimos un mapa de títulos a URLs y otro mapa de títulos a URL de imagen
    val linkMap = mapOf(
        "haz ejercicio" to "https://www.mayoclinic.org/es/healthy-lifestyle/stress-management/in-depth/exercise-and-stress/art-20044469",
        "medita" to "https://www.mayoclinic.org/es/tests-procedures/meditation/in-depth/meditation/art-20045858",
        "lee" to "https://theconversation.com/leer-para-que-215277",
        "camina" to "https://www.mayoclinic.org/es/healthy-lifestyle/fitness/in-depth/walking/art-20046261",
        "desconéctate" to "https://www.generali.es/blog/generalimasqueseguros/desconectar-tecnologia/"
    )
    val imageMap = mapOf(
        "haz ejercicio" to "https://uvn-brightspot.s3.amazonaws.com/assets/vixes/imj/vivirsalud/H/Hacer-ejercicio-mejora-la-memoria-2.jpg",
        "medita" to "https://static.sadhguru.org/d/46272/1633197086-1633197085450.jpg",
        "lee" to "https://images.unsplash.com/photo-1512820790803-83ca734da794",
        "camina" to "https://fundaciondelcorazon.com/images/stories/Andar.jpg",
        "desconéctate" to "https://static.wixstatic.com/media/24e8f3_8a310eeb66924818a1737cdf2bed5d95~mv2.png/v1/fill/w_568,h_320,al_c,q_85,usm_0.66_1.00_0.01,enc_avif,quality_auto/24e8f3_8a310eeb66924818a1737cdf2bed5d95~mv2.png"
    )
    // Buscamos la URL correspondiente (usamos lowercase para ignorar mayúsculas)
    val link = linkMap[title.lowercase()]
    val imageUrl = imageMap[title.lowercase()] ?: "https://via.placeholder.com/50"

    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .size(width = 120.dp, height = 100.dp)
            .clickable(enabled = link != null) {
                link?.let {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(it))
                    context.startActivity(intent)
                }
            },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFB3C1DC).copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Imagen de referencia
            AsyncImage(
                model = imageUrl,
                contentDescription = "Imagen de referencia",
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color.Black
            )
        }
    }
}


// --------------------- StressSection ---------------------
@Composable
fun StressSection(
    days: List<String>,
    stressLevels: List<Int>,
    selectedDayIndex: Int?,
    onDaySelected: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFB3C1DC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Nivel de Estrés",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val averageStress = stressLevels.average().toInt()
                Text(
                    text = "$averageStress",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                Spacer(modifier = Modifier.width(16.dp))
                val trendText = if (selectedDayIndex != null) {
                    val diff = stressLevels[selectedDayIndex] - 50
                    val sign = if (diff >= 0) "+" else ""
                    "Este ${days[selectedDayIndex]}: $sign$diff%"
                } else "Sin medición"
                val diffColor = if (selectedDayIndex != null && stressLevels[selectedDayIndex] >= 50) Color.Red else Color.Green
                Text(
                    text = trendText,
                    color = diffColor,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEachIndexed { index, day ->
                    StressBar(
                        value = stressLevels[index],
                        dayLabel = day,
                        isSelected = selectedDayIndex == index,
                        onClick = { onDaySelected(index) }
                    )
                }
            }
        }
    }
}

@Composable
fun StressBar(value: Int, dayLabel: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = if (value == 50) {
        Color.Gray
    } else {
        when {
            value < 25 -> Color.Blue
            value < 50 -> Color.Yellow
            value < 75 -> Color(0xFFFFA500)
            else -> Color.Red
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .width(12.dp)
                .height((value * 2).dp)
                .background(color, shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) Color.Black else Color.Transparent,
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = dayLabel, fontSize = 10.sp)
        Text(text = "$value%", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

// --------------------- ECGSection ---------------------
@Composable
fun ECGSection(
    ecgData: List<Float>,
    isMeasuring: Boolean,
    waveOffset: Float,
    onStartMeasurement: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFB3C1DC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Electrocardiograma",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (ecgData.isNotEmpty()) {
                ECGGraph(ecgData = ecgData, waveOffset = waveOffset)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Sin datos", color = Color.DarkGray)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onStartMeasurement,
                enabled = !isMeasuring,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isMeasuring) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp)
                    )
                    Text("Midiendo...")
                } else {
                    Text("Iniciar")
                }
            }
        }
    }
}

@Composable
fun ECGGraph(ecgData: List<Float>, waveOffset: Float) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(16.dp)
    ) {
        if (ecgData.isNotEmpty()) {
            val totalPoints = ecgData.size
            val widthPerPoint = size.width / (totalPoints - 1)
            val centerY = size.height / 2
            val path = Path().apply {
                moveTo(0f, centerY - ecgData[0])
                ecgData.forEachIndexed { index, value ->
                    lineTo(index * widthPerPoint, centerY - value)
                }
            }
            drawPath(
                path = path,
                color = Color.Red,
                style = Stroke(width = 2f)
            )
        }
    }
}

// --------------------- ClimateSection: muestra clima real en la Card ---------------------
@Composable
fun ClimateSection() {
    var cityName by remember { mutableStateOf("Cargando...") }
    var temperature by remember { mutableStateOf("--") }
    var description by remember { mutableStateOf("") }

    val apiKey = "af4984ca846ca35ff7102160c3d75758"
    val cityToFetch = "Ciudad Juárez"

    val retrofit = remember {
        Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val service = remember { retrofit.create(WeatherApiService::class.java) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    service.getWeatherByCity(cityToFetch, apiKey)
                }
                cityName = response.name
                temperature = response.main.temp.toInt().toString()
                description = response.weather.firstOrNull()?.description ?: ""
            } catch (e: Exception) {
                Toast.makeText(context, "Error clima: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFB3C1DC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Clima",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$temperature° C",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = description.replaceFirstChar { it.uppercaseChar() },
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = cityName,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

// --------------------- EventsSection ---------------------
@Composable
fun EventsSection(firstEventText: String = "Sin eventos") {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFB3C1DC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Eventos",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = firstEventText,
                fontSize = 16.sp,
                color = Color(0xFF333333)
            )
        }
    }
}

// --------------------- BottomNavBar ---------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavBar() {
    val context = LocalContext.current
    NavigationBar(
        containerColor = Color(0xFFBAD0E7),
        contentColor = Color.Gray
    ) {
        NavigationBarItem(
            selected = true,
            onClick = { /* Permanece en Home (MainActivity) */ },
            icon = {
                Icon(
                    painter = rememberVectorPainter(Icons.Default.Home),
                    contentDescription = "Home"
                )
            }
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                val intent = Intent(context, CalendarActivity::class.java)
                context.startActivity(intent)
            },
            icon = {
                Icon(
                    painter = rememberVectorPainter(Icons.Default.DateRange),
                    contentDescription = "Calendar"
                )
            }
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                val intent = Intent(context, WeatherActivity::class.java)
                context.startActivity(intent)
            },
            icon = {
                Icon(
                    painter = rememberVectorPainter(Icons.Default.Cloud),
                    contentDescription = "Weather"
                )
            }
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                val intent = Intent(context, IAActivity::class.java)
                context.startActivity(intent)
            },
            icon = {
                Icon(
                    painter = rememberVectorPainter(Icons.Default.Psychology),
                    contentDescription = "IA"
                )
            }
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                val intent = Intent(context, ProfileActivity::class.java)
                context.startActivity(intent)
            },
            icon = {
                Icon(
                    painter = rememberVectorPainter(Icons.Default.Person),
                    contentDescription = "Profile"
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    JDLW1Theme {
        MainScreen(userName = "Usuario", stressLevels = remember { mutableStateListOf(50,50,50,50,50,50,50) })
    }
}
