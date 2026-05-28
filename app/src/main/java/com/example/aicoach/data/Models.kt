package com.example.aicoach.data

import com.google.gson.annotations.SerializedName

data class Profile(
    var age: Int = 0,
    var height: Int = 0,
    var weight: Int = 0,
    var gender: String = "",
    var apiKey: String = "",
    var provider: String = "gemini", // "gemini" or "openai"
    var modelName: String = "gemini-2.5-flash"
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imagePaths: List<String> = emptyList(), // Support for multiple images
    val isError: Boolean = false
)

data class Workout(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val sportType: String = "Course",
    val distanceKm: Double? = 0.0,
    val durationMinutes: Double? = 0.0,
    val pace: String? = "",
    val speedKmh: Double? = null,
    val heartRateAvg: Int? = 0,
    val heartRateMax: Int? = 0,
    val calories: Int? = 0,
    val altitude: Int? = null,
    val elevationGain: Int? = 0,
    val mainWorkoutDistanceKm: Double? = null,
    val cadence: Int? = 0,
    val cadenceMax: Int? = null,
    val symmetry: String? = "",
    val contactTimeMs: Int? = null,
    val flightTimeMs: Int? = null,
    val regularity: String? = null,
    val verticalOscillation: String? = null,
    val laps: String? = "",
    val speedMaxKmh: Double? = null,
    val steps: Int? = null,
    val elevationMin: Int? = null,
    val elevationMax: Int? = null,
    val trainingScore: Int? = null,
    val rpeScore: Int? = null,
    val sweatLossMl: Int? = null,
    val stiffness: String? = null,
    val weather: String? = null,
    val date: Long = System.currentTimeMillis(), // For calendar
    val timestamp: Long = System.currentTimeMillis(),
    val imagePaths: List<String> = emptyList(),
    val aiNotes: String = ""
)

data class GlobalContextEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val content: String
)

data class ChatSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    var title: String = "Nouvelle discussion",
    var messages: MutableList<ChatMessage> = mutableListOf(),
    var isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class AppDatabase(
    var profile: Profile = Profile(),
    var chatHistory: MutableList<ChatMessage> = mutableListOf(), // legacy
    var chatSessions: MutableList<ChatSession> = mutableListOf(),
    var activeSessionId: String? = null,
    var globalContext: MutableList<GlobalContextEntry> = mutableListOf(),
    var workouts: MutableList<Workout> = mutableListOf()
)
