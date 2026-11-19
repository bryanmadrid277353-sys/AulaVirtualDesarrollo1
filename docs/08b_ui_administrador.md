# 8b. Interfaz de Usuario: Módulo Administrador

[⬅️ Anterior](08a_ui_compartida.md) | [📑 Índice](00_INDICE.md) | [Siguiente ➡️](08c_ui_profesor.md)

Este documento detalla todas las pantallas, diálogos, adapters y componentes de la interfaz de usuario pertenecientes al rol **Administrador (`ADMIN`)** en la aplicación **Aula Virtual**.

---

## Tabla de Contenido
- [8b.1 Resumen de Pantallas del Administrador](#8b1-resumen-de-pantallas-del-administrador)
- [8b.2 Dashboard Administrador (AdminHomeFragment)](#8b2-dashboard-administrador-adminhomefragment)
- [8b.3 Gestión de Usuarios (AdminAdminListFragment, AdminProfessorListFragment, AdminStudentListFragment)](#8b3-gestión-de-usuarios-adminadminlistfragment-adminprofessorlistfragment-adminstudentlistfragment)
- [8b.4 Detalle de Encargado y Asignación (AdminEncargadoDetailFragment)](#8b4-detalle-de-encargado-y-asignación-adminencargadodetailfragment)
- [8b.5 Gestión de Materias e Inscripciones (AdminSubjectListFragment y AdminEnrollmentListFragment)](#8b5-gestión-de-materias-e-inscripciones-adminsubjectlistfragment-y-adminenrollmentlistfragment)
- [8b.6 Gestión de Instalaciones y Horarios (AdminFacilityListFragment, AdminFacilityDetailFragment, AdminFacilitySchedulesFragment, AdminFacilityTimetableFragment)](#8b6-gestión-de-instalaciones-y-horarios-adminfacilitylistfragment-adminfacilitydetailfragment-adminfacilityschedulesfragment-adminfacilitytimetablefragment)
- [8b.7 Edición Global de Calificaciones (AdminEditGradeFragment y AdminDetailFragment)](#8b7-edición-global-de-calificaciones-admineditgradefragment-y-admindetailfragment)

---

## 8b.1 Resumen de Pantallas del Administrador

| Clase | File Path | Layout XML | Descripción / Funcionalidad |
| :--- | :--- | :--- | :--- |
| `AdminHomeFragment` | `.../ui/AdminHomeFragment.java` | `fragment_admin_home.xml` | Panel de control central con tarjetas de métricas globales y accesos a cada catálogo. |
| `AdminAdminListFragment` | `.../ui/AdminAdminListFragment.java` | `fragment_admin_dashboard.xml` | Listado y registro de usuarios con rol Administrador. |
| `AdminProfessorListFragment`| `.../ui/AdminProfessorListFragment.java`| `fragment_admin_dashboard.xml` | Listado y alta de docentes con asignación de materias. |
| `AdminStudentListFragment` | `.../ui/AdminStudentListFragment.java` | `fragment_admin_dashboard.xml` | Gestión de estudiantes menores de edad y búsqueda por carnet/nombre. |
| `AdminEncargadoDetailFragment`| `.../ui/AdminEncargadoDetailFragment.java`| `fragment_admin_encargado_detail.xml`| Ficha del Encargado y vinculación de alumnos a su cargo. |
| `AdminSubjectListFragment` | `.../ui/AdminSubjectListFragment.java` | `fragment_admin_dashboard.xml` | Catálogo de materias, asignación de docentes y color del curso. |
| `AdminEnrollmentListFragment`| `.../ui/AdminEnrollmentListFragment.java`| `fragment_admin_dashboard.xml` | Gestión de inscripciones de estudiantes en materias. |
| `AdminFacilityListFragment` | `.../ui/AdminFacilityListFragment.java` | `fragment_admin_dashboard.xml` | Lista de aulas, laboratorios y canchas deportivas. |
| `AdminFacilityDetailFragment`| `.../ui/AdminFacilityDetailFragment.java`| `fragment_admin_facility_detail.xml`| Ficha técnica y capacidad de una instalación específica. |
| `AdminFacilitySchedulesFragment`|`.../ui/AdminFacilitySchedulesFragment.java`|`fragment_admin_facility_schedules.xml`| Gestión de bloques horarios por día de la semana para instalaciones. |
| `AdminFacilityTimetableFragment`|`.../ui/AdminFacilityTimetableFragment.java`|`fragment_facility_timetable.xml`| Matriz visual de horario semanal de ocupación de instalaciones. |
| `AdminDetailFragment` | `.../ui/AdminDetailFragment.java` | `fragment_detail.xml` | Ficha universal de detalle para ver o editar cualquier entidad. |
| `AdminEditGradeFragment` | `.../ui/AdminEditGradeFragment.java` | `fragment_edit_grades.xml` | Sobrescritura administrativa directa de calificaciones por alumno. |

[⬆️ Volver al inicio del documento](#8b-interfaz-de-usuario-módulo-administrador)

---

## 8b.2 Dashboard Administrador (AdminHomeFragment)

- **Ruta del Archivo**: `app/src/main/java/com/aula/virtual/ui/AdminHomeFragment.java`
- **Layout Asociado**: `app/src/main/res/layout/fragment_admin_home.xml`

### Descripción
Constituye la pantalla de entrada para los usuarios con rol `ADMIN`. Presenta métricas estadísticas calculadas en tiempo real (total de estudiantes, profesores, encargados, materias activas e instalaciones) y tarjetas táctiles con navegación directa hacia cada sección administrativa.

[⬆️ Volver al inicio del documento](#8b-interfaz-de-usuario-módulo-administrador)

---

## 8b.3 Gestión de Usuarios (AdminAdminListFragment, AdminProfessorListFragment, AdminStudentListFragment)

- **Rutas de Archivo**: `.../ui/AdminAdminListFragment.java`, `.../ui/AdminProfessorListFragment.java`, `.../ui/AdminStudentListFragment.java`
- **Adapters**: `StudentAdapter.java`, `SearchableAdapter.java`

### Diálogos de Formulario Creados
- **`dialog_add_admin.xml`**: Campos de Carnet (`ADMxxx`), Nombre, Correo y Contraseña.
- **`dialog_add_professor.xml`**: Campos de Carnet (`PROFxxx`), Nombre, Correo, Especialidad y Contraseña.
- **`dialog_add_encargado.xml`**: Campos de Carnet (`ENCxxx`), Nombre, Teléfono, Correo y Dirección.
- **`dialog_add_student_general.xml`**: Registro de estudiante menor de edad (`ESTxxx`), Nombre, Grado y foto.

[⬆️ Volver al inicio del documento](#8b-interfaz-de-usuario-módulo-administrador)

---

## 8b.4 Detalle de Encargado y Asignación (AdminEncargadoDetailFragment)

- **Ruta del Archivo**: `app/src/main/java/com/aula/virtual/ui/AdminEncargadoDetailFragment.java`
- **Layout Asociado**: `app/src/main/res/layout/fragment_admin_encargado_detail.xml`

### Descripción
Muestra la información de contacto del padre/tutor y la lista de estudiantes menores de edad que tiene asignados a su cargo.
- **Diálogo `dialog_add_student_to_encargado.xml`**: Despliega la lista de estudiantes registrados no asignados o permite seleccionar un alumno para vincularlo al `encargadoId` de este usuario.

[⬆️ Volver al inicio del documento](#8b-interfaz-de-usuario-módulo-administrador)

---

## 8b.5 Gestión de Materias e Inscripciones (AdminSubjectListFragment y AdminEnrollmentListFragment)

- **Rutas de Archivo**: `.../ui/AdminSubjectListFragment.java` y `.../ui/AdminEnrollmentListFragment.java`
- **Adapters**: `SubjectAdapter.java`

### Funcionalidad
- Permite crear nuevas asignaturas asignándoles un código institucional (ej.: `MAT101`), nombre, sección, profesor responsable y un color característico de la paleta permitida (`allowed_colors`).
- Permite inscribir o retirar alumnos de las materias activas mediante `dialog_add_subject.xml`.

[⬆️ Volver al inicio del documento](#8b-interfaz-de-usuario-módulo-administrador)

---

## 8b.6 Gestión de Instalaciones y Horarios (AdminFacilityListFragment, AdminFacilityDetailFragment, AdminFacilitySchedulesFragment, AdminFacilityTimetableFragment)

- **Rutas de Archivo**: `.../ui/AdminFacilityListFragment.java`, `AdminFacilityDetailFragment.java`, `AdminFacilitySchedulesFragment.java`, `AdminFacilityTimetableFragment.java`
- **Adapters**: `FacilityAdapter.java`, `ScheduleAdapter.java`

### Funcionalidad
- **Instalaciones**: Alta y control de aulas, laboratorios de computación o ciencias y canchas deportivas mediante `dialog_add_facility.xml`.
- **Horarios**: Asignación de franjas horarias por día de la semana (`1=Lunes` a `7=Domingo`), hora inicio/fin y materia que ocupará el espacio mediante `dialog_schedule_detail.xml`.
- **Timetable Matriz**: Vista visual estructurada tipo rejilla cuadrícula (`bg_timetable_grid.xml`) que previene cruces de horarios entre materias o profesores en la misma aula.

[⬆️ Volver al inicio del documento](#8b-interfaz-de-usuario-módulo-administrador)

---

## 8b.7 Edición Global de Calificaciones (AdminEditGradeFragment y AdminDetailFragment)

- **Rutas de Archivo**: `.../ui/AdminEditGradeFragment.java` y `.../ui/AdminDetailFragment.java`
- **Layouts**: `fragment_edit_grades.xml` y `fragment_detail.xml`

### Funcionalidad
Otorga a los Administradores el permiso especial de corrección o sobreescritura directa sobre cualquier calificación o registro académico en caso de reclamos institucionales o correcciones extemporáneas mediante `dialog_edit_grades.xml`.

[⬆️ Volver al inicio del documento](#8b-interfaz-de-usuario-módulo-administrador)

---
[⬅️ Anterior](08a_ui_compartida.md) | [📑 Índice](00_INDICE.md) | [Siguiente ➡️](08c_ui_profesor.md)
