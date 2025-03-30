@file:SuppressLint("NewApi") // Para suprimir warnings de "API level 26" en LocalDate, etc.
package com.example.jdlw1

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.example.jdlw1.ui.theme.JDLW1Theme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.HashMap

/**
 * Representa cada evento/recordatorio en Firestore.
 * Se guarda la fecha en formato "yyyy-MM-dd" y un título definido por el usuario.
 */
data class MyEvent(
    val id: String = "",
    val userId: String = "",
    val date: String = "",  // "yyyy-MM-dd"
    val title: String = ""  // Texto del recordatorio
)

/**
 * Activity que muestra la agenda (eventos) y permite agregar, editar o eliminar.
 */
class CalendarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JDLW1Theme {
                CalendarScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen() {
    // 1) Obtenemos el contexto desde un composable
    val context = LocalContext.current

    // 2) Usuario actual (ya autenticado)
    val currentUser = FirebaseAuth.getInstance().currentUser

    // 3) Lista de eventos de Firestore
    val events = remember { mutableStateListOf<MyEvent>() }

    // 4) Cargar eventos al iniciar (o cuando cambie el usuario)
    LaunchedEffect(currentUser?.uid) {
        if (currentUser == null) return@LaunchedEffect
        val uid = currentUser.uid

        Firebase.firestore.collection("events")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Toast.makeText(
                        context,
                        "Error al cargar eventos: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }
                if (querySnapshot != null) {
                    val tempList = mutableListOf<MyEvent>()
                    for (doc in querySnapshot.documents) {
                        val event = doc.toObject(MyEvent::class.java)
                        if (event != null) {
                            tempList.add(event.copy(id = doc.id))
                        }
                    }
                    events.clear()
                    events.addAll(tempList)
                }
            }
    }

    // 5) Estados para mostrar/ocultar el diálogo y evento seleccionado
    var showEventDialog by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<MyEvent?>(null) }

    // 6) Funciones internas que usan "context" (ya que LocalContext es composable)
    fun createOrUpdateEvent(id: String?, name: String, date: LocalDate) {
        if (currentUser == null) {
            Toast.makeText(
                context,
                "Usuario no autenticado",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val uid = currentUser.uid
        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val docRef = if (id == null) {
            Firebase.firestore.collection("events").document()
        } else {
            Firebase.firestore.collection("events").document(id)
        }
        val newEvent = MyEvent(
            id = docRef.id,
            userId = uid,
            date = dateStr,
            title = name
        )
        docRef.set(newEvent)
            .addOnFailureListener {
                Toast.makeText(
                    context,
                    "Error al guardar el evento: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    fun deleteEvent(event: MyEvent) {
        if (event.id.isNotBlank()) {
            Firebase.firestore.collection("events").document(event.id).delete()
                .addOnFailureListener {
                    Toast.makeText(
                        context,
                        "Error al eliminar: ${it.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    // 7) Scaffold con FAB y barra inferior
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Nuevo evento => selectedEvent = null
                    selectedEvent = null
                    showEventDialog = true
                },
                containerColor = Color(0xFF1980E6),
                contentColor = Color(0xFFBAD0E7)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar evento")
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = { CalendarBottomNavBar() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFBAD0E7))
                .padding(innerPadding)
        ) {
            // Barra superior (título "Mi agenda")
            TopBar()

            // Mostrar la fecha actual real (ejemplo: "Miércoles, 29 de marzo")
            val locale = Locale("es", "ES")
            val formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", locale)
            val currentDate = LocalDate.now()
            val dateText = currentDate.format(formatter).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(locale) else it.toString()
            }

            Text(
                text = dateText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111418),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                textAlign = TextAlign.Start
            )

            // Agrupar eventos por fecha (yyyy-MM-dd) y ordenarlos
            val groupedEvents = events.groupBy { it.date }.toSortedMap()

            // Mostrar lista agrupada
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                groupedEvents.forEach { (dateString, eventList) ->
                    // Parsear la fecha "yyyy-MM-dd" a LocalDate
                    val parsedDate = runCatching {
                        LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    }.getOrNull()

                    // Formatear la fecha en español
                    val dateLabel = if (parsedDate != null) {
                        parsedDate.format(formatter).replaceFirstChar { first ->
                            if (first.isLowerCase()) first.titlecase(locale) else first.toString()
                        }
                    } else dateString

                    // Cabecera con la fecha
                    item {
                        Text(
                            text = dateLabel,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF111418),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // Eventos de ese día
                    items(eventList) { event ->
                        AgendaItem(
                            event = event,
                            onEdit = {
                                // Editar => asignar selectedEvent y mostrar diálogo
                                selectedEvent = event
                                showEventDialog = true
                            },
                            onDelete = {
                                deleteEvent(event)
                            }
                        )
                    }
                }
            }
        }
    }

    // 8) Diálogo para agregar/editar un evento
    if (showEventDialog) {
        EventDialog(
            eventToEdit = selectedEvent,
            onDismiss = { showEventDialog = false },
            onSave = { id, name, localDate ->
                createOrUpdateEvent(id, name, localDate)
                showEventDialog = false
            }
        )
    }
}

/**
 * Barra superior con título "Mi agenda" y botón de calendario.
 */
@Composable
fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFBAD0E7))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Mi agenda",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111418),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        IconButton(
            onClick = { /* Acción adicional */ },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Calendario",
                tint = Color(0xFF111418)
            )
        }
    }
}

/**
 * Un ítem de la lista de eventos, con botón Editar y Eliminar.
 */
@Composable
fun AgendaItem(
    event: MyEvent,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFBAD0E7))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFF0F2F4), shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Evento",
                tint = Color(0xFF111418),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = event.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF111418)
            )
        }
        // Botón Editar
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Editar",
                tint = Color(0xFF1980E6)
            )
        }
        // Botón Eliminar
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Eliminar",
                tint = Color.Red
            )
        }
    }
}

/**
 * Diálogo para crear o editar un evento.
 */
@Composable
fun EventDialog(
    eventToEdit: MyEvent?,
    onDismiss: () -> Unit,
    onSave: (id: String?, name: String, date: LocalDate) -> Unit
) {
    val context = LocalContext.current

    // Nombre del evento (si es edición, cargamos el existente)
    var eventName by remember { mutableStateOf(eventToEdit?.title ?: "") }

    // Fecha inicial (si es edición, parseamos la guardada)
    val initialDate = if (eventToEdit != null && eventToEdit.date.isNotEmpty()) {
        runCatching {
            LocalDate.parse(eventToEdit.date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        }.getOrNull() ?: LocalDate.now()
    } else LocalDate.now()

    var selectedDate by remember { mutableStateOf(initialDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSave(eventToEdit?.id, eventName, selectedDate)
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        title = {
            Text(if (eventToEdit == null) "Nuevo evento" else "Editar evento")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = eventName,
                    onValueChange = { eventName = it },
                    label = { Text("Nombre del evento") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                val formatter = DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", Locale("es", "ES"))
                Text("Fecha: ${selectedDate.format(formatter)}", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { showDatePicker = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1980E6)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Seleccionar fecha",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Seleccionar fecha", color = Color.White)
                }
            }
        }
    )

    // DatePickerDialog nativo (Android)
    if (showDatePicker) {
        val year = selectedDate.year
        val month = selectedDate.monthValue - 1 // 0-based
        val day = selectedDate.dayOfMonth

        DatePickerDialog(
            context,
            { _, selYear, selMonth, selDay ->
                selectedDate = LocalDate.of(selYear, selMonth + 1, selDay)
                showDatePicker = false
            },
            year,
            month,
            day
        ).show()
    }
}

/**
 * Barra de navegación inferior, renombrada para no chocar con la de MainActivity.
 */
@Composable
fun CalendarBottomNavBar() {
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
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home"
                )
            }
        )
        NavigationBarItem(
            selected = true,
            onClick = { /* Acción para Calendar */ },
            icon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Calendario"
                )
            }
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                val intent = Intent(context, WeatherActivity::class.java)
                context.startActivity(intent)


                /* Acción para Clima */ },
            icon = {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = "Clima"
                )
            }
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                val intent = Intent(context, IAActivity::class.java)
                context.startActivity(intent)

                /* Acción para IA */ },
            icon = {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "IA"
                )
            }
        )
        NavigationBarItem(
            selected = false,
            onClick = { /* Acción para Profile */

                val intent = Intent(context, ProfileActivity::class.java)
                context.startActivity(intent)

            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Perfil"
                )
            }
        )
    }
}

/**
 * Vista previa en modo diseño, sin conectarse a Firestore.
 */
@Preview(showBackground = true)
@Composable
fun CalendarScreenPreview() {
    JDLW1Theme {
        val fakeEvents = listOf(
            MyEvent(id = "1", date = "2025-03-26", title = "Evento Hoy"),
            MyEvent(id = "2", date = "2025-03-26", title = "Otro evento Hoy"),
            MyEvent(id = "3", date = "2025-03-29", title = "Evento 29 de Marzo")
        )

        // Se agrupan y muestran como en runtime
        Column {
            TopBar()
            val locale = Locale("es", "ES")
            val formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", locale)
            val now = LocalDate.now()
            val dateText = now.format(formatter).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(locale) else it.toString()
            }
            Text(
                text = dateText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111418),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val groupedEvents = fakeEvents.groupBy { it.date }.toSortedMap()
            LazyColumn {
                groupedEvents.forEach { (dateString, eventList) ->
                    val parsedDate = runCatching {
                        LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    }.getOrNull()
                    val dateLabel = if (parsedDate != null) {
                        parsedDate.format(formatter).replaceFirstChar { first ->
                            if (first.isLowerCase()) first.titlecase(locale) else first.toString()
                        }
                    } else dateString
                    item {
                        Text(
                            text = dateLabel,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF111418),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(eventList) { event ->
                        AgendaItem(
                            event = event,
                            onEdit = {},
                            onDelete = {}
                        )
                    }
                }
            }
        }
    }
}
