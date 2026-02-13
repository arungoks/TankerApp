---
stepsCompleted: [1, 2, 3, 4, 5, 6, 7, 8, 9]
inputDocuments: ['planning-artifacts/architecture.md', 'planning-artifacts/prd.md']
workflowType: 'architecture'
project_name: 'TankerApp'
user_name: 'Arun'
date: '2026-02-13'
---

# Architecture Decision Document: Cloud Implementation (MongoDB Atlas - Direct)

_This document outlines the architectural decisions for the Cloud-based implementation using MongoDB Atlas via Direct Connection (due to App Services deprecation)._

## 1. Initiative Context

The original TankerApp architecture focused on a **Local-Only** persistence strategy using Room Database.
This document explores and defines the architecture for a **Cloud-First** variant using MongoDB Atlas via Kotlin Driver.

### Drivers for Change
- **Data Persistence:** Move from local device storage to Cloud.
- **Accessibility:** Enable potential multi-device access.
- **Reliability:** Cloud backup inherent in Atlas.
- **Constraint:** **Atlas App Services (Realm) is deprecated** (as of 2026). We must use a direct driver connection.

## 2. Project Context Analysis (Cloud Variant)

### Functional Adjustments
- **Persistence Layer:** Move from **Room (Local SQLite)** to **MongoDB Kotlin Driver (Coroutines)**.
- **Connection Model:** Direct TCP connection to MongoDB Atlas Cluster.
- **Offline Capability:** **LOST/LIMITED**. Unlike Realm, the standard driver is "Online-First".
  - *Decision:* The app will primarily require an internet connection to function.
  - *Mitigation:* We can implement a basic caching layer, but true offline-first sync is out of scope for MVP without Realm.
- **Authentication:** Direct connection using Connection String + SCRAM (Username/Password).

### Non-Functional Requirements
- **Performance:** Direct queries to cloud. Latency covers network RTT.
- **Security:** Connection String embedded in app (obfuscated).
  - *Risk Acceptance:* Low-risk utility app; acceptable for MVP.
- **Cost:** **MongoDB Atlas M0 Tier** (Free) is sufficient.

## 3. Technology Strategy

### Core Technology Stack (Cloud Variant)
- **Database (Cloud):** **MongoDB Atlas (M0 Shared Tier)**.
- **Client Driver:** **MongoDB Kotlin Driver (Sync or Coroutine)**.
  - *Rationale:* Official driver for Kotlin, allows standard MongoDB Query Language (MQL).
- **Authentication:** **SCRAM (Username/Password)** via Connection String.
- **Sync Protocol:** **None** (Direct CRUD).

### Authentication Flow (Hardcoded)
1.  **Startup:** App initializes `MongoClient` with the hardcoded connection string.
2.  **Session:** The connection pool manages connectivity.
3.  **No Login Screen:** User sees the home screen immediately.

## 4. Data Architecture (BSON Schema)

Data is stored as BSON documents. We will use Kotlin Data Classes with `@BsonId` annotations.

### Data Models
- **Apartment:**
  - `_id`: ObjectId
  - `number`: String
  - `residents`: List<Resident> (Embedded)

- **Vacancy:**
  - `_id`: ObjectId
  - `apartment_id`: ObjectId (Reference)
  - `start_date`: String (ISO)
  - `end_date`: String (ISO)

- **Tanker:**
  - `_id`: ObjectId
  - `date`: String (ISO)

- **BillingCycle:**
  - `_id`: ObjectId
  - `start_date`: String
  - `end_date`: String

## 5. Security & Connection

### Connection String
The app connects using a Direct Connection URI:
`mongodb+srv://admin:ostraadmin@cluster0.ajbviox.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0`

-   **Cluster**: `Cluster0` (cluster0.ajbviox.mongodb.net)
-   **User**: `admin`
-   **Password**: `ostraadmin`
-   **Database**: `tanker_db` (Proposed)

*Security Note:* This connection string grants full admin access. In a production app, we would use a restricted user, but for this MVP, we proceed with direct admin access.

## 6. Deployment and Setup Steps

To activate this Cloud Architecture, follow these manual setup steps in the **MongoDB Atlas Console**.

### Phase 1: Create the Cluster
1.  **Sign Up/Login**: Go to [MongoDB Atlas](https://www.mongodb.com/cloud/atlas).
2.  **Create Project**: Name it "TankerApp".
3.  **Build a Database**:
    -   Select **M0 Sandbox (Shared)** (Free Tier).
    -   Provider: **AWS**.
    -   Region: Closest to you.
    -   Cluster Name: `Cluster0`.

### Phase 2: Create Database User
1.  **Database Access**: Go to **Security** -> **Database Access**.
2.  **Add New Database User**:
    -   Authentication Method: **Password**.
    -   Username: `tanker_user` (This will be hardcoded).
    -   Password: `TankerApp2026!` (This will be hardcoded).
    -   **Privileges**: Read and write to any database (or scope to `tanker_db`).
    -   **Add User**.

### Phase 3: Network Access
1.  **Network Access**: Go to **Security** -> **Network Access**.
2.  **Add IP Address**:
    -   Button: **Allow Access from Anywhere**.
    -   Entry: `0.0.0.0/0` (includes 1 week expiration option? Select "Permanent" if available, or remember to refresh).
    -   *Why?* Mobile phones change IPs constantly; we cannot whitelist specific IPs.

### Phase 4: Get Connection String
1.  **Connect**: Go to **Database** -> **Connect**.
2.  **Driver**: Select **Drivers**.
3.  **Selection**: Include full driver code example? Select **Kotlin (Coroutines)**.
4.  **Copy**: Copy the connection string.
    -   `mongodb+srv://tanker_user:TankerApp2026!@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority&appName=TankerApp`

### Phase 5: Android Implementation
1.  **Dependencies**: Add `org.mongodb:mongodb-driver-kotlin-coroutine` to `build.gradle.kts`.
2.  **Code**:
    ```kotlin
    val connectionString = ConnectionString("mongodb+srv://tanker_user:TankerApp2026!@cluster0.xxxxx.mongodb.net/...")
    val mongoClient = MongoClient.create(connectionString)
    val database = mongoClient.getDatabase("tanker_db")
    // usage: database.getCollection<Tanker>("tankers").insertOne(...)
    ```

## 7. Next Steps

- [ ] Create TankerApp project in MongoDB Atlas.
- [ ] Create Database User & Whitelist IP `0.0.0.0/0`.
- [ ] Get Connection String.
- [ ] Update Android `build.gradle.kts` with Mongo Driver.
- [ ] Implement `MongoRepository` replacing `RoomRepository`.
