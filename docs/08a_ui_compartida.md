# 8a. Interfaz de Usuario: Pantallas Compartidas

[⬅️ Anterior](07_repositorios_y_viewmodels.md) | [📑 Índice](00_INDICE.md) | [Siguiente ➡️](08b_ui_administrador.md)

Este documento forma parte del módulo de Interfaz de Usuario y describe la actividad principal (`MainActivity`), así como los fragmentos, diálogos y adaptadores compartidos accesibles por múltiples roles dentro de la aplicación **Aula Virtual**.

---

## Tabla de Contenido
- [8a.1 Resumen de Pantallas Compartidas](#8a1-resumen-de-pantallas-compartidas)
- [8a.2 Actividad Principal (MainActivity)](#8a2-actividad-principal-mainactivity)
- [8a.3 Pantalla de Inicio de Sesión (LoginFragment)](#8a3-pantalla-de-inicio-de-sesión-loginfragment)
- [8a.4 Perfil de Usuario (StudentProfileFragment)](#8a4-perfil-de-usuario-studentprofilefragment)
- [8a.5 Ajustes y Configuración (SettingsFragment y SettingsAdvancedFragment)](#8a5-ajustes-y-configuración-settingsfragment-y-settingsadvancedfragment)
- [8a.6 Notificaciones (NotificationListFragment y NotificationSettingsFragment)](#8a6-notificaciones-notificationlistfragment-y-notificationsettingsfragment)
- [8a.7 Mensajería Directa (DirectMessagesFragment y DirectChatFragment)](#8a7-mensajería-directa-directmessagesfragment-y-directchatfragment)
- [8a.8 Foros de Discusión (SubjectBlogFragment y BlogDiscussionFragment)](#8a8-foros-de-discusión-subjectblogfragment-y-blogdiscussionfragment)
- [8a.9 Calendario Académico (AcademicCalendarFragment)](#8a9-calendario-académico-academiccalendarfragment)

---

## 8a.1 Resumen de Pantallas Compartidas

| Pantalla / Clase | File Path | Layout XML | Propósito Principal |
| :--- | :--- | :--- | :--- |
| `MainActivity` | `.../MainActivity.java` | `activity_main.xml` | Contenedor principal con Toolbar, DrawerNavigation y BottomNavigationView. |
| `LoginFragment` | `.../ui/LoginFragment.java` | `fragment_login.xml` | Autenticación con carnet/clave, huella biométrica y selector de servidor. |
| `StudentProfileFragment` | `.../ui/StudentProfileFragment.java` | `fragment_student_profile.xml` | Muestra datos del usuario, foto de perfil, carnet digital y permite cambio de clave. |
| `SettingsFragment` | `.../ui/SettingsFragment.java` | `fragment_settings.xml` | Opciones generales: tema oscuro, notificaciones, cambio de cuenta y acerca de. |
| `SettingsAdvancedFragment` | `.../ui/SettingsAdvancedFragment.java`| `fragment_settings_advanced.xml` | Panel avanzado: backup JSON/SQL, sincronización offline, impersonación. |
| `NotificationListFragment` | `.../ui/NotificationListFragment.java` | `fragment_notification_list.xml` | Historial de notificaciones recibidas con indicador de no leídas y búsqueda. |
| `NotificationSettingsFragment`| `.../ui/NotificationSettingsFragment.java`| `fragment_notification_settings.xml`| Configuración de frecuencia y alertas de notificaciones. |
| `DirectMessagesFragment` | `.../ui/DirectMessagesFragment.java` | `fragment_direct_messages.xml` | Listado de chats activos y botón para iniciar nueva conversación. |
| `DirectChatFragment` | `.../ui/DirectChatFragment.java` | `fragment_direct_chat.xml` | Chat en vivo estilo WhatsApp entre dos usuarios con burbujas de mensaje. |
| `SubjectBlogFragment` | `.../ui/SubjectBlogFragment.java` | `fragment_subject_blog.xml` | Foro de la materia con lista de temas de discusión publicados por el docente. |
| `BlogDiscussionFragment` | `.../ui/BlogDiscussionFragment.java` | `fragment_blog_discussion.xml` | Hilo de discusión con entrada completa y caja para enviar comentarios. |
| `AcademicCalendarFragment` | `.../ui/AcademicCalendarFragment.java` | `fragment_academic_calendar.xml` | Vista de períodos lectivos (1-5), fechas inicio/fin y lista de asuetos. |

[⬆️ Volver al inicio del documento](#8a-interfaz-de-usuario-pantallas-compartidas)

---

## 8a.2 Actividad Principal (MainActivity)

- **Ruta del Archivo**: `app/src/main/java/com/aula/virtual/MainActivity.java`
- **Layout Asociado**: `app/src/main/res/layout/activity_main.xml`

### Descripción y Elementos Principales
`MainActivity` es la actividad única (`Single Activity Architecture`) que alberga el `NavHostFragment`. Administra la barra superior (`MaterialToolbar`), el panel lateral desplegable (`DrawerLayout` + `NavigationView`) y la barra de navegación inferior opcional (`BottomNavigationView`).

### Ciclo de Vida y Métodos Clave
- `onCreate(Bundle savedInstanceState)`: Configura el tema con `ThemeHelper`, inicializa ViewBinding, el `NavController` y escucha cambios de destino para ajustar el título de la barra.
- `setupRoleBasedNavigation(String role)`: Limpia e infla dinámicamente el menú del Drawer según el rol (`R.menu.drawer_menu_admin`, `R.menu.drawer_menu_professor` o `R.menu.drawer_menu_student`).
- `onDestinationChanged(...)`: Oculta o muestra la Toolbar/Drawer dependiendo de si la pantalla actual es `LoginFragment`.

[⬆️ Volver al inicio del documento](#8a-interfaz-de-usuario-pantallas-compartidas)

---

## 8a.3 Pantalla de Inicio de Sesión (LoginFragment)

- **Ruta del Archivo**: `app/src/main/java/com/aula/virtual/ui/LoginFragment.java`
- **Layout Asociado**: `app/src/main/res/layout/fragment_login.xml`

### Descripción y Elementos Principales
Permite el ingreso de usuarios mediante carnet institucional y contraseña, así como el acceso directo por huella digital mediante `BiometricPrompt`.

### Diálogos Asociados
- **`dialog_configure_server.xml`**: Cuadro de diálogo *"Configurar Conexión"* que permite seleccionar el modo de servidor (Cloudflare Tunnel, Emulador, Red Local Wi-Fi o VPN Tailscale) y ajustar la URL Base del backend.
- **`dialog_saved_accounts.xml`**: Permite seleccionar una cuenta previamente guardada en `CredentialsManager`.

[⬆️ Volver al inicio del documento](#8a-interfaz-de-usuario-pantallas-compartidas)

---

## 8a.4 Perfil de Usuario (StudentProfileFragment)

- **Ruta del Archivo**: `app/src/main/java/com/aula/virtual/ui/StudentProfileFragment.java`
- **Layout Asociado**: `app/src/main/res/layout/fragment_student_profile.xml`

### Descripción y Elementos Principales
Visualiza la ficha de información personal del usuario logueado, su foto de avatar, carnet digital institucional con código de barras/QR simulado y botones para editar datos o modificar la clave.

### Diálogos Asociados
- **`dialog_edit_profile.xml`**: Edición de nombre, correo y dirección domiciliar.
- **`dialog_change_password.xml`**: Validación de contraseña actual y nueva clave con hasher BCrypt.
- **`dialog_change_avatar.xml`** / **`dialog_crop_image.xml`**: Selección de imagen desde cámara/galería y recorte mediante `CropImageView.java`.

[⬆️ Volver al inicio del documento](#8a-interfaz-de-usuario-pantallas-compartidas)

---

## 8a.5 Ajustes y Configuración (SettingsFragment y SettingsAdvancedFragment)

- **Rutas de Archivo**: `.../ui/SettingsFragment.java` y `.../ui/SettingsAdvancedFragment.java`
- **Layouts**: `fragment_settings.xml` y `fragment_settings_advanced.xml`

### Descripción y Elementos Principales
- `SettingsFragment`: Ofrece modificadores para alternar el Modo Oscuro/Claro mediante `ThemeHelper`, configurar notificaciones y cerrar sesión.
- `SettingsAdvancedFragment`: Herramientas avanzadas de administración local. Permite realizar respaldos de la base de datos Room (`LocalDatabaseBackupManager.java`), importar archivos JSON de restauración, sincronizar registros pendientes en offline y ejecutar la simulación de sesión (**Impersonación** de otros usuarios).

[⬆️ Volver al inicio del documento](#8a-interfaz-de-usuario-pantallas-compartidas)

---

## 8a.6 Notificaciones (NotificationListFragment y NotificationSettingsFragment)

- **Rutas de Archivo**: `.../ui/NotificationListFragment.java` y `.../ui/NotificationSettingsFragment.java`
- **Layouts**: `fragment_notification_list.xml` y `fragment_notification_settings.xml`

### Descripción y Elementos Principales
Muestra la lista de notificaciones institucionales e individuales. Permite filtrar por leídas/no leídas, marcar todas como leídas y abrir el cuadro de diálogo `dialog_create_notification.xml` (si el usuario es Admin o Profesor) para enviar avisos masivos por FCM.

[⬆️ Volver al inicio del documento](#8a-interfaz-de-usuario-pantallas-compartidas)

---

## 8a.7 Mensajería Directa (DirectMessagesFragment y DirectChatFragment)

- **Rutas de Archivo**: `.../ui/DirectMessagesFragment.java` y `.../ui/DirectChatFragment.java`
- **Layouts**: `fragment_direct_messages.xml` y `fragment_direct_chat.xml`
- **Adapters**: `ChatRoomAdapter.java` y `NewChatContactAdapter.java`

### Descripción y Elementos Principales
- `DirectMessagesFragment`: Presenta las conversaciones activas ordenadas por la fecha del último mensaje. Incluye un botón flotante (FAB) que despliega `dialog_new_chat_selector.xml` para buscar contactos y crear una nueva sala de chat.
- `DirectChatFragment`: Interfaz de chat individual en tiempo real. Utiliza un `RecyclerView` con burbujas de diálogo diferenciadas (`bg_bubble_me.xml` a la derecha en azul y `bg_bubble_others.xml` a la izquierda en gris), actualización periódica/polling y caja de texto con botón enviar.

[⬆️ Volver al inicio del documento](#8a-interfaz-de-usuario-pantallas-compartidas)

---

## 8a.8 Foros de Discusión (SubjectBlogFragment y BlogDiscussionFragment)

- **Rutas de Archivo**: `.../ui/SubjectBlogFragment.java` y `.../ui/BlogDiscussionFragment.java`
- **Layouts**: `fragment_subject_blog.xml` y `fragment_blog_discussion.xml`
- **Adapters**: `BlogAdapter.java` y `BlogCommentAdapter.java`

### Descripción y Elementos Principales
- `SubjectBlogFragment`: Lista los temas de discusión publicados por el docente dentro de una materia específica. Los profesores pueden crear nuevos temas o reordenarlos mediante arrastre.
- `BlogDiscussionFragment`: Muestra el contenido detallado de un tema y la lista cronológica de comentarios de los participantes (profesores y encargados), permitiendo agregar nuevas respuestas.

[⬆️ Volver al inicio del documento](#8a-interfaz-de-usuario-pantallas-compartidas)

---

## 8a.9 Calendario Académico (AcademicCalendarFragment)

- **Ruta del Archivo**: `app/src/main/java/com/aula/virtual/ui/AcademicCalendarFragment.java`
- **Layout Asociado**: `app/src/main/res/layout/fragment_academic_calendar.xml`

### Descripción y Elementos Principales
Despliega el calendario institucional dividiendo el año lectivo en los 5 Períodos evaluativos con sus rangos de fechas de inicio y fin. Adicionalmente, muestra la lista de días no lectivos / feriados (`non_school_days`). Si el usuario posee el rol `ADMIN`, se habilitan botones para editar las fechas de los períodos o agregar días de asueto.

[⬆️ Volver al inicio del documento](#8a-interfaz-de-usuario-pantallas-compartidas)

---
[⬅️ Anterior](07_repositorios_y_viewmodels.md) | [📑 Índice](00_INDICE.md) | [Siguiente ➡️](08b_ui_administrador.md)
