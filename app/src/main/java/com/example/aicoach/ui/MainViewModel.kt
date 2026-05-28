package com.example.aicoach.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicoach.data.AppDatabase
import com.example.aicoach.data.ChatMessage
import com.example.aicoach.data.Profile
import com.example.aicoach.data.StorageManager
import com.example.aicoach.data.Workout
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import java.io.ByteArrayOutputStream
import android.util.Base64
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class MainViewModel(private val context: Context) : ViewModel() {

    val storage = StorageManager(context)
    val database: StateFlow<AppDatabase> = storage.database

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _isGeneratingRec = MutableStateFlow(false)
    val isGeneratingRec: StateFlow<Boolean> = _isGeneratingRec

    private val _dailyRecommendation = MutableStateFlow<Workout?>(null)
    val dailyRecommendation: StateFlow<Workout?> = _dailyRecommendation

    fun saveProfile(profile: Profile) {
        storage.updateProfile(profile)
    }

    fun exportDatabase(): String {
        return storage.exportDatabaseJson()
    }

    fun importDatabase(jsonStr: String): Boolean {
        return storage.importDatabaseJson(jsonStr)
    }

    fun deleteWorkout(id: String) {
        viewModelScope.launch {
            storage.removeWorkout(id)
        }
    }

    fun renameChatSession(id: String, newName: String) {
        storage.renameChatSession(id, newName)
    }

    fun hardDeleteChatSession(id: String) {
        storage.hardDeleteChatSession(id)
    }

    fun restoreChatSession(id: String) {
        storage.restoreChatSession(id)
    }

    fun emptyTrash() {
        storage.emptyTrash()
    }

    fun sendMessage(text: String, imageUris: List<Uri> = emptyList()) {
        viewModelScope.launch {
            _isGenerating.value = true

            // Copy images to internal storage
            val localImagePaths = mutableListOf<String>()
            imageUris.forEach { uri ->
                copyImageToInternalStorage(uri)?.let { localImagePaths.add(it) }
            }

            val userMsg = ChatMessage(role = "user", content = text, imagePaths = localImagePaths)
            val activeId = database.value.activeSessionId ?: storage.createChatSession("Nouvelle discussion")
            storage.addChatMessage(activeId, userMsg)

            executeAiRequest(userMsg, activeId)
        }
    }

    fun generateRecommendation() {
        viewModelScope.launch {
            _isGeneratingRec.value = true
            try {
                val apiKey = database.value.profile.apiKey
                if (apiKey.isBlank()) {
                    _isGeneratingRec.value = false
                    return@launch
                }
                
                val provider = database.value.profile.provider.lowercase().trim()
                val modelName = database.value.profile.modelName.trim()
                val isGemini = provider == "gemini" || provider.isBlank()
                
                val sysPrompt = """
Tu es un coach sportif IA. L'utilisateur veut une recommandation d'entraînement pour aujourd'hui basée sur son historique.
Historique: ${com.google.gson.Gson().toJson(database.value.workouts.takeLast(10))}
Génère un entraînement idéal en JSON. NE METS AUCUN RETOUR A LA LIGNE DANS LA BALISE.
Format STRICT attendu:
###REC:{"title":"Running Matinal - 5km","distanceKm":5.0,"durationMinutes":30.0,"pace":"6:00"}###
                """.trimIndent()

                withContext(Dispatchers.IO) {
                    val urlStr = if (isGemini) {
                        "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                    } else {
                        "https://api.openai.com/v1/chat/completions"
                    }
                    val url = java.net.URL(urlStr)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    if (!isGemini) {
                        connection.setRequestProperty("Authorization", "Bearer $apiKey")
                    }
                    connection.doOutput = true

                    val root = org.json.JSONObject()
                    if (isGemini) {
                        val contents = org.json.JSONArray()
                        val contentObj = org.json.JSONObject()
                        contentObj.put("role", "user")
                        contentObj.put("parts", org.json.JSONArray().put(org.json.JSONObject().put("text", sysPrompt)))
                        contents.put(contentObj)
                        root.put("contents", contents)
                    } else {
                        root.put("model", if (modelName.isBlank()) "gpt-4o" else modelName)
                        val messages = org.json.JSONArray()
                        messages.put(org.json.JSONObject().put("role", "user").put("content", sysPrompt))
                        root.put("messages", messages)
                    }

                    java.io.OutputStreamWriter(connection.outputStream).use { it.write(root.toString()) }

                    if (connection.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                        val responseStr = connection.inputStream.bufferedReader().readText()
                        val respObj = org.json.JSONObject(responseStr)
                        var responseText = ""
                        if (isGemini) {
                            val candidates = respObj.getJSONArray("candidates")
                            responseText = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                        } else {
                            val choices = respObj.getJSONArray("choices")
                            responseText = choices.getJSONObject(0).getJSONObject("message").getString("content")
                        }

                        val addIdx = responseText.indexOf("###REC:")
                        if (addIdx != -1) {
                            val endIdx = responseText.indexOf("###", addIdx + 7)
                            if (endIdx != -1) {
                                val jsonStr = responseText.substring(addIdx + 7, endIdx).trim()
                                try {
                                    val jsonObj = org.json.JSONObject(jsonStr)
                                    val rec = Workout(
                                        title = jsonObj.optString("title", "Recommandation"),
                                        distanceKm = jsonObj.optDouble("distanceKm", 0.0),
                                        durationMinutes = jsonObj.optDouble("durationMinutes", 0.0),
                                        pace = jsonObj.optString("pace", "")
                                    )
                                    _dailyRecommendation.value = rec
                                } catch (e: Exception) {}
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _isGeneratingRec.value = false
        }
    }

    fun retryLastMessage() {
        viewModelScope.launch {
            val activeId = database.value.activeSessionId ?: return@launch
            val session = database.value.chatSessions.firstOrNull { it.id == activeId } ?: return@launch
            val lastMsg = session.messages.lastOrNull()
            if (lastMsg != null && lastMsg.role == "model" && lastMsg.isError) {
                // Remove the error message
                storage.removeLastChatMessage(activeId)
                // Find the user message before it
                val userMsg = session.messages.lastOrNull { it.role == "user" }
                if (userMsg != null) {
                    _isGenerating.value = true
                    executeAiRequest(userMsg, activeId)
                }
            }
        }
    }

    private suspend fun executeAiRequest(userMsg: ChatMessage, activeId: String) {
        val apiKey = database.value.profile.apiKey
        if (apiKey.isBlank()) {
            storage.addChatMessage(activeId, ChatMessage(role = "model", content = "Veuillez configurer votre clé API dans les paramètres.", isError = true))
            _isGenerating.value = false
            return
        }

            try {
                withContext(Dispatchers.IO) {
                    val provider = database.value.profile.provider.lowercase().trim()
                    val modelName = database.value.profile.modelName.trim()
                    val db = database.value
                    val session = db.chatSessions.firstOrNull { it.id == activeId } ?: return@withContext
                    
                    val globalContextStr = db.globalContext.joinToString("\n") { "[${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it.timestamp))}] ${it.content}" }

                    val sysPrompt = """
Tu es un coach sportif IA ultra-avancé.
Profil: Âge=${db.profile.age}, Taille=${db.profile.height}cm, Poids=${db.profile.weight}kg, Sexe=${db.profile.gender}.
Aujourd'hui, nous sommes le: ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())}.

CONTEXTE GLOBAL DE L'UTILISATEUR (TRÈS IMPORTANT) :
${if (globalContextStr.isBlank()) "Aucun contexte global enregistré pour l'instant." else globalContextStr}

Historique des entraînements (JSON): ${com.google.gson.Gson().toJson(db.workouts)}

Tu as le pouvoir d'ajouter, modifier ou supprimer des entraînements.
REGLE TRES IMPORTANTE : Si l'utilisateur montre une capture d'écran de ses statistiques (Samsung Health, Strava, etc.), décrit un entraînement qu'il vient de faire, ou te demande de l'enregistrer, TU DOIS OBLIGATOIREMENT utiliser la balise ###ADD_WORKOUT:...### pour l'enregistrer. C'est ta fonction principale.
CONSIGNE D'EXTRACTION ABSOLUE : Tu dois analyser l'image et extraire ABSOLUMENT TOUTES les données techniques visibles et les encoder directement dans le JSON.
N'oublie pas de définir "sportType" à "Course" ou "Vélo" en fonction du contexte de l'entraînement. Si c'est un vélo, met absolument "Vélo".
N'oublie pas de définir la date exacte dans "dateStr" (AAAA-MM-JJ) en fonction du contexte fourni par l'utilisateur.

SI l'utilisateur demande explicitement de modifier un entraînement, utilise la balise ###EDIT_WORKOUT:...###.

Voici les champs possibles (optionnels) pour le JSON : title, sportType, dateStr, distanceKm, durationMinutes, pace, speedKmh, speedMaxKmh, heartRateAvg, heartRateMax, calories, altitude, elevationGain, elevationMin, elevationMax, cadence, cadenceMax, mainWorkoutDistanceKm, contactTimeMs, flightTimeMs, regularity, verticalOscillation, symmetry, laps, steps, trainingScore, rpeScore, sweatLossMl, stiffness, weather.

Utilise STRICTEMENT ce format (pas de markdown ```json) :

POUR AJOUTER: ###ADD_WORKOUT:{"title":"...","sportType":"Course","dateStr":"2026-05-26","speedKmh":12.5,"contactTimeMs":230,"regularity":"parfaite"}###
POUR MODIFIER: ###EDIT_WORKOUT:ID_DE_L_ENTRAINEMENT|{"title":"...","distanceKm":...}###
POUR SUPPRIMER: ###DELETE_WORKOUT:ID_DE_L_ENTRAINEMENT###
                    """.trimIndent()

                    val isGemini = provider == "gemini" || provider.isBlank()
                    val urlStr = if (isGemini) {
                        "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                    } else {
                        "https://api.openai.com/v1/chat/completions"
                    }

                    val url = URL(urlStr)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    if (!isGemini) {
                        connection.setRequestProperty("Authorization", "Bearer $apiKey")
                    }
                    connection.doOutput = true

                    val root = JSONObject()
                    val b64List = mutableListOf<String>()
                    
                    userMsg.imagePaths.forEach { path ->
                        val bitmap = BitmapFactory.decodeFile(path)
                        if (bitmap != null) {
                            val baos = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                            b64List.add(Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP))
                        }
                    }

                    if (isGemini) {
                        val systemInstruction = JSONObject()
                        val sysParts = JSONArray()
                        sysParts.put(JSONObject().put("text", sysPrompt))
                        systemInstruction.put("parts", sysParts)
                        root.put("system_instruction", systemInstruction)

                        val contents = JSONArray()
                        
                        session.messages.takeLast(10).filter { !it.isError && it.id != userMsg.id && it.content.isNotBlank() }.forEach { pastMsg ->
                            val histObj = JSONObject()
                            histObj.put("role", if (pastMsg.role == "ai") "model" else "user")
                            val histParts = JSONArray()
                            histParts.put(JSONObject().put("text", pastMsg.content))
                            histObj.put("parts", histParts)
                            contents.put(histObj)
                        }

                        val contentObj = JSONObject()
                        contentObj.put("role", "user")
                        val parts = JSONArray()
                        
                        b64List.forEach { b64 ->
                            val inlineData = JSONObject()
                            inlineData.put("mime_type", "image/jpeg")
                            inlineData.put("data", b64)
                            parts.put(JSONObject().put("inline_data", inlineData))
                        }
                        parts.put(JSONObject().put("text", userMsg.content))
                        contentObj.put("parts", parts)
                        contents.put(contentObj)
                        root.put("contents", contents)
                    } else {
                        // OpenAI format
                        root.put("model", if (modelName.isBlank()) "gpt-4o" else modelName)
                        val messages = JSONArray()
                        messages.put(JSONObject().put("role", "system").put("content", sysPrompt))
                        
                        session.messages.takeLast(10).filter { !it.isError && it.id != userMsg.id && it.content.isNotBlank() }.forEach { pastMsg ->
                            val histObj = JSONObject()
                            histObj.put("role", if (pastMsg.role == "ai") "assistant" else "user")
                            histObj.put("content", pastMsg.content)
                            messages.put(histObj)
                        }
                        
                        val msgObj = JSONObject()
                        msgObj.put("role", "user")
                        if (b64List.isNotEmpty()) {
                            val contentArray = JSONArray()
                            contentArray.put(JSONObject().put("type", "text").put("text", userMsg.content))
                            b64List.forEach { b64 ->
                                val imgObj = JSONObject().put("url", "data:image/jpeg;base64,$b64")
                                contentArray.put(JSONObject().put("type", "image_url").put("image_url", imgObj))
                            }
                            msgObj.put("content", contentArray)
                        } else {
                            msgObj.put("content", userMsg.content)
                        }
                        messages.put(msgObj)
                        root.put("messages", messages)
                    }

                    OutputStreamWriter(connection.outputStream).use { it.write(root.toString()) }

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val responseStr = connection.inputStream.bufferedReader().readText()
                        val respObj = JSONObject(responseStr)
                        var responseText = ""
                        
                        if (isGemini) {
                            val candidates = respObj.getJSONArray("candidates")
                            responseText = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                        } else {
                            val choices = respObj.getJSONArray("choices")
                            responseText = choices.getJSONObject(0).getJSONObject("message").getString("content")
                        }
                        
                        var finalMsg = responseText
                        try {
                            val gson = com.google.gson.Gson()
                            
                            // Check ADD
                            val addRegex = Regex("###ADD_WORKOUT:(.*?)###", RegexOption.DOT_MATCHES_ALL)
                            addRegex.findAll(finalMsg).toList().forEach { match ->
                                val jsonStr = match.groupValues[1].trim()
                                try {
                                    val jsonObj = org.json.JSONObject(jsonStr)
                                    var workoutDate = System.currentTimeMillis()
                                    val dateStr = jsonObj.optString("dateStr", "")
                                    if (dateStr.isNotBlank()) {
                                        try {
                                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                            val parsedDate = sdf.parse(dateStr)
                                            if (parsedDate != null) workoutDate = parsedDate.time
                                        } catch (e: Exception) {}
                                    }

                                    val newWorkout = Workout(
                                        id = java.util.UUID.randomUUID().toString(),
                                        title = jsonObj.optString("title", "Entraînement"),
                                        sportType = jsonObj.optString("sportType", "Course"),
                                        distanceKm = jsonObj.optDouble("distanceKm", 0.0),
                                        durationMinutes = jsonObj.optDouble("durationMinutes", 0.0),
                                        pace = jsonObj.optString("pace", ""),
                                        speedKmh = if (jsonObj.has("speedKmh")) jsonObj.optDouble("speedKmh") else null,
                                        speedMaxKmh = if (jsonObj.has("speedMaxKmh")) jsonObj.optDouble("speedMaxKmh") else null,
                                        heartRateAvg = jsonObj.optInt("heartRateAvg", 0),
                                        heartRateMax = jsonObj.optInt("heartRateMax", 0),
                                        calories = jsonObj.optInt("calories", 0),
                                        altitude = if (jsonObj.has("altitude")) jsonObj.optInt("altitude") else null,
                                        elevationGain = jsonObj.optInt("elevationGain", 0),
                                        elevationMin = if (jsonObj.has("elevationMin")) jsonObj.optInt("elevationMin") else null,
                                        elevationMax = if (jsonObj.has("elevationMax")) jsonObj.optInt("elevationMax") else null,
                                        mainWorkoutDistanceKm = if (jsonObj.has("mainWorkoutDistanceKm")) jsonObj.optDouble("mainWorkoutDistanceKm") else null,
                                        cadence = jsonObj.optInt("cadence", 0),
                                        cadenceMax = if (jsonObj.has("cadenceMax")) jsonObj.optInt("cadenceMax") else null,
                                        symmetry = jsonObj.optString("symmetry", ""),
                                        contactTimeMs = if (jsonObj.has("contactTimeMs")) jsonObj.optInt("contactTimeMs") else null,
                                        flightTimeMs = if (jsonObj.has("flightTimeMs")) jsonObj.optInt("flightTimeMs") else null,
                                        regularity = if (jsonObj.has("regularity")) jsonObj.optString("regularity") else null,
                                        verticalOscillation = if (jsonObj.has("verticalOscillation")) jsonObj.optString("verticalOscillation") else null,
                                        steps = if (jsonObj.has("steps")) jsonObj.optInt("steps") else null,
                                        trainingScore = if (jsonObj.has("trainingScore")) jsonObj.optInt("trainingScore") else null,
                                        rpeScore = if (jsonObj.has("rpeScore")) jsonObj.optInt("rpeScore") else null,
                                        sweatLossMl = if (jsonObj.has("sweatLossMl")) jsonObj.optInt("sweatLossMl") else null,
                                        stiffness = if (jsonObj.has("stiffness")) jsonObj.optString("stiffness") else null,
                                        weather = if (jsonObj.has("weather")) jsonObj.optString("weather") else null,
                                        laps = jsonObj.optString("laps", ""),
                                        imagePaths = userMsg.imagePaths,
                                        date = workoutDate
                                    )
                                    storage.addWorkout(newWorkout)
                                    finalMsg = finalMsg.replace(match.value, "\n[ACTION:ADD:${newWorkout.id}|${newWorkout.title}]\n")
                                } catch (e: Exception) {
                                    finalMsg = finalMsg.replace(match.value, "\n[ACTION:ERROR]\n")
                                }
                            }
                            
                            // Check EDIT
                            val editRegex = Regex("###EDIT_WORKOUT:(.*?)###", RegexOption.DOT_MATCHES_ALL)
                            editRegex.findAll(finalMsg).toList().forEach { match ->
                                val content = match.groupValues[1].trim()
                                val parts = content.split("|", limit = 2)
                                if (parts.size == 2) {
                                    val workoutId = parts[0].trim()
                                    val jsonStr = parts[1].trim()
                                    try {
                                        val jsonObj = org.json.JSONObject(jsonStr)
                                        val existing = database.value.workouts.find { it.id == workoutId }
                                        if (existing != null) {
                                            var workoutDate = existing.date
                                            val dateStr = jsonObj.optString("dateStr", "")
                                            if (dateStr.isNotBlank()) {
                                                try {
                                                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                                    val parsedDate = sdf.parse(dateStr)
                                                    if (parsedDate != null) workoutDate = parsedDate.time
                                                } catch (e: Exception) {}
                                            }

                                            val editedWorkout = existing.copy(
                                                title = if (jsonObj.has("title")) jsonObj.getString("title") else existing.title,
                                                date = workoutDate,
                                                distanceKm = if (jsonObj.has("distanceKm")) jsonObj.optDouble("distanceKm") else existing.distanceKm,
                                                durationMinutes = if (jsonObj.has("durationMinutes")) jsonObj.optDouble("durationMinutes") else existing.durationMinutes,
                                                pace = if (jsonObj.has("pace")) jsonObj.getString("pace") else existing.pace,
                                                speedKmh = if (jsonObj.has("speedKmh")) jsonObj.optDouble("speedKmh") else existing.speedKmh,
                                                speedMaxKmh = if (jsonObj.has("speedMaxKmh")) jsonObj.optDouble("speedMaxKmh") else existing.speedMaxKmh,
                                                heartRateAvg = if (jsonObj.has("heartRateAvg")) jsonObj.optInt("heartRateAvg") else existing.heartRateAvg,
                                                heartRateMax = if (jsonObj.has("heartRateMax")) jsonObj.optInt("heartRateMax") else existing.heartRateMax,
                                                calories = if (jsonObj.has("calories")) jsonObj.optInt("calories") else existing.calories,
                                                altitude = if (jsonObj.has("altitude")) jsonObj.optInt("altitude") else existing.altitude,
                                                elevationGain = if (jsonObj.has("elevationGain")) jsonObj.optInt("elevationGain") else existing.elevationGain,
                                                elevationMin = if (jsonObj.has("elevationMin")) jsonObj.optInt("elevationMin") else existing.elevationMin,
                                                elevationMax = if (jsonObj.has("elevationMax")) jsonObj.optInt("elevationMax") else existing.elevationMax,
                                                mainWorkoutDistanceKm = if (jsonObj.has("mainWorkoutDistanceKm")) jsonObj.optDouble("mainWorkoutDistanceKm") else existing.mainWorkoutDistanceKm,
                                                cadence = if (jsonObj.has("cadence")) jsonObj.optInt("cadence") else existing.cadence,
                                                cadenceMax = if (jsonObj.has("cadenceMax")) jsonObj.optInt("cadenceMax") else existing.cadenceMax,
                                                symmetry = if (jsonObj.has("symmetry")) jsonObj.getString("symmetry") else existing.symmetry,
                                                contactTimeMs = if (jsonObj.has("contactTimeMs")) jsonObj.optInt("contactTimeMs") else existing.contactTimeMs,
                                                flightTimeMs = if (jsonObj.has("flightTimeMs")) jsonObj.optInt("flightTimeMs") else existing.flightTimeMs,
                                                regularity = if (jsonObj.has("regularity")) jsonObj.getString("regularity") else existing.regularity,
                                                verticalOscillation = if (jsonObj.has("verticalOscillation")) jsonObj.getString("verticalOscillation") else existing.verticalOscillation,
                                                steps = if (jsonObj.has("steps")) jsonObj.optInt("steps") else existing.steps,
                                                trainingScore = if (jsonObj.has("trainingScore")) jsonObj.optInt("trainingScore") else existing.trainingScore,
                                                rpeScore = if (jsonObj.has("rpeScore")) jsonObj.optInt("rpeScore") else existing.rpeScore,
                                                sweatLossMl = if (jsonObj.has("sweatLossMl")) jsonObj.optInt("sweatLossMl") else existing.sweatLossMl,
                                                stiffness = if (jsonObj.has("stiffness")) jsonObj.getString("stiffness") else existing.stiffness,
                                                weather = if (jsonObj.has("weather")) jsonObj.getString("weather") else existing.weather,
                                                laps = if (jsonObj.has("laps")) jsonObj.getString("laps") else existing.laps,
                                                imagePaths = existing.imagePaths + userMsg.imagePaths
                                            )
                                            
                                            val diffs = mutableListOf<String>()
                                            if (existing.distanceKm != editedWorkout.distanceKm) diffs.add("Distance")
                                            if (existing.durationMinutes != editedWorkout.durationMinutes) diffs.add("Temps")
                                            if (existing.date != editedWorkout.date) diffs.add("Date")
                                            if (existing.title != editedWorkout.title) diffs.add("Titre")
                                            val summaryStr = if (diffs.isNotEmpty()) "Modifié: " + diffs.joinToString(", ") else "Mise à jour effectuée"
                                            
                                            storage.editWorkout(editedWorkout)
                                            finalMsg = finalMsg.replace(match.value, "\n[ACTION:EDIT:${editedWorkout.id}|$summaryStr]\n")
                                        } else {
                                            finalMsg = finalMsg.replace(match.value, "\n[ACTION:ERROR]\n")
                                        }
                                    } catch (e: Exception) {
                                        finalMsg = finalMsg.replace(match.value, "\n[ACTION:ERROR]\n")
                                    }
                                } else {
                                    finalMsg = finalMsg.replace(match.value, "\n[ACTION:ERROR]\n")
                                }
                            }

                            // Check DELETE
                            val delRegex = Regex("###DELETE_WORKOUT:(.*?)###", RegexOption.DOT_MATCHES_ALL)
                            delRegex.findAll(finalMsg).toList().forEach { match ->
                                val workoutId = match.groupValues[1].trim()
                                storage.removeWorkout(workoutId)
                                finalMsg = finalMsg.replace(match.value, "\n[ACTION:DELETE:${workoutId}]\n")
                            }
                            
                        } catch (e: Exception) {
                            e.printStackTrace()
                            finalMsg += "\n(Erreur d'action IA: ${e.message})"
                        }

                        storage.addChatMessage(activeId, ChatMessage(role = "model", content = finalMsg))
                        
                        // Auto-save context every 10 messages
                        val activeSession = storage.database.value.chatSessions.find { it.id == activeId }
                        if (activeSession != null && activeSession.messages.size > 0 && activeSession.messages.size % 10 == 0) {
                            generateGlobalContext { 
                                kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                                    android.widget.Toast.makeText(context, "Contexte global auto-sauvegardé", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        val errorStr = connection.errorStream.bufferedReader().readText()
                        storage.addChatMessage(activeId, ChatMessage(role = "model", content = "Erreur HTTP $responseCode : $errorStr", isError = true))
                    }
                }
            } catch (e: Exception) {
                storage.addChatMessage(activeId, ChatMessage(role = "model", content = "Erreur de connexion : ${e.message}", isError = true))
            }

            _isGenerating.value = false
    }

    private fun copyImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val file = File(context.filesDir, "img_${UUID.randomUUID()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = storage.exportToJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val json = input.bufferedReader().readText()
                    storage.importFromJson(json)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun generateGlobalContext(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val apiKey = database.value.profile.apiKey
                if (apiKey.isBlank()) {
                    onResult(null)
                    return@launch
                }
                
                withContext(Dispatchers.IO) {
                    val provider = database.value.profile.provider.lowercase().trim()
                    val modelName = database.value.profile.modelName.trim()
                    val db = database.value
                    
                    val allMessages = db.chatSessions.filter { !it.isDeleted }.flatMap { it.messages }.sortedBy { it.timestamp }.takeLast(30)
                    val historyJson = com.google.gson.Gson().toJson(allMessages.map { mapOf("role" to it.role, "content" to it.content) })
                    
                    val sysPrompt = """
Tu es un assistant IA spécialisé dans la synthèse. 
L'utilisateur a une application de coaching sportif avec un système de "Contexte Global".
Voici les derniers messages de la conversation (JSON) :
$historyJson

Ton but est de résumer les informations importantes, les décisions prises, les objectifs de l'utilisateur, ou tout détail qui doit être gardé en mémoire sur le long terme.
Sois concis et liste les points importants sous forme de tirets.
Ne parle pas de l'application elle-même, résume uniquement le contexte de l'utilisateur.
                    """.trimIndent()

                    val isGemini = provider == "gemini" || provider.isBlank()
                    val urlStr = if (isGemini) {
                        "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                    } else {
                        "https://api.openai.com/v1/chat/completions"
                    }

                    val url = java.net.URL(urlStr)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    if (!isGemini) {
                        connection.setRequestProperty("Authorization", "Bearer $apiKey")
                    }
                    connection.doOutput = true

                    val root = JSONObject()
                    if (isGemini) {
                        val contents = JSONArray()
                        val contentObj = JSONObject()
                        contentObj.put("role", "user")
                        contentObj.put("parts", JSONArray().put(JSONObject().put("text", sysPrompt)))
                        contents.put(contentObj)
                        root.put("contents", contents)
                    } else {
                        root.put("model", if (modelName.isBlank()) "gpt-4o" else modelName)
                        val messages = JSONArray()
                        messages.put(JSONObject().put("role", "user").put("content", sysPrompt))
                        root.put("messages", messages)
                    }

                    java.io.OutputStreamWriter(connection.outputStream).use { it.write(root.toString()) }

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val responseStr = connection.inputStream.bufferedReader().readText()
                        val respObj = JSONObject(responseStr)
                        var responseText = ""
                        if (isGemini) {
                            val candidates = respObj.getJSONArray("candidates")
                            responseText = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                        } else {
                            val choices = respObj.getJSONArray("choices")
                            responseText = choices.getJSONObject(0).getJSONObject("message").getString("content")
                        }
                        
                        storage.updateGlobalContext(responseText.trim())
                        withContext(Dispatchers.Main) {
                            onResult(responseText.trim())
                        }
                    } else {
                        withContext(Dispatchers.Main) { onResult(null) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onResult(null) }
            }
        }
    }
}
