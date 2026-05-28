package com.example.aicoach.data

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class StorageManager(private val context: Context) {

    private val gson = Gson()
    private val dbFile = File(context.filesDir, "AppDatabase.json")

    private val _database = MutableStateFlow(AppDatabase())
    val database: StateFlow<AppDatabase> = _database

    private val undoHistory = java.util.Stack<List<Workout>>()

    init {
        loadDatabase()
    }

    private fun loadDatabase() {
        if (dbFile.exists()) {
            try {
                FileReader(dbFile).use { reader ->
                    val db = gson.fromJson(reader, AppDatabase::class.java)
                    if (db != null) {
                        // Migration logic
                        if (db.chatHistory.isNotEmpty() && db.chatSessions.isEmpty()) {
                            val defaultSession = ChatSession(
                                title = "Discussion Principale",
                                messages = db.chatHistory.toMutableList()
                            )
                            db.chatSessions.add(defaultSession)
                            db.activeSessionId = defaultSession.id
                            db.chatHistory.clear()
                        }
                        if (db.chatSessions.isEmpty()) {
                            val newSession = ChatSession(title = "Nouvelle discussion")
                            db.chatSessions.add(newSession)
                            db.activeSessionId = newSession.id
                        }
                        if (db.activeSessionId == null && db.chatSessions.isNotEmpty()) {
                            db.activeSessionId = db.chatSessions.firstOrNull { !it.isDeleted }?.id
                        }
                        
                        // TEMPORARY: Clear trash on load
                        db.chatSessions.removeAll { it.isDeleted }
                        
                        _database.value = db
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            val db = AppDatabase()
            val newSession = ChatSession(title = "Discussion Principale")
            db.chatSessions.add(newSession)
            db.activeSessionId = newSession.id
            _database.value = db
        }
    }

    private fun saveDatabase() {
        try {
            FileWriter(dbFile).use { writer ->
                gson.toJson(_database.value, writer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateProfile(profile: Profile) {
        _database.value = _database.value.copy(profile = profile)
        saveDatabase()
    }

    fun addChatMessage(chatId: String, message: ChatMessage) {
        val currentDb = _database.value
        val sessions = currentDb.chatSessions.map { it.copy(messages = it.messages.toMutableList()) }.toMutableList()
        val index = sessions.indexOfFirst { it.id == chatId }
        if (index != -1) {
            sessions[index].messages.add(message)
            _database.value = currentDb.copy(chatSessions = sessions)
            saveDatabase()
        }
    }

    fun createChatSession(title: String): String {
        val currentDb = _database.value
        val sessions = currentDb.chatSessions.map { it.copy() }.toMutableList()
        val newSession = ChatSession(title = title)
        sessions.add(0, newSession)
        _database.value = currentDb.copy(chatSessions = sessions, activeSessionId = newSession.id)
        saveDatabase()
        return newSession.id
    }

    fun setActiveSession(chatId: String) {
        _database.value = _database.value.copy(activeSessionId = chatId)
        saveDatabase()
    }

    fun deleteChatSession(chatId: String) {
        val currentDb = _database.value
        val sessions = currentDb.chatSessions.map { it.copy() }.toMutableList()
        val index = sessions.indexOfFirst { it.id == chatId }
        if (index != -1) {
            sessions[index].isDeleted = true
            val newActive = if (currentDb.activeSessionId == chatId) {
                sessions.firstOrNull { !it.isDeleted }?.id ?: createChatSession("Nouvelle discussion")
            } else currentDb.activeSessionId
            
            _database.value = currentDb.copy(chatSessions = sessions, activeSessionId = newActive)
            saveDatabase()
        }
    }

    fun renameChatSession(chatId: String, newName: String) {
        val currentDb = _database.value
        val sessions = currentDb.chatSessions.map { it.copy() }.toMutableList()
        val index = sessions.indexOfFirst { it.id == chatId }
        if (index != -1) {
            sessions[index] = sessions[index].copy(title = newName)
            _database.value = currentDb.copy(chatSessions = sessions)
            saveDatabase()
        }
    }

    fun hardDeleteChatSession(chatId: String) {
        val currentDb = _database.value
        val sessions = currentDb.chatSessions.map { it.copy() }.toMutableList()
        val index = sessions.indexOfFirst { it.id == chatId }
        if (index != -1) {
            sessions.removeAt(index)
            val newActive = if (currentDb.activeSessionId == chatId) {
                sessions.firstOrNull { !it.isDeleted }?.id ?: createChatSession("Nouvelle discussion")
            } else currentDb.activeSessionId
            
            _database.value = currentDb.copy(chatSessions = sessions, activeSessionId = newActive)
            saveDatabase()
        }
    }

    fun restoreChatSession(chatId: String) {
        val currentDb = _database.value
        val sessions = currentDb.chatSessions.map { it.copy() }.toMutableList()
        val index = sessions.indexOfFirst { it.id == chatId }
        if (index != -1) {
            sessions[index].isDeleted = false
            _database.value = currentDb.copy(chatSessions = sessions, activeSessionId = chatId)
            saveDatabase()
        }
    }

    fun emptyTrash() {
        val currentDb = _database.value
        val sessions = currentDb.chatSessions.filter { !it.isDeleted }.map { it.copy() }.toMutableList()
        if (sessions.isEmpty()) {
            val newSession = ChatSession(title = "Nouvelle discussion")
            sessions.add(newSession)
        }
        val newActive = sessions.firstOrNull { it.id == currentDb.activeSessionId }?.id ?: sessions.first().id
        _database.value = currentDb.copy(chatSessions = sessions, activeSessionId = newActive)
        saveDatabase()
    }

    fun updateGlobalContext(content: String) {
        val currentDb = _database.value
        val contextList = currentDb.globalContext.toMutableList()
        contextList.add(GlobalContextEntry(content = content))
        _database.value = currentDb.copy(globalContext = contextList)
        saveDatabase()
    }

    fun addWorkout(workout: Workout) {
        saveUndoState()
        val newList = _database.value.workouts.toMutableList()
        newList.add(workout)
        _database.value = _database.value.copy(workouts = newList)
        saveDatabase()
    }
    
    fun editWorkout(workout: Workout) {
        saveUndoState()
        val newList = _database.value.workouts.toMutableList()
        val index = newList.indexOfFirst { it.id == workout.id }
        if (index != -1) {
            newList[index] = workout
            _database.value = _database.value.copy(workouts = newList)
            saveDatabase()
        }
    }

    fun removeLastChatMessage(chatId: String) {
        val currentDb = _database.value
        val sessions = currentDb.chatSessions.map { it.copy(messages = it.messages.toMutableList()) }.toMutableList()
        val index = sessions.indexOfFirst { it.id == chatId }
        if (index != -1 && sessions[index].messages.isNotEmpty()) {
            sessions[index].messages.removeLast()
            _database.value = currentDb.copy(chatSessions = sessions)
            saveDatabase()
        }
    }
    
    fun removeWorkout(workoutId: String) {
        saveUndoState()
        val newList = _database.value.workouts.toMutableList()
        newList.removeAll { it.id == workoutId }
        _database.value = _database.value.copy(workouts = newList)
        saveDatabase()
    }

    private fun saveUndoState() {
        undoHistory.push(_database.value.workouts.toList())
        if (undoHistory.size > 10) {
            undoHistory.removeElementAt(0)
        }
    }

    fun undoLastWorkoutAction() {
        if (undoHistory.isNotEmpty()) {
            val previousState = undoHistory.pop()
            _database.value = _database.value.copy(workouts = previousState.toMutableList())
            saveDatabase()
        }
    }

    fun exportDatabaseJson(): String {
        return gson.toJson(_database.value)
    }

    fun importDatabaseJson(jsonStr: String): Boolean {
        return try {
            val db = gson.fromJson(jsonStr, AppDatabase::class.java)
            if (db != null) {
                _database.value = db
                saveDatabase()
                true
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun removeLastChatMessage() {
        val newList = _database.value.chatHistory.toMutableList()
        if (newList.isNotEmpty()) {
            newList.removeAt(newList.size - 1)
            _database.value = _database.value.copy(chatHistory = newList)
            saveDatabase()
        }
    }

    // For Export
    fun exportToJson(): String {
        return gson.toJson(_database.value)
    }

    // For Import
    fun importFromJson(jsonString: String): Boolean {
        return try {
            val db = gson.fromJson(jsonString, AppDatabase::class.java)
            if (db != null) {
                _database.value = db
                saveDatabase()
                true
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
