package com.example.jdlw1.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// ---------- Data classes y Retrofit API ----------
data class WeatherResponse(val name: String, val main: MainData, val weather: List<WeatherData>)
data class MainData(val temp: Float)
data class WeatherData(val description: String, val icon: String)

interface WeatherApiService {
    @GET("weather")
    suspend fun getWeatherByCity(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "es"
    ): WeatherResponse
}

class WeatherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherScreen()
        }
    }
}

@Composable
fun WeatherScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var cityName by remember { mutableStateOf("Cargando...") }
    var temperature by remember { mutableStateOf("--") }
    var description by remember { mutableStateOf("") }
    var iconCode by remember { mutableStateOf("") }

    val apiKey = "af4984ca846ca35ff7102160c3d75758"
    val cityToFetch = "Ciudad Juárez"

    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/data/2.5/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val service = retrofit.create(WeatherApiService::class.java)

    LaunchedEffect(cityToFetch) {
        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) { service.getWeatherByCity(cityToFetch, apiKey) }
                cityName = response.name
                temperature = response.main.temp.toInt().toString()
                description = response.weather.firstOrNull()?.description ?: ""
                iconCode = response.weather.firstOrNull()?.icon ?: ""
            } catch (e: Exception) {
                cityName = "Error"
                description = "No disponible"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFBAD0E7), Color(0xFFBAD0E7))))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = cityName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "$temperature° C", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.Blue)
            Spacer(modifier = Modifier.height(4.dp))
            if (iconCode.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://openweathermap.org/img/wn/$iconCode@2x.png")
                        .crossfade(true)
                        .build(),
                    contentDescription = "Weather Icon",
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description.replaceFirstChar { it.uppercaseChar() }, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
