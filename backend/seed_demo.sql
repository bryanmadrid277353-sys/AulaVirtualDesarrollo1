-- ==============================================================================
-- SCRIPT DE DATOS DE DEMOSTRACIÓN Y PRUEBA (SEED DEMO) - AULA VIRTUAL
-- ==============================================================================
-- Este archivo contiene datos ficticios realistas para poblar el servidor MySQL.
-- Incluye instrucciones de formatos y tipos de datos requeridos para futuras adiciones.
--
-- INSTRUCCIONES DE USO EN MYSQL / XAMPP:
-- 1. Asegúrese de haber ejecutado previamente init.sql para crear la base de datos y tablas.
-- 2. Ejecute este script en MySQL Workbench, phpMyAdmin o mediante consola:
--    mysql -u root -p aula_virtual < backend/seed_demo.sql
-- ==============================================================================

USE aula_virtual;

-- Desactivar temporalmente revisión de claves foráneas para limpieza de tablas
SET FOREIGN_KEY_CHECKS = 0;

-- Limpieza preventiva de tablas con DELETE en lugar de TRUNCATE (Evita error #1701 en InnoDB)
DELETE FROM blog_comments;
DELETE FROM blog_entries;
DELETE FROM facility_schedules;
DELETE FROM user_subject_colors;
DELETE FROM attendance_records;
DELETE FROM non_school_days;
DELETE FROM academic_periods;
DELETE FROM conduct_records;
DELETE FROM enrollments;
DELETE FROM subjects;
DELETE FROM facilities;
DELETE FROM students;
DELETE FROM notifications;
DELETE FROM users;

-- Reinicio de contadores AUTO_INCREMENT
ALTER TABLE blog_comments AUTO_INCREMENT = 1;
ALTER TABLE blog_entries AUTO_INCREMENT = 1;
ALTER TABLE facility_schedules AUTO_INCREMENT = 1;
ALTER TABLE user_subject_colors AUTO_INCREMENT = 1;
ALTER TABLE attendance_records AUTO_INCREMENT = 1;
ALTER TABLE non_school_days AUTO_INCREMENT = 1;
ALTER TABLE conduct_records AUTO_INCREMENT = 1;
ALTER TABLE enrollments AUTO_INCREMENT = 1;
ALTER TABLE subjects AUTO_INCREMENT = 1;
ALTER TABLE facilities AUTO_INCREMENT = 1;
ALTER TABLE students AUTO_INCREMENT = 1;
ALTER TABLE notifications AUTO_INCREMENT = 1;
ALTER TABLE users AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

-- ==============================================================================
-- 1. TABLA: users (Usuarios del sistema)
-- ==============================================================================
-- ESTRUCTURA Y FORMATO DE DATOS PARA FUTURAS ADICIONES:
-- - id: INT AUTO_INCREMENT (Clave Primaria).
-- - carnet: VARCHAR(10) UNIQUE (Obligatorio). Formato según rol:
--     * Administrador: 'ADMIN' + 5 dígitos (Ej: 'ADMIN12345')
--     * Profesor:      'PROF'  + 6 dígitos (Ej: 'PROF100001')
--     * Encargado:     'ENC'   + 7 dígitos (Ej: 'ENC1444444')
-- - name: VARCHAR(255) (Obligatorio) - Nombre completo.
-- - password: VARCHAR(255) (Obligatorio) - Contraseña de acceso (Ej: 'ASD###').
-- - role: VARCHAR(50) (Obligatorio) - Valores permitidos: 'ADMIN', 'PROFESSOR', 'ENCARGADO'.
-- - address: TEXT (Opcional) - Dirección domiciliar.
-- - personal_email: VARCHAR(255) (Opcional) - Correo electrónico de contacto.
-- - can_change_photo: BOOLEAN (1 = Permitido, 0 = Bloqueado).
-- ==============================================================================

INSERT INTO users (id, carnet, name, password, role, address, personal_email, can_change_photo) VALUES
(1, 'ADMIN12345', 'Admin Maestro', 'ASD###', 'ADMIN', 'Oficina Central Edificio A, Nivel 3', 'admin.maestro@aulavirtual.edu', 1),
(2, 'PROF100001', 'Prof. Ricardo Valenzuela', 'ASD###', 'PROFESSOR', 'Colonia Escalón, Calle San José #12', 'r.valenzuela@aulavirtual.edu', 1),
(3, 'PROF100002', 'Profa. María Elena Rivas', 'ASD###', 'PROFESSOR', 'Avenida Las Camelias #45', 'm.rivas@aulavirtual.edu', 1),
(4, 'PROF100003', 'Prof. Carlos Alberto Mendoza', 'ASD###', 'PROFESSOR', 'Urbanización Jardines de la Libertad', 'c.mendoza@aulavirtual.edu', 1),
(5, 'ENC1444444', 'Bryan Antonio Madrid Cortez', 'ASD###', 'ENCARGADO', 'Residencial Los Robles, Senda 4 #18', 'bryan.madrid@gmail.com', 1),
(6, 'ENC1444445', 'Sofia Isabela Gomez Hernandez', 'ASD###', 'ENCARGADO', 'Colonia Miramonte, Pasaje B #22', 'sofia.gomez@gmail.com', 1),
(7, 'ENC1444446', 'Fernando Jose Martinez Lopez', 'ASD###', 'ENCARGADO', 'Avenida España, Condominio Plaza #3', 'fernando.martinez@outlook.com', 1);

-- ==============================================================================
-- 2. TABLA: students (Estudiantes menores de edad)
-- ==============================================================================
-- ESTRUCTURA Y FORMATO DE DATOS PARA FUTURAS ADICIONES:
-- - id: INT AUTO_INCREMENT (Clave Primaria).
-- - encargadoId: INT (Clave Foránea a users.id donde role = 'ENCARGADO').
-- - carnet: VARCHAR(20) UNIQUE (Obligatorio) - Formato: 'EST' + 7 dígitos (Ej: 'EST6197088').
-- - name: VARCHAR(255) (Obligatorio) - Nombre completo del alumno menor de edad.
-- - grade: VARCHAR(100) (Obligatorio) - Grado o nivel (Ej: '5º Primaria', '2º Bachillerato').
-- ==============================================================================

INSERT INTO students (id, encargadoId, carnet, name, grade) VALUES
(1, 5, 'EST6197088', 'Diego Salvador Madrid Cortez', '5º Primaria'),
(2, 5, 'EST6204112', 'Valeria Sofía Madrid Cortez', '2º Primaria'),
(3, 6, 'EST6301122', 'Mateo Alexander Gomez Hernandez', '5º Primaria'),
(4, 7, 'EST6409833', 'Camila Beatríz Martinez Lopez', '1º Bachillerato');

-- ==============================================================================
-- 3. TABLA: facilities (Instalaciones y Aulas)
-- ==============================================================================
-- ESTRUCTURA Y FORMATO DE DATOS PARA FUTURAS ADICIONES:
-- - id: INT AUTO_INCREMENT (Clave Primaria).
-- - code: VARCHAR(50) UNIQUE (Obligatorio) - Formato: 'INSTA' + 5 dígitos (Ej: 'INSTA10001').
-- - name: VARCHAR(255) (Obligatorio) - Nombre del aula o espacio físico.
-- - type: VARCHAR(100) (Obligatorio) - Tipo: 'Aulas', 'Laboratorio', 'Deportes', 'Biblioteca'.
-- - description: TEXT (Opcional) - Capacidad y ubicación.
-- ==============================================================================

INSERT INTO facilities (id, code, name, type, description) VALUES
(1, 'INSTA10001', 'Aula Magna A-1', 'Aulas', 'Edificio Central, 1º Nivel. Capacidad para 40 alumnos con proyector multimedia.'),
(2, 'INSTA10002', 'Laboratorio de Cómputo 1', 'Laboratorio', 'Edificio de Tecnología, 2º Nivel. Equipado con 30 ordenadores e internet de alta velocidad.'),
(3, 'INSTA10003', 'Cancha Multiusos', 'Deportes', 'Área deportiva techada para educación física y actividades cívicas.'),
(4, 'INSTA10004', 'Biblioteca General', 'Biblioteca', 'Área de lectura, consulta bibliográfica y salas de estudio silencioso.');

-- ==============================================================================
-- 4. TABLA: subjects (Materias y Asignaturas)
-- ==============================================================================
-- ESTRUCTURA Y FORMATO DE DATOS PARA FUTURAS ADICIONES:
-- - id: INT AUTO_INCREMENT (Clave Primaria).
-- - code: VARCHAR(50) UNIQUE (Obligatorio) - Formato: 'ASIGN' + 5 dígitos (Ej: 'ASIGN10001').
-- - name: VARCHAR(255) (Obligatorio) - Nombre completo de la materia.
-- - description: TEXT (Opcional) - Contenido o programa sintético.
-- - professorId: INT (Clave Foránea a users.id donde role = 'PROFESSOR').
-- - section: VARCHAR(10) (Default '01') - Formato de 2 dígitos (Ej: '01', '02').
-- - color: VARCHAR(50) (Default 'BLUE') - Nombre del color oficial o código hexadecimal.
-- ==============================================================================

INSERT INTO subjects (id, code, name, description, professorId, section, color) VALUES
(1, 'ASIGN10001', 'Matemáticas 5º Grado', 'Aritmética, geometría básica, fracciones y resolución de problemas aplicados.', 2, '01', 'BLUE'),
(2, 'ASIGN10002', 'Lenguaje y Literatura', 'Gramática, ortografía, redacción estructurada y comprensión lectora.', 3, '01', 'GREEN'),
(3, 'ASIGN10003', 'Ciencias Naturales', 'Biología fundamental, ecosistemas, método científico y cuidado medioambiental.', 4, '01', 'YELLOW_DARK'),
(4, 'ASIGN10004', 'Sociales e Historia', 'Geografía nacional, historia universal contemporánea y formación cívica.', 2, '01', 'PURPLE');

-- ==============================================================================
-- 5. TABLA: enrollments (Inscripciones y Calificaciones)
-- ==============================================================================
-- ESTRUCTURA Y FORMATO DE DATOS PARA FUTURAS ADICIONES:
-- - id: INT AUTO_INCREMENT (Clave Primaria).
-- - studentId: INT (Clave Foránea a users.id o id del estudiante).
-- - subjectId: INT (Clave Foránea a subjects.id).
-- - grade1..grade5: DOUBLE (Escala de 0.0 a 10.0) - Notas por periodo.
-- - color: VARCHAR(50) - Color personalizado de la materia para la vista del alumno.
-- ==============================================================================

INSERT INTO enrollments (id, studentId, subjectId, grade1, grade2, grade3, grade4, grade5, color) VALUES
(1, 1, 1, 8.5, 9.0, 8.0, NULL, NULL, 'BLUE'),
(2, 1, 2, 9.0, 8.5, 9.5, NULL, NULL, 'GREEN'),
(3, 1, 3, 8.0, 8.5, 9.0, NULL, NULL, 'YELLOW_DARK'),
(4, 2, 1, 9.5, 10.0, 9.0, NULL, NULL, 'BLUE'),
(5, 3, 1, 7.5, 8.0, 8.5, NULL, NULL, 'BLUE'),
(6, 4, 4, 9.0, 9.2, 9.5, NULL, NULL, 'PURPLE');

-- ==============================================================================
-- 6. TABLA: facility_schedules (Programación de Horarios Semanales)
-- ==============================================================================
-- ESTRUCTURA Y FORMATO DE DATOS PARA FUTURAS ADICIONES:
-- - id: INT AUTO_INCREMENT (Clave Primaria).
-- - facilityId: INT (Clave Foránea a facilities.id).
-- - subjectId: INT (Clave Foránea a subjects.id).
-- - professorId: INT (Clave Foránea a users.id donde role = 'PROFESSOR').
-- - days: VARCHAR(255) - Días de la semana (Ej: 'Lunes, Miércoles').
-- - startTime: VARCHAR(10) - Formato de 12 horas con AM/PM (Ej: '07:30 AM'). REQUERIDO PARA TRIGGERS.
-- - endTime: VARCHAR(10) - Formato de 12 horas con AM/PM (Ej: '09:00 AM'). REQUERIDO PARA TRIGGERS.
-- - color: VARCHAR(50) - Color representativo del bloque en la cuadrícula.
-- ==============================================================================

INSERT INTO facility_schedules (id, facilityId, subjectId, professorId, days, startTime, endTime, color) VALUES
(1, 1, 1, 2, 'Lunes, Miércoles', '07:30 AM', '09:00 AM', 'BLUE'),
(2, 1, 2, 3, 'Martes, Jueves', '07:30 AM', '09:00 AM', 'GREEN'),
(3, 2, 3, 4, 'Viernes', '09:15 AM', '11:00 AM', 'YELLOW_DARK'),
(4, 4, 4, 2, 'Lunes, Miércoles', '10:00 AM', '11:30 AM', 'PURPLE');

-- ==============================================================================
-- 7. TABLA: notifications (Avisos y Comunicados Institucionales)
-- ==============================================================================
-- ESTRUCTURA Y FORMATO DE DATOS PARA FUTURAS ADICIONES:
-- - id: INT AUTO_INCREMENT (Clave Primaria).
-- - title: VARCHAR(255) (Obligatorio) - Título del comunicado.
-- - message: TEXT (Obligatorio) - Mensaje detallado.
-- - targetType: VARCHAR(50) - Destinatarios: 'ALL', 'ROLE_STUDENTS', 'ROLE_PROFESSORS', 'SUBJECT'.
-- - targetValue: VARCHAR(255) - 'TODOS', 'ESTUDIANTES', 'PROFESORES' o Nombre de Materia.
-- - senderName: VARCHAR(255) - Nombre del emisor (Ej: 'Administración').
-- - timestamp: VARCHAR(100) - Fecha y hora en formato 'YYYY-MM-DD HH:MM'.
-- ==============================================================================

INSERT INTO notifications (id, title, message, targetType, targetValue, senderName, timestamp) VALUES
(1, 'Bienvenido al Ciclo Escolar 2025', 'Estimada comunidad educativa, les damos la más cordial bienvenida al nuevo ciclo lectivo 2025.', 'ALL', 'TODOS', 'Administración', '2025-01-15 08:00'),
(2, 'Reunión Presencial de Padres y Encargados', 'Se convoca a la primera reunión ordinaria de padres y tutores este viernes a las 3:00 PM en el Auditorio Principal.', 'ROLE_STUDENTS', 'ESTUDIANTES', 'Prof. Ricardo Valenzuela', '2025-01-20 10:30'),
(3, 'Entrega de Guía de Ejercicios de Matemáticas', 'Recordatorio: La fecha límite para subir la solución de la Guía 1 de Aritmética vence mañana a las 11:59 PM.', 'SUBJECT', 'Matemáticas 5º Grado', 'Prof. Ricardo Valenzuela', '2025-01-22 14:15');

-- ==============================================================================
-- 8. TABLA: direct_messages (Conversaciones Privadas 1 a 1)
-- ==============================================================================
INSERT INTO direct_messages (id, chatRoomId, senderId, receiverId, message, timestamp, isRead, initiatorId) VALUES
(1, 'chat_2_5', 2, 5, 'Hola, le adjunto el reporte de desempeño de Diego.', '10:42 AM', 0, 2),
(2, 'chat_3_5', 5, 3, 'Muchas gracias profesor, ya confirmamos los temas con Diego.', 'Ayer', 1, 5),
(3, 'chat_2_4', 4, 2, '¿Pudo revisar las actividades pendientes de Ciencia?', 'Lunes', 1, 4),
(4, 'chat_1_2', 1, 2, 'Recordatorio de entrega de planificaciones semanales.', '10 Oct', 1, 1);

-- ==============================================================================
-- 9. TABLA: grade_import_log (Bitácora de Sincronización Excel)
-- ==============================================================================
INSERT INTO grade_import_log (id, subjectId, sheetName, fileName, fileHash, importedAt, rowsOk, rowsError, errorDetail) VALUES
(1, 1, 'Notas', 'Plantilla_Notas_ASIGN10001.xlsx', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', '2025-01-22 10:00:00', 3, 0, NULL);

-- ==============================================================================
-- 10. TABLA: conduct_records (Registros de Conducta de Ejemplo)
-- ==============================================================================
INSERT INTO conduct_records (id, studentId, subjectId, period, attention_participation, rules_routines, self_regulation_effort, respect_collaboration, responsibility, observations, updatedAt) VALUES
(1, 1, 1, 1, 5, 4, 5, 5, 4, 'Excelente desempeño y participación activa en clase.', '2025-01-22 10:00:00'),
(2, 1, 1, 2, 4, 5, 4, 4, 5, 'Muy bien en trabajo colaborativo.', '2025-01-22 10:00:00'),
(3, 2, 1, 1, 5, 5, 5, 5, 5, 'Comportamiento sobresaliente.', '2025-01-22 10:00:00');

-- ==============================================================================
-- 11. TABLAS: academic_periods, non_school_days Y attendance_records
-- ==============================================================================
INSERT INTO academic_periods (period, startDate, endDate) VALUES
(1, '2025-01-16', '2025-05-08'),
(2, '2025-05-09', '2025-08-29'),
(3, '2025-08-30', '2025-12-20');

INSERT INTO non_school_days (id, day, reason) VALUES
(1, '2025-05-01', 'Día del Trabajo'),
(2, '2025-08-06', 'Fiestas Agostinas'),
(3, '2025-09-15', 'Día de la Independencia');

INSERT INTO attendance_records (id, studentId, subjectId, classDate, status, justification, updatedAt) VALUES
(1, 1, 1, '2025-01-20', 1, NULL, '2025-01-20 08:00:00'),
(2, 1, 1, '2025-01-22', 1, NULL, '2025-01-22 08:00:00'),
(3, 1, 1, '2025-01-27', 3, 'Incapacidad médica por gripe', '2025-01-27 08:00:00');

-- ==============================================================================
-- 8. TABLAS: blog_entries Y blog_comments (Foro de la Materia)
-- ==============================================================================
-- ESTRUCTURA Y FORMATO DE DATOS PARA FUTURAS ADICIONES:
-- - blog_entries:
--     * category: 'Aviso', 'Tarea', 'Parcial', 'Info'.
--     * position: INT - Orden de arrastre.
-- - blog_comments:
--     * timestamp: Fecha/Hora legible (Ej: '2025-01-22 15:30').
-- ==============================================================================

INSERT INTO blog_entries (id, subjectId, category, title, content, position) VALUES
(1, 1, 'Parcial 1', 'Guía de Repaso para el Primer Parcial de Matemáticas', 'Estimados estudiantes, adjunto la guía de estudio correspondiente a los temas de fracciones y proporcionalidad vistos en clase.', 0),
(2, 1, 'Info', 'Horario de Consultas y Asesorías Académicas', 'Las asesorías académicas de Matemáticas se realizarán en la Biblioteca los días miércoles de 1:00 PM a 2:00 PM.', 1);

INSERT INTO blog_comments (id, blogEntryId, userId, content, timestamp) VALUES
(1, 1, 2, 'Recuerden repasar los ejercicios 12 al 20 de la página 45.', '2025-01-22 15:30'),
(2, 1, 5, 'Muchas gracias Profesor, ya estamos revisando los temas con Diego.', '2025-01-22 16:15');

-- ==============================================================================
-- FIN DEL SCRIPT DE DATOS DEMOSTRATIVOS
-- ==============================================================================
