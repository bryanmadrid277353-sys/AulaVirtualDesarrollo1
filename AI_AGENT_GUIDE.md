# 🤖 AI Agent & Developer Architecture Guide: Aula Virtual (Android / FastAPI)

This guide provides a structured technical overview of the **Aula Virtual** Android application and backend architecture to optimize context efficiency and navigation for AI agents and developers.

---

## 🏗️ Tech Stack & Architecture

- **Pattern:** MVVM (Model-View-ViewModel) with Android Architecture Components (`ViewModel`, `LiveData`).
- **Networking:** Retrofit 2 + Gson (`ApiService.java`, `RetrofitClient.java`).
- **Local Persistence:** Room Database (`AppDatabase.java`, DAOs, Entities).
- **Navigation:** Jetpack Navigation Component (`nav_graph.xml`).
- **UI Design System:** Material 3 Components, dynamic theming (`ThemeHelper.java`), custom Vector Drawables.
- **Backend:** Python FastAPI + SQLAlchemy + MySQL / MariaDB (`backend/main.py`).

---

## 📂 Package Structure (`com.aula.virtual`)

```
com.aula.virtual/
├── data/
│   ├── ApiService.java              # Retrofit API interface (REST endpoints)
│   ├── RetrofitClient.java          # Retrofit client singleton & URL configuration
│   ├── VirtualAulaRepository.java   # Repository layer (handles Online & Local/Offline SQLite)
│   ├── entity/                      # Room & JSON Data Models (User, Subject, BlogEntry, etc.)
│   └── local/                       # Room DAOs & AppDatabase
└── ui/
    ├── MainViewModel.java           # Central ViewModel powering fragments
    ├── ThemeHelper.java             # Dark mode, accent colors, and local mode preferences
    ├── DialogUtils.java             # Material Dialog builders & styling helpers
    ├── ImageUtils.java              # Profile image loading, cropping, and Base64 conversion
    ├── AdminHomeFragment.java       # Admin dashboard & navigation hub
    ├── ProfessorHomeFragment.java   # Professor dashboard & agenda hub
    ├── StudentHomeFragment.java     # Student dashboard & profile hub
    ├── AdminStudentListFragment.java# Alumnos management
    ├── AdminProfessorListFragment.java# Profesores management
    ├── AdminSubjectListFragment.java# Materias management
    ├── ProfessorSubjectListFragment.java# Professor assigned subjects & scheduling
    ├── AdminEnrollmentListFragment.java# Student enrollments & grades ("Materias y Notas")
    ├── SubjectBlogFragment.java     # Subject discussion forum & assignments
    ├── BlogDiscussionFragment.java  # Thread comments & chat
    └── SettingsAdvancedFragment.java# Offline sync, server configuration, data management
```

---

## 🔑 Core Architectural Conventions

1. **Backend URL Configuration per Build Variant:**
   - The backend URL is **NEVER** hardcoded or edited in source files (such as `RetrofitClient.java`).
   - It is configured via `local.properties` (using `backend.url.debug` and `backend.url.release` keys, template provided in `local.properties.example`) and injected into `BuildConfig.BASE_URL` by `app/build.gradle.kts`.
   - **CRITICAL RULE FOR AI AGENTS:** Never hardcode backend URLs, IP addresses, or ephemeral Cloudflare tunnel URLs inside `.java`, `.kt`, or `.xml` source files, and never check `local.properties` into Git version control.

2. **Dual Storage / Connection Modes:**
   - **Server Mode:** Communicates with remote FastAPI/MySQL backend via Retrofit.
   - **Local Mode (`ThemeHelper.MODE_LOCAL`):** Operates entirely offline using Room SQLite inside the device (`VirtualAulaRepository` checks `isLocalMode()`).

3. **User Roles & Permissions:**
   - `"ADMIN"`: Full system access across all management modules.
   - `"PROFESSOR"`: Manages assigned subjects, grades, class scheduling, and forums.
   - `"STUDENT"`: Views enrolled subjects, grades, calendar, and forum discussions.

4. **UI Styling Standards:**
   - **Cards & Buttons:** MaterialCardView with `strokeWidth="0dp"` (flat design without outer border stroke).
   - **Icons:** Pure white tint (`app:tint="@color/white"`) across header action buttons, carnet buttons, book buttons, and bottom nav buttons.

---

## 🚀 Common AI Agent Workflows

- **Backend URL & Environment Adjustments:** Advise users to update `local.properties` (e.g. `backend.url.debug=...`) and run Gradle Sync; do NOT modify `RetrofitClient.java` or Java constants.
- **Adding a new Management Fragment:** Register fragment in `nav_graph.xml`, add corresponding DAO queries if local, add Retrofit endpoint in `ApiService.java`, expose LiveData in `MainViewModel.java`, and bind views using ViewBinding.
- **Modifying Sync / Local Operations:** Update `VirtualAulaRepository.java` to ensure both Room DB operations and Retrofit calls are correctly handled for offline/online modes.
- **UI Changes:** Always verify dark mode compatibility in `ThemeHelper.java` and ensure Material 3 theme attributes (`?attr/colorPrimary`, `?attr/colorSurface`, etc.) are respected.
