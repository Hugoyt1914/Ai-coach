# AICoach 🏃‍♂️🧠

AICoach is an intelligent, privacy-first personal training assistant built for Android. Unlike standard tracking apps, AICoach leverages the power of Large Language Models (LLMs) to automatically understand, extract, and manage your workout data directly from natural conversation or screenshots.

![AICoach App](https://img.shields.io/badge/Platform-Android-green.svg)
![Status](https://img.shields.io/badge/Status-Beta-orange.svg)

## ✨ Features

- 💬 **Conversational Tracking:** Just tell the AI what you did today (e.g., "I ran 5km in 25 minutes") or drop a screenshot of your smartwatch/Strava app. The AI will automatically extract the data and add the workout to your calendar.
- 🔥 **Gamification & Streaks:** Keep your motivation high! If you stay inactive for more than 84 hours (3.5 days), your 🔥 streak drops to zero. A dynamic countdown keeps you aware of your deadline.
- 📅 **Smart Calendar:** A minimalist, clear view of your month. View all your running 🏃 and cycling 🚲 activities at a glance.
- 🧠 **Global Context Memory:** The AI remembers your goals, your weight, your age, and your past performance. It automatically generates and updates a "Global Context" every 10 interactions to ensure it never loses track of who you are.
- 🗑️ **Multi-Chat & Trash System:** Create multiple discussion threads. Deleted chats are moved to a dedicated Trash bin where they are excluded from the AI's memory, giving you full control over your data.
- 🔑 **Bring Your Own Key (BYOK):** Absolute privacy. Connect your own Gemini or OpenAI API key. Your data never passes through a middleman server.

## 🚀 Download & Installation

You can download the latest Android APK directly from the `release` folder.

1. Go to the [release/](release/) folder in this repository.
2. Download `AICoach.apk`.
3. Transfer it to your Android device (or download it directly from your phone).
4. Open the APK. Your phone may ask you to enable "Install from unknown sources."
5. Enjoy your new AI Coach!

## ⚙️ Configuration

When you first launch the app:
1. Navigate to the **Profile** tab (bottom right).
2. Enter your physical details (Age, Height, Weight).
3. Insert your **Gemini** or **OpenAI API Key**.
   - *Don't have one? You can get a free Gemini API key from [Google AI Studio](https://aistudio.google.com/).*
4. Save your profile. You are ready to chat!

## 🛠️ Built With
- **Kotlin** & **Jetpack Compose** (100% Modern UI Toolkit)
- **Coroutines & Flow** for asynchronous logic
- **Gson** for local JSON storage

## 🤝 Contributing
Feel free to open an issue or submit a pull request if you want to add new features, support new sports, or improve the AI prompting logic.

---
*Created with ❤️ for runners and cyclists who want smarter tracking.*
