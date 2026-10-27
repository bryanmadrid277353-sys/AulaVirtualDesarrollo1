-- ==============================================================================
-- SCRIPT DE MIGRACIÓN: REGISTRO DE ASISTENCIA Y CALENDARIO ACADÉMICO
-- ==============================================================================
-- Este script agrega las tablas del calendario académico (períodos lectivos, asuetos)
-- y el registro de asistencia por matriz de fechas con catálogo de estados 0-3.
-- ==============================================================================

USE aula_virtual;

-- 1. Tabla de Períodos Académicos (Períodos 1 a 5)
CREATE TABLE IF NOT EXISTS academic_periods (
    period TINYINT PRIMARY KEY CHECK (period BETWEEN 1 AND 5),
    startDate DATE NOT NULL,
    endDate DATE NOT NULL,
    CONSTRAINT check_period_dates CHECK (startDate <= endDate)
);

-- 2. Tabla de Días No Lectivos / Asuetos / Feriados
CREATE TABLE IF NOT EXISTS non_school_days (
    id INT AUTO_INCREMENT PRIMARY KEY,
    day DATE NOT NULL UNIQUE,
    reason VARCHAR(255) NOT NULL
);

-- 3. Tabla de Catálogo de Estados de Asistencia (0 a 3)
CREATE TABLE IF NOT EXISTS attendance_states (
    value TINYINT PRIMARY KEY CHECK (value BETWEEN 0 AND 3),
    label VARCHAR(50) NOT NULL UNIQUE
);

-- Población inicial del catálogo de estados
INSERT IGNORE INTO attendance_states (value, label) VALUES
(0, 'Sin evaluar'),
(1, 'Asistió'),
(2, 'No asistió'),
(3, 'Justificado');

-- 4. Tabla de Registros de Asistencia por Estudiante, Materia y Fecha de Clase
CREATE TABLE IF NOT EXISTS attendance_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    studentId INT NOT NULL,
    subjectId INT NOT NULL,
    classDate DATE NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 CHECK (status BETWEEN 0 AND 3),
    justification TEXT NULL,
    updatedAt VARCHAR(100) NULL,
    FOREIGN KEY (studentId) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subjectId) REFERENCES subjects(id) ON DELETE CASCADE,
    UNIQUE (studentId, subjectId, classDate)
);

-- Índice de rendimiento para búsquedas por materia y fecha
CREATE INDEX idx_attendance_subject_date ON attendance_records(subjectId, classDate);

-- 5. Vista Calculada de Resumen de Asistencia
CREATE OR REPLACE VIEW attendance_summary AS
SELECT
    studentId,
    subjectId,
    SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS attended_count,
    SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS absent_count,
    SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) AS excused_count,
    SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS unevaluated_count,
    COUNT(*) AS total_classes,
    CASE
        WHEN (SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) + SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) + SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END)) = 0 THEN NULL
        ELSE (SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) + SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END)) /
             (SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) + SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) + SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END)) * 100.0
    END AS attendance_percentage
FROM attendance_records
GROUP BY studentId, subjectId;
