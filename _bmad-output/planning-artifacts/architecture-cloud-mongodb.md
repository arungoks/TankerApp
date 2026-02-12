---
stepsCompleted: [1]
inputDocuments: ['planning-artifacts/architecture.md', 'planning-artifacts/prd.md']
workflowType: 'architecture'
project_name: 'TankerApp'
user_name: 'Arun'
date: '2026-02-12'
---

# Architecture Decision Document: Cloud Implementation (MongoDB Atlas)

_This document outlines the architectural decisions for the alternative Cloud-based implementation using MongoDB Atlas._

## 1. Initiative Context

The original TankerApp architecture focused on a **Local-Only** persistence strategy using Room Database.
This document explores and defines the architecture for a **Cloud-First** variant using MongoDB Atlas, enabling data synchronization, potential multi-user features, and cloud backup.

### Drivers for Change
- **Data Persistence:** Move from local device storage to Cloud.
- **Accessibility:** Enable potential multi-device access.
- **Reliability:** Cloud backup inherent in Atlas.

## 2. Project Context Analysis (Cloud Variant)

### Functional Adjustments
- **Persistence Layer:** Move from **Room (Local SQLite)** to **MongoDB Atlas Device SDK (Realm)**.
- **Data Synchronization:** Implement **Atlas Device Sync** to automatically replicate data between the local device and the MongoDB Atlas cloud.
- **Authentication:** Cloud sync requires an authenticated user session.
    - *Constraint:* Apps cannot sync anonymously without a distinct user ID.
    - *Decision Required:* Choice between Anonymous Auth (frictionless) or Email/Password (allows restore on new device).

### Non-Functional Requirements
- **Offline Capability:** **Critical**. The app must function 100% offline. Realm provides this out-of-the-box (local-first writes, background sync).
- **Data Safety:** Cloud provides automatic backup, mitigating the risk of device loss.
- **Performance:** Realm is highly performant for object graphs but requires different schema design patterns than SQL (e.g., embedding vs referencing).
- **Cost:** **MongoDB Atlas M0 Tier** (Free) is sufficient for the projected data volume (< 500MB).

### Scale & Complexity
- **Primary Domain:** Mobile (Android) + Backend-as-a-Service (Atlas App Services).
- **Complexity:** Moderate.
    - *Added Complexity:* Managing App Services configuration, Schema Rules, and Sync permissions.
    - *Reduced Complexity:* No manual backup/restore logic needed; it's automatic.

### Cross-Cutting Concerns
- **Network State Management:** UI must reflect 'Syncing', 'Offline', and 'Connected' states.
- **Conflict Resolution:** 'Last-Write-Wins' is the default and appropriate strategy for a single-caretaker scenario.

## 3. Technology Strategy

### Core Technology Stack (Cloud Variant)
- **Database (Cloud):** **MongoDB Atlas (M0 Shared Tier)**.
  - *Rationale:* Free tier provides sufficient storage (512MB) for thousands of billing cycles.
- **Database (Create):** **Atlas Device SDK (Realm Kotlin SDK)**.
  - *Rationale:* Native Kotlin SDK with Coroutines support replaces Room.
- **Sync Protocol:** **Atlas Flexible Sync**.
  - *Rationale:* Modern sync protocol that allows filtering data (e.g., syncing only the current user's data) without complex partitioning strategies.
- **Authentication:** **Email/Password**.
  - *Rationale:* Provides secure identity management and enables device switching/restoration without data loss.

### Authentication Flow
1. **Login Screen:** New entry point for the app.
2. **Registration:** Sign up with Email/Password.
3. **Session:** Long-lived session (refresh tokens handled by SDK).
4. **Logout:** Explicit logout clears local data (optional) or retains for next login.

## 4. Data Architecture (Realm Schema)

### Schema Design Principles
- **No Foreign Keys:** Relationships are direct object references or lists of objects.
- **Embedded Objects:** Use embedded objects for strictly owned data (e.g., BillingLineItem inside BillingReport).
- **Partitioning:** Data is scoped by owner_id to ensure users only see their own data.

### Data Models
- **User:**
  - _id: String (Atlas Auth ID) - Primary Key
  - email: String (Index)
  - created_at: Date

- **Apartment:**
  - _id: ObjectId - Primary Key
  - 
umber: String (Index)
  - owner_id: String (Index for Sync)
  - esidents: List<Resident> (Embedded) -> Name, Phone

- **Vacancy:**
  - _id: ObjectId - Primary Key
  - partment_id: ObjectId (Reference)
  - start_date: Date
  - end_date: Date (Optional)
  - owner_id: String (Index for Sync)

- **Tanker:**
  - _id: ObjectId - Primary Key
  - date: Date
  - supplier: String (Optional)
  - owner_id: String (Index for Sync)

- **BillingCycle:**
  - _id: ObjectId - Primary Key
  - start_date: Date
  - end_date: Date
  - is_active: Boolean
  - owner_id: String (Index for Sync)

## 5. Atlas App Services Authentication

### Workflow
1.  **Frontend (Android):** The app uses the **Realm Kotlin SDK** to interact with Atlas App Services.
2.  **User Action:** User enters Email/Password and clicks 'Register' or 'Login'.
3.  **SDK Call:** pp.emailPasswordAuth.registerUser(email, password) (Registration) or pp.login(Credentials.emailPassword(email, password)) (Login).
4.  **Backend (Atlas):** Atlas verifies credentials against its built-in User Management system.
5.  **Token Issuance:** Upon success, Atlas returns an **Access Token** and **Refresh Token**.
6.  **Session Management:** The SDK automatically caches these tokens securely. Subsequent app launches use pp.currentUser() to check for an existing valid session, bypassing the login screen.
7.  **Data Access:** All database queries are automatically scoped to the logged-in user.id based on Sync Rules defined in the Atlas Console.


## 6. Connection & Synchronization Mechanism

The connection between the Android App and MongoDB Atlas is **NOT** a standard REST API or JDBC connection. Instead, it uses **Atlas App Services** (formerly Realm) via a specialized SDK.

### The Connection Bridge: Atlas App Services
1.  **App ID**: You create an "App Service" in the MongoDB Atlas Dashboard. This generates a unique **App ID**.
2.  **SDK Initialization**: In `TankerApplication.kt`, we initialize the Realm SDK:
    ```kotlin
    val app = App.create("tankerapp-xxxxx")
    ```
3.  **Communication Protocol**: The SDK establishes a securely encrypted **WebSocket** connection to the Atlas cloud.
    -   *Why WebSocket?* It allows **Real-Time Synchronization**. Changes in the cloud are "pushed" to the device instantly.
4.  **Offline-First Networking**:
    -   If the internet is **ON**: The WebSocket stays open, syncing data immediately.
    -   If the internet is **OFF**: The SDK writes to the local database file. It queues these changes.
    -   **Reconnection**: As soon as the internet returns, the SDK detects it, reconnects the WebSocket, and uploads queued changes automatically.


## 7. Authentication Strategy: Anonymous Authentication

Given the requirement for **Waitless Access** (no login screen), we will use **Anonymous Authentication**.

### How it Works
1.  **Invisible Login**: When the user opens the app, the SDK silently calls `app.login(Credentials.anonymous())`.
2.  **Unique ID**: Atlas assigns a unique `user.id` to this installation.
3.  **Persistence**: The SDK stores this `user.id` securely on the device.
4.  **Automatic Re-Login**: On subsequent launches, the SDK reuses the stored `user.id`.

### Pros & Cons
-   **Pros**: **Zero Friction**. The user experience is identical to a local-only app.
-   **Cons**:
    -   **Data is tied to the Device**: If the user uninstalls the app or clears app data, the `user.id` is lost. The data remains in the cloud (orphaned) but the new installation gets a new ID and starts fresh.
    -   **No Restore**: Cannot restore data on a new phone.

### Mitigation Strategy
-   **Future Upgrade**: We can implement "Link Credentials" later. If the user decides to create an account, we can link their existing Anonymous ID to an Email/Password, preserving their data.


## 7. Authentication Strategy: Hardcoded Credentials (Single-Tenant Mode)

To meet the requirement of **"No User Authentication UI"** while ensuring persistent data access across re-installations, we will use a **Single Hardcoded Account** strategy.

### Strategy Overview
Instead of asking the user to log in, the app will automatically log in using a pre-defined Email/Password embedded in the app code or configuration.

-   **Credentials**: A dedicated "service account" (e.g., `caretaker@tankerapp.local`) with a fixed password.
-   **Mechanism**: On app launch, the SDK executes `app.login(Credentials.emailPassword("fixed_email", "fixed_password"))`.
-   **Result**: All installations of the app (on Ramesh's phone, or if he switches phones) connect to the **SAME** Atlas User ID.
-   **Data Persistence**: Since the User ID is constant, data is preserved even if the app is uninstalled and reinstalled.

### Security Implications (Acceptable Risk)
-   **Risk**: If someone decompiles the APK, they can extract the credentials.
-   **Mitigation**: Given this is a private utility app for a specific building with non-sensitive data (water tanker counts), this risk is acceptable.
-   **Isolation**: We can restrict this user's permissions in Atlas to only read/write their own partition.

### Implementation Logic
1.  **Check Current User**: `if (app.currentUser() != null)` -> Proceed to Home.
2.  **Auto-Login**: `else` -> Call `app.login(hardcodedCredentials)`.
    -   *Success*: Proceed to Home.
    -   *Failure*: Retry or show generic "Sync Error".

This approach combines the "Zero Friction" of Anonymous Auth with the "Data Persistence" of Email/Password Auth.


## 8. Deployment and Setup Steps

To activate this Cloud Architecture, follow these manual setup steps in the **MongoDB Atlas Console**.

### Phase 1: Create the Cluster
1.  **Sign Up/Login**: Go to [MongoDB Atlas](https://www.mongodb.com/cloud/atlas).
2.  **Create Project**: Name it "TankerApp".
3.  **Build a Database**:
    -   Select **M0 Sandbox (Shared)** (Free Tier).
    -   Provider: **AWS**.
    -   Region: Closest to you (e.g., `us-east-1` or `ap-south-1`).
    -   Cluster Name: `Cluster0` (Default).
4.  **Create Database User**:
    -   Username: `admin`.
    -   Password: [Secure Password].
    -   *Note: This is for DB Administration, NOT for the App.*
5.  **Network Access**: Add `0.0.0.0/0` (Allow Access from Anywhere) to the IP Access List.

### Phase 2: Configure App Services (Realm)
1.  **App Services Tab**: Click "App Services" in the top navigation.
2.  **Create New App**:
    -   App Name: `TankerApp-Service`.
    -   Location: `Virginia (us-east-1)` or closest available.
    -   Link to Cluster: Select `Cluster0`.
3.  **Authentication**:
    -   Go to **Authentication** -> **Providers**.
    -   Select **Email/Password**.
    -   Enable "Email Confirmation": **OFF** (Keep it simple).
    -   Enable "Password Reset": **OFF**.
    -   **Click SAVE**.
4.  **Create Service User**:
    -   Go to **App Users**.
    -   Click **Add New User**.
    -   Email: `caretaker@tankerapp.local` (This is the hardcoded email).
    -   Password: `TankerApp2026!` (This is the hardcoded password).
    -   **Create User**.

### Phase 3: Enable Device Sync
1.  **Device Sync**: In the sidebar navigate to **Build** -> **Device Sync**.
2.  **Enable Sync**: Click "Start Sync".
    -   Flexible Sync: **YES**.
    -   Development Mode: **ON** (To allow schema changes from device).
    -   Database Name: `tankerdb`.
    -   Queryable Fields: Add `owner_id`.
3.  **Permissions**:
    -   Define a permission role that allows read/write access based on `owner_id`.
    -   *Example Rule:* `{ "owner_id": "%%user.id" }`.

### Phase 4: Connect Android App
1.  **Get App ID**: In the App Services Dashboard, copy the App ID (e.g., `tankerapp-service-xxxxx`).
2.  **Update Application Code**:
    -   Open `TankerApplication.kt`.
    -   Replace placeholder `YOUR_APP_ID` with the copied ID.
    -   Update hardcoded credentials with the email/password created in Phase 2.
3.  **Build & Run**: Launch the app. It should authenticate automatically and create the schema in the cloud.


## 9. Next Steps

- [ ] Create TankerApp project in MongoDB Atlas.
- [ ] Set up App Services and Device Sync.
- [ ] Implement Realm SDK in Android App.

