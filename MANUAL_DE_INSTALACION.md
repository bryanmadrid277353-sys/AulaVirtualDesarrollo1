# 📘 Guía de Instalación y Configuración del Proyecto - Aula Virtual

Este documento proporciona los pasos detallados y las herramientas necesarias para instalar, configurar y ejecutar el proyecto **Aula Virtual** desde cero en una nueva computadora con una instalación reciente de **Android Studio**.

---

## 🔗 Links de Descarga de Requisitos Previos

Antes de comenzar, asegúrate de descargar e instalar las siguientes herramientas en la nueva PC:

| Herramienta | Descripción | Link de Descarga |
| :--- | :--- | :--- |
| **Android Studio** | IDE oficial para desarrollo Android (incluye Android SDK, Emulador y JDK) | [Descargar Android Studio](https://developer.android.com/studio) |
| **XAMPP** | Servidor local para gestionar la base de datos MySQL / MariaDB | [Descargar XAMPP](https://www.apachefriends.org/es/index.html) |
| **Python** (v3.10 o superior) | Entorno de ejecución para el servidor backend FastAPI | [Descargar Python](https://www.python.org/downloads/) |
| **Git** | Control de versiones para clonar el repositorio | [Descargar Git](https://git-scm.com/downloads) |

---

## 🛠️ Pasos de Instalación y Configuración

### Paso 1: Clonar u Obtener el Proyecto

Abre la terminal o PowerShell y clona el repositorio (o copia la carpeta del proyecto en tu disco local):

```bash
git clone <URL_DEL_REPOSITORIO>
cd MyApplication
```

---

### Paso 2: Configurar la Base de Datos (MySQL con XAMPP)

1. Abre el **XAMPP Control Panel**.
2. Inicia el servicio **MySQL** (y **Apache** si deseas usar phpMyAdmin).
3. Abre tu navegador web e ingresa a phpMyAdmin:
   [http://localhost/phpmyadmin/](http://localhost/phpmyadmin/)
4. Crea una nueva base de datos llamada `aula_virtual`:
   - Haz clic en **Nueva**.
   - Nombre de la base de datos: `aula_virtual`.
   - Cotejamiento: `utf8mb4_unicode_ci`.
   - Haz clic en **Crear**.
5. Importa la estructura y datos iniciales:
   - Selecciona la base de datos `aula_virtual`.
   - Ve a la pestaña **Importar**.
   - Selecciona el archivo `backend/init.sql` ubicado en la carpeta del proyecto.
   - Haz clic en **Importar** (o **Continuar**) al final de la página.

> **Método alternativo por Consola/Terminal:**
> ```bash
> mysql -u root -e "CREATE DATABASE aula_virtual CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
> mysql -u root aula_virtual < backend/init.sql
> ```

---

### Paso 3: Configurar y Levantar el Servidor Backend (Python) desde la Terminal de Android Studio

Puedes ejecutar todo el servidor backend directamente desde la pestaña **Terminal integrada de Android Studio** sin necesidad de abrir ventanas externas.

#### 1. Abrir la Terminal en Android Studio:
- En la parte inferior de Android Studio, haz clic en la pestaña **Terminal** (o presiona `Alt + F12` en Windows/Linux, `Option + F12` en Mac).

#### 2. Entrar a la carpeta backend:
```bash
cd backend
```

#### 3. Crear y activar el Entorno Virtual de Python (Recomendado):
- **En Windows (Terminal de Android Studio / PowerShell):**
  ```powershell
  python -m venv venv
  .\venv\Scripts\activate
  ```
- **En macOS / Linux (Terminal de Android Studio):**
  ```bash
  python3 -m venv venv
  source venv/bin/activate
  ```
  *(Verás que la terminal ahora muestra `(venv)` al inicio de la línea de comandos).*

#### 4. Instalar las dependencias del servidor:
Copia y ejecuta el siguiente comando para instalar todos los paquetes requeridos:
```bash
pip install fastapi uvicorn sqlalchemy mysql-connector-python pydantic
```

#### 5. Verificar credenciales de la base de datos MySQL:
Abre desde el explorador de archivos de Android Studio el archivo `backend/main.py` y confirma la cadena de conexión:
```python
DATABASE_URL = "mysql+mysqlconnector://root:@localhost/aula_virtual"
```
*(Si tu usuario `root` de MySQL en XAMPP tiene contraseña, colócala entre `root:` y `@localhost`, por ejemplo: `root:mi_password@localhost`).*

#### 6. Ejecutar el Servidor Backend:
Ejecuta el script ejecutable de Python directamente:
```powershell
py backend/run_backend.py
```
*O ejecuta el archivo de comandos de Windows:*
```cmd
.\backend\run_backend.bat
```

#### 7. Comprobación del servidor:
Una vez iniciado, verás un mensaje que dice `Application startup complete` e `Uvicorn running on http://0.0.0.0:8000`.
Abre tu navegador e ingresa a:
👉 [http://localhost:8000/docs](http://localhost:8000/docs) (debe cargar la documentación interactiva Swagger UI de FastAPI).

> 💡 **Nota:** Deja esta pestaña de la Terminal de Android Studio abierta mientras estés probando la aplicación para que el servidor permanezca activo.


---

### Paso 4: Configurar la Aplicación Android en Android Studio

1. Abre **Android Studio**.
2. En la pantalla principal, selecciona **Open** y elige la carpeta raíz del proyecto (`MyApplication`).
3. Espera a que Android Studio descargue las dependencias Gradle y sincronice el proyecto (**Gradle Sync**).
4. Verifica que la versión del JDK de Gradle esté configurada en Java 17 o Java 11:
   - Ve a `File` > `Settings` (o `Android Studio` > `Settings` en macOS).
   - Navega a `Build, Execution, Deployment` > `Build Tools` > `Gradle`.
   - En **Gradle JDK**, selecciona `Embedded JDK` o `JDK 17`.

---

### Paso 5: Configurar el Modo de Conexión del Backend mediante local.properties

La dirección URL del backend **nunca se edita en el código fuente** Java (`RetrofitClient.java`). Se selecciona cambiando una sola línea (`backend.mode`) en el archivo `local.properties` en la raíz del proyecto, inyectándose en tiempo de compilación a través de `BuildConfig.BASE_URL` y `BuildConfig.BACKEND_MODE`.

1. Copia el archivo de plantilla `local.properties.example` y renómbralo como `local.properties` (o abre tu archivo `local.properties` existente).
2. Selecciona el modo de conexión deseado en la clave `backend.mode` (`emulador`, `lan`, `cloudflare` o `tailscale`) y ajusta la URL correspondiente:

```properties
# local.properties (NO se sube a Git)
backend.mode=emulador

backend.url.emulador=http://10.0.2.2:8000/
backend.url.lan=http://192.168.1.35:8000/
backend.url.cloudflare=https://api.tudominio.com/
backend.url.tailscale=http://100.x.y.z:8000/
backend.url.release=https://api.tudominio.com/
```

---

#### 📊 Tabla Comparativa de Modos de Conexión

| Modo (`backend.mode`) | Escenario de Uso | ¿Requiere HTTPS? | ¿Expone a Internet? | Apto para Release | Requisitos |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **`emulador`** | Pruebas en emulador AVD de Android Studio | No (`http://`) | No | No | Emulador Android corriendo en la misma PC |
| **`lan`** | Teléfono físico en la misma red Wi-Fi | No (`http://`) | No | No | PC y teléfono en el mismo router/Wi-Fi |
| **`cloudflare`** | Acceso remoto público con Quick Tunnel | Sí (`https://`) | Sí (público) | No | `run_cloudflare_tunnel.bat` o ejecutable `cloudflared` |
| **`tailscale`** | VPN mesh privada entre PC y teléfono | No (`http://`) | No (red privada) | No | App Tailscale instalada en PC y teléfono con misma cuenta |

> ⚠️ **Importante sobre entornos de prueba:** Los modos `emulador`, `lan`, `cloudflare` y `tailscale` son exclusivamente para desarrollo y pruebas. Para compilaciones de `release`, Gradle ignora `backend.mode` y utiliza estrictamente `backend.url.release` con protocolo seguro HTTPS.

---

#### Opción A: Si usas el Emulador de Android Studio (AVD)
El emulador de Android accede a la PC local a través de la IP virtual `10.0.2.2`:
```properties
backend.mode=emulador
backend.url.emulador=http://10.0.2.2:8000/
```

#### Opción B: Si usas un Dispositivo Físico (Teléfono en la misma Wi-Fi)
1. Asegúrate de que tu teléfono y tu PC estén conectados a la **misma red Wi-Fi**.
2. Obtén la IP local de tu computadora con `ipconfig` (Windows) o `ifconfig` (macOS/Linux), por ejemplo `192.168.1.35`.
3. Configura en `local.properties`:
   ```properties
   backend.mode=lan
   backend.url.lan=http://192.168.1.35:8000/
   ```

#### Opción C: Despliegue Remoto Global HTTPS con Cloudflare Quick Tunnel (`cloudflared`)
1. Inicia FastAPI en `http://localhost:8000`.
2. Ejecuta el script del túnel en la terminal de Android Studio: `.\backend\run_cloudflare_tunnel.bat`.
3. El script actualizará automáticamente `backend.url.debug` o `backend.url.cloudflare` en tu `local.properties`.
4. Configura en `local.properties`:
   ```properties
   backend.mode=cloudflare
   backend.url.cloudflare=https://xxxx.trycloudflare.com/
   ```

#### Opción D: Conexión Remota Privada mediante Tailscale (VPN Mesh)
Tailscale crea una red privada segura (Tailnet) entre tus dispositivos (PC y teléfono) sin exponer el backend a internet público ni requerir túneles efímeros.

1. **Crear cuenta en Tailscale:** Regístrate gratis en [tailscale.com](https://tailscale.com) (inicia sesión con Google, GitHub o Microsoft).
2. **Instalar en la PC:** Descarga e instala Tailscale en tu computadora de desarrollo e inicia sesión.
3. **Instalar en el Teléfono:** Descarga Tailscale desde Google Play Store en tu teléfono móvil e inicia sesión con la **misma cuenta**.
4. **Obtener la dirección de la PC:** En la app de Tailscale (o en el panel de administración), copia la IP IPv4 privada de la PC (forma `100.x.y.z`) o el nombre MagicDNS de la PC (ej.: `mi-pc.tailXXXX.ts.net`).
5. **Verificar el Backend y Firewall:**
   - Asegúrate de que Uvicorn se ejecute escuchando en todas las interfaces: `uvicorn main:app --host 0.0.0.0 --port 8000`.
   - Revisa el Firewall de Windows para permitir tráfico entrante en el puerto `8000` a través de la interfaz de red de Tailscale.
6. **Probar conectividad desde el teléfono:** Abre el navegador en el teléfono móvil e ingresa a `http://100.x.y.z:8000/docs` o `http://mi-pc.tailXXXX.ts.net:8000/docs`. Debe cargar la documentación interactiva Swagger de FastAPI.
7. **Configurar en `local.properties`:**
   ```properties
   backend.mode=tailscale
   backend.url.tailscale=http://100.x.y.z:8000/
   ```
   *(o `backend.url.tailscale=http://mi-pc.tailXXXX.ts.net:8000/`)*

> 💡 **Nota sobre uso de Tailscale:** Tailscale es para uso personal/desarrollo no comercial en su plan gratuito. Cada dispositivo de prueba debe tener la app de Tailscale activa conectada a la misma tailnet.

---

### Paso 6: Compilar y Ejecutar la Aplicación

1. Tras cambiar `backend.mode` o cualquier URL en `local.properties`, realiza **Sync Project with Gradle Files** en Android Studio.
2. Abre el **Device Manager** o conecta tu teléfono físico.
3. Haz clic en el botón **Run 'app'** (`Shift + F10`).
4. Revisa Logcat filtrando por `RetrofitClient` para verificar el modo y URL en uso:
   `D/RetrofitClient: Backend Mode: tailscale, Base URL: http://100.x.y.z:8000/`

#### 🔑 Credenciales de Prueba por Rol:

| Rol | Carnet | Contraseña |
| :--- | :--- | :--- |
| **Administrador** | `ADM001` | `admin` |
| **Docente / Profesor** | `DOC001` | `profesor` |
| **Estudiante** | `EST001` | `estudiante` |

---

## ❓ Solución de Problemas Comunes

1. **Modo no válido o error de compilación Gradle en `backend.mode`:**
   - Si colocas un modo no soportado en `local.properties`, la compilación se detendrá con el mensaje: `Error en local.properties: 'backend.mode' no es válido... Modos permitidos: emulador, lan, cloudflare, tailscale`.
   - Corrige la clave `backend.mode` en `local.properties` y vuelve a sincronizar.

2. **Teléfono no conecta en modo Tailscale (`http://100.x.y.z:8000/`):**
   - **App no activa:** Revisa que la VPN de Tailscale esté en estado `Connected` tanto en la PC como en el teléfono.
   - **Cuentas distintas:** Confirma que ambos dispositivos hayan iniciado sesión con la misma cuenta de usuario.
   - **Bloqueo de Firewall:** Confirma que el Firewall de Windows permita conexiones entrantes al puerto `8000` en redes privadas/públicas.
   - **Uvicorn en localhost:** Confirma que el backend ejecute Uvicorn con `--host 0.0.0.0` y no únicamente `--host 127.0.0.1`.

3. **Error `Cleartext HTTP traffic not permitted`:**
   - Ocurre si intentas usar un esquema `http://` en la compilación de `release`.
   - En variante `release`, el build exige obligatoriamente un protocolo HTTPS seguro (`https://`). Si `backend.url.release` en `local.properties` inicia con `http://`, Gradle detendrá el build.
   - Confirma que estás compilando la variante `debug` (`app:assembleDebug`) cuando utilices URLs HTTP sin cifrar (`10.0.2.2`, `192.168.x.x` o `100.x.y.z`).

4. **URL sin `/` final o sin protocolo `http://` / `https://`:**
   - Gradle normaliza automáticamente las URLs agregando `/` al final si falta.
   - Si la URL no empieza por `http://` o `https://`, Gradle cancelará el build con una advertencia explicativa.

5. **Túnel Cloudflare expirado (`*.trycloudflare.com` no responde):**
   - Vuelve a ejecutar `run_cloudflare_tunnel.bat`, copia la nueva URL en `backend.url.cloudflare` de `local.properties` y vuelve a compilar la app.

6. **Error al conectar con la base de datos en Python (`Can't connect to MySQL server`):**
   - Verifica que el servicio MySQL esté iniciado en el panel de XAMPP y revisa `DATABASE_URL` en `backend/main.py`.
