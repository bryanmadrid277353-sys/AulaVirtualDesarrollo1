# 2. Roles y Permisos

[⬅️ Anterior](01_introduccion_y_arquitectura.md) | [📑 Índice](00_INDICE.md) | [Siguiente ➡️](03_autenticacion_y_seguridad.md)

Este documento describe los tipos de usuario definidos en el sistema **Aula Virtual**, la matriz de permisos y capacidades por rol, la lógica de redirección tras el inicio de sesión y las cuentas de prueba disponibles en la base de datos.

---

## Tabla de Contenido
- [2.1 Tipos de Usuario (Roles)](#21-tipos-de-usuario-roles)
- [2.2 Matriz de Permisos y Capacidades](#22-matriz-de-permisos-y-capacidades)
- [2.3 Lógica de Redirección según Rol](#23-lógica-de-redirección-según-rol)
- [2.4 Cuentas de Demostración](#24-cuentas-de-demostración)

---

## 2.1 Tipos de Usuario (Roles)

El sistema define tres roles principales de acceso en la tabla `users` (columna `role`):

1. **`ADMIN` (Administrador del Sistema)**:
   - Posee control total sobre el sistema institucional.
   - Administra cuentas de usuarios (crear/editar/eliminar Administradores, Profesores, Encargados).
   - Administra registros de Estudiantes menores de edad y su vinculación con Encargados.
   - Crea y asigna Materias/Asignaturas a Profesores.
   - Configura Instalaciones físicas (Aulas, Laboratorios) y asigna sus Horarios de uso.
   - Define el Calendario Académico (Períodos de 1 a 5 y Días no lectivos/Vacaciones).
   - Puede simular la identidad de cualquier otro usuario mediante la función de "Impersonación".

2. **`PROFESSOR` (Docente / Profesor)**:
   - Tiene acceso a las materias que le han sido asignadas por el Administrador.
   - Consulta el listado de alumnos inscritos en sus asignaturas.
   - Modifica y actualiza las calificaciones por período (Notas 1 a 5) de sus estudiantes.
   - Registra y gestiona la Asistencia Diaria por fecha y materia (Presente, Ausente, Llegada Tardía, Permiso).
   - Evalúa los 5 criterios del módulo de Conducta y escribe observaciones pedagógicas.
   - Genera e importa plantillas masivas en formato Excel (`.xlsx`) para notas y conducta.
   - Crea entradas en el Foro/Blog de sus materias y responde comentarios.
   - Emite notificaciones directas o citatorios a los encargados de sus alumnos.

3. **`ENCARGADO` (Encargado de Estudiante / Padre o Tutor)**:
   - Representa al responsable legal de uno o más estudiantes menores de edad (quienes no poseen cuenta ni clave propia).
   - Selecciona entre los alumnos asignados a su cargo mediante un selector desplegable.
   - Visualiza el cuadro de honor/resumen de calificaciones por período de sus representados.
   - Revisa el desglose detallado de conducta (los 5 criterios de evaluación) y observaciones del docente.
   - Consulta el historial completo de asistencia (días asistidos, faltas, tardanzas).
   - Participa en el Foro/Blog de las materias de sus alumnos y envía mensajes directos en tiempo real al docente.
   - Recibe notificaciones push institucionales y avisos individuales.

[⬆️ Volver al inicio del documento](#2-roles-y-permisos)

---

## 2.2 Matriz de Permisos y Capacidades

Ruta fuente de validaciones: `app/src/main/java/com/aula/virtual/ui/MainActivity.java` y `backend/main.py`.

| Módulo / Función | `ADMIN` | `PROFESSOR` | `ENCARGADO` |
| :--- | :---: | :---: | :---: |
| **Inicio de Sesión y Biometría** | ✅ Sí | ✅ Sí | ✅ Sí |
| **Gestión de Usuarios (Admins, Profes, Encargados)** | ✅ Crear/Editar/Eliminar | ❌ No | ❌ No |
| **Gestión de Estudiantes (Crear / Asignar Encargado)** | ✅ Sí | 👁️ Solo Lectura (sus materias) | 👁️ Solo Lectura (sus hijos) |
| **Gestión de Materias (Crear / Asignar Profe)** | ✅ Sí | 👁️ Ver asignadas | 👁️ Ver inscritas de sus hijos |
| **Inscripción de Alumnos a Materias** | ✅ Sí | ❌ No | ❌ No |
| **Gestión de Notas (Ingreso / Edición)** | ✅ Override global | ✅ En sus materias | 👁️ Solo Lectura |
| **Gestión de Conducta (Evaluación 1-5)** | ✅ Override global | ✅ En sus materias | 👁️ Solo Lectura |
| **Gestión de Asistencia (Tomar lista por fecha)** | ✅ Override global | ✅ En sus materias | 👁️ Solo Lectura |
| **Importar / Exportar Excel (.xlsx)** | ✅ Sí | ✅ En sus materias | ❌ No |
| **Gestión de Instalaciones y Horarios Físicos** | ✅ Crear/Editar | 👁️ Solo Lectura (Consultar) | 👁️ Solo Lectura (Consultar) |
| **Calendario Académico (Períodos / Días libres)** | ✅ Configurar | 👁️ Consultar | 👁️ Consultar |
| **Foro / Blog de Materia** | ✅ Moderar / Publicar | ✅ Publicar / Comentar | ✅ Comentar / Ver |
| **Mensajería Directa (Chat)** | ✅ Con todos | ✅ Con Encargados / Admins | ✅ Con Profesores / Admins |
| **Emisión de Notificaciones Masivas** | ✅ Sí | ✅ A sus alumnos | ❌ No |
| **Impersonación de Usuarios** | ✅ Sí | ❌ No | ❌ No |
| **Copia de Seguridad y Restauración DB Local** | ✅ Sí | ❌ No | ❌ No |

[⬆️ Volver al inicio del documento](#2-roles-y-permisos)

---

## 2.3 Lógica de Redirección según Rol

Tras la autenticación exitosa en `LoginFragment.java`, las credenciales y la entidad `User` se persisten mediante `CredentialsManager.java` y se notifica a `MainActivity.java` para configurar el menú lateral (Navigation Drawer) y redirigir al fragmento inicial según el rol del usuario:

```java
// Ubicación: app/src/main/java/com/aula/virtual/MainActivity.java
private void setupRoleBasedNavigation(String role) {
    NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.nav_graph);
    if ("ADMIN".equalsIgnoreCase(role)) {
        navGraph.setStartDestination(R.id.adminHomeFragment);
        navigationView.getMenu().clear();
        navigationView.inflateMenu(R.menu.drawer_menu_admin);
    } else if ("PROFESSOR".equalsIgnoreCase(role)) {
        navGraph.setStartDestination(R.id.professorHomeFragment);
        navigationView.getMenu().clear();
        navigationView.inflateMenu(R.menu.drawer_menu_professor);
    } else { // ENCARGADO / Default
        navGraph.setStartDestination(R.id.studentHomeFragment);
        navigationView.getMenu().clear();
        navigationView.inflateMenu(R.menu.drawer_menu_student);
    }
    navController.setGraph(navGraph);
}
```

[⬆️ Volver al inicio del documento](#2-roles-y-permisos)

---

## 2.4 Cuentas de Demostración

Ubicación fuente: `backend/seed_demo.sql` e `init.sql`.

Estas cuentas de prueba se insertan automáticamente al inicializar la base de datos con los scripts de seed:

| Rol | Carnet (Usuario) | Contraseña Plano | Hash BCrypt en DB | Nombre de Usuario |
| :--- | :--- | :--- | :--- | :--- |
| **Administrador** | `ADM001` | `admin123` | `$2b$12$...` | Administrador General |
| **Profesor** | `PROF001` | `prof123` | `$2b$12$...` | Carlos Mendoza |
| **Profesor** | `PROF002` | `prof123` | `$2b$12$...` | Ana Martínez |
| **Encargado** | `ENC001` | `enc123` | `$2b$12$...` | Roberto Gómez (Padre de Juan Gómez) |
| **Encargado** | `ENC002` | `enc123` | `$2b$12$...` | Elena Rodríguez (Madre de Sofía Rodríguez) |

> [!NOTE]
> Los estudiantes (ejemplo: Juan Gómez `EST001`, Sofía Rodríguez `EST002`) no poseen contraseñas de acceso al sistema; sus registros académicos son gestionados exclusivamente por sus respectivos Encargados (`ENC001` y `ENC002`).

[⬆️ Volver al inicio del documento](#2-roles-y-permisos)

---
[⬅️ Anterior](01_introduccion_y_arquitectura.md) | [📑 Índice](00_INDICE.md) | [Siguiente ➡️](03_autenticacion_y_seguridad.md)
