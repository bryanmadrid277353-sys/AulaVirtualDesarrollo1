-- Script de inicialización para Aula Virtual
-- Este archivo define la estructura de la base de datos para el sistema de gestión académica.
-- Las descripciones de las columnas sirven para guiar a sistemas automatizados de generación de datos.

CREATE DATABASE IF NOT EXISTS aula_virtual;
USE aula_virtual;

-- Tabla de Colores Permitidos: Listado estricto de colores soportados por el tema de la aplicación móvil
CREATE TABLE IF NOT EXISTS allowed_colors (
    color_name VARCHAR(50) PRIMARY KEY -- Nombre identificador del color (Ej: 'BLUE_LIGHT')
);

-- Inserción de la paleta oficial de colores del sistema (Soportada en ThemeHelper)
INSERT IGNORE INTO allowed_colors (color_name) VALUES
('BLUE_LIGHT'), ('BLUE'), ('BLUE_DARK'),
('RED_LIGHT'), ('RED'), ('RED_DARK'),
('GREEN_LIGHT'), ('GREEN'), ('GREEN_DARK'),
('PURPLE_LIGHT'), ('PURPLE'), ('PURPLE_DARK'),
('CYAN_LIGHT'), ('CYAN'), ('CYAN_DARK'),
('YELLOW_LIGHT'), ('YELLOW'), ('YELLOW_DARK');

-- Tabla de Usuarios (Cuentas con Acceso: Administradores, Profesores, Encargados del Estudiante)
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,          -- Identificador único interno
    carnet VARCHAR(10) NOT NULL UNIQUE,         -- Código de identificación institucional (Ej: ENC1234567, ADM...)
    name VARCHAR(255) NOT NULL,                 -- Nombre completo del Encargado / Usuario
    password VARCHAR(255) NOT NULL,             -- Contraseña de acceso
    role VARCHAR(50) NOT NULL,                  -- Rol del usuario: 'ADMIN', 'ENCARGADO' o 'PROFESSOR'
    address TEXT,                               -- Dirección domiciliar del Encargado
    personal_email VARCHAR(255),                -- Correo electrónico del Encargado
    profile_image LONGTEXT,                     -- Foto de perfil en Base64
    can_change_photo BOOLEAN DEFAULT 1          -- Flag: 1 permite cambiar foto, 0 bloquea
);

-- Tabla de Estudiantes (Menores de edad sin acceso/contraseña directa)
CREATE TABLE IF NOT EXISTS students (
    id INT AUTO_INCREMENT PRIMARY KEY,          -- Identificador único interno del estudiante
    encargadoId INT,                            -- ID del Encargado a cargo (FK a users.id)
    carnet VARCHAR(20) NOT NULL UNIQUE,         -- Carnet/Código del estudiante
    name VARCHAR(255) NOT NULL,                 -- Nombre completo del estudiante menor de edad
    grade VARCHAR(100),                         -- Grado o sección del alumno
    profile_image LONGTEXT,                     -- Foto del estudiante
    FOREIGN KEY (encargadoId) REFERENCES users(id) ON DELETE SET NULL
);

-- Tabla de Materias: Cursos académicos ofrecidos
CREATE TABLE IF NOT EXISTS subjects (
    id INT AUTO_INCREMENT PRIMARY KEY,          -- Identificador único interno
    code VARCHAR(50) UNIQUE,                    -- Código/Carnet institucional de la materia (Ej: ASIGN12345)
    name VARCHAR(255) NOT NULL,                 -- Nombre de la materia (Ej: Matemática II)
    description TEXT,                           -- Contenido o resumen de la materia
    professorId INT,                            -- ID del usuario con rol 'PROFESSOR' asignado
    section VARCHAR(10) DEFAULT '01',           -- Sección de la materia (Ej: 01, 02)
    color VARCHAR(50) DEFAULT 'BLUE',           -- Color representativo para la interfaz (Ej: 'RED', 'GREEN', 'PURPLE')
    FOREIGN KEY (professorId) REFERENCES users(id) ON DELETE CASCADE
);

-- Tabla de Inscripciones y Notas: Relaciona alumnos con materias y sus calificaciones
CREATE TABLE IF NOT EXISTS enrollments (
    id INT AUTO_INCREMENT PRIMARY KEY,          -- Identificador único interno
    studentId INT NOT NULL,                     -- ID del estudiante menor de edad (FK a students.id)
    subjectId INT NOT NULL,                     -- ID de la materia inscrita
    grade1 DOUBLE,                              -- Nota del primer periodo (Escala 0.0 - 10.0)
    grade2 DOUBLE,                              -- Nota del segundo periodo (Escala 0.0 - 10.0)
    grade3 DOUBLE,                              -- Nota del tercer periodo (Escala 0.0 - 10.0)
    grade4 DOUBLE,                              -- Nota del cuarto periodo (Escala 0.0 - 10.0)
    grade5 DOUBLE,                              -- Nota del quinto periodo (Escala 0.0 - 10.0)
    average DOUBLE GENERATED ALWAYS AS (
        CASE
            WHEN (IF(grade1 IS NULL,0,1) + IF(grade2 IS NULL,0,1) + IF(grade3 IS NULL,0,1) + IF(grade4 IS NULL,0,1) + IF(grade5 IS NULL,0,1)) = 0 THEN NULL
            ELSE (COALESCE(grade1,0) + COALESCE(grade2,0) + COALESCE(grade3,0) + COALESCE(grade4,0) + COALESCE(grade5,0)) /
                 (IF(grade1 IS NULL,0,1) + IF(grade2 IS NULL,0,1) + IF(grade3 IS NULL,0,1) + IF(grade4 IS NULL,0,1) + IF(grade5 IS NULL,0,1))
        END
    ) STORED,                                   -- Promedio calculated automáticamente ignorando notas NULL
    color VARCHAR(50) DEFAULT 'BLUE',           -- Color preferido para la vista
    FOREIGN KEY (studentId) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subjectId) REFERENCES subjects(id) ON DELETE CASCADE,
    UNIQUE (studentId, subjectId)               -- Evita inscripciones duplicadas del mismo alumno en la misma materia
);

-- Tabla de Historial/Bitácora de Importación de Notas y Conducta desde Excel (.xlsx)
CREATE TABLE IF NOT EXISTS grade_import_log (
    id INT AUTO_INCREMENT PRIMARY KEY,          -- Identificador único de la importación
    subjectId INT NOT NULL,                     -- ID de la materia sincronizada
    sheetName VARCHAR(50) DEFAULT 'Notas',      -- Nombre de la hoja procesada ('Notas' o 'Conducta')
    fileName VARCHAR(255) NOT NULL,             -- Nombre del archivo Excel procesado
    fileHash VARCHAR(64) NOT NULL,              -- Hash SHA-256 del contenido para detección de cambios
    importedAt VARCHAR(100) NOT NULL,           -- Timestamp de la importación
    rowsOk INT DEFAULT 0,                       -- Registros de estudiantes actualizados exitosamente
    rowsError INT DEFAULT 0,                    -- Registros con errores
    errorDetail TEXT,                           -- Detalle técnico de errores por fila
    FOREIGN KEY (subjectId) REFERENCES subjects(id) ON DELETE CASCADE
);

-- Tabla de Niveles de Conducta (Catálogo 1-5)
CREATE TABLE IF NOT EXISTS conduct_levels (
    value TINYINT PRIMARY KEY CHECK (value BETWEEN 1 AND 5), -- Valor numérico (1: Malo ... 5: Excelente)
    label VARCHAR(50) NOT NULL UNIQUE                        -- Texto legible
);

INSERT IGNORE INTO conduct_levels (value, label) VALUES
(1, 'Malo'), (2, 'Regular'), (3, 'Bueno'), (4, 'Muy bueno'), (5, 'Excelente');

-- Tabla de Registros de Conducta por Estudiante, Materia y Período
CREATE TABLE IF NOT EXISTS conduct_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    studentId INT NOT NULL,                     -- ID del estudiante (FK a students.id)
    subjectId INT NOT NULL,                     -- ID de la materia (FK a subjects.id)
    period TINYINT NOT NULL CHECK (period BETWEEN 1 AND 5), -- Período evaluado (1-5)
    attention_participation TINYINT NULL CHECK (attention_participation IS NULL OR attention_participation BETWEEN 1 AND 5),
    rules_routines TINYINT NULL CHECK (rules_routines IS NULL OR rules_routines BETWEEN 1 AND 5),
    self_regulation_effort TINYINT NULL CHECK (self_regulation_effort IS NULL OR self_regulation_effort BETWEEN 1 AND 5),
    respect_collaboration TINYINT NULL CHECK (respect_collaboration IS NULL OR respect_collaboration BETWEEN 1 AND 5),
    responsibility TINYINT NULL CHECK (responsibility IS NULL OR responsibility BETWEEN 1 AND 5),
    observations TEXT NULL,                     -- Observaciones opcionales del docente
    updatedAt VARCHAR(100) NULL,                -- Timestamp de última actualización
    average DOUBLE GENERATED ALWAYS AS (
        CASE
            WHEN (IF(attention_participation IS NULL,0,1) + IF(rules_routines IS NULL,0,1) + IF(self_regulation_effort IS NULL,0,1) + IF(respect_collaboration IS NULL,0,1) + IF(responsibility IS NULL,0,1)) = 0 THEN NULL
            ELSE (COALESCE(attention_participation,0) + COALESCE(rules_routines,0) + COALESCE(self_regulation_effort,0) + COALESCE(respect_collaboration,0) + COALESCE(responsibility,0)) /
                 (IF(attention_participation IS NULL,0,1) + IF(rules_routines IS NULL,0,1) + IF(self_regulation_effort IS NULL,0,1) + IF(respect_collaboration IS NULL,0,1) + IF(responsibility IS NULL,0,1))
        END
    ) STORED,                                   -- Promedio calculado automáticamente ignorando NULL
    FOREIGN KEY (studentId) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subjectId) REFERENCES subjects(id) ON DELETE CASCADE,
    UNIQUE (studentId, subjectId, period)
);

-- Tabla de Períodos Académicos (Períodos 1 a 5)
CREATE TABLE IF NOT EXISTS academic_periods (
    period TINYINT PRIMARY KEY CHECK (period BETWEEN 1 AND 5),
    startDate DATE NOT NULL,
    endDate DATE NOT NULL,
    CONSTRAINT check_period_dates CHECK (startDate <= endDate)
);

-- Tabla de Días No Lectivos / Asuetos / Feriados
CREATE TABLE IF NOT EXISTS non_school_days (
    id INT AUTO_INCREMENT PRIMARY KEY,
    day DATE NOT NULL UNIQUE,
    reason VARCHAR(255) NOT NULL
);

-- Tabla de Catálogo de Estados de Asistencia (0 a 3)
CREATE TABLE IF NOT EXISTS attendance_states (
    value TINYINT PRIMARY KEY CHECK (value BETWEEN 0 AND 3),
    label VARCHAR(50) NOT NULL UNIQUE
);

INSERT IGNORE INTO attendance_states (value, label) VALUES
(0, 'Sin evaluar'),
(1, 'Asistió'),
(2, 'No asistió'),
(3, 'Justificado');

-- Tabla de Registros de Asistencia por Estudiante, Materia y Fecha de Clase
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

CREATE INDEX idx_attendance_subject_date ON attendance_records(subjectId, classDate);

-- Vista Calculada de Resumen de Asistencia
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

-- Tabla de Colores Personalizados: Preferencias estéticas de horario por usuario
CREATE TABLE IF NOT EXISTS user_subject_colors (
    id INT AUTO_INCREMENT PRIMARY KEY,          -- Identificador único interno
    userId INT NOT NULL,                        -- ID del usuario (Alumno o Profesor)
    subjectId INT NOT NULL,                     -- ID de la materia
    color VARCHAR(50) DEFAULT 'BLUE',           -- Color CSS o nombre de color (Ej: 'YELLOW_DARK')
    FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (subjectId) REFERENCES subjects(id) ON DELETE CASCADE,
    UNIQUE (userId, subjectId)
);

-- Tabla de Blog de Materias: Publicaciones de tareas, avisos o exámenes
CREATE TABLE IF NOT EXISTS blog_entries (
    id INT AUTO_INCREMENT PRIMARY KEY,          -- Identificador único interno
    subjectId INT NOT NULL,                     -- ID de la materia dueña del blog
    category VARCHAR(50),                       -- Tipo de publicación: 'Aviso', 'Tarea', 'Parcial'
    title VARCHAR(255) NOT NULL,                -- Título descriptivo de la asignación
    content TEXT,                               -- Instrucciones detalladas o cuerpo del aviso
    position INT DEFAULT 0,                     -- Orden de visualización (Drag and Drop)
    FOREIGN KEY (subjectId) REFERENCES subjects(id) ON DELETE CASCADE
);

-- Tabla de Comentarios del Blog: Discusión entre usuarios sobre una entrada
CREATE TABLE IF NOT EXISTS blog_comments (
    id INT AUTO_INCREMENT PRIMARY KEY,          -- Identificador único interno
    blogEntryId INT NOT NULL,                   -- ID de la entrada comentada
    userId INT NOT NULL,                        -- ID del usuario que comentó
    content TEXT NOT NULL,                      -- Contenido del mensaje
    timestamp VARCHAR(50),                      -- Fecha y hora del comentario
    FOREIGN KEY (blogEntryId) REFERENCES blog_entries(id) ON DELETE CASCADE,
    FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE
);

-- Tabla de Instalaciones: Lugares físicos dentro de la institución
CREATE TABLE IF NOT EXISTS facilities (
    id INT AUTO_INCREMENT PRIMARY KEY,          -- Identificador único interno
    code VARCHAR(50) UNIQUE,                    -- Código/Carnet institucional de la instalación (Ej: INSTA10001)
    name VARCHAR(255) NOT NULL,                 -- Nombre o código del aula/lugar (Ej: Aula BJ 205)
    type VARCHAR(100),                          -- Clasificación: 'Aulas', 'Laboratorio', 'Deportes', 'Biblioteca'
    description TEXT                            -- Detalles adicionales como ubicación o capacidad
);

-- Tabla de Horarios por Instalación: Programación semanal de clases
CREATE TABLE IF NOT EXISTS facility_schedules (
    id INT AUTO_INCREMENT PRIMARY KEY,          -- Identificador único interno
    facilityId INT NOT NULL,                    -- ID del lugar físico
    subjectId INT NOT NULL,                     -- ID de la materia que se impartirá
    professorId INT NOT NULL,                   -- ID del profesor que impartirá la clase
    days VARCHAR(255),                          -- Días de la semana (Ej: "Lunes, Miercoles, Viernes")
    startTime VARCHAR(10),                      -- Hora de inicio en formato 24h o 12h con AM/PM (Ej: "08:00 AM")
    endTime VARCHAR(10),                        -- Hora de finalización (Ej: "10:00 AM")
    color VARCHAR(50) DEFAULT 'BLUE',           -- Color para representar el bloque en el calendario
    FOREIGN KEY (facilityId) REFERENCES facilities(id) ON DELETE CASCADE,
    FOREIGN KEY (subjectId) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (professorId) REFERENCES users(id) ON DELETE CASCADE
);

-- Tabla de Notificaciones y Avisos: Almacena avisos por canal y globales
CREATE TABLE IF NOT EXISTS notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,          -- Identificador único interno
    title VARCHAR(255) NOT NULL,                -- Título del aviso
    message TEXT NOT NULL,                      -- Cuerpo del mensaje
    targetType VARCHAR(50) NOT NULL,            -- Destinatario: 'ALL', 'ROLE_STUDENTS', 'ROLE_PROFESSORS', 'ROLE_ADMINS', 'SUBJECT', 'FACILITY'
    targetValue VARCHAR(255) NULL,              -- Valor específico (Nombre de materia o instalación)
    senderName VARCHAR(255) NOT NULL,           -- Nombre del usuario emisor
    timestamp VARCHAR(100) NOT NULL             -- Fecha y hora de envío
);

-- Tabla de Mensajes Directos: Conversaciones privadas 1 a 1 entre dos usuarios
CREATE TABLE IF NOT EXISTS direct_messages (
    id INT AUTO_INCREMENT PRIMARY KEY,          -- Identificador único interno del mensaje
    chatRoomId VARCHAR(100) NOT NULL,           -- Identificador único del chat privado (Ej: 'chat_1_5')
    senderId INT NOT NULL,                      -- ID del usuario que envía el mensaje
    receiverId INT NOT NULL,                    -- ID del usuario destinatario
    message TEXT NOT NULL,                      -- Contenido del mensaje privado
    timestamp VARCHAR(100) NOT NULL,            -- Fecha y hora del mensaje (Ej: '10:42 AM')
    isRead INT DEFAULT 0,                       -- Estado de lectura: 0=No leído, 1=Leído
    initiatorId INT NOT NULL,                   -- ID del usuario que inició la conversación en este chat
    FOREIGN KEY (senderId) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiverId) REFERENCES users(id) ON DELETE CASCADE
);

-- Triggers de Validación de Límites y Traslape de Horarios para evitar colisiones
DELIMITER //

CREATE TRIGGER before_insert_facility_schedule
BEFORE INSERT ON facility_schedules
FOR EACH ROW
BEGIN
    DECLARE overlap_count INT;

    -- Convertir formatos de tiempo de texto (Ej: "08:00 AM") a minutos totales del día para comparaciones lineales precisas
    SET @new_start = TIME_TO_SEC(STR_TO_DATE(NEW.startTime, '%h:%i %p')) / 60;
    SET @new_end = TIME_TO_SEC(STR_TO_DATE(NEW.endTime, '%h:%i %p')) / 60;

    -- Validar que la hora de inicio sea anterior a la de finalización
    IF @new_start >= @new_end THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Error: La hora de inicio debe ser strictly menor a la hora de finalización.';
    END IF;

    -- 1. Validar traslape en la misma Aula/Instalación para los mismos días
    SELECT COUNT(*) INTO overlap_count
    FROM facility_schedules
    WHERE facilityId = NEW.facilityId
      AND (
          -- Verificación básica de coincidencia de subcadena de días
          NEW.days LIKE CONCAT('%', days, '%') OR days LIKE CONCAT('%', NEW.days, '%')
      )
      AND (
          @new_start < (TIME_TO_SEC(STR_TO_DATE(endTime, '%h:%i %p')) / 60) AND
          @new_end > (TIME_TO_SEC(STR_TO_DATE(startTime, '%h:%i %p')) / 60)
      );

    IF overlap_count > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Error de Traslape: Esta aula ya está ocupada en los días y rango de horario solicitados.';
    END IF;

    -- 2. Validar traslape para el mismo Profesor (No puede estar en dos aulas a la vez)
    SELECT COUNT(*) INTO overlap_count
    FROM facility_schedules
    WHERE professorId = NEW.professorId
      AND (
          NEW.days LIKE CONCAT('%', days, '%') OR days LIKE CONCAT('%', NEW.days, '%')
      )
      AND (
          @new_start < (TIME_TO_SEC(STR_TO_DATE(endTime, '%h:%i %p')) / 60) AND
          @new_end > (TIME_TO_SEC(STR_TO_DATE(startTime, '%h:%i %p')) / 60)
      );

    IF overlap_count > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Error de Traslape: El profesor ya tiene un horario asignado en este mismo intervalo.';
    END IF;
END//

CREATE TRIGGER before_update_facility_schedule
BEFORE UPDATE ON facility_schedules
FOR EACH ROW
BEGIN
    DECLARE overlap_count INT;

    SET @new_start = TIME_TO_SEC(STR_TO_DATE(NEW.startTime, '%h:%i %p')) / 60;
    SET @new_end = TIME_TO_SEC(STR_TO_DATE(NEW.endTime, '%h:%i %p')) / 60;

    IF @new_start >= @new_end THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Error: La hora de inicio debe ser strictly menor a la hora de finalización.';
    END IF;

    -- 1. Validar traslape en la misma Aula (Excluyendo el propio registro que se actualiza)
    SELECT COUNT(*) INTO overlap_count
    FROM facility_schedules
    WHERE facilityId = NEW.facilityId
      AND id <> NEW.id
      AND (
          NEW.days LIKE CONCAT('%', days, '%') OR days LIKE CONCAT('%', NEW.days, '%')
      )
      AND (
          @new_start < (TIME_TO_SEC(STR_TO_DATE(endTime, '%h:%i %p')) / 60) AND
          @new_end > (TIME_TO_SEC(STR_TO_DATE(startTime, '%h:%i %p')) / 60)
      );

    IF overlap_count > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Error de Traslape: Esta aula ya está ocupada en los días y rango de horario solicitados.';
    END IF;

    -- 2. Validar traslape para el mismo Profesor
    SELECT COUNT(*) INTO overlap_count
    FROM facility_schedules
    WHERE professorId = NEW.professorId
      AND id <> NEW.id
      AND (
          NEW.days LIKE CONCAT('%', days, '%') OR days LIKE CONCAT('%', NEW.days, '%')
      )
      AND (
          @new_start < (TIME_TO_SEC(STR_TO_DATE(endTime, '%h:%i %p')) / 60) AND
          @new_end > (TIME_TO_SEC(STR_TO_DATE(startTime, '%h:%i %p')) / 60)
      );

    IF overlap_count > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Error de Traslape: El profesor ya tiene un horario asignado en este mismo intervalo.';
    END IF;
END//

DELIMITER ;

-- Inserción del Admin Maestro inicial
-- Datos de prueba base:
-- Carnet (User login): ADMIN12345
-- Password: ASD###
-- Rol: Administrador del sistema
INSERT IGNORE INTO users (carnet, name, password, role)
VALUES ('ADMIN12345', 'Admin Maestro', 'ASD###', 'ADMIN');
