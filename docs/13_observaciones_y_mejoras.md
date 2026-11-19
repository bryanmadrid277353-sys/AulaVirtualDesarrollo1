# 13. Observaciones y Recomendaciones de Mejora

[⬅️ Anterior](12_recursos_y_configuracion.md) | [📑 Índice](00_INDICE.md) | [Siguiente ➡️](14_glosario.md)

Este documento presenta un análisis crítico del código fuente del proyecto **Aula Virtual**, identificando oportunidades de refactorización, aspectos de seguridad a fortalecer y recomendaciones priorizadas de mejora técnica.

---

## Tabla de Contenido
- [13.1 Análisis de Código y Arquitectura](#131-análisis-de-código-y-arquitectura)
- [13.2 Oportunidades de Refactorización](#132-oportunidades-de-refactorización)
- [13.3 Matriz Priorizada de Recomendaciones](#133-matriz-priorizada-de-recomendaciones)

---

## 13.1 Análisis de Código y Arquitectura

### 1. Tamaño y Complejidad de Archivos Principales
- **`VirtualAulaRepository.java`** (2,719 líneas) y **`main.py`** (1,771 líneas): Acumulan una alta cantidad de líneas de código. En el cliente Android, el repositorio asume múltiples responsabilidades (usuarios, notas, asistencia, conducta, mensajería, backup, etc.). En el backend, `main.py` contiene todas las rutas REST en un solo archivo.
- *Recomendación*: Dividir `VirtualAulaRepository` en sub-repositorios especializados (`UserRepository`, `AcademicRepository`, `ChatRepository`) y modularizar FastAPI utilizando `APIRouter` por dominios de negocio.

### 2. Estrategia de Migración de Room Database
- `AppDatabase.java` utiliza `.fallbackToDestructiveMigration()`. En entornos de producción, si se incrementa la versión de la base de datos sin definir un objeto `Migration`, los datos guardados offline por el usuario en SQLite se eliminarán por completo.
- *Recomendación*: Definir migraciones explícitas (`Migration(8, 9)`) para garantizar la persistencia de los datos del usuario entre actualizaciones de la aplicación.

[⬆️ Volver al inicio del documento](#13-observaciones-y-recomendaciones-de-mejora)

---

## 13.2 Oportunidades de Refactorización

### 1. Optimización del Manejo de Imágenes de Perfil (Base64 vs URLs Estáticas)
- Actualmente, las imágenes de perfil se almacenan como cadenas `LONGTEXT` en Base64 dentro de MySQL (`profile_image`) y se transmiten directamente en los cuerpos de respuesta JSON.
- Esto incrementa significativamente el peso de los paquetes de red y el consumo de memoria RAM al deserializar cadenas Base64 extensas.
- *Recomendación*: Implementar un servicio de carga de archivos en el backend que guarde la imagen en el disco del servidor o en un bucket S3 y almacene únicamente la URL relativa (`/static/avatars/user_123.jpg`) en la base de datos.

### 2. Manejo de Transmisión de Credenciales en Login
- El endpoint de inicio de sesión actual utiliza `@GET("login")` enviando las credenciales en la Query String (`?carnet=...&password=...`).
- *Recomendación*: Cambiar el endpoint a un método `@POST("login")` transmitiendo las credenciales dentro del cuerpo de la petición HTTP (`JSON Body`) y forzar HTTPS deshabilitando `usesCleartextTraffic`.

[⬆️ Volver al inicio del documento](#13-observaciones-y-recomendaciones-de-mejora)

---

## 13.3 Matriz Priorizada de Recomendaciones

| Prioridad | Área | Archivo Afectado | Descripción del Cambio | Impacto Esperado |
| :--- | :--- | :--- | :--- | :--- |
| 🔴 **ALTA** | Seguridad | `ApiService.java` / `main.py` | Migrar `/login` de `GET` con query params a `POST` con cuerpo JSON. | Evita el registro de contraseñas planas en logs del servidor HTTP. |
| 🔴 **ALTA** | Seguridad | `AndroidManifest.xml` | Desactivar `usesCleartextTraffic="true"` y forzar HTTPS en la URL base. | Previene ataques Man-in-the-Middle (MitM) en redes públicas. |
| 🟡 **MEDIA** | Rendimiento| `users` / `students` DB | Sustituir imágenes Base64 en JSON por URLs relativas a archivos estáticos. | Disminuye el peso de las respuestas REST y acelera la carga de la UI. |
| 🟡 **MEDIA** | Arquitectura| `VirtualAulaRepository.java` | Dividir el repositorio monolítico en sub-repositorios por dominio de negocio. | Facilita el mantenimiento, legibilidad y pruebas unitarias. |
| 🟡 **MEDIA** | Backend | `backend/main.py` | Modularizar los endpoints utilizando `APIRouter` de FastAPI. | Mejora la organización y escalabilidad del servidor. |
| 🟢 **BAJA** | Persistencia| `AppDatabase.java` | Reemplazar `destructiveMigration` por objetos `Migration` explícitos de Room. | Evita la pérdida accidental de datos offline al actualizar la app. |

[⬆️ Volver al inicio del documento](#13-observaciones-y-recomendaciones-de-mejora)

---
[⬅️ Anterior](12_recursos_y_configuracion.md) | [📑 Índice](00_INDICE.md) | [Siguiente ➡️](14_glosario.md)
