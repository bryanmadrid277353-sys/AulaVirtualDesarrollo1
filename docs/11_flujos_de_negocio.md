# 11. Flujos de Negocio Principales

[⬅️ Anterior](10_catalogo_clases_y_metodos.md) | [📑 Índice](00_INDICE.md) | [Siguiente ➡️](12_recursos_y_configuracion.md)

Este documento detalla los flujos de negocio end-to-end más críticos de la aplicación **Aula Virtual**, incluyendo diagramas de secuencia Mermaid para ilustrar la interacción entre componentes UI, ViewModels, Repositorio, Base de Datos local y Servidor Backend.

---

## Tabla de Contenido
- [11.1 Flujo 1: Autenticación y Carga de Sesión](#111-flujo-1-autenticación-y-carga-de-sesión)
- [11.2 Flujo 2: Ingreso y Actualización de Calificaciones](#112-flujo-2-ingreso-y-actualización-de-calificaciones)
- [11.3 Flujo 3: Importación Masiva de Notas desde Excel (.xlsx)](#113-flujo-3-importación-masiva-de-notas-desde-excel-xlsx)
- [11.4 Flujo 4: Registro de Asistencia y Emisión de Alertas FCM](#114-flujo-4-registro-de-asistencia-y-emisión-de-alertas-fcm)

---

## 11.1 Flujo 1: Autenticación y Carga de Sesión

Este flujo describe el proceso desde que el usuario ingresa sus credenciales en `LoginFragment` hasta la configuración del panel lateral de navegación según su rol.

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuario
    participant UI as LoginFragment
    participant CM as CredentialsManager
    participant VM as MainViewModel
    participant Repo as VirtualAulaRepository
    participant API as FastAPI Backend

    U->>UI: Ingresa Carnet y Contraseña
    UI->>VM: login(carnet, password)
    VM->>Repo: login(carnet, password)
    Repo->>API: GET /login?carnet=...&password=...
    API-->>Repo: Retorna User (JSON)
    Repo->>Repo: Guarda usuario en Room SQLite
    Repo-->>VM: Exito (Objeto User)
    VM-->>UI: Notifica resultado positivo
    UI->>CM: saveLoginCredentials(User, password)
    UI->>UI: Notifica a MainActivity.setupRoleBasedNavigation(role)
    UI->>U: Redirige a Dashboard del Rol (Admin/Profe/Encargado)
```

[⬆️ Volver al inicio del documento](#11-flujos-de-negocio-principales)

---

## 11.2 Flujo 2: Ingreso y Actualización de Calificaciones

Describe el procedimiento mediante el cual un docente modifica la nota de un estudiante para un período determinado.

```mermaid
sequenceDiagram
    autonumber
    actor P as Profesor
    participant UI as SubjectDetailFragment (Tabla Notas)
    participant Dialog as DialogEditGrades
    participant VM as MainViewModel
    participant Repo as VirtualAulaRepository
    participant DB as Room SQLite
    participant API as FastAPI Backend

    P->>UI: Pulsa celda de Nota Período 1
    UI->>Dialog: Despliega cuadro de edición de nota
    P->>Dialog: Ingresa valor (ej: 8.5) y presiona "Guardar"
    Dialog->>VM: updateEnrollmentGrade(enrollmentId, grade1=8.5)
    VM->>Repo: updateEnrollment(enrollment)
    Repo->>DB: EnrollmentDao.update(enrollment)
    Repo->>API: PUT /enrollments/{id}
    API-->>Repo: Confirmación 200 OK
    Repo-->>VM: Notifica actualización completada
    VM-->>UI: LiveData Observer recalcula Promedio en pantalla
    UI->>P: Muestra badge actualizado (Verde >= 6.0)
```

[⬆️ Volver al inicio del documento](#11-flujos-de-negocio-principales)

---

## 11.3 Flujo 3: Importación Masiva de Notas desde Excel (.xlsx)

Detalla el procesamiento automatizado de planillas de calificaciones cargadas por el docente.

```mermaid
sequenceDiagram
    autonumber
    actor P as Profesor
    participant UI as SubjectDetailFragment
    participant Mgr as GradeExcelImporter
    participant POI as Apache POI Library
    participant VM as MainViewModel
    participant Repo as VirtualAulaRepository

    P->>UI: Presiona "Importar Excel" y selecciona archivo .xlsx
    UI->>VM: importGradesFromExcel(fileUri)
    VM->>Mgr: readAndParseExcel(ctx, fileUri)
    Mgr->>POI: Abre Workbook y lee hoja 'Notas'
    POI-->>Mgr: Filas de datos (Carnet, N1, N2, N3, N4, N5)
    Mgr->>Mgr: Valida formato numérico (0.0 - 10.0) y carnets
    Mgr-->>VM: Retorna objeto ImportResult (filas exito/error)
    VM->>Repo: Actualiza inscripciones procesadas masivamente
    VM-->>UI: Despliega resumen de importación y detalle de errores
```

[⬆️ Volver al inicio del documento](#11-flujos-de-negocio-principales)

---

## 11.4 Flujo 4: Registro de Asistencia y Emisión de Alertas FCM

Muestra cómo se registra la lista diaria de clase y se envía una notificación push en tiempo real al dispositivo móvil del Encargado.

```mermaid
sequenceDiagram
    autonumber
    actor P as Profesor
    participant UI as SubjectDetailFragment (Tab Asistencia)
    participant VM as MainViewModel
    participant Repo as VirtualAulaRepository
    participant API as FastAPI Backend
    participant FCM as Firebase Cloud Messaging
    actor Enc as Encargado (Padre)

    P->>UI: Marca estudiante como "AUSENTE" y guarda
    UI->>VM: saveAttendanceRecord(record)
    VM->>Repo: saveAttendanceRecord(record)
    Repo->>API: POST /attendance-records
    API->>API: Guarda registro en MySQL
    API->>FCM: Envia payload push para EncargadoId
    FCM-->>Enc: Notificación Push: "Inasistencia registrada en Matemática"
    API-->>Repo: Confirmacion 200 OK
    Repo-->>VM: Actualiza UI docente
```

[⬆️ Volver al inicio del documento](#11-flujos-de-negocio-principales)

---
[⬅️ Anterior](10_catalogo_clases_y_metodos.md) | [📑 Índice](00_INDICE.md) | [Siguiente ➡️](12_recursos_y_configuracion.md)
