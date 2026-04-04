# TankerApp (v3.4)

**TankerApp** is a native Android application designed to streamline water tanker tracking and billing for apartment complexes. It replaces error-prone paper records with a simple, digital tool that helps caretakers track usage, manage vacancies, and generate fair, occupancy-based billing reports.

## 🚀 Features

*   **Calendar Interface**: Intuitive, calendar-based logging for tanker deliveries and apartment vacancies.
*   **Smart Tracking**: Automatically counts tanker deliveries (e.g., 0/8) and notifies the user when a billing cycle is complete.
*   **Vacancy & Occupancy Management**: Easily mark apartments as vacant or update occupancy counts for specific dates.
*   **Configurable Pricing**: Tanker costs are configurable via `assets/config.properties`, allowing easy updates without code changes.
*   **Automated Billing**: Generates instant, accurate billing reports based on daily occupancy and per-head costs.
*   **Detailed Reporting**: Export billing reports as **CSV** with a detailed day-wise breakdown of occupancy and costs per apartment.
*   **History View**: Access a log of past billing cycles and reports.
*   **Cloud Sync**: All data is synced across devices using Firebase Firestore, with robust offline support.
*   **Centralized Management**: Manage apartment lists and occupancy data in a shared cloud database.

## 🛠️ Technology Stack

*   **Language**: [Kotlin](https://kotlinlang.org/)
*   **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
*   **Database**: [Room](https://developer.android.com/training/data-storage/room) (SQLite)
*   **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
*   **Architecture**: MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF)
*   **Asynchrony**: Kotlin Coroutines & Flow
*   **Backend / Cloud**: [Firebase](https://firebase.google.com/) (Firestore & Auth)

## 📂 Project Structure

The Android project is located in the `TankerApp/` directory.

```text
TankerApp/app/src/main/java/com/arun/tankerapp/
├── core/
│   ├── data/           # Database entities, DAOs, and Repositories
│   ├── designsystem/   # Theme, Color, Type, and shared UI components
│   └── util/           # Utility classes (PDF Generator, Date Extensions)
├── di/                 # Hilt Dependency Injection modules
├── feature/            # Feature-based packages
│   ├── calendar/       # Calendar screen and logic
│   ├── report/         # Report generation screen and logic
|   └── history/        # History screen and logic
└── ui/                 # App entry point and Navigation
```

## ⚡ Getting Started

### Prerequisites
*   Android Studio Ladybug or newer.
*   JDK 17 or higher.

### Installation

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/arungoks/TankerApp.git
    cd TankerApp
    ```

2.  **Open in Android Studio**:
    *   Launch Android Studio.
    *   Select **Open** and navigate to the `TankerApp/TankerApp` directory (the one containing `build.gradle.kts`).

3.  **Build and Run**:
    *   Let Gradle sync the project dependencies.
    *   Connect an Android device or start an emulator (API Level 26+).
    *   Click the **Run** button (green arrow) in Android Studio.

## 📝 Usage

1.  **Setup**: On first launch, the app initializes with a master list of apartments.
2.  **Log Activity**: Tap a date on the calendar to mark a tanker delivery or select apartments that are vacant.
3.  **Monitor Usage**: Watch the tanker counter increment.
4.  **Generate Report**: When the cycle completes (e.g., 8 tankers), click "Generate Report" to view and share the billing details.

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
