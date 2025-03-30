package com.example.jdlw1

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jdlw1.ui.theme.JDLW1Theme
import com.google.firebase.annotations.concurrent.Background
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// ---------- Data classes y Retrofit API ----------
data class WeatherResponse(
    val name: String, // Ciudad
    val main: MainData,
    val weather: List<WeatherData>
)

data class MainData(
    val temp: Float
)

data class WeatherData(
    val description: String,
    val icon: String
)

interface WeatherApiService {
    @GET("weather")
    suspend fun getWeatherByCity(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "es"
    ): WeatherResponse
}

// ---------- WeatherActivity ----------
class WeatherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JDLW1Theme {
                WeatherScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen() {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFBAD0E7))
                            .padding(8.dp) // Padding para el texto
                    ) {
                        Text(
                            text = "Weather",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                // Si quieres un icono de retroceso:
                // navigationIcon = {
                //     IconButton(onClick = { /* onBack */ }) {
                //         Icon(
                //             imageVector = Icons.Default.ArrowBack,
                //             contentDescription = "Back"
                //         )
                //     }
                // }
            )
        },
        bottomBar = { WeatherBottomNavBar() }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Fondo degradado suave, congruente con tus otros screens
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFBAD0E7),
                            Color(0xFFBAD0E7)
                        )
                    )
                )
        ) {
            WeatherContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        }
    }
}

/**
 * Muestra la info de clima en una Card con bordes redondeados,
 * similar a tus otras pantallas con Cards.
 */
@Composable
fun WeatherContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estados para la UI
    var cityName by remember { mutableStateOf("Cargando...") }
    var temperature by remember { mutableStateOf("--") }
    var description by remember { mutableStateOf("") }
    var iconCode by remember { mutableStateOf("") }

    // Reemplaza con tu API key real
    val apiKey = "af4984ca846ca35ff7102160c3d75758"
    val cityToFetch = "Ciudad Juárez"

    // Configura Retrofit
    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/data/2.5/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val service = retrofit.create(WeatherApiService::class.java)

    // Llamada a la API
    LaunchedEffect(cityToFetch) {
        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    service.getWeatherByCity(cityToFetch, apiKey)
                }
                cityName = response.name
                temperature = response.main.temp.toInt().toString()
                description = response.weather.firstOrNull()?.description ?: ""
                iconCode = response.weather.firstOrNull()?.icon ?: ""
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Card para la info del clima
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFB3C1DC)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Nombre de la ciudad
                Text(
                    text = cityName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                // Temperatura y icono en una fila
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$temperature° C",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 48.sp,
                        color = Color.Blue
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    if (iconCode.isNotEmpty()) {
                        val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(iconUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Weather Icon",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                // Descripción del clima
                Text(
                    text = description.replaceFirstChar { it.uppercaseChar() },
                    fontWeight = FontWeight.Normal,
                    fontSize = 20.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/**
 * Barra inferior con íconos: Home, Calendar, Weather, More, Profile
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherBottomNavBar() {
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
                (context as? Activity)?.finish()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home"
                )
            }
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                val intent = Intent(context, CalendarActivity::class.java)
                context.startActivity(intent)
                (context as? Activity)?.finish()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Calendar"
                )
            }
        )
        NavigationBarItem(
            selected = true, // Weather activo
            onClick = { /* Ya estamos en WeatherActivity */ },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Cloud,
                    contentDescription = "Weather"
                )
            }
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                val intent = Intent(context, IAActivity::class.java)
                context.startActivity(intent)

                /* Acción para More */ },
            icon = {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "IA"
                )
            }
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                val intent = Intent(context, ProfileActivity::class.java)
                context.startActivity(intent)
                (context as? Activity)?.finish()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile"
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WeatherScreenPreview() {
    JDLW1Theme {
        WeatherScreen()
    }
}
