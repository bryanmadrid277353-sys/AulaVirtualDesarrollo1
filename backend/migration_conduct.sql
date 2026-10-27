-- ==============================================================================
-- SCRIPT DE MIGRACIÓN: REGISTRO DE CONDUCTA - AULA VIRTUAL
-- ==============================================================================
-- Este script agrega el soporte para el registro de conducta por período
-- y actualiza la tabla grade_import_log para identificar la hoja sincronizada.
-- ==============================================================================

USE aula_virtual;

-- 1. Tabla de Niveles de Conducta (Catálogo 1 a 5)
CREATE TABLE IF NOT EXISTS conduct_levels (
    value TINYINT PRIMARY KEY CHECK (value BETWEEN 1 AND 5),
    label VARCHAR(50) NOT NULL UNIQUE
);

-- Población inicial del catálogo de niveles
INSERT IGNORE INTO conduct_levels (value, label) VALUES
(1, 'Malo'),
(2, 'Regular'),
(3, 'Bueno'),
(4, 'Muy bueno'),
(5, 'Excelente');

-- 2. Tabla de Registros de Conducta por Estudiante, Materia y Período (1 a 5)
CREATE TABLE IF NOT EXISTS conduct_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    studentId INT NOT NULL,
    subjectId INT NOT NULL,
    period TINYINT NOT NULL CHECK (period BETWEEN 1 AND 5),
    attention_participation TINYINT NULL CHECK (attention_participation IS NULL OR attention_participation BETWEEN 1 AND 5),
    rules_routines TINYINT NULL CHECK (rules_routines IS NULL OR rules_routines BETWEEN 1 AND 5),
    self_regulation_effort TINYINT NULL CHECK (self_regulation_effort IS NULL OR self_regulation_effort BETWEEN 1 AND 5),
    respect_collaboration TINYINT NULL CHECK (respect_collaboration IS NULL OR respect_collaboration BETWEEN 1 AND 5),
    responsibility TINYINT NULL CHECK (responsibility IS NULL OR responsibility BETWEEN 1 AND 5),
    observations TEXT NULL,
    updatedAt VARCHAR(100) NULL,
    average DOUBLE GENERATED ALWAYS AS (
        CASE
            WHEN (IF(attention_participation IS NULL,0,1) + IF(rules_routines IS NULL,0,1) + IF(self_regulation_effort IS NULL,0,1) + IF(respect_collaboration IS NULL,0,1) + IF(responsibility IS NULL,0,1)) = 0 THEN NULL
            ELSE (COALESCE(attention_participation,0) + COALESCE(rules_routines,0) + COALESCE(self_regulation_effort,0) + COALESCE(respect_collaboration,0) + COALESCE(responsibility,0)) /
                 (IF(attention_participation IS NULL,0,1) + IF(rules_routines IS NULL,0,1) + IF(self_regulation_effort IS NULL,0,1) + IF(respect_collaboration IS NULL,0,1) + IF(responsibility IS NULL,0,1))
        END
    ) STORED,
    FOREIGN KEY (studentId) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subjectId) REFERENCES subjects(id) ON DELETE CASCADE,
    UNIQUE (studentId, subjectId, period)
);

-- 3. Extensión de la Bitácora de Sincronización (grade_import_log)
-- Se añade la columna sheetName para distinguir entre las hojas 'Notas' y 'Conducta'
SET @dbname = DATABASE();
SET @tablename = 'grade_import_log';
SET @columnname = 'sheetName';
SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @dbname
        AND TABLE_NAME = @tablename
        AND COLUMN_NAME = @columnname
    ) > 0,
    'SELECT 1',
    'ALTER TABLE grade_import_log ADD COLUMN sheetName VARCHAR(50) DEFAULT "Notas";'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;
