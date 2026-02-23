# College Bus Tracker - Project Documentation

## 📋 Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Technology Stack](#technology-stack)
3. [Project Structure](#project-structure)
4. [Database Schema](#database-schema)
5. [Directory & File Breakdown](#directory--file-breakdown)
6. [API Endpoints](#api-endpoints)
7. [Frontend Overview](#frontend-overview)

---

## Architecture Overview

**Architecture Pattern**: 3-Tier Layered Architecture with WebSocket Real-time Communication

### Architecture Layers:
1. **Presentation Layer** (Controllers) - REST API endpoints + WebSocket handlers
2. **Business Logic Layer** (Services) - Core business operations, caching, scheduling
3. **Data Access Layer** (Repositories) - Database operations via Spring Data JPA
4. **Entity/Model Layer** - Database entities with JPA annotations
5. **DTO Layer** - Data transfer objects for API requests/responses

### Key Design Decisions:
- **Real-time Updates**: WebSocket (STOMP) for live bus location broadcasting to students
- **Performance Optimization**: In-memory caching (`ConcurrentHashMap`) for active bus locations to reduce DB queries
- **Batch Processing**: Scheduled task that periodically flushes pending locations to database
- **Security**: BCrypt password encryption for admins, phone-number-based driver authentication
- **CORS Support**: Enabled for frontend integration across different origins

---

## Technology Stack

| Component | Technology |
|-----------|-----------|
| **Framework** | Spring Boot 4.0.2 |
| **Language** | Java 17 |
| **Database** | MySQL with Hibernate JPA |
| **Real-time** | WebSocket (STOMP) with SockJS |
| **Security** | Spring Security + BCrypt |
| **Build Tool** | Maven |
| **Utilities** | Lombok (for boilerplate reduction) |
| **Connection Pool** | HikariCP |

---

## Project Structure

```
college_bus_tracker/
├── src/
│   ├── main/
│   │   ├── java/com/college/bustracker/
│   │   │   ├── CollegeBusTrackerApplication.java      # Spring Boot entry point
│   │   │   ├── config/                                 # Configuration classes
│   │   │   ├── controller/                             # REST API controllers
│   │   │   ├── service/                                # Business logic services
│   │   │   ├── entity/                                 # JPA entities (database models)
│   │   │   ├── repository/                             # Data access layer (JPA repos)
│   │   │   └── dto/                                    # Data transfer objects
│   │   └── resources/
│   │       ├── application.properties                  # Spring configuration
│   │       └── static/                                 # Frontend HTML/CSS/JS
│   └── test/
│       └── CollegeBusTrackerApplicationTests.java      # Unit tests
├── pom.xml                                              # Maven dependencies
├── Dockerfile                                           # Container configuration
└── README.md                                            # Project readme
```

---

## Database Schema

### Entity Relationships:
```
Admin (1) ──────────┐
                    └──> Admin.createdBy (self-referential)

Bus (1) ──────────────> (n) Assignment

Driver (1) ────────────> (n) Assignment

Assignment (1) ────────> (n) Location
```

### Tables:

| Table | Purpose |
|-------|---------|
| **admins** | Admin users with role-based access control |
| **buses** | Available buses in the system (e.g., "Boys 1", "Girls 2") |
| **drivers** | Drivers registered in the system (phone-based authentication) |
| **assignments** | Maps driver + bus during an active tracking session |
| **locations** | GPS coordinates history for each assignment (indexed for fast queries) |

---

## Directory & File Breakdown

### 🔧 Config Directory (`config/`)

#### **CorsConfig.java**
Enables Cross-Origin Resource Sharing for development. Allows frontend (any origin) to call backend APIs. Configured to allow GET, POST, PUT, DELETE, OPTIONS, PATCH with any headers.

#### **SecurityConfig.java**
Spring Security configuration. Disables CSRF (for API calls), enables CORS, permits all requests (development setup), disables default login form. Provides BCryptPasswordEncoder bean for admin password hashing.

#### **WebSocketConfig.java**
Configures STOMP WebSocket endpoints at `/ws` with SockJS fallback. Sets up message broker with `/topic` prefix for broadcasting and `/app` for incoming client messages. Enables real-time location updates.

#### **SchedulingConfig.java**
Enables Spring's `@Scheduled` annotation support. Allows background tasks (like periodic DB flush) to execute at configured intervals.

#### **DataInitializer.java**
Runs at application startup. Creates default super-admin (`nitcadmin/nitc@123`) and 6 default buses (`Boys 1/2`, `Girls 1/2`, `Day Scholar 1/2`) if not present.

---

### 📦 Entity Directory (`entity/`)

#### **Bus.java**
Represents a bus in the system. Fields: `id` (PK), `busName` (unique), `createdAt`. Used to track which buses are available for driver assignment.

#### **Driver.java**
Represents a driver. Fields: `id` (PK), `phoneNumber` (unique, login identifier), `name`, `createdAt`. Drivers authenticate via phone number (no password).

#### **Admin.java**
Represents admin users. Fields: `id`, `username` (unique), `password` (encrypted), `name`, `createdBy` (self-referential for audit trail), `createdAt`. Admins manage buses and drivers.

#### **Assignment.java**
Represents an active tracking session (driver + bus + timeline). Fields: `id`, `bus`, `driver`, `startedAt`, `endedAt`, `isActive` (Boolean flag). One assignment per active bus tracking.

#### **Location.java**
GPS coordinate record. Fields: `id`, `assignment` (FK), `latitude`, `longitude`, `timestamp`. Indexed on `(assignment_id, timestamp)` for efficient historical queries.

---

### 🎮 Controller Directory (`controller/`)

#### **AdminController.java**
REST endpoints for admin operations. Routes: `/api/admin/login` (POST), `/api/admin/admins` (GET/POST/DELETE), `/api/admin/buses` (GET/POST/DELETE), `/api/admin/drivers` (GET/POST/DELETE). Returns JSON responses with success/error messages.

#### **DriverController.java**
REST endpoints for driver operations. Routes: `/api/driver/login` (POST), `/api/driver/buses/available` (GET), `/api/driver/start-tracking` (POST), `/api/driver/stop-tracking` (POST). Handles driver authentication and tracking lifecycle.

#### **LocationController.java**
Hybrid REST + WebSocket controller. WebSocket handler `@MessageMapping("/location")` receives GPS updates from connected drivers. REST endpoints: `/bus/{busId}/location` (GET), `/buses/locations` (GET) for students to fetch current bus positions.

#### **BusController.java**
REST endpoint `/api/buses` (GET) for fetching all buses. Used by student UI to display available buses. Minimal functionality as bus management is in AdminController.

---

### 🔄 Service Directory (`service/`)

#### **BusService.java**
Business logic for bus operations. Methods: `getAllBuses()`, `getAvailableBuses()` (excludes buses in active tracking), `addBus()`, `deleteBus()` (prevents deletion of actively tracked buses). Returns DTOs for API responses.

#### **DriverService.java**
Handles driver authentication and management. `login()` validates phone number (simple, no password), `getAllDrivers()`, `addDriver()`, `deleteDriver()`. Uses repository to check if driver exists in system (authorization gate).

#### **AdminService.java**
Admin operations: `login()` (username + password with BCrypt validation), `getAllAdmins()`, `addAdmin()` (tracks who created it), `deleteAdmin()` (prevents self-deletion and non-creators from deleting others).

#### **AssignmentService.java**
Core tracking logic. `startTracking()` creates assignment record and sets bus as active, `stopTracking()` marks assignment as inactive. Manages the lifecycle of driver-bus tracking sessions.

#### **LocationService.java**
Most complex service. Maintains in-memory cache (`activeLocations`) of current bus positions for O(1) student queries. WebSocket updates go to RAM first, then `pendingLocations` queue. Scheduled task (`@Scheduled`) periodically flushes queue to database in batch. Methods: `updateLocationAndGetBroadcast()` (cache + queue update), `getCurrentLocation()` (read from cache), `getAllBusLocations()`, scheduled `flushPendingLocationsToDatabase()`.

---

### 💾 Repository Directory (`repository/`)

#### **BusRepository.java**
JPA repository for Bus entity. Methods: `findByBusName()`, `existsByBusName()`. Extends `JpaRepository` for basic CRUD + custom queries.

#### **DriverRepository.java**
JPA repository for Driver. Methods: `findByPhoneNumber()`, `existsByPhoneNumber()`. Used for driver login validation and uniqueness checks.

#### **AdminRepository.java**
JPA repository for Admin. Methods: `findByUsername()`, `existsByUsername()`. Used for admin login and preventing duplicate usernames.

#### **AssignmentRepository.java**
JPA repository for Assignment. Methods: `findByBusIdAndIsActiveTrue()`, `findActiveAssignmentByBusId()`. Queries for checking bus availability and fetching active tracking sessions.

#### **LocationRepository.java**
JPA repository for Location. Methods: `findLatestByAssignmentId()` (optimized query for last known position). High-volume table (receives frequent GPS updates).

---

### 📮 DTO Directory (`dto/`)

**Data Transfer Objects** for API contracts (request/response payloads):

| DTO | Purpose |
|-----|---------|
| **AdminDTO** | Transfer admin data: `id`, `username`, `name` |
| **AdminLoginRequestDTO** | Login payload: `username`, `password` |
| **AdminLoginResponseDTO** | Login response: `success`, `message`, `adminId` |
| **BusDTO** | Bus data: `id`, `busName` |
| **BusLocationResponseDTO** | Current bus position: `busId`, `busName`, `latitude`, `longitude`, `timestamp`, `hasLocation` |
| **BusSelectionDTO** | Driver selecting bus: `driverId`, `busId` |
| **DriverDTO** | Driver data: `id`, `phoneNumber`, `name` |
| **DriverLoginRequestDTO** | Driver login payload: `phoneNumber` |
| **DriverLoginResponseDTO** | Driver login response: `success`, `message`, `driverId` |
| **LocationDTO** | GPS update payload: `assignmentId`, `latitude`, `longitude`, `timestamp` |
| **LocationBroadcastDTO** | WebSocket broadcast message: `busId`, `busName`, `latitude`, `longitude`, `timestamp` |
| **AssignmentDTO** | Assignment data: `id`, `busId`, `driverId`, `startedAt`, `endedAt`, `isActive` |
| **ApiResponseDTO** | Generic response: `success`, `message`, `data` |

---

## API Endpoints

### Admin APIs (`/api/admin`)
```
POST   /login                    Login with username + password
GET    /admins                   Fetch all admins
POST   /admins                   Create new admin (requires createdBy ID)
DELETE /admins/{id}              Delete admin (requires current admin ID)
GET    /buses                    List all buses
POST   /buses                    Add new bus
DELETE /buses/{id}               Remove bus
GET    /drivers                  List all drivers
POST   /drivers                  Register new driver
DELETE /drivers/{id}             Remove driver
```

### Driver APIs (`/api/driver`)
```
POST   /login                    Login with phone number only
GET    /buses/available          Get buses not currently tracked
POST   /start-tracking           Begin tracking (select bus)
POST   /stop-tracking            End tracking session
```

### Location APIs
```
GET    /bus/{busId}/location     Get current position of a bus (REST)
GET    /buses/locations          Get all active bus locations
WS     /ws                       WebSocket connection for real-time updates
```

### Public APIs (`/api/buses`)
```
GET    /                         Fetch all buses (student view)
```

---

## Frontend Overview

### Static Files (`src/main/resources/static/`)

#### **index.html**
Student bus tracking interface. Displays list of buses, real-time bus positions via WebSocket, map/coordinates display.

#### **admin.html**
Admin dashboard. CRUD operations for admins, buses, drivers. User management and system configuration UI.

#### **driver.html**
Driver interface. Login screen, select bus to track, start/stop tracking buttons, GPS location transmission.

#### **CSS Styling**
- `style.css` - Global styles (reset, typography, layout)
- `admin.css` - Admin-specific styling (forms, tables, dashboard)
- `driver.css` - Driver interface styling (tracking UI, buttons)

#### **JavaScript Logic**
- `student.js` - WebSocket client for live location updates, map integration
- `admin.js` - CRUD forms, API calls for admin management
- `driver.js` - Location tracking (Geolocation API), WebSocket connection, tracking lifecycle

---

## Key Features Summary

| Feature | Description |
|---------|-------------|
| **Real-time Tracking** | WebSocket pushes bus locations to all connected students instantly |
| **Driver Authentication** | Phone-number based (simple, pre-registered by admin) |
| **Admin Panel** | Create/manage admins, buses, and drivers |
| **Performance** | In-memory caching + batch DB writes reduce latency and load |
| **Scalability** | Threadsafe collections (ConcurrentHashMap, CopyOnWriteArrayList) for concurrent updates |
| **Data Persistence** | MySQL with Hibernate ORM, indexed Location queries |
| **Security** | CORS enabled, BCrypt hashed admin passwords, CSRF disabled for APIs |

---

## Deployment Info

**Docker Support**: Dockerfile included for containerization.

**Build**: `mvn clean package` (creates JAR in target/)

**Run**: `java -jar college_bus_tracker-0.0.1-SNAPSHOT.jar`

**Database**: Requires MySQL instance (configured via environment variables in `application.properties`)

---

## Notes for Developers

1. **WebSocket Debugging**: Connect to `/ws` endpoint using any STOMP client to test real-time updates.
2. **Location Flush**: Default scheduled interval is configurable (check `LocationService.flushPendingLocationsToDatabase()` for @Scheduled cron).
3. **Admin Creation**: Super admin (`nitcadmin`) auto-created on startup if not present.
4. **Driver Authentication**: Drivers don't have passwords; phone verification happens server-side (not OTP, just registration check).
5. **Assignment Cache**: Improves location update performance; cleared when driver stops tracking.

---

**Created**: February 2026 | **Java Version**: 17 | **Spring Boot**: 4.0.2

