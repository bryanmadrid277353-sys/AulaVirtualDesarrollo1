# 5. API Backend (FastAPI)

[⬅️ Anterior](04_modelo_de_datos.md) | [📑 Índice](00_INDICE.md) | [Siguiente ➡️](06_datos_en_android.md)

Este documento describe la arquitectura del backend desarrollado en **FastAPI (Python)**, la configuración del servidor, los ORM de SQLAlchemy, el listado completo de endpoints REST y las instrucciones de despliegue y ejecución.

---

## Tabla de Contenido
- [5.1 Estructura y Configuración de main.py](#51-estructura-y-configuración-de-mainpy)
- [5.2 Catálogo Completo de Endpoints REST](#52-catálogo-completo-de-endpoints-rest)
- [5.3 Manejo de Errores y Validaciones](#53-manejo-de-errores-y-validaciones)
- [5.4 Guía de Despliegue y Ejecución del Backend](#54-guía-de-despliegue-y-ejecución-del-backend)

---

## 5.1 Estructura y Configuración de main.py

Ruta del archivo fuente: `backend/main.py`

El servidor backend está implementado en un único módulo principal de Python que integra la definición de la aplicación FastAPI, la configuración del middleware de CORS, los modelos de base de datos relacional (SQLAlchemy ORM), los esquemas de validación de datos (Pydantic) y los controladores de rutas HTTP.

### Componentes Clave
1. **Conexión a la Base de Datos**:
   ```python
   # Ubicación: backend/main.py
   DATABASE_URL = "mysql+pymysql://root:@localhost/aula_virtual"
   engine = create_engine(DATABASE_URL, pool_pre_ping=True)
   SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
   ```
2. **Middleware CORS**:
   Configurado para permitir solicitudes desde cualquier origen (`allow_origins=["*"]`), habilitando la comunicación directa desde dispositivos Android locales y el panel de administración web.
3. **Inyección de Dependencia de Sesión DB**:
   Función `get_db()` que abre una sesión por cada petición entrante y garantiza su cierre seguro (`db.close()`) en un bloque `finally`.

[⬆️ Volver al inicio del documento](#5-api-backend-fastapi)

---

## 5.2 Catálogo Completo de Endpoints REST

A continuación se detallan los servicios expuestos por la API Backend (`backend/main.py`):

### 5.2.1 Autenticación y Usuarios (`/login`, `/users`)

| Método | Ruta | Descripción | Parámetros / Body | Respuesta JSON | Rol Consumidor |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/` | Health check básico de la API | N/A | `{"status": "ok"}` | Todos |
| `GET` | `/login` | Iniciar sesión y autenticar usuario | Query: `carnet`, `password`, `fcm_token` | Objeto `UserOut` | Todos |
| `GET` | `/users/students` | Listar usuarios con rol 'ENCARGADO' | N/A | `List[UserOut]` | ADMIN |
| `GET` | `/users/admins` | Listar usuarios con rol 'ADMIN' | N/A | `List[UserOut]` | ADMIN |
| `GET` | `/users/professors` | Listar usuarios con rol 'PROFESSOR' | N/A | `List[UserOut]` | ADMIN |
| `GET` | `/users/encargados` | Listar usuarios encargados de familia | N/A | `List[UserOut]` | ADMIN / PROFESSOR |
| `POST` | `/users` | Registrar una nueva cuenta de usuario | Body: `UserCreate` | Objeto `UserOut` | ADMIN |
| `PUT` | `/users/{user_id}` | Actualizar datos de un usuario existente | Path: `user_id`, Body: `UserCreate` | Objeto `UserOut` | ADMIN / Todos (Perfil) |
| `DELETE` | `/users/{user_id}` | Eliminar la cuenta de un usuario | Path: `user_id` | `{"message": "User deleted"}` | ADMIN |
| `PUT` | `/users/{userId}/fcm-token` | Actualizar Token FCM de notificaciones push | Path: `userId`, Query: `fcm_token` | `{"message": "FCM token updated"}` | Todos |

---

### 5.2.2 Estudiantes menores de edad (`/students`)

| Método | Ruta | Descripción | Parámetros / Body | Respuesta JSON | Rol Consumidor |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/students` | Listar todos los estudiantes | N/A | `List[StudentSchema]` | ADMIN |
| `GET` | `/students/encargado/{encargado_id}` | Obtener estudiantes a cargo de un encargado | Path: `encargado_id` | `List[StudentSchema]` | ENCARGADO / ADMIN |
| `POST` | `/students` | Registrar un nuevo estudiante | Body: `StudentSchema` | Objeto `StudentSchema` | ADMIN |
| `GET` | `/students/{student_id}` | Obtener detalle de un estudiante | Path: `student_id` | Objeto `StudentSchema` | Todos |
| `PUT` | `/students/{student_id}` | Actualizar estudiante | Path: `student_id`, Body: `StudentSchema` | Objeto `StudentSchema` | ADMIN |
| `DELETE` | `/students/{student_id}` | Eliminar estudiante | Path: `student_id` | `{"message": "Student deleted"}` | ADMIN |

---

### 5.2.3 Materias y Calificaciones (`/subjects`, `/enrollments`)

| Método | Ruta | Descripción | Parámetros / Body | Respuesta JSON | Rol Consumidor |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/subjects` | Listar todas las materias creadas | N/A | `List[Subject]` | Todos |
| `POST` | `/subjects` | Crear una nueva asignatura | Body: `Subject` | Objeto `Subject` | ADMIN |
| `PUT` | `/subjects/{sub_id}` | Editar información de materia | Path: `sub_id`, Body: `Subject` | Objeto `Subject` | ADMIN |
| `DELETE` | `/subjects/{sub_id}` | Eliminar materia | Path: `sub_id` | `{"message": "Subject deleted"}` | ADMIN |
| `GET` | `/subjects/professor/{prof_id}` | Listar materias asignadas a un docente | Path: `prof_id` | `List[Subject]` | PROFESSOR / ADMIN |
| `GET` | `/subjects/{sub_id}/enrollments` | Listar alumnos inscritos con sus notas | Path: `sub_id` | `List[StudentInSubjectInfo]` | PROFESSOR / ADMIN |
| `GET` | `/enrollments/student/{student_id}`| Obtener notas por materia de un alumno | Path: `student_id` | `List[StudentGradeInfo]` | ENCARGADO / ADMIN |
| `POST` | `/enrollments` | Inscribir un estudiante a una materia | Body: `Enrollment` | Objeto `Enrollment` | ADMIN |
| `PUT` | `/enrollments/{en_id}` | Actualizar calificaciones (1-5) | Path: `en_id`, Body: `Enrollment` | Objeto `Enrollment` | PROFESSOR / ADMIN |

---

### 5.2.4 Asistencia y Conducta (`/attendance-records`, `/conduct-records`)

| Método | Ruta | Descripción | Parámetros / Body | Respuesta JSON | Rol Consumidor |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/subjects/{sub_id}/attendance-records` | Obtener historial de asistencia de una materia | Path: `sub_id` | `List[AttendanceRecordSchema]` | PROFESSOR / ADMIN |
| `GET` | `/students/{student_id}/subjects/{sub_id}/attendance-records` | Asistencia individual de estudiante por materia | Path: `student_id`, `sub_id` | `List[AttendanceRecordSchema]` | ENCARGADO / ADMIN |
| `POST` | `/attendance-records` | Guardar o actualizar toma de asistencia | Body: `AttendanceRecordSchema` | Objeto `AttendanceRecordSchema` | PROFESSOR / ADMIN |
| `GET` | `/conduct-levels` | Listar catálogo de niveles de conducta | N/A | `List[ConductLevelSchema]` | Todos |
| `GET` | `/subjects/{sub_id}/conduct-records` | Obtener evaluaciones de conducta de materia | Path: `sub_id` | `List[ConductRecordSchema]` | PROFESSOR / ADMIN |
| `POST` | `/conduct-records` | Guardar/Actualizar evaluación de conducta | Body: `ConductRecordSchema` | Objeto `ConductRecordSchema` | PROFESSOR / ADMIN |

---

### 5.2.5 Foros, Mensajería Directa y Notificaciones (`/blog`, `/direct-messages`, `/notifications`)

| Método | Ruta | Descripción | Parámetros / Body | Respuesta JSON | Rol Consumidor |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/subjects/{sub_id}/blog` | Listar entradas de foro de una materia | Path: `sub_id` | `List[BlogEntry]` | Todos |
| `POST` | `/blog` | Crear publicación en el foro | Body: `BlogEntry` | Objeto `BlogEntry` | PROFESSOR / ADMIN |
| `POST` | `/blog/comments` | Agregar comentario a publicación | Body: `BlogComment` | Objeto `BlogComment` | Todos |
| `GET` | `/direct-messages/rooms/{user_id}` | Obtener salas de chat activas de usuario | Path: `user_id` | `List[ChatRoomInfoSchema]` | Todos |
| `GET` | `/direct-messages/history/{user_id}/{contact_id}` | Historial de mensajes entre 2 usuarios | Path: `user_id`, `contact_id` | `List[DirectMessageSchema]` | Todos |
| `POST` | `/direct-messages` | Enviar mensaje directo privado | Body: `DirectMessageSchema` | Objeto `DirectMessageSchema` | Todos |
| `GET` | `/notifications` | Consultar avisos y notificaciones | N/A | `List[NotificationSchema]` | Todos |
| `POST` | `/notifications` | Emitir aviso masivo y notificación FCM | Body: `NotificationSchema` | Objeto `NotificationSchema` | ADMIN / PROFESSOR |

[⬆️ Volver al inicio del documento](#5-api-backend-fastapi)

---

## 5.3 Manejo de Errores y Validaciones

FastAPI utiliza la librería Pydantic para la validación automática de datos de entrada. En caso de discrepancia de tipos o ausencia de campos obligatorios, el servidor responde automáticamente con un código HTTP `422 Unprocessable Entity` y el detalle del error en formato JSON.

Para errores de lógica de negocio o recursos inexistentes, se lanzan excepciones explícitas mediante `HTTPException`:
- `400 Bad Request`: Parámetros inválidos (ej. carnet duplicado o contraseña incorrecta).
- `404 Not Found`: Recurso no encontrado en MySQL.
- `500 Internal Server Error`: Falla en la ejecución de consultas SQL o errores no controlados.

[⬆️ Volver al inicio del documento](#5-api-backend-fastapi)

---

## 5.4 Guía de Despliegue y Ejecución del Backend

El proyecto cuenta con scripts `.bat` y `.py` automatizados para simplificar el inicio del servidor y su exposición hacia los dispositivos móviles.

### Ejecución Local con Uvicorn
Archivo: `backend/run_backend.py` / `run_backend.bat`
```bash
python run_backend.py
```
Inicia la aplicación escuchando en todas las interfaces de red locales (`0.0.0.0`) en el puerto `8000`.

### Ejecución con Túnel Cloudflare Automatizado
Archivo: `backend/run_tunnel_and_update_app.py`
Este script realiza las siguientes acciones:
1. Ejecuta el binario `cloudflared.exe` para generar una URL HTTPS pública dinámica (ej.: `https://xxx-xxx.trycloudflare.com`).
2. Captura automáticamente la URL generada desde la consola.
3. Actualiza dinámicamente la constante `BASE_URL` en el archivo `app/src/main/java/com/aula/virtual/data/RetrofitClient.java` del proyecto Android.

[⬆️ Volver al inicio del documento](#5-api-backend-fastapi)

---
[⬅️ Anterior](04_modelo_de_datos.md) | [📑 Índice](00_INDICE.md) | [Siguiente ➡️](06_datos_en_android.md)
