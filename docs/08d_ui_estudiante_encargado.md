# 8d. Interfaz de Usuario: Módulo Encargado y Estudiante

[⬅️ Anterior](08c_ui_profesor.md) | [📑 Índice](00_INDICE.md) | [Siguiente ➡️](09_navegacion.md)

Este documento describe la interfaz de usuario, pantallas de consulta y tarjetas de seguimiento académico orientadas a **Encargados (`ENCARGADO`)** y estudiantes en la aplicación **Aula Virtual**.

---

## Tabla de Contenido
- [8d.1 Resumen de Pantallas de Encargado/Estudiante](#8d1-resumen-de-pantallas-de-encargadoestudiante)
- [8d.2 Inicio del Encargado y Selector de Hijos (StudentHomeFragment)](#8d2-inicio-del-encargado-y-selector-de-hijos-studenthomefragment)
- [8d.3 Dashboard de Rendimiento Académico (StudentDashboardFragment)](#8d3-dashboard-de-rendimiento-académico-studentdashboardfragment)
- [8d.4 Ficha Detalle de Materia y Notas (StudentDetailFragment)](#8d4-ficha-detalle-de-materia-y-notas-studentdetailfragment)
- [8d.5 Reporte de Asistencia (StudentAttendanceFragment)](#8d5-reporte-de-asistencia-studentattendancefragment)
- [8d.6 Reporte de Conducta y Convivencia (StudentConductFragment)](#8d6-reporte-de-conducta-y-convivencia-studentconductfragment)

---

## 8d.1 Resumen de Pantallas de Encargado/Estudiante

| Clase | File Path | Layout XML | Descripción / Funcionalidad |
| :--- | :--- | :--- | :--- |
| `StudentHomeFragment` | `.../ui/StudentHomeFragment.java` | `fragment_student_home.xml` | Pantalla de inicio con selector desplegable de alumno a cargo y resumen de materias. |
| `StudentDashboardFragment` | `.../ui/StudentDashboardFragment.java` | `fragment_student_dashboard.xml` | Vista consolidada del promedio general acumulado y cuadro de honor. |
| `StudentDetailFragment` | `.../ui/StudentDetailFragment.java` | `fragment_detail.xml` | Detalle de calificaciones por período (1 a 5) de una materia seleccionada. |
| `StudentAttendanceFragment`|`.../ui/StudentAttendanceFragment.java`|`fragment_student_attendance.xml`| Historial y estadísticas de asistencia (presentes, faltas, tardanzas). |
| `StudentConductFragment` | `.../ui/StudentConductFragment.java` | `fragment_student_conduct.xml` | Boleta de evaluación de los 5 criterios de conducta y observaciones. |

[⬆️ Volver al inicio del documento](#8d-interfaz-de-usuario-módulo-encargado-y-estudiante)

---

## 8d.2 Inicio del Encargado y Selector de Hijos (StudentHomeFragment)

- **Ruta del Archivo**: `app/src/main/java/com/aula/virtual/ui/StudentHomeFragment.java`
- **Layout Asociado**: `app/src/main/res/layout/fragment_student_home.xml`

### Descripción
Constituye el punto de acceso principal para los padres de familia o encargados tutores.
- **Selector de Estudiante**: Si el encargado posee múltiples hijos matriculados en la institución, la parte superior incluye un spinner desplegable para alternar la vista entre un alumno u otro.
- **Tarjetas de Asignaturas**: Lista las materias en las que está inscrito el estudiante seleccionado, mostrando el nombre de la materia, nombre del profesor titular, promedio ponderado actual y badge de color.

[⬆️ Volver al inicio del documento](#8d-interfaz-de-usuario-módulo-encargado-y-estudiante)

---

## 8d.3 Dashboard de Rendimiento Académico (StudentDashboardFragment)

- **Ruta del Archivo**: `app/src/main/java/com/aula/virtual/ui/StudentDashboardFragment.java`
- **Layout Asociado**: `app/src/main/res/layout/fragment_student_dashboard.xml`

### Descripción
Proporciona una visión ejecutiva del desempeño del estudiante:
- **Promedio General**: Muestra una tarjeta destacada con el promedio global acumulado del estudiante en todas sus materias.
- **Progreso por Período**: Desglose gráfico/numérico del avance en los 5 períodos evaluativos.
- **Alertas Académicas**: Resalta asignaturas con rendimiento inferior a 6.0 puntos en color rojo (`bg_badge_danger.xml`).

[⬆️ Volver al inicio del documento](#8d-interfaz-de-usuario-módulo-encargado-y-estudiante)

---

## 8d.4 Ficha Detalle de Materia y Notas (StudentDetailFragment)

- **Ruta del Archivo**: `app/src/main/java/com/aula/virtual/ui/StudentDetailFragment.java`
- **Layout Asociado**: `app/src/main/res/layout/fragment_detail.xml`

### Descripción
Al pulsar sobre una tarjeta de materia en el inicio, se navega a esta vista detallada:
- Desglose de las 5 notas oficiales de los períodos 1 a 5 con estado (Aprobado / Reprobado).
- Botón directo de **Contacto con el Docente**: Inicia una conversación de chat privado en tiempo real (`DirectChatFragment`) con el profesor de la materia.
- Acceso directo al **Foro de la Materia** (`SubjectBlogFragment`).

[⬆️ Volver al inicio del documento](#8d-interfaz-de-usuario-módulo-encargado-y-estudiante)

---

## 8d.5 Reporte de Asistencia (StudentAttendanceFragment)

- **Ruta del Archivo**: `app/src/main/java/com/aula/virtual/ui/StudentAttendanceFragment.java`
- **Layout Asociado**: `app/src/main/res/layout/fragment_student_attendance.xml`

### Descripción
Visualiza el historial completo de asistencia del estudiante en la materia:
- **Resumen Estadístico**: Total de días evaluados, número de asistencias (`PRESENTE`), ausencias (`AUSENTE`), llegadas tardías (`LATE`) y permisos justificados (`PERMIT`).
- **Historial Cronológico**: Lista desplazable por fechas indicando el estado del alumno y las observaciones del docente en caso de justificaciones.

[⬆️ Volver al inicio del documento](#8d-interfaz-de-usuario-módulo-encargado-y-estudiante)

---

## 8d.6 Reporte de Conducta y Convivencia (StudentConductFragment)

- **Ruta del Archivo**: `app/src/main/java/com/aula/virtual/ui/StudentConductFragment.java`
- **Layout Asociado**: `app/src/main/res/layout/fragment_student_conduct.xml`

### Descripción
Despliega la boleta de evaluación de convivencia escolar por período evaluativo:
- Puntuación obtenida en cada uno de los 5 criterios (escala de 1 a 5):
  1. Atención y Participación.
  2. Cumplimiento de Normas y Rutinas.
  3. Autorregulación y Esfuerzo.
  4. Respeto y Colaboración.
  5. Responsabilidad.
- Promedio de conducta calculado y cuadro de texto con las **Observaciones cualitativas** ingresadas por el profesor.

[⬆️ Volver al inicio del documento](#8d-interfaz-de-usuario-módulo-encargado-y-estudiante)

---
[⬅️ Anterior](08c_ui_profesor.md) | [📑 Índice](00_INDICE.md) | [Siguiente ➡️](09_navegacion.md)
