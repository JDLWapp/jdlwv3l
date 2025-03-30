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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jdlw1.ui.theme.JDLW1Theme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import androidx.compose.foundation.shape.RoundedCornerShape

// ---------- Data classes y Retrofit para la API de OpenAI ----------
data class ChatCompletionRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<Message>,
    val max_tokens: Int = 200,
    val temperature: Double = 0.7
)

data class Message(
    val role: String,  // "user", "system" o "assistant"
    val content: String
)

data class ChatCompletionResponse(
    val choices: List<Choice>?
)

data class Choice(
    val message: Message?
)

interface OpenAIApiService {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}

// Data class para recomendaciones (aunque en este caso usaremos un único bloque de texto)
data class Recommendation(
    val text: String
)

/**
 * Pantalla principal IAActivity.
 */
class IAActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JDLW1Theme {
                IAScreen()
            }
        }
    }
}

/**
 * Scaffold con TopAppBar y BottomNavBar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IAScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFBAD0E7)) // Aplica fondo a toda la pantalla
    )
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mi Asistente IA", fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = { IABottomNavBar() }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            IAContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        }
    }
}

/**
 * Contenido principal:
 * - Se lee el nivel de estrés desde Firestore.
 * - Se muestra la respuesta de la IA como un único bloque de texto.
 * - La caja de texto se fija al fondo, simulando un chat.
 */
@Composable
fun IAContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estado para el nivel de estrés (leído desde Firestore)
    var stressLevel by remember { mutableStateOf(0) }

    // Leer el nivel de estrés desde Firestore
    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            val uid = user.uid
            Firebase.firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val storedLevels = document.get("stressLevels") as? List<Number>
                        if (!storedLevels.isNullOrEmpty()) {
                            stressLevel = storedLevels.map { it.toInt() }.average().toInt()
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error al leer estrés", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Estado para la respuesta de la IA (un solo bloque de texto)
    var recommendationText by remember { mutableStateOf("") }
    // Estado para el input del usuario
    var userInput by remember { mutableStateOf("") }

    // API key de OpenAI (reemplaza con la tuya real)
    val openAiApiKey = "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"

    // OkHttpClient con interceptor para añadir la API key
    val client = remember {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $openAiApiKey")
                    .build()
                chain.proceed(newRequest)
            }
            .build()
    }

    // Configura Retrofit
    val retrofit = remember {
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val openAIApiService = remember { retrofit.create(OpenAIApiService::class.java) }

    Column(
        modifier = modifier.background(Color(0xFFBAD0E7))
    ) {
        // Área superior: muestra el nivel de estrés y la respuesta de la IA (scrollable)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Tu nivel de estrés actual es: $stressLevel",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111111),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "Recomendación:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            // Muestra la respuesta completa de la IA en un solo bloque de texto
            if (recommendationText.isNotBlank()) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F2F4)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = recommendationText,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp),
                        color = Color.Black
                    )
                }
            } else {
                Text(
                    text = "No hay recomendaciones aún.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
        // Área inferior: caja de texto y botón de enviar (chatbox)
        Column {
            OutlinedTextField(
                value = userInput,
                onValueChange = { userInput = it },
                label = { Text("Pregunta algo a la IA...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (userInput.isNotBlank()) {
                        scope.launch {
                            try {
                                val response = callOpenAIForRecommendations(
                                    openAIApiService,
                                    userInput,
                                    stressLevel
                                )
                                // Concatenamos las recomendaciones en un solo bloque de texto
                                recommendationText = response.joinToString("\n")
                                userInput = ""
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enviar")
            }
        }
    }
}

/**
 * Llamada a la API de OpenAI con backoff.
 */
suspend fun callOpenAIForRecommendations(
    openAIApiService: OpenAIApiService,
    userQuery: String,
    stressLevel: Int
): List<String> {
    val systemPrompt = "Eres un asistente de bienestar enfocado en reducir el estrés. El nivel de estrés actual del usuario es $stressLevel."
    val messages = listOf(
        Message(role = "system", content = systemPrompt),
        Message(role = "user", content = userQuery)
    )
    val request = ChatCompletionRequest(
        model = "gpt-3.5-turbo",
        messages = messages,
        max_tokens = 200,
        temperature = 0.7
    )

    var attempt = 0
    val maxAttempts = 3
    var response: ChatCompletionResponse? = null
    while (attempt < maxAttempts) {
        try {
            response = openAIApiService.createChatCompletion(request = request)
            break
        } catch (e: Exception) {
            if (e is HttpException && e.code() == 429) {
                attempt++
                val delayTime = 2000L * attempt
                withContext(Dispatchers.IO) { delay(delayTime) }
            } else {
                throw e
            }
        }
    }
    if (response == null) {
        return listOf("No se pudo obtener respuesta de la API tras varios intentos")
    }
    val fullText = response.choices?.firstOrNull()?.message?.content ?: "Sin respuesta"
    // Devuelve el texto completo en una lista de un solo elemento
    return listOf(fullText)
}

/**
 * Barra de navegación inferior con los íconos: Home, Calendar, Weather, IA, Profile.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IABottomNavBar() {
    val context = LocalContext.current
    NavigationBar(
        containerColor = Color(0xFFBAD0E7),
        contentColor = Color.Gray
    ) {
        // Home
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
        // Calendar
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
        // Weather
        NavigationBarItem(
            selected = false,
            onClick = {
                val intent = Intent(context, WeatherActivity::class.java)
                context.startActivity(intent)
                (context as? Activity)?.finish()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = "Weather"
                )
            }
        )
        // IA (activo)
        NavigationBarItem(
            selected = true,
            onClick = { /* Ya estamos en IAActivity */ },
            icon = {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "IA"
                )
            }
        )
        // Profile
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
fun IAScreenPreview() {
    JDLW1Theme {
        IAScreen()
    }
}
