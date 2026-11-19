# 10. Catálogo de Clases y Métodos

[⬅️ Anterior](09_navegacion.md) | [📑 Índice](00_INDICE.md) | [Siguiente ➡️](11_flujos_de_negocio.md)

Este documento constituye el catálogo técnico completo de referencia del proyecto **Aula Virtual**, listando las clases por paquete y sus firmas de métodos principales tanto en el cliente Android como en el backend Python.

---

## Tabla de Contenido
- [10.1 Paquete com.aula.virtual (Raíz)](#101-paquete-comaulavirtual-raíz)
- [10.2 Paquete com.aula.virtual.data (Red y Repositorio)](#102-paquete-comaulavirtualdata-red-y-repositorio)
- [10.3 Paquete com.aula.virtual.data.local (Room DAOs y Backup)](#103-paquete-comaulavirtualdatalocal-room-daos-y-backup)
- [10.4 Paquete com.aula.virtual.excel (Procesamiento POI .xlsx)](#104-paquete-comaulavirtualexcel-procesamiento-poi-xlsx)
- [10.5 Paquete com.aula.virtual.service y util (Servicios y Helpers)](#105-paquete-comaulavirtualservice-y-util-servicios-y-helpers)
- [10.6 Catálogo de Módulos Backend (Python)](#106-catálogo-de-módulos-backend-python)

---

## 10.1 Paquete com.aula.virtual (Raíz)

### Clase `MainActivity`
- **Ruta**: `app/src/main/java/com/aula/virtual/MainActivity.java`
- **Tipo**: `AppCompatActivity`
- **Propósito**: Actividad principal y contenedora de navegación.

| Firma de Método | Parámetros | Retorno | Descripción |
| :--- | :--- | :--- | :--- |
| `onCreate(Bundle savedInstanceState)` | `Bundle` | `void` | Inicializa interfaz, bindings y NavController. |
| `setupRoleBasedNavigation(String role)` | `String role` | `void` | Configura el menú del Drawer según el rol. |
| `onSupportNavigateUp()` | Ninguno | `boolean` | Maneja el botón atrás de la barra superior. |

[⬆️ Volver al inicio del documento](#10-catálogo-de-clases-y-métodos)

---

## 10.2 Paquete com.aula.virtual.data (Red y Repositorio)

### Clase `RetrofitClient`
- **Ruta**: `app/src/main/java/com/aula/virtual/data/RetrofitClient.java`
- **Tipo**: `Util / Singleton`

| Firma de Método | Parámetros | Retorno | Descripción |
| :--- | :--- | :--- | :--- |
| `getApiService(Context context)` | `Context` | `ApiService` | Retorna la instancia singleton de la API REST. |
| `setCustomBaseUrl(Context ctx, String url)` | `Context, String` | `void` | Actualiza la URL base de red en tiempo de ejecución. |

---

### Clase `VirtualAulaRepository`
- **Ruta**: `app/src/main/java/com/aula/virtual/data/VirtualAulaRepository.java`
- **Tipo**: `Repository Pattern`

| Firma de Método | Parámetros | Retorno | Descripción |
| :--- | :--- | :--- | :--- |
| `getInstance(Context context)` | `Context` | `VirtualAulaRepository` | Singleton del repositorio. |
| `login(String carnet, String password, Callback<User> cb)`|`String, String, Callback`|`void`| Autentica credenciales en red u offline. |
| `getStudents(Callback<List<User>> cb)` | `Callback` | `void` | Obtiene lista de encargados de familia. |
| `createStudent(Student student, Callback<Student> cb)` | `Student, Callback` | `void` | Registra un nuevo estudiante menor de edad. |
| `updateEnrollment(Enrollment en, Callback<Enrollment> cb)`|`Enrollment, Callback`|`void`| Actualiza calificaciones de períodos 1-5. |
| `saveAttendanceRecord(AttendanceRecord r, Callback cb)`| `AttendanceRecord, Callback`|`void`| Persiste registro de asistencia diaria. |
| `saveConductRecord(ConductRecord r, Callback cb)` | `ConductRecord, Callback` | `void` | Persiste evaluación de conducta (criterios 1-5). |

[⬆️ Volver al inicio del documento](#10-catálogo-de-clases-y-métodos)

---

## 10.3 Paquete com.aula.virtual.data.local (Room DAOs y Backup)

### Clase `LocalDatabaseBackupManager`
- **Ruta**: `app/src/main/java/com/aula/virtual/data/local/LocalDatabaseBackupManager.java`

| Firma de Método | Parámetros | Retorno | Descripción |
| :--- | :--- | :--- | :--- |
| `exportBackupToJson(Context context)` | `Context` | `File` | Exporta todas las tablas de Room a un JSON formateado. |
| `importBackupFromJson(Context ctx, Uri jsonUri)` | `Context, Uri` | `boolean` | Restaura la base de datos desde un archivo JSON. |

---

### DAOs Principales de Room
- `UserDao`: `insertUser(User u)`, `getUserByCarnet(String c)`, `getAllUsers()`.
- `StudentDao`: `getStudentsByEncargado(int id)`, `insertStudent(Student s)`.
- `EnrollmentDao`: `getEnrollmentsByStudent(int sId)`, `update(Enrollment e)`.
- `AttendanceRecordDao`: `getAttendanceBySubjectAndDate(int subId, String date)`.
- `ConductRecordDao`: `getConductByStudentAndSubject(int stId, int subId, int period)`.

[⬆️ Volver al inicio del documento](#10-catálogo-de-clases-y-métodos)

---

## 10.4 Paquete com.aula.virtual.excel (Procesamiento POI .xlsx)

### Clase `ExcelTemplateGenerator`
- **Ruta**: `app/src/main/java/com/aula/virtual/excel/ExcelTemplateGenerator.java`

| Firma de Método | Parámetros | Retorno | Descripción |
| :--- | :--- | :--- | :--- |
| `generateGradeTemplate(Context ctx, Subject sub, List<StudentInSubjectInfo> list)` | `Context, Subject, List` | `File` | Genera archivo Excel `.xlsx` oficial con pestaña 'Notas' y 'Conducta'. |

---

### Clase `GradeExcelImporter`
- **Ruta**: `app/src/main/java/com/aula/virtual/excel/GradeExcelImporter.java`

| Firma de Método | Parámetros | Retorno | Descripción |
| :--- | :--- | :--- | :--- |
| `readAndParseExcel(Context ctx, Uri uri)` | `Context, Uri` | `ImportResult` | Parsea el archivo Excel, valida carnets y retorna objeto `ImportResult`. |

[⬆️ Volver al inicio del documento](#10-catálogo-de-clases-y-métodos)

---

## 10.5 Paquete com.aula.virtual.service y util (Servicios y Helpers)

### Clase `CredentialsManager`
- **Ruta**: `app/src/main/java/com/aula/virtual/ui/CredentialsManager.java`

| Firma de Método | Parámetros | Retorno | Descripción |
| :--- | :--- | :--- | :--- |
| `saveLoginCredentials(Context ctx, User u, String pwd)` | `Context, User, String` | `void` | Guarda usuario y clave cifrada con AES-256 GCM. |
| `hasStoredCredentials(Context ctx)` | `Context` | `boolean` | Retorna true si hay sesión previa guardada. |
| `getStoredUser(Context ctx)` | `Context` | `User` | Recupera objeto usuario almacenado. |
| `clearCredentials(Context ctx)` | `Context` | `void` | Cierra sesión eliminando las claves. |

---

### Clase `PasswordHasher`
- **Ruta**: `app/src/main/java/com/aula/virtual/util/PasswordHasher.java`

| Firma de Método | Parámetros | Retorno | Descripción |
| :--- | :--- | :--- | :--- |
| `hashPassword(String plainPassword)` | `String` | `String` | Genera hash BCrypt con costo 12. |
| `checkPassword(String plain, String hashed)` | `String, String` | `boolean` | Compara contraseña plana contra hash BCrypt. |

[⬆️ Volver al inicio del documento](#10-catálogo-de-clases-y-métodos)

---

## 10.6 Catálogo de Módulos Backend (Python)

### Módulo `backend/main.py`

| Función Python | Decorador Ruta | Descripción |
| :--- | :--- | :--- |
| `login(...)` | `@app.get("/login")` | Valida carnet/clave en MySQL y actualiza token FCM. |
| `create_user(...)` | `@app.post("/users")` | Aplica hash BCrypt y crea usuario en DB. |
| `get_students_by_encargado(...)`| `@app.get("/students/encargado/{id}")` | Devuelve lista de hijos asociados a un tutor. |
| `update_enrollment(...)` | `@app.put("/enrollments/{en_id}")` | Recibe notas del 1 al 5 y actualiza fila en MySQL. |
| `save_attendance_record(...)` | `@app.post("/attendance-records")` | Guarda o sobrescribe estado de asistencia diaria. |
| `save_conduct_record(...)` | `@app.post("/conduct-records")` | Guarda evaluación cuantitativa de conducta. |

---

### Módulo `backend/security.py`

| Función Python | Parámetros | Retorno | Descripción |
| :--- | :--- | :--- | :--- |
| `verify_password(plain, hashed)` | `str, str` | `bool` | Verificación de hash BCrypt con passlib. |
| `get_password_hash(password)` | `str` | `str` | Generación de hash BCrypt. |
| `create_access_token(data, expires)`| `dict, timedelta` | `str` | Generación de token JWT. |

[⬆️ Volver al inicio del documento](#10-catálogo-de-clases-y-métodos)

---
[⬅️ Anterior](09_navegacion.md) | [📑 Índice](00_INDICE.md) | [Siguiente ➡️](11_flujos_de_negocio.md)
