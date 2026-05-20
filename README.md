# InfoMate v12.4
### Powered by Socrates Kipruto

**InfoMate v12.4** is a premium, personal AI companion designed to provide a seamless, context-aware experience. Built with a futuristic "Living UI" and a robust neural backend, InfoMate goes beyond a standard chatbot to become a true extension of your digital workflow.

## 🚀 Key Features

-   **Living UI**: A synchronized "Iris" and Voice Spectrum that reacts in real-time to AI speech and user input.
-   **Voice-First Interaction**: Full support for "Hey InfoMate" wake-word activation, high-quality Text-to-Speech (TTS), and Speech-to-Text (STT).
-   **Neural Memory (RAG)**: Long-term memory storage using 768-dimension vector embeddings (Google Gemini optimized) stored in Supabase.
-   **Contextual Awareness**: Real-time access to device status (Battery, Time) and deep personalization through phone data pattern analysis (Calendar, Contacts, SMS, and Call Logs).
-   **Premium Aesthetics**: A "Gold Standard" interface featuring glassmorphism, ambient radial glows, and high-fidelity haptic feedback.
-   **Secure Backend**: Comprehensive Supabase schema with Row Level Security (RLS) to protect your personal data.

## 🛠️ Technical Stack

-   **Frontend**: Jetpack Compose, Material 3, Kotlin Coroutines & Flow.
-   **AI Engine**: Agent Orchestration with Reasoning Streams.
-   **Backend**: Supabase (PostgreSQL with pgvector).
-   **Voice**: Android TTS Engine & SpeechRecognizer API.
-   **Haptics**: Advanced VibrationEffect integration.

## 📂 Project Structure

-   `/app`: The main Android application module.
-   `/supabase`: Backend SQL schemas and configurations.
-   `.github/workflows`: Automated build and CI/CD pipelines.

## 🏗️ Installation & Setup

1.  **Clone the Repo**:
    ```bash
    git clone https://github.com/Socky254/InfoMate.git
    ```
2.  **Database Configuration**:
    -   Run `infomate_v9_master_schema.sql` in your Supabase SQL Editor.
    -   Run `backend_fix.sql` to ensure optimal performance.
3.  **App Configuration**:
    -   Update `Config.kt` with your Supabase URL and API Keys.
4.  **Build**:
    -   Open in Android Studio and sync with Gradle.
    -   Run `./gradlew assembleDebug` to generate an APK.

---
*Created and Maintained by Socrates Kipruto*
