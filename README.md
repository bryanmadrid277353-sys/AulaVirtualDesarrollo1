# 🎓 Aula Virtual - Aplicación Móvil & Backend API

Plataforma de gestión académica compuesta por una **aplicación móvil Android** desarrollada en Java y un **servidor Backend API REST** desarrollado en Python con FastAPI y MySQL.

---

## Índice de la Documentación

1. [Descripción General del Proyecto](#descripción-general-del-proyecto)
2. [Objetivo Principal](#objetivo-principal)
3. [Misión y Visión](#misión-y-visión)
4. [Manual de Instalación](#manual-de-instalación)
    - [Requisitos previos](#requisitos-previos)
    - [Instalación paso a paso](#instalación-paso-a-paso)
    - [Solución de problemas comunes](#solución-de-problemas-comunes)
5. [Documentación Técnica (carpeta docs)](#documentación-técnica-carpeta-docs)
6. [Tecnologías Utilizadas](#tecnologías-utilizadas)
7. [Usuarios y Credenciales de Prueba](#usuarios-y-credenciales-de-prueba)
8. [Estructura del Proyecto](#estructura-del-proyecto)
9. [Arquitectura y Roles](#arquitectura-y-roles)
10. [Recomendaciones a Futuro](#recomendaciones-a-futuro)

---

## Descripción General del Proyecto

**Aula Virtual** es una plataforma de gestión académica pensada para instituciones educativas de nivel primario y secundario. Conecta a tres actores principales: **administradores**, **profesores** y **encargados** (padres o tutores) de estudiantes menores de edad. Los estudiantes también pueden ingresar con su carnet.

Funcionalidades principales:

- **Gestión administrativa:** usuarios, estudiantes vinculados a sus encargados, materias, inscripciones, instalaciones físicas (aulas, laboratorios) y horarios semanales.
- **Seguimiento académico:** notas por período (1 a 5), evaluación de conducta con 5 criterios y control de asistencia diaria (presente, ausente, llegada tardía y permiso).
- **Comunicación:** foros por materia, mensajería directa entre usuarios y notificaciones push con Firebase Cloud Messaging.
- **Calendario académico:** períodos lectivos y días no lectivos.
- **Excel:** generación e importación de planillas `.xlsx` de notas, asistencia y conducta.
- **Funcionamiento sin conexión:** caché local con Room y respaldo de la base de datos local.
- **Panel web:** interfaz web ligera de administración servida por el propio backend.

---

## Objetivo Principal

Facilitar la comunicación, el seguimiento académico y la administración de una institución educativa mediante una aplicación móvil accesible, que mantenga informados a los encargados sobre notas, asistencia y conducta, y que siga siendo utilizable aun sin conexión a Internet.

---

## Misión y Visión

**Misión:** Ofrecer una herramienta móvil sencilla, segura y confiable que simplifique la administración de materias, notas, asistencia y comunicación entre administradores, docentes y encargados, apoyando el proceso de enseñanza y aprendizaje.

**Visión:** Convertirse en una plataforma de aula virtual integral y escalable, adaptable a distintas instituciones educativas, que sea referente por su facilidad de uso, su disponibilidad en cualquier lugar y su evolución continua.

---

## Manual de Instalación

Resumen para instalar y ejecutar el proyecto desde cero en una computadora con una versión reciente de Android Studio. La guía completa está en 👉 [**MANUAL_DE_INSTALACION.md**](MANUAL_DE_INSTALACION.md).

### Requisitos previos

| Herramienta | Descripción | Descarga |
| --- | --- | --- |
| **Android Studio** | IDE oficial de Android (incluye SDK, emulador y JDK). El proyecto compila con Java 17 | [Descargar](https://developer.android.com/studio) |
| **XAMPP** | Servidor local para la base de datos MySQL / MariaDB | [Descargar](https://www.apachefriends.org/es/index.html) |
| **Python 3.10+** | Entorno de ejecución del backend FastAPI | [Descargar](https://www.python.org/downloads/) |
| **Git** | Control de versiones para clonar el repositorio | [Descargar](https://git-scm.com/downloads) |

### Instalación paso a paso

**1. Clonar el proyecto**

```bash
git clone https://github.com/bryanmadrid277353-sys/AulaVirtualDesarrollo1.git
cd AulaVirtualDesarrollo1
```

**2. Instalar las dependencias del backend** (terminal de Android Studio)

```bash
cd backend
python -m venv venv
.\venv\Scripts\activate        # Windows
# source venv/bin/activate     # macOS / Linux
pip install fastapi uvicorn sqlalchemy mysql-connector-python pydantic
```

**3. Crear la base de datos**

Inicia **MySQL** desde el XAMPP Control Panel y, con el entorno virtual activo, ejecuta desde la carpeta `backend`:

```bash
py sync_database.py            # o, desde la raíz del proyecto: Sincronizar_BD.bat
```

El script crea la base `aula_virtual`, ejecuta `init.sql` (estructura y administrador inicial) y `seed_demo.sql` (datos de demostración). También puedes importar `backend/init.sql` y `backend/seed_demo.sql` manualmente desde [phpMyAdmin](http://localhost/phpmyadmin/) o con el cliente `mysql`.

> La conexión por defecto es `mysql+mysqlconnector://root:@localhost/aula_virtual` (usuario `root` sin contraseña, como en XAMPP). El backend permite cambiarla con la variable de entorno `AULA_DATABASE_URL`; `sync_database.py` usa la conexión por defecto.

**4. Levantar el backend**

```bash
py run_backend.py              # o en Windows: .\run_backend.bat
```

Verifica que funcione abriendo <http://localhost:8000/docs> (Swagger UI). La API arranca en modo de seguridad `monitor` (`AULA_AUTH_MODE`), compatible con la app Android actual. Deja esta terminal abierta mientras pruebas la app.

**5. Abrir la app en Android Studio**

Abre la carpeta raíz del proyecto (**Open**), espera el *Gradle Sync* y confirma en `File` > `Settings` > `Build, Execution, Deployment` > `Build Tools` > `Gradle` que **Gradle JDK** sea `JDK 17` o `Embedded JDK`.

**6. Configurar la dirección del servidor**

La dirección del backend se configura en el archivo `local.properties` (copiando `local.properties.example`) seleccionando el modo en `backend.mode` (`emulador` | `lan` | `cloudflare` | `tailscale`):

| Modo (`backend.mode`) | Escenario | Clave en `local.properties` | Valor de ejemplo |
| --- | --- | --- | --- |
| `emulador` | Emulador de Android Studio | `backend.url.emulador` | `http://10.0.2.2:8000/` |
| `lan` | Teléfono físico (misma Wi-Fi) | `backend.url.lan` | `http://<IP_DE_TU_PC>:8000/` |
| `cloudflare` | Túnel remoto con Cloudflare | `backend.url.cloudflare` | `https://<subdominio>.trycloudflare.com/` |
| `tailscale` | VPN privada Tailscale (PC y Móvil) | `backend.url.tailscale` | `http://100.x.y.z:8000/` |
| *(Release)* | Servidor Producción (Release) | `backend.url.release` | `https://api.tudominio.com/` |

> ⚠️ **Nota:** Los modos de prueba (`emulador`, `lan`, `cloudflare`, `tailscale`) funcionan únicamente en compilaciones `debug`. Para `release`, Gradle requiere obligatoriamente una URL segura con protocolo HTTPS (`backend.url.release`).

---

## Configuración del Backend por Modo de Conexión

La aplicación Android utiliza `BuildConfig.BASE_URL` y `BuildConfig.BACKEND_MODE` para inyectar la URL del servidor Retrofit según el modo configurado en `local.properties`:

- **`debug`**: Lee `backend.mode` para seleccionar la URL de `backend.url.<modo>`. Permite tráfico HTTP en claro en desarrollo mediante `network_security_config.xml` (incluyendo IPs variables de Tailscale `100.x.y.z`).
- **`release`**: Fuerza el uso de `backend.url.release` en HTTPS, rechazando esquemas `http://` y bloqueando tráfico en claro.

Tras modificar `local.properties`, sincroniza el proyecto con Gradle (**Sync Project with Gradle Files**) y recompila la aplicación.

---

**7. Compilar y ejecutar**

Crea o selecciona un dispositivo virtual con **Android 10 (API 29)** o superior, pulsa **Run 'app'** (`Shift + F10`) e inicia sesión con alguna de las [credenciales de prueba](#usuarios-y-credenciales-de-prueba).

### Solución de problemas comunes

- **`Failed to connect to /10.0.2.2:8000` o `/192.168.x.x:8000`:** confirma que el backend esté corriendo, permite el puerto 8000 en el Firewall de Windows y, en dispositivo físico, que PC y teléfono estén en la misma red Wi-Fi.
- **`Can't connect to MySQL server`:** verifica que MySQL esté iniciado en XAMPP. Si tu usuario `root` tiene contraseña, define `AULA_DATABASE_URL` antes de iniciar el backend.
- **Error de sincronización de Gradle:** usa `File` > `Invalidate Caches...` > `Invalidate and Restart` y comprueba tu conexión a Internet.

---

## Documentación Técnica (carpeta docs)

La carpeta [`docs/`](docs/) reúne la documentación técnica completa. Empieza por el [**índice general (00_INDICE.md)**](docs/00_INDICE.md).

| Nº | Documento | Contenido |
| :---: | :--- | :--- |
| 01 | [Introducción y Arquitectura](docs/01_introduccion_y_arquitectura.md) | Propósito, stack tecnológico, requisitos y arquitectura por capas |
| 02 | [Roles y Permisos](docs/02_roles_y_permisos.md) | Tipos de usuario, matriz de permisos y redirección por rol |
| 03 | [Autenticación y Seguridad](docs/03_autenticacion_y_seguridad.md) | Flujo de login, almacenamiento cifrado, biometría y riesgos |
| 04 | [Modelo de Datos](docs/04_modelo_de_datos.md) | Diagrama entidad-relación y detalle de tablas |
| 05 | [Backend API (FastAPI)](docs/05_backend_api.md) | Configuración, endpoints REST y ejecución |
| 06 | [Datos en Android](docs/06_datos_en_android.md) | Retrofit, Room y sincronización offline/online |
| 07 | [Repositorios y ViewModels](docs/07_repositorios_y_viewmodels.md) | `VirtualAulaRepository`, `MainViewModel` y flujo de datos |
| 08a | [UI: Pantallas Compartidas](docs/08a_ui_compartida.md) | Login, perfil, ajustes, chat, foros y calendario |
| 08b | [UI: Módulo Administrador](docs/08b_ui_administrador.md) | Gestión de usuarios, materias, instalaciones y horarios |
| 08c | [UI: Módulo Profesor](docs/08c_ui_profesor.md) | Materias, notas, conducta, asistencia y Excel |
| 08d | [UI: Módulo Encargado/Estudiante](docs/08d_ui_estudiante_encargado.md) | Selector de hijos, promedios, asistencia y conducta |
| 09 | [Navegación en Android](docs/09_navegacion.md) | `nav_graph.xml`, menús por rol y flujos de pantalla |
| 10 | [Catálogo de Clases y Métodos](docs/10_catalogo_clases_y_metodos.md) | Referencia técnica por paquete Java y módulos Python |
| 11 | [Flujos de Negocio](docs/11_flujos_de_negocio.md) | Diagramas de secuencia de los procesos clave |
| 12 | [Recursos y Configuración](docs/12_recursos_y_configuracion.md) | Manifest, dependencias Gradle y archivos del backend |
| 13 | [Observaciones y Mejoras](docs/13_observaciones_y_mejoras.md) | Análisis del código y recomendaciones priorizadas |
| 14 | [Glosario](docs/14_glosario.md) | Términos técnicos y del dominio académico |

Otros documentos del repositorio: [MANUAL_DE_INSTALACION.md](MANUAL_DE_INSTALACION.md) (guía detallada de instalación) y [AI_AGENT_GUIDE.md](AI_AGENT_GUIDE.md) (guía técnica de arquitectura para desarrolladores).

---

## Tecnologías Utilizadas

### Cliente Móvil (Android)

- **Lenguaje:** Java 17
- **SDK:** Compile SDK 36 | Target SDK 35 | **SDK mínimo:** Android 10 (API 29)
- **Herramientas de compilación:** Android Gradle Plugin 9.5.1 y Gradle 9.8.0 (wrapper incluido)
- **Componentes:**
    - Architecture Components (ViewModel, LiveData, ViewBinding) y Navigation Component
    - Retrofit 2.11.0, Gson y OkHttp 4.12.0 (consumo de API REST)
    - Room 2.8.5 (persistencia offline y caché; base de datos local versión 9)
    - Material Components 1.14.0
    - Biometric Prompt y EncryptedSharedPreferences (seguridad)
    - Firebase Cloud Messaging (notificaciones push)
    - Apache POI 5.2.3 (planillas Excel `.xlsx`)
    - BCrypt (`at.favre.lib:bcrypt`) para el manejo de contraseñas en el cliente

### Backend API y Túnel

- **Lenguaje:** Python 3.10+
- **Framework:** FastAPI + Uvicorn, con validación mediante Pydantic
- **ORM:** SQLAlchemy
- **Base de datos:** MySQL / MariaDB (vía XAMPP) con el conector `mysql-connector-python`
- **Seguridad:** módulo `security.py` con hash de contraseñas PBKDF2-SHA256, tokens de sesión firmados con HMAC-SHA256, política de acceso por rol (modos `monitor` y `enforce`) y límite de intentos de inicio de sesión
- **Panel web:** archivos estáticos en `backend/web/`
- **Conexión remota de desarrollo:** Tailscale VPN mesh privada (`http://100.x.y.z:8000/`) y Cloudflare Quick Tunnel (`cloudflared`, HTTPS) para pruebas sin abrir puertos en el router

---

## Usuarios y Credenciales de Prueba

`backend/init.sql` crea el administrador inicial y `backend/seed_demo.sql` agrega los usuarios de demostración. Todas las cuentas usan la contraseña inicial `ASD###`:

| Rol | Carnet de ejemplo | Contraseña | Origen |
| :--- | :--- | :--- | :--- |
| **Administrador** | `ADMIN12345` | `ASD###` | `init.sql` |
| **Profesor** | `PROF100001`, `PROF100002`, `PROF100003` | `ASD###` | `seed_demo.sql` |
| **Encargado** | `ENC1444444`, `ENC1444445`, `ENC1444446` | `ASD###` | `seed_demo.sql` |
| **Estudiante** | `EST6197088` (y otros del seed) | `ASD###` o su propio carnet | `seed_demo.sql` |

Los estudiantes no tienen contraseña propia en la base de datos: ingresan con su carnet y la contraseña genérica (configurable con `AULA_STUDENT_DEFAULT_PASSWORD`). La contraseña inicial del administrador puede definirse con `AULA_ADMIN_PASSWORD` antes de crearse.

> ⚠️ Son credenciales solo para pruebas y desarrollo. Cámbialas antes de cualquier despliegue real.

---

## Estructura del Proyecto

```
AulaVirtualDesarrollo1/
├── app/                              # Aplicación Android
│   ├── src/debug/                    # Configuración exclusiva para variante debug (network security config)
│   └── src/main/
│       ├── java/com/aula/virtual/
│       │   ├── MainActivity.java     # Actividad única con NavHostFragment
│       │   ├── data/                 # ApiService, RetrofitClient, repositorio, entidades y Room (local/)
│       │   ├── excel/                # Importación y exportación de planillas Excel
│       │   ├── service/              # Servicio de notificaciones push (FCM)
│       │   ├── ui/                   # Fragments, adapters, ViewModel y utilidades de interfaz
│       │   └── util/                 # Utilidades (hash de contraseñas, etc.)
│       └── res/                      # Layouts, menús, temas, drawables y nav_graph.xml
├── backend/                          # Servidor Backend API (Python + FastAPI)
│   ├── main.py                       # Endpoints REST, modelos ORM y esquemas
│   ├── security.py                   # Hash de contraseñas, tokens, roles y límite de intentos
│   ├── init.sql                      # Estructura de la base de datos y administrador inicial
│   ├── seed_demo.sql                 # Datos de demostración
│   ├── migration_*.sql               # Migraciones de asistencia y conducta
│   ├── sync_database.py              # Crea la base y ejecuta init.sql y seed_demo.sql
│   ├── run_backend.py / .bat         # Inicio del servidor Uvicorn
│   ├── run_cloudflare_tunnel.bat     # Inicio del túnel Cloudflare
│   ├── run_tunnel_and_update_app.py  # Túnel de prueba Cloudflare
│   └── web/                          # Panel de administración web
├── docs/                             # Documentación técnica completa (18 archivos)
├── gradle/                           # Wrapper de Gradle y catálogo de versiones
├── local.properties.example          # Plantilla versionada de URLs del backend por variante
├── Sincronizar_BD.bat                # Atajo para sincronizar la base de datos (Windows)
├── AI_AGENT_GUIDE.md                 # Guía técnica de arquitectura
├── MANUAL_DE_INSTALACION.md          # Guía detallada de instalación
└── README.md                         # Descripción general del proyecto
```

---

## Arquitectura y Roles

- **Patrón:** MVVM con Architecture Components (`ViewModel`, `LiveData`) y un repositorio central (`VirtualAulaRepository`) que combina Room y Retrofit.
- **Modos de funcionamiento:**
    - *Modo servidor:* la app consume el backend FastAPI/MySQL con Retrofit.
    - *Modo local:* la app opera sin conexión con Room (SQLite) dentro del dispositivo.
- **Roles de usuario:**

| Rol | Alcance |
| :--- | :--- |
| `ADMIN` | Acceso completo: usuarios, estudiantes, materias, inscripciones, instalaciones, horarios, calendario y corrección de notas |
| `PROFESSOR` | Sus materias asignadas: notas, conducta, asistencia, importación de Excel, foros y avisos a encargados |
| `ENCARGADO` | Consulta de notas, asistencia y conducta de los estudiantes a su cargo; foros y mensajería con docentes |
| `STUDENT` | Ingreso con su carnet para consultar su información académica |

Para el detalle de permisos, pantallas y flujos, consulta la [documentación técnica](#documentación-técnica-carpeta-docs).

---

## Recomendaciones a Futuro

**Seguridad**
- Activar el modo `AULA_AUTH_MODE=enforce` del backend cuando la app Android envíe el token de sesión en cada petición; hoy la API corre en modo `monitor`.
- Migrar el inicio de sesión de la app a `POST /login` (el backend ya lo admite; la app aún usa el `GET` obsoleto con la contraseña en la URL).
- Desactivar `usesCleartextTraffic` y usar HTTPS fuera de entornos de prueba.
- Excluir del repositorio el archivo `backend/.aula_secret` (clave de firma de tokens), regenerarlo y añadirlo al `.gitignore`.
- Cambiar las contraseñas iniciales `ASD###` y definir las variables de entorno `AULA_ADMIN_PASSWORD` y `AULA_STUDENT_DEFAULT_PASSWORD` en despliegues reales.
- Reemplazar las fotos de perfil en Base64 dentro del JSON por archivos estáticos con URL relativa.

**Backend e infraestructura**
- Crear un archivo `requirements.txt` para instalar las dependencias con un solo comando.
- Hacer que `sync_database.py` use también `AULA_DATABASE_URL`.
- Modularizar `main.py` con `APIRouter` por dominio (usuarios, académico, mensajería, etc.).
- Contenerizar backend y base de datos con Docker y desplegar el servidor en la nube, para no depender de la PC del desarrollador.

**Aplicación móvil**
- Configurar la URL del backend por variantes de compilación (`debug` / `release`) en lugar de una constante que cambia con cada túnel.
- Sustituir `fallbackToDestructiveMigration` por migraciones explícitas de Room.
- Dividir `VirtualAulaRepository` en repositorios por dominio (usuarios, académico, mensajería).
- Valorar una migración gradual a Kotlin y Jetpack Compose.

**Calidad y mantenimiento**
- Ampliar las pruebas: hoy existen pruebas unitarias de los importadores de Excel; faltan pruebas del repositorio y de los endpoints.
- Configurar integración continua con GitHub Actions para compilar y probar en cada cambio.
- Añadir descripción, temas y una licencia al repositorio.