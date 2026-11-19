# 12. Recursos y Configuración

[⬅️ Anterior](11_flujos_de_negocio.md) | [📑 Índice](00_INDICE.md) | [Siguiente ➡️](13_observaciones_y_mejoras.md)

Este documento detalla los archivos de configuración del manifiesto de Android, la tabla completa de dependencias de Gradle, la estructura de recursos XML (colores, layouts, drawables) y la configuración del entorno backend.

---

## Tabla de Contenido
- [12.1 Manifest de Android (AndroidManifest.xml)](#121-manifest-de-android-androidmanifestxml)
- [12.2 Dependencias Gradle y Librerías](#122-dependencias-gradle-y-librerías)
- [12.3 Catálogo de Recursos UI (Values, Drawables, Layouts)](#123-catálogo-de-recursos-ui-values-drawables-layouts)
- [12.4 Archivos de Configuración del Backend](#124-archivos-de-configuración-del-backend)

---

## 12.1 Manifest de Android (AndroidManifest.xml)

Ruta del archivo fuente: `app/src/main/AndroidManifest.xml`

### Permisos Declarados
- `android.permission.INTERNET`: Permite realizar peticiones HTTP/HTTPS a la API REST de FastAPI.
- `android.permission.ACCESS_NETWORK_STATE`: Permite verificar la conectividad a Internet antes de realizar peticiones de red.
- `android.permission.CAMERA`: Requerido opcionalmente para capturar fotografías de perfil directamente desde el dispositivo.
- `android.permission.POST_NOTIFICATIONS`: Requerido en Android 13+ (API 33+) para mostrar avisos y notificaciones push.
- `android.permission.VIBRATE`: Proporciona respuesta háptica al recibir mensajes o notificaciones.

### Componentes Declarados
- `MainActivity`: Declarada como la actividad principal de inicio (`android.intent.action.MAIN` / `LAUNCHER`) con modo de ajuste de teclado `adjustResize`.
- `VirtualFirebaseMessagingService`: Servicio en segundo plano para procesar notificaciones FCM entrantes.

[⬆️ Volver al inicio del documento](#12-recursos-y-configuración)

---

## 12.2 Dependencias Gradle y Librerías

Ruta del archivo fuente: `app/build.gradle.kts`

| Dependencia / Artefacto | Versión | Propósito / Uso en la Aplicación |
| :--- | :--- | :--- |
| `libs.appcompat` | `1.7.0` | Compatibilidad de componentes visuales nativos. |
| `libs.material` | `1.12.0` | Componentes de diseño Material Design 3 (Cards, Toolbars, Badges). |
| `libs.constraintlayout` | `2.2.0` | Layouts flexibles de alto rendimiento. |
| `libs.navigation.fragment` / `ui` | `2.8.5` | Navegación declarativa entre fragmentos (`nav_graph.xml`). |
| `libs.lifecycle.viewmodel` / `livedata`| `2.8.7` | Arquitectura reactiva MVVM y gestión del ciclo de vida. |
| `libs.retrofit.core` / `gson` | `2.11.0` | Cliente REST HTTP y conversor JSON. |
| `libs.okhttp.logging` | `4.12.0` | Logging de peticiones HTTP en consola. |
| `libs.room.runtime` / `compiler` | `2.6.1` | Motor de base de datos relacional local SQLite. |
| `libs.androidx.biometric` | `1.1.0` | Autenticación biométrica nativa (Huella / Rostro). |
| `libs.androidx.security.crypto` | `1.1.0-alpha06` | Cifrado AES-256 GCM para SharedPreferences. |
| `firebase-messaging` | BoM `33.8.0` | Recepción de notificaciones push en tiempo real. |
| `org.apache.poi:poi-ooxml` | `5.2.3` | Generación y lectura de plantillas Excel (`.xlsx`). |
| `at.favre.lib:bcrypt` | `0.10.2` | Hashing seguro de contraseñas de usuarios. |

[⬆️ Volver al inicio del documento](#12-recursos-y-configuración)

---

## 12.3 Catálogo de Recursos UI (Values, Drawables, Layouts)

Ubicación: `app/src/main/res/`

### 1. Colores y Temas (`values/colors.xml`, `ThemeHelper.java`)
El sistema soporta dinamismo de temas Claro y Oscuro (`ThemeHelper.java`), integrando una paleta oficial de colores para materias e insignias:
- `BLUE_LIGHT`, `BLUE`, `BLUE_DARK`
- `RED_LIGHT`, `RED`, `RED_DARK`
- `GREEN_LIGHT`, `GREEN`, `GREEN_DARK`
- `PURPLE_LIGHT`, `PURPLE`, `PURPLE_DARK`
- `CYAN_LIGHT`, `CYAN`, `CYAN_DARK`
- `YELLOW_LIGHT`, `YELLOW`, `YELLOW_DARK`

### 2. Drawables Clave (`drawable/`)
- `bg_bubble_me.xml` / `bg_bubble_others.xml`: Burbujas de mensajería para el chat.
- `ic_fingerprint.xml`: Vector gráfico para login biométrico.
- `bg_timetable_grid.xml`: Cuadrícula para horarios de instalaciones.
- `bg_badge_success.xml` / `bg_badge_danger.xml`: Indicadores de estado aprobado/reprobado.

[⬆️ Volver al inicio del documento](#12-recursos-y-configuración)

---

## 12.4 Archivos de Configuración del Backend

Ubicación: `backend/`

- `init.sql`: Script DDL para creación de tablas e inserción de catálogos iniciales en MySQL.
- `seed_demo.sql`: Script DML para poblar usuarios de prueba (`ADM001`, `PROF001`, `ENC001`), materias, inscriptos y notas.
- `config.yml`: Archivo de configuración local para parámetros de ejecución del servidor.
- `.aula_secret`: Archivo con la clave secreta institucional para firma de tokens JWT.

[⬆️ Volver al inicio del documento](#12-recursos-y-configuración)

---
[⬅️ Anterior](11_flujos_de_negocio.md) | [📑 Índice](00_INDICE.md) | [Siguiente ➡️](13_observaciones_y_mejoras.md)
