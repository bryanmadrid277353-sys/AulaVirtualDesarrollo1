package com.aula.virtual.data.local;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.aula.virtual.data.entity.Attendance;
import com.aula.virtual.data.entity.BlogComment;
import com.aula.virtual.data.entity.BlogEntry;
import com.aula.virtual.data.entity.Enrollment;
import com.aula.virtual.data.entity.Facility;
import com.aula.virtual.data.entity.FacilitySchedule;
import com.aula.virtual.data.entity.Notification;
import com.aula.virtual.data.entity.Student;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.data.entity.UserSubjectColor;

@Database(
    entities = {
        User.class,
        Student.class,
        Subject.class,
        Facility.class,
        Enrollment.class,
        FacilitySchedule.class,
        BlogEntry.class,
        BlogComment.class,
        Notification.class,
        UserSubjectColor.class,
        Attendance.class
    },
    version = 2,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();
    public abstract StudentDao studentDao();
    public abstract SubjectDao subjectDao();
    public abstract FacilityDao facilityDao();
    public abstract EnrollmentDao enrollmentDao();
    public abstract FacilityScheduleDao facilityScheduleDao();
    public abstract BlogDao blogDao();
    public abstract NotificationDao notificationDao();
    public abstract UserSubjectColorDao userSubjectColorDao();
    public abstract AttendanceDao attendanceDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "aula_virtual_db"
                    )
                    .addCallback(new Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            seedInitialData(db);
                        }

                        @Override
                        public void onOpen(@NonNull SupportSQLiteDatabase db) {
                            super.onOpen(db);
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    private static void seedInitialData(SupportSQLiteDatabase db) {
        db.execSQL("INSERT OR IGNORE INTO users (id, carnet, name, password, role, can_change_photo) VALUES " +
                "(1, 'ADMIN12345', 'Admin Maestro', 'ASD###', 'ADMIN', 1), " +
                "(2, 'PROF100001', 'Prof. Ricardo Valenzuela', 'ASD###', 'PROFESSOR', 1), " +
                "(3, 'EST6197088', 'Diego Salvador Madrid Cortez', 'ASD###', 'STUDENT', 1), " +
                "(4, 'ENC1444444', 'Bryan Antonio Madrid Cortez', 'ASD###', 'ENCARGADO', 1);");

        db.execSQL("INSERT OR IGNORE INTO students (id, carnet, name, grade, encargadoId) VALUES " +
                "(1, 'EST6197088', 'Diego Salvador Madrid Cortez', '5º Primaria', 4), " +
                "(2, 'EST6204112', 'Valeria Sofía Madrid Cortez', '2º Primaria', 4);");

        db.execSQL("INSERT OR IGNORE INTO facilities (id, name, type, description) VALUES " +
                "(1, 'Aula Magna A-1', 'Aulas', 'Edificio Central, 1º Nivel. Capacidad para 40 alumnos.'), " +
                "(2, 'Laboratorio de Cómputo 1', 'Laboratorio', 'Equipado con 30 ordenadores e internet de alta velocidad.'), " +
                "(3, 'Cancha Multiusos', 'Deportes', 'Área deportiva para educación física y actividades cívicas.'), " +
                "(4, 'Biblioteca General', 'Biblioteca', 'Área de lectura, consulta bibliográfica y estudio silencioso.');");

        db.execSQL("INSERT OR IGNORE INTO subjects (id, name, description, professorId, section, color) VALUES " +
                "(1, 'Matemáticas 5º Grado', 'Aritmética, geometría básica y resolución de problemas.', 2, '01', '#1E88E5'), " +
                "(2, 'Lenguaje y Literatura', 'Gramática, ortografía y comprensión lectora.', 2, '01', '#43A047'), " +
                "(3, 'Ciencias Naturales', 'Biología, ciencias del entorno y conservación medioambiental.', 2, '01', '#FB8C00'), " +
                "(4, 'Sociales e Historia', 'Geografía nacional, historia universal y formación cívica.', 2, '01', '#8E24AA');");

        db.execSQL("INSERT OR IGNORE INTO enrollments (id, studentId, subjectId, grade1, grade2, grade3, grade4, grade5, color, cycle, periodsCount) VALUES " +
                "(1, 1, 1, 8.5, 9.0, 8.0, 9.5, 9.0, 'BLUE', 'Ciclo 2025', 5), " +
                "(2, 1, 2, 9.0, 8.5, 9.5, 8.0, 9.0, 'BLUE', 'Ciclo 2025', 5), " +
                "(3, 1, 3, 8.0, 8.5, 9.0, 8.5, 9.5, 'BLUE', 'Ciclo 2025', 5), " +
                "(4, 2, 1, 9.5, 10.0, 9.0, 9.5, 10.0, 'BLUE', 'Ciclo 2025', 5);");

        db.execSQL("INSERT OR IGNORE INTO facility_schedules (id, facilityId, subjectId, professorId, days, startTime, endTime, color) VALUES " +
                "(1, 1, 1, 2, 'Lunes, Miércoles', '07:30', '09:00', '#1E88E5'), " +
                "(2, 1, 2, 2, 'Martes, Jueves', '07:30', '09:00', '#43A047'), " +
                "(3, 2, 3, 2, 'Viernes', '09:15', '11:00', '#FB8C00');");

        db.execSQL("INSERT OR IGNORE INTO notifications (id, title, message, targetType, targetValue, senderName, timestamp, iconType) VALUES " +
                "(1, 'Bienvenido al ciclo escolar 2025', 'Estimada comunidad educativa, les damos la bienvenida al nuevo ciclo lectivo 2025.', 'ALL', 'TODOS', 'Administración', '2025-01-15 08:00', 'SYSTEM'), " +
                "(2, 'Reunión de Padres y Encargados', 'Se convoca a reunión presencial de padres y tutores este viernes a las 3:00 PM.', 'ROLE_STUDENTS', 'ESTUDIANTES', 'Prof. Ricardo Valenzuela', '2025-01-20 10:30', 'GENERAL'), " +
                "(3, 'Entrega de Tarea de Matemáticas', 'Recordatorio: La entrega de la guía de ejercicios de Matemáticas es mañana.', 'SUBJECT', 'Matemáticas 5º Grado', 'Prof. Ricardo Valenzuela', '2025-01-22 14:15', 'ACADEMIC');");

        db.execSQL("INSERT OR IGNORE INTO blog_entries (id, subjectId, category, title, content, position) VALUES " +
                "(1, 1, 'Parcial 1', 'Guía de repaso para el Primer Parcial', 'Estimados estudiantes, adjunto la guía de estudio correspondiente a los temas vistos en clase.', 0), " +
                "(2, 1, 'Info', 'Horario de consultas y asesorías', 'Las asesorías académicas de Matemáticas se realizarán los días miércoles de 1:00 PM a 2:00 PM.', 1);");
    }
}
