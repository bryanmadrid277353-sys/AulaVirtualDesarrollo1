# 8c. Interfaz de Usuario: Módulo Profesor

[⬅️ Anterior](08b_ui_administrador.md) | [📑 Índice](00_INDICE.md) | [Siguiente ➡️](08d_ui_estudiante_encargado.md)

Este documento detalla la interfaz de usuario, las vistas de gestión académica y los diálogos del módulo **Profesor (`PROFESSOR`)** en la aplicación **Aula Virtual**.

---

## Tabla de Contenido
- [8c.1 Resumen de Pantallas del Profesor](#8c1-resumen-de-pantallas-del-profesor)
- [8c.2 Inicio Docente (ProfessorHomeFragment)](#8c2-inicio-docente-professorhomefragment)
- [8c.3 Asignaturas e Inscriptos (ProfessorSubjectListFragment y ProfessorStudentListFragment)](#8c3-asignaturas-e-inscriptos-professorsubjectlistfragment-y-professorstudentlistfragment)
- [8c.4 Centro de Control de Materia (SubjectDetailFragment)](#8c4-centro-de-control-de-materia-subjectdetailfragment)
- [8c.5 Gestión de Notas y Planilla Excel (GradeAdapter y DialogExcelGradeManager)](#8c5-gestión-de-notas-y-planilla-excel-gradeadapter-y-dialogexcelgrademanager)
- [8c.6 Evaluación de Conducta y Asistencia (ConductAdapter y AttendanceDateAdapter)](#8c6-evaluación-de-conducta-y-asistencia-conductadapter-y-attendancedateadapter)

---

## 8c.1 Resumen de Pantallas del Profesor

| Clase | File Path | Layout XML | Propósito / Funcionalidad |
| :--- | :--- | :--- | :--- |
| `ProfessorHomeFragment` | `.../ui/ProfessorHomeFragment.java` | `fragment_professor_home.xml` | Dashboard del docente con resumen de materias del día, accesos rápidos y avisos. |
| `ProfessorSubjectListFragment`|`.../ui/ProfessorSubjectListFragment.java`| `fragment_admin_dashboard.xml` | Vista en lista/cuadrícula de las asignaturas impartidas por el profesor. |
| `ProfessorStudentListFragment`|`.../ui/ProfessorStudentListFragment.java`| `fragment_admin_dashboard.xml` | Listado de estudiantes inscritos en la materia seleccionada. |
| `SubjectDetailFragment` | `.../ui/SubjectDetailFragment.java` | `fragment_subject_detail.xml` | Hub principal de la materia: Calificaciones, Conducta, Asistencia, Foro y Participantes. |
| `SubjectParticipantsFragment`|`.../ui/SubjectParticipantsFragment.java`| `fragment_detail.xml` | Lista de contacto de alumnos y sus respectivos Encargados. |

[⬆️ Volver al inicio del documento](#8c-interfaz-de-usuario-módulo-profesor)

---

## 8c.2 Inicio Docente (ProfessorHomeFragment)

- **Ruta del Archivo**: `app/src/main/java/com/aula/virtual/ui/ProfessorHomeFragment.java`
- **Layout Asociado**: `app/src/main/res/layout/fragment_professor_home.xml`

### Descripción
Muestra el panel de inicio personalizado para el docente logueado:
- Tarjeta de bienvenida con horario de clases del día.
- Accesos rápidos para ingreso de calificaciones y toma de asistencia.
- Acceso a emisión de citatorios dirigidos a los encargados mediante `dialog_select_target.xml` y `dialog_select_student_citation.xml`.

[⬆️ Volver al inicio del documento](#8c-interfaz-de-usuario-módulo-profesor)

---

## 8c.3 Asignaturas e Inscriptos (ProfessorSubjectListFragment y ProfessorStudentListFragment)

- **Rutas de Archivo**: `.../ui/ProfessorSubjectListFragment.java` y `.../ui/ProfessorStudentListFragment.java`
- **Layouts e Items**: `item_professor_subject_card.xml`, `item_professor_subject_grid_card.xml`

### Descripción
Presenta las asignaturas que el profesor imparte en la institución. Permite alternar la vista entre tarjetas verticales (lista) y tarjetas en cuadrícula (grid) mostrando el código, sección, cantidad de alumnos inscritos y color personalizado.

[⬆️ Volver al inicio del documento](#8c-interfaz-de-usuario-módulo-profesor)

---

## 8c.4 Centro de Control de Materia (SubjectDetailFragment)

- **Ruta del Archivo**: `app/src/main/java/com/aula/virtual/ui/SubjectDetailFragment.java`
- **Layout Asociado**: `app/src/main/res/layout/fragment_subject_detail.xml`

### Descripción
Constituye el núcleo operativo del docente para una materia elegida. Dispone de pestañas/secciones superiores para navegar entre:
1. **Calificaciones**: Tabla interactiva de notas del período 1 al 5.
2. **Conducta**: Evaluación cuali-cuantitativa de convivencia escolar.
3. **Asistencia**: Control diario de asistencia por fecha.
4. **Foro / Blog**: Tablón de anuncios y discusiones pedagógicas.
5. **Participantes**: Lista de alumnos y opción de chat directo con los padres.

[⬆️ Volver al inicio del documento](#8c-interfaz-de-usuario-módulo-profesor)

---

## 8c.5 Gestión de Notas y Planilla Excel (GradeAdapter y DialogExcelGradeManager)

- **Ruta del Archivo Adapter**: `app/src/main/java/com/aula/virtual/ui/GradeAdapter.java`
- **Layouts**: `item_grade.xml`, `dialog_edit_grades.xml`, `dialog_excel_grade_manager.xml`

### Características de Calificación
- **Modificación Directa**: Permite pulsar en la celda de cualquier período (1 a 5) para abrir `dialog_edit_grades.xml` e ingresar el valor numérico (0.0 a 10.0).
- **Cálculo Automático**: El promedio se recalcula instantáneamente en la interfaz y se resalta según la escala (Verde $\ge 6.0$, Rojo $< 6.0$).
- **Sincronización Excel**: Mediante `dialog_excel_grade_manager.xml`, el docente puede:
  - Generar y descargar la plantilla vacía u oficial `.xlsx` de la materia (`ExcelTemplateGenerator.java`).
  - Cargar una plantilla completada desde el almacenamiento móvil (`GradeExcelImporter.java`).
  - Consultar el historial de importaciones pasadas (`GradeImportLog`).

[⬆️ Volver al inicio del documento](#8c-interfaz-de-usuario-módulo-profesor)

---

## 8c.6 Evaluación de Conducta y Asistencia (ConductAdapter y AttendanceDateAdapter)

- **Rutas de los Adapters**: `.../ui/ConductAdapter.java` y `.../ui/AttendanceDateAdapter.java`
- **Layouts**: `item_conduct_period.xml`, `item_attendance_date_card.xml`

### 1. Módulo de Conducta (`ConductAdapter.java`)
Permite evaluar a cada estudiante en los 5 criterios normativos (escala de 1 a 5):
1. Atención y Participación.
2. Cumplimiento de Normas y Rutinas.
3. Autorregulación y Esfuerzo.
4. Respeto y Colaboración.
5. Responsabilidad.
Permite además adjuntar observaciones pedagógicas escritas.

### 2. Módulo de Asistencia (`AttendanceDateAdapter.java`)
Presenta un selector de fecha y la lista de alumnos con selectores circulares para marcar el estado:
- `PRESENTE` (Verde `bg_circle_primary.xml`)
- `AUSENTE` (Rojo `bg_badge_danger.xml`)
- `LLEGADA TARDÍA` (Naranja `bg_circle_orange.xml`)
- `PERMISO / JUSTIFICADO` (Azul)

[⬆️ Volver al inicio del documento](#8c-interfaz-de-usuario-módulo-profesor)

---
[⬅️ Anterior](08b_ui_administrador.md) | [📑 Índice](00_INDICE.md) | [Siguiente ➡️](08d_ui_estudiante_encargado.md)
