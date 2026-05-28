package com.example.aicoach.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.aicoach.data.ChatMessage
import com.example.aicoach.data.ChatSession
import com.example.aicoach.data.Profile
import com.example.aicoach.data.Workout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import java.util.*

val DeepNightBlue = Color(0xFF0C2B4B)
val ConceptTeal = Color(0xFF1ABC9C)
val LightSurface = Color(0xFFF4F6F9)
val UserBubbleColor = DeepNightBlue
val AIBubbleColor = Color(0xFFE2E8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf("chat") }
    var selectedWorkout by remember { mutableStateOf<Workout?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showContextDialog by remember { mutableStateOf(false) }

    if (selectedWorkout != null) {
        WorkoutDetailScreen(viewModel = viewModel, workout = selectedWorkout!!, onClose = { selectedWorkout = null })
        return
    }

    if (showContextDialog) {
        GlobalContextDialog(viewModel, onDismiss = { showContextDialog = false })
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                ChatDrawerContent(viewModel, onClose = { scope.launch { drawerState.close() } })
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
                        LaunchedEffect(Unit) {
                            while(true) {
                                kotlinx.coroutines.delay(1000)
                                currentTime = System.currentTimeMillis()
                            }
                        }
                        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(currentTime))
                        Text(timeStr, fontSize = 18.sp, color = DeepNightBlue, fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = DeepNightBlue)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showContextDialog = true }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Contexte Global", tint = ConceptTeal)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            floatingActionButton = {
            if (currentScreen == "workouts") {
                FloatingActionButton(
                    onClick = { currentScreen = "chat" },
                    containerColor = ConceptTeal,
                    contentColor = Color.White
                ) {
                    Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = "Ajouter")
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == "chat",
                    onClick = { currentScreen = "chat" },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Coach") },
                    label = { Text("Coach", fontWeight = if (currentScreen == "chat") FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = DeepNightBlue,
                        indicatorColor = DeepNightBlue
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == "workouts",
                    onClick = { currentScreen = "workouts" },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Agenda") },
                    label = { Text("Agenda", fontWeight = if (currentScreen == "workouts") FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = DeepNightBlue,
                        indicatorColor = DeepNightBlue
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == "profile",
                    onClick = { currentScreen = "profile" },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                    label = { Text("Profil", fontWeight = if (currentScreen == "profile") FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = DeepNightBlue,
                        indicatorColor = DeepNightBlue
                    )
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(LightSurface)) {
            when (currentScreen) {
                "chat" -> ChatScreen(viewModel, onWorkoutClick = { id -> 
                    selectedWorkout = viewModel.database.value.workouts.find { it.id == id }
                })
                "workouts" -> CalendarScreen(viewModel, onWorkoutClick = { selectedWorkout = it })
                "profile" -> ProfileScreen(viewModel)
            }
        }
    }
    }
}

@Composable
fun ChatDrawerContent(viewModel: MainViewModel, onClose: () -> Unit) {
    val database = viewModel.database.collectAsState().value
    var sessionToRename by remember { mutableStateOf<ChatSession?>(null) }
    var newSessionTitle by remember { mutableStateOf("") }
    var showTrashDialog by remember { mutableStateOf(false) }

    if (sessionToRename != null) {
        AlertDialog(
            onDismissRequest = { sessionToRename = null },
            title = { Text("Renommer la discussion") },
            text = {
                OutlinedTextField(
                    value = newSessionTitle,
                    onValueChange = { newSessionTitle = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameChatSession(sessionToRename!!.id, newSessionTitle)
                    sessionToRename = null
                }) { Text("Renommer", color = ConceptTeal) }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRename = null }) { Text("Annuler") }
            }
        )
    }

    if (showTrashDialog) {
        TrashDialog(viewModel, onDismiss = { showTrashDialog = false })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mes Discussions", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DeepNightBlue)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                viewModel.storage.createChatSession("Nouvelle discussion")
                onClose()
            },
            colors = ButtonDefaults.buttonColors(containerColor = ConceptTeal),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Nouvelle discussion")
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(database.chatSessions.filter { !it.isDeleted }) { session ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable {
                        viewModel.storage.setActiveSession(session.id)
                        onClose()
                    },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        session.title,
                        fontWeight = if (session.id == database.activeSessionId) FontWeight.Bold else FontWeight.Normal,
                        color = if (session.id == database.activeSessionId) ConceptTeal else Color.DarkGray,
                        modifier = Modifier.weight(1f)
                    )
                    Row {
                        IconButton(onClick = {
                            sessionToRename = session
                            newSessionTitle = session.title
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Renommer", tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { viewModel.storage.deleteChatSession(session.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.Red.copy(alpha=0.6f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
                HorizontalDivider(color = Color.LightGray.copy(alpha=0.3f))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { showTrashDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray.copy(alpha=0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.DeleteSweep, contentDescription = "Corbeille", tint = Color.Red)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Corbeille", color = Color.Red)
        }
    }
}

@Composable
fun TrashDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val database = viewModel.database.collectAsState().value
    val deletedSessions = database.chatSessions.filter { it.isDeleted }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.Red)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Corbeille", color = Color.Red)
            }
        },
        text = {
            Column {
                if (deletedSessions.isEmpty()) {
                    Text("La corbeille est vide.", color = Color.Gray)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(deletedSessions) { session ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    session.title,
                                    color = Color.DarkGray,
                                    modifier = Modifier.weight(1f)
                                )
                                Row {
                                    IconButton(onClick = { viewModel.restoreChatSession(session.id) }) {
                                        Icon(Icons.Default.Restore, contentDescription = "Restaurer", tint = ConceptTeal, modifier = Modifier.size(24.dp))
                                    }
                                    IconButton(onClick = { viewModel.hardDeleteChatSession(session.id) }) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = "Supprimer", tint = Color.Red, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha=0.3f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.emptyTrash()
                onDismiss()
            }) { Text("Vider la corbeille", color = Color.Red) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

@Composable
fun GlobalContextDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val database = viewModel.database.collectAsState().value
    var isGenerating by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ConceptTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Contexte Global", color = DeepNightBlue)
            }
        },
        text = {
            Column {
                Text("Ce contexte est fourni à l'IA lors de chaque interaction pour qu'elle se souvienne de vos objectifs et de votre historique.", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    if (database.globalContext.isEmpty()) {
                        item { Text("Aucun contexte pour l'instant.", color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) }
                    } else {
                        items(database.globalContext) { entry ->
                            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(entry.timestamp))
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(dateStr, fontSize = 10.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(entry.content, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
                if (isGenerating) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = ConceptTeal)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer", color = DeepNightBlue) }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    isGenerating = true
                    viewModel.generateGlobalContext { isGenerating = false }
                },
                enabled = !isGenerating
            ) {
                Text("Mettre à jour", color = ConceptTeal)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: MainViewModel, onWorkoutClick: (String) -> Unit) {
    val database = viewModel.database.collectAsState().value
    val isGenerating = viewModel.isGenerating.collectAsState().value
    var textInput by remember { mutableStateOf("") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    
    val listState = rememberLazyListState()
    
    val activeSession = database.chatSessions.firstOrNull { it.id == database.activeSessionId }
    val messages = activeSession?.messages ?: emptyList()

    // Auto-scroll logic
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            delay(100)
            listState.animateScrollToItem(messages.size)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        selectedImageUris = uris
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.size(64.dp).background(DeepNightBlue.copy(alpha=0.1f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Sports, contentDescription = null, modifier = Modifier.size(32.dp), tint = DeepNightBlue)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Bonjour ! Comment puis-je t'aider aujourd'hui ?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepNightBlue,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(messages) { msg ->
                    ChatMessageBubble(msg, onRetry = { viewModel.retryLastMessage() }, onWorkoutClick = onWorkoutClick)
                }
                if (isGenerating) {
                    item {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally),
                            color = DeepNightBlue
                        )
                    }
                }
            }
        }

        if (selectedImageUris.isNotEmpty()) {
            LazyRow(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                items(selectedImageUris.size) { index ->
                    val uri = selectedImageUris[index]
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedImageUris = selectedImageUris.filter { it != uri } },
                            modifier = Modifier.align(Alignment.TopEnd).size(20.dp).background(Color.Black.copy(alpha=0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Retirer", tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }

        // Quick action chips just above input
        if (messages.isEmpty()) {
            LazyRow(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                item { QuickActionConceptChip("Conseils nutritionnels ?") { textInput = it } }
                item { QuickActionConceptChip("Planifier ma semaine ?") { textInput = it } }
                item { QuickActionConceptChip("Améliorer ma course ?") { textInput = it } }
            }
        }

        // Pill Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(Color.White, RoundedCornerShape(50.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(50.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { imagePicker.launch("image/*") }) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Photos", tint = Color.Gray)
            }
            TextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Écrire un message...", color = Color.LightGray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            IconButton(
                onClick = {
                    if (textInput.isNotBlank() || selectedImageUris.isNotEmpty()) {
                        viewModel.sendMessage(textInput, selectedImageUris)
                        textInput = ""
                        selectedImageUris = emptyList()
                    }
                },
                modifier = Modifier.background(ConceptTeal, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer", tint = Color.White)
            }
        }
    }
}

@Composable
fun QuickActionConceptChip(text: String, onClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .border(1.dp, ConceptTeal, RoundedCornerShape(20.dp))
            .clickable { onClick(text) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text, color = ConceptTeal, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ActionCard(title: String, subtitle: String, onClick: (() -> Unit)?) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(enabled = onClick != null) { onClick?.invoke() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = DeepNightBlue, fontSize = 14.sp)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun ChatMessageBubble(msg: ChatMessage, onRetry: () -> Unit, onWorkoutClick: (String) -> Unit) {
    val isUser = msg.role == "user"
    val align = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (isUser) UserBubbleColor else AIBubbleColor
    val textColor = if (isUser) Color.White else DeepNightBlue
    
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = if(isUser) Alignment.End else Alignment.Start) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(shape)
                .background(bgColor)
                .padding(14.dp)
        ) {
            Column {
                if (msg.imagePaths.isNotEmpty()) {
                    LazyRow(modifier = Modifier.padding(bottom = 8.dp)) {
                        items(msg.imagePaths.size) { index ->
                            AsyncImage(
                                model = msg.imagePaths[index],
                                contentDescription = null,
                                modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)).padding(end = 4.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                if (msg.content.isNotBlank()) {
                    val lines = msg.content.split("\n")
                    for (line in lines) {
                        when {
                            line.startsWith("[ACTION:ADD:") -> {
                                val parts = line.substringAfter("[ACTION:ADD:").substringBefore("]").split("|")
                                val id = parts[0]
                                val summary = if (parts.size > 1) parts[1] else "Cliquez pour voir les détails"
                                ActionCard("✅ Entraînement Ajouté", summary) { onWorkoutClick(id) }
                            }
                            line.startsWith("[ACTION:EDIT:") -> {
                                val parts = line.substringAfter("[ACTION:EDIT:").substringBefore("]").split("|")
                                val id = parts[0]
                                val summary = if (parts.size > 1) parts[1] else "Cliquez pour voir les détails"
                                ActionCard("✏️ Entraînement Modifié", summary) { onWorkoutClick(id) }
                            }
                            line.startsWith("[ACTION:DELETE:") -> {
                                ActionCard("🗑️ Entraînement Supprimé", "L'entraînement a été retiré de l'agenda", null)
                            }
                            line.startsWith("[ACTION:ERROR]") -> {
                                ActionCard("⚠️ Erreur", "L'IA n'a pas pu formater les données.", null)
                            }
                            else -> if (line.isNotBlank()) {
                                Text(text = line, color = textColor, fontSize = 15.sp)
                            }
                        }
                    }
                }
                if (msg.isError) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Réessayer", color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: MainViewModel, onWorkoutClick: (Workout) -> Unit) {
    val database = viewModel.database.collectAsState().value
    var viewMode by remember { mutableStateOf("Semaine") }
    var currentDate by remember { mutableStateOf(Calendar.getInstance()) }
    var weekOffset by remember { mutableStateOf(0) }
    var monthOffset by remember { mutableStateOf(0) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }

    val recommendation = viewModel.dailyRecommendation.collectAsState().value
    val isGeneratingRec = viewModel.isGeneratingRec.collectAsState().value

    val sortedWorkouts = database.workouts.sortedByDescending { it.timestamp }
    var streakCount = 0
    var countdownText = "0h 0m"
    var hasStreak = false
    
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(1000)
            now = System.currentTimeMillis()
        }
    }

    if (sortedWorkouts.isNotEmpty()) {
        val maxGapMs = 3L * 24 * 60 * 60 * 1000 + 12L * 60 * 60 * 1000 // 84 hours
        val lastWorkoutTime = sortedWorkouts.first().timestamp
        val timeSinceLast = now - lastWorkoutTime

        if (timeSinceLast <= maxGapMs) {
            hasStreak = true
            streakCount = 1
            var prevTime = lastWorkoutTime
            for (i in 1 until sortedWorkouts.size) {
                val currTime = sortedWorkouts[i].timestamp
                if (prevTime - currTime <= maxGapMs) {
                    streakCount++
                    prevTime = currTime
                } else {
                    break
                }
            }
            val remainingMs = maxGapMs - timeSinceLast
            val rh = remainingMs / (1000 * 60 * 60)
            val rm = (remainingMs / (1000 * 60)) % 60
            countdownText = "${rh}h ${rm}m"
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        
        if (hasStreak) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Default.LocalFireDepartment, contentDescription = "Série", tint = Color(0xFFFF5722), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("$streakCount entraînements", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DeepNightBlue)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Expire dans: $countdownText", color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Recommandation Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepNightBlue),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("RECOMMANDATION DU JOUR", color = Color.White.copy(alpha=0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { viewModel.generateRecommendation() }, modifier = Modifier.size(24.dp)) {
                        if (isGeneratingRec) {
                            CircularProgressIndicator(color = ConceptTeal, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Générer", tint = ConceptTeal)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                if (recommendation != null) {
                    Text(recommendation.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("${recommendation.distanceKm} km", color = Color.LightGray, fontSize = 14.sp)
                        val rPaceStr = recommendation.pace ?: ""
                        val rPace = if (rPaceStr.endsWith("/km", ignoreCase=true) || rPaceStr.endsWith("/ KM", ignoreCase=true)) rPaceStr else "${rPaceStr}/km"
                        Text(rPace, color = Color.LightGray, fontSize = 14.sp)
                        val rDurValue = recommendation.durationMinutes ?: 0.0
                        val rh = rDurValue.toInt() / 60
                        val rm = rDurValue.toInt() % 60
                        val rDur = if (rh > 0) "${rh}h${rm.toString().padStart(2, '0')}" else "${rm} min"
                        Text(rDur, color = Color.LightGray, fontSize = 14.sp)
                    }
                } else {
                    Text("Appuyez sur rafraîchir pour générer un entraînement idéal basé sur votre profil.", color = Color.White, fontSize = 14.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp).background(Color.White.copy(alpha=0.2f), CircleShape).padding(2.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Coach AI", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        // Calendar Logic Setup
        val cal = currentDate.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val calForTitle = cal.clone() as Calendar
        if (viewMode == "Semaine") {
            calForTitle.add(Calendar.WEEK_OF_YEAR, weekOffset)
        } else {
            calForTitle.add(Calendar.MONTH, monthOffset)
        }
        val currentMonthYearFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val currentMonthYearText = currentMonthYearFormatter.format(calForTitle.time).uppercase()

        // Agenda Tabs
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (viewMode == "Semaine") weekOffset-- else monthOffset-- }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Précédent", tint = DeepNightBlue)
                }
                Text(currentMonthYearText, color = DeepNightBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 4.dp))
                IconButton(onClick = { if (viewMode == "Semaine") weekOffset++ else monthOffset++ }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Suivant", tint = DeepNightBlue)
                }
            }
            Row {
                Text("Sem", color = if (viewMode == "Semaine") DeepNightBlue else Color.Gray, fontWeight = if (viewMode == "Semaine") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { viewMode = "Semaine" }.padding(end = 8.dp))
                Text("Mois", color = if (viewMode == "Mois") DeepNightBlue else Color.Gray, fontWeight = if (viewMode == "Mois") FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { viewMode = "Mois" })
            }
        }

        val daysList = mutableListOf<Triple<String, Int, Long>>()
        if (viewMode == "Semaine") {
            cal.add(Calendar.WEEK_OF_YEAR, weekOffset)
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            for (i in 0..6) {
                val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time).take(3).replaceFirstChar { it.uppercase() }
                daysList.add(Triple(dayName, cal.get(Calendar.DAY_OF_MONTH), cal.timeInMillis))
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
        } else {
            cal.add(Calendar.MONTH, monthOffset)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val emptyCells = (firstDayOfWeek + 5) % 7
            
            for (i in 0 until emptyCells) {
                daysList.add(Triple("", 0, 0L))
            }
            
            for (i in 1..maxDays) {
                val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time).take(3).replaceFirstChar { it.uppercase() }
                daysList.add(Triple(dayName, i, cal.timeInMillis))
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.padding(horizontal = 16.dp).heightIn(max = if (viewMode == "Semaine") 80.dp else 300.dp)
        ) {
            items(daysList.size) { index ->
                val dayData = daysList[index]
                val dayStart = dayData.third
                val dayEnd = dayStart + 86400000L
                val todayMs = System.currentTimeMillis()
                val isToday = dayData.second > 0 && todayMs in dayStart until dayEnd
                // Get workouts for this exact day
                val dayWorkouts = if (dayData.second > 0) database.workouts.filter { it.date >= dayStart && it.date < dayEnd } else emptyList()
                val hasWorkout = dayWorkouts.isNotEmpty()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(4.dp).clickable {
                        if (hasWorkout) {
                            selectedDate = if (selectedDate == dayStart) null else dayStart
                        }
                    }
                ) {
                    if (viewMode == "Semaine" || index < 7) {
                        val headerText = if (viewMode == "Mois") {
                            listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")[index]
                        } else dayData.first
                        Text(headerText, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    if (dayData.second > 0) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(when {
                                    selectedDate == dayStart -> ConceptTeal
                                    hasWorkout -> ConceptTeal.copy(alpha=0.5f)
                                    isToday -> Color(0xFFADD8E6)
                                    else -> Color.Transparent
                                }),
                            contentAlignment = Alignment.Center
                        ) {
                            val isSunday = dayData.first.startsWith("Dim", ignoreCase = true)
                            val numColor = when {
                                hasWorkout -> Color.White
                                isSunday -> Color.Red
                                else -> DeepNightBlue
                            }
                            Text(dayData.second.toString(), color = numColor, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }
            }
        }

        Text("VOS ACTIVITÉS RÉCENTES", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp), fontSize = 14.sp, color = DeepNightBlue)

        val displayedWorkouts = if (selectedDate != null) {
            database.workouts.filter { it.date >= selectedDate!! && it.date < selectedDate!! + 86400000L }
        } else {
            database.workouts
        }

        if (displayedWorkouts.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.DirectionsRun, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                Spacer(modifier = Modifier.height(16.dp))
                Text(if (selectedDate != null) "Aucun entraînement ce jour" else "Aucune séance récente", color = Color.Gray, fontWeight = FontWeight.Medium)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                items(displayedWorkouts.reversed()) { workout ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onWorkoutClick(workout) },
                        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(DeepNightBlue, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                val wIcon = if (workout.sportType.equals("vélo", ignoreCase=true) || workout.sportType.equals("velo", ignoreCase=true)) Icons.Default.DirectionsBike else Icons.Default.DirectionsRun
                                Icon(wIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(workout.title.ifBlank { "Entraînement" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DeepNightBlue)
                                val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                Text(df.format(Date(workout.date)), fontSize = 12.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Score", fontSize = 10.sp, color = Color.Gray)
                                Text("8.5", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ConceptTeal)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutDetailScreen(viewModel: MainViewModel, workout: Workout, onClose: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer l'entraînement") },
            text = { Text("Voulez-vous vraiment supprimer cet entraînement ? Cette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteWorkout(workout.id)
                    onClose()
                }) { Text("Supprimer", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annuler") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(LightSurface)) {
        Box(modifier = Modifier.fillMaxWidth().background(DeepNightBlue).padding(top = 40.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Retour", tint = Color.White)
                    }
                    Text(workout.title.ifBlank { "Détails" }, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.Red.copy(alpha = 0.8f))
                }
            }
        }
        
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            val df = SimpleDateFormat("EEEE dd MMMM yyyy", Locale.getDefault())
            Text(df.format(Date(workout.date)), fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))

            // Main Stats Concept
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatBox("Distance", "${workout.distanceKm} km")
                val wDurValue = workout.durationMinutes ?: 0.0
                val h = wDurValue.toInt() / 60
                val m = wDurValue.toInt() % 60
                val durStr = if (h > 0) "${h}h${m.toString().padStart(2, '0')}" else "${m} min"
                StatBox("Durée", durStr)
                val wPaceStr = workout.pace ?: ""
                val wPace = if (wPaceStr.endsWith("/km", ignoreCase=true) || wPaceStr.endsWith("/ KM", ignoreCase=true)) wPaceStr else "${wPaceStr}/km"
                StatBox("Allure", wPace)
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Advanced Stats
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.elevatedCardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Données avancées", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp), color = DeepNightBlue)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("FC Moyenne:", color = Color.Gray)
                        Text("${workout.heartRateAvg} bpm", fontWeight = FontWeight.Bold, color = DeepNightBlue)
                    }
                    
                    val addStat = @Composable { label: String, value: String ->
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha=0.3f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, color = Color.Gray)
                            Text(value, fontWeight = FontWeight.Bold, color = DeepNightBlue)
                        }
                    }
                    
                    addStat("Calories:", "${workout.calories} kcal")
                    addStat("Dénivelé:", "${workout.elevationGain} m")
                    addStat("Cadence:", "${workout.cadence} ppm")
                    
                    if (workout.speedKmh != null) addStat("Vitesse:", "${workout.speedKmh} km/h")
                    if (workout.speedMaxKmh != null) addStat("Vitesse Max:", "${workout.speedMaxKmh} km/h")
                    if (workout.steps != null) addStat("Pas:", "${workout.steps}")
                    if (workout.altitude != null) addStat("Altitude:", "${workout.altitude} m")
                    if (workout.elevationMin != null) addStat("Alt. minimale:", "${workout.elevationMin} m")
                    if (workout.elevationMax != null) addStat("Alt. maximale:", "${workout.elevationMax} m")
                    if (workout.cadenceMax != null) addStat("Cadence Max:", "${workout.cadenceMax} ppm")
                    if (workout.mainWorkoutDistanceKm != null) addStat("Dist. principale:", "${workout.mainWorkoutDistanceKm} km")
                    if (workout.contactTimeMs != null) addStat("Temps contact:", "${workout.contactTimeMs} ms")
                    if (workout.flightTimeMs != null) addStat("Temps en l'air:", "${workout.flightTimeMs} ms")
                    if (workout.regularity != null) addStat("Régularité:", workout.regularity)
                    if (workout.verticalOscillation != null) addStat("Oscill. verticale:", workout.verticalOscillation)
                    if (workout.symmetry != null && workout.symmetry.isNotBlank()) addStat("Symétrie:", workout.symmetry)
                    if (workout.stiffness != null) addStat("Rigidité:", workout.stiffness)
                    if (workout.sweatLossMl != null) addStat("Transpiration:", "${workout.sweatLossMl} ml")
                    if (workout.rpeScore != null) addStat("RPE (Effort):", "${workout.rpeScore}/10")
                    if (workout.trainingScore != null) addStat("Score entraînement:", "${workout.trainingScore}")
                    if (workout.weather != null) addStat("Météo:", workout.weather)
                }
            }

            if (workout.laps?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(16.dp))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.elevatedCardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Tours (Laps)", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp), color = DeepNightBlue)
                        Text(workout.laps ?: "", fontSize = 14.sp, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = DeepNightBlue)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: MainViewModel) {
    val database = viewModel.database.collectAsState().value
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(viewModel.exportDatabase().toByteArray())
                }
                android.widget.Toast.makeText(context, "Sauvegarde réussie !", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Erreur de sauvegarde", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val jsonStr = input.bufferedReader().use { it.readText() }
                    if (viewModel.importDatabase(jsonStr)) {
                        android.widget.Toast.makeText(context, "Données restaurées !", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        android.widget.Toast.makeText(context, "Fichier invalide", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Erreur de restauration", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    var age by remember { mutableStateOf(database.profile.age.toString()) }
    var height by remember { mutableStateOf(database.profile.height.toString()) }
    var weight by remember { mutableStateOf(database.profile.weight.toString()) }
    var gender by remember { mutableStateOf(database.profile.gender) }
    var provider by remember { mutableStateOf(database.profile.provider) }
    var modelName by remember { mutableStateOf(database.profile.modelName) }
    var apiKey by remember { mutableStateOf(database.profile.apiKey) }

    Column(modifier = Modifier.fillMaxSize().background(LightSurface)) {
        // Concept Header Overlapping
        Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(DeepNightBlue)) {
            Column(modifier = Modifier.fillMaxSize().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Profil & Paramètres", color = Color.White, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = DeepNightBlue, modifier = Modifier.size(40.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Utilisateur Pro", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-30).dp).verticalScroll(rememberScrollState())) {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.elevatedCardElevation(4.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Informations Physiques", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DeepNightBlue, modifier = Modifier.padding(bottom = 16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = age, onValueChange = { age = it }, label = { Text("Âge") },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint=Color.Gray) },
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = height, onValueChange = { height = it }, label = { Text("Taille") },
                            leadingIcon = { Icon(Icons.Default.Height, contentDescription = null, tint=Color.Gray) },
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = weight, onValueChange = { weight = it }, label = { Text("Poids (kg)") },
                        leadingIcon = { Icon(Icons.Default.MonitorWeight, contentDescription = null, tint=Color.Gray) },
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Sexe", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("Homme", "Femme", "Autre").forEach { option ->
                            val selected = gender == option
                            OutlinedButton(
                                onClick = { gender = option },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) DeepNightBlue else Color.Transparent,
                                    contentColor = if (selected) Color.White else DeepNightBlue
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text(option) }
                        }
                    }
                }
            }

            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.elevatedCardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Paramètres IA", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DeepNightBlue, modifier = Modifier.padding(bottom = 16.dp))
                    OutlinedTextField(
                        value = provider, onValueChange = { provider = it }, label = { Text("Service (gemini ou openai)") },
                        leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null, tint=Color.Gray) },
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = modelName, onValueChange = { modelName = it }, label = { Text("Modèle") },
                        leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null, tint=Color.Gray) },
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = apiKey, onValueChange = { apiKey = it }, label = { Text("Clé API") },
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint=Color.Gray) },
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.elevatedCardElevation(4.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Sauvegarde et Restauration", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DeepNightBlue, modifier = Modifier.padding(bottom = 16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(onClick = { exportLauncher.launch("Sauvegarde_AICoach.json") }, modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                            Text("Exporter", color = DeepNightBlue)
                        }
                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }, modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                            Text("Importer", color = DeepNightBlue)
                        }
                    }
                }
            }
            
            Button(
                onClick = {
                    viewModel.saveProfile(Profile(age.toIntOrNull() ?: 0, height.toIntOrNull() ?: 0, weight.toIntOrNull() ?: 0, gender, apiKey, provider, modelName))
                    android.widget.Toast.makeText(context, "Profil mis à jour !", android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ConceptTeal)
            ) {
                Text("Modifier", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
