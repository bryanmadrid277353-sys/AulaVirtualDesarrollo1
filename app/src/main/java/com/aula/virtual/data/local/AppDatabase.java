package com.aula.virtual.data.local;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.aula.virtual.data.entity.AcademicPeriod;
import com.aula.virtual.data.entity.AttendanceRecord;
import com.aula.virtual.data.entity.AttendanceState;
import com.aula.virtual.data.entity.BlogComment;
import com.aula.virtual.data.entity.BlogEntry;
import com.aula.virtual.data.entity.ConductLevel;
import com.aula.virtual.data.entity.ConductRecord;
import com.aula.virtual.data.entity.DirectMessage;
import com.aula.virtual.data.entity.Enrollment;
import com.aula.virtual.data.entity.Facility;
import com.aula.virtual.data.entity.FacilitySchedule;
import com.aula.virtual.data.entity.GradeImportLog;
import com.aula.virtual.data.entity.ModificationLog;
import com.aula.virtual.data.entity.NonSchoolDay;
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
        DirectMessage.class,
        GradeImportLog.class,
        ConductLevel.class,
        ConductRecord.class,
        AcademicPeriod.class,
        NonSchoolDay.class,
        AttendanceState.class,
        AttendanceRecord.class,
        ModificationLog.class
    },
    version = 9,
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
    public abstract DirectMessageDao directMessageDao();
    public abstract GradeImportLogDao gradeImportLogDao();
    public abstract ConductRecordDao conductRecordDao();
    public abstract AcademicCalendarDao academicCalendarDao();
    public abstract ModificationLogDao modificationLogDao();
    public abstract AttendanceRecordDao attendanceRecordDao();

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
                "(3, 'PROF100002', 'Profa. María Elena Rivas', 'ASD###', 'PROFESSOR', 1), " +
                "(4, 'PROF100003', 'Prof. Carlos Alberto Mendoza', 'ASD###', 'PROFESSOR', 1), " +
                "(5, 'ENC1444444', 'Bryan Antonio Madrid Cortez', 'ASD###', 'ENCARGADO', 1), " +
                "(6, 'ENC144445', 'Sofia Isabela Gomez Hernandez', 'ASD###', 'ENCARGADO', 1), " +
                "(7, 'ENC144446', 'Fernando Jose Martinez Lopez', 'ASD###', 'ENCARGADO', 1);");

        db.execSQL("INSERT OR IGNORE INTO students (id, encargadoId, carnet, name, grade) VALUES " +
                "(1, 5, 'EST6197088', 'Diego Salvador Madrid Cortez', '5º Primaria'), " +
                "(2, 5, 'EST6204112', 'Valeria Sofía Madrid Cortez', '2º Primaria'), " +
                "(3, 6, 'EST6301122', 'Mateo Alexander Gomez Hernandez', '5º Primaria'), " +
                "(4, 7, 'EST6409833', 'Camila Beatríz Martinez Lopez', '1º Bachillerato');");

        db.execSQL("INSERT OR IGNORE INTO facilities (id, code, name, type, description) VALUES " +
                "(1, 'INSTA10001', 'Aula Magna A-1', 'Aulas', 'Edificio Central, 1º Nivel. Capacidad para 40 alumnos con proyector multimedia.'), " +
                "(2, 'INSTA10002', 'Laboratorio de Cómputo 1', 'Laboratorio', 'Edificio de Tecnología, 2º Nivel. Equipado con 30 ordenadores e internet de alta velocidad.'), " +
                "(3, 'INSTA10003', 'Cancha Multiusos', 'Deportes', 'Área deportiva techada para educación física y actividades cívicas.'), " +
                "(4, 'INSTA10004', 'Biblioteca General', 'Biblioteca', 'Área de lectura, consulta bibliográfica y salas de estudio silencioso.');");

        db.execSQL("INSERT OR IGNORE INTO subjects (id, code, name, description, professorId, section, color) VALUES " +
                "(1, 'ASIGN10001', 'Matemáticas 5º Grado', 'Aritmética, geometría básica, fracciones y resolución de problemas aplicados.', 2, '01', 'BLUE'), " +
                "(2, 'ASIGN10002', 'Lenguaje y Literatura', 'Gramática, ortografía, redacción estructurada y comprensión lectora.', 3, '01', 'GREEN'), " +
                "(3, 'ASIGN10003', 'Ciencias Naturales', 'Biología fundamental, ecosistemas, método científico y cuidado medioambiental.', 4, '01', 'YELLOW_DARK'), " +
                "(4, 'ASIGN10004', 'Sociales e Historia', 'Geografía nacional, historia universal contemporánea y formación cívica.', 2, '01', 'PURPLE');");

        db.execSQL("INSERT OR IGNORE INTO facility_schedules (id, facilityId, subjectId, professorId, days, startTime, endTime, color) VALUES " +
                "(1, 1, 1, 2, 'Lunes, Miércoles', '07:30 AM', '09:00 AM', 'BLUE'), " +
                "(2, 1, 2, 3, 'Martes, Jueves', '07:30 AM', '09:00 AM', 'GREEN'), " +
                "(3, 2, 3, 4, 'Viernes', '09:15 AM', '11:00 AM', 'YELLOW_DARK'), " +
                "(4, 4, 4, 2, 'Lunes, Miércoles', '10:00 AM', '11:30 AM', 'PURPLE');");

        db.execSQL("INSERT OR IGNORE INTO enrollments (id, studentId, subjectId, grade1, grade2, grade3, grade4, grade5, color) VALUES " +
                "(1, 1, 1, 8.5, 9.0, 8.0, NULL, NULL, 'BLUE'), " +
                "(2, 1, 2, 9.0, 8.5, 9.5, NULL, NULL, 'GREEN'), " +
                "(3, 1, 3, 8.0, 8.5, 9.0, NULL, NULL, 'YELLOW_DARK'), " +
                "(4, 2, 1, 9.5, 10.0, 9.0, NULL, NULL, 'BLUE'), " +
                "(5, 3, 1, 7.5, 8.0, 8.5, NULL, NULL, 'BLUE'), " +
                "(6, 4, 4, 9.0, 9.2, 9.5, NULL, NULL, 'PURPLE');");

        db.execSQL("INSERT OR IGNORE INTO conduct_levels (value, label) VALUES " +
                "(1, 'Malo'), (2, 'Regular'), (3, 'Bueno'), (4, 'Muy bueno'), (5, 'Excelente');");

        db.execSQL("INSERT OR IGNORE INTO conduct_records (id, studentId, subjectId, period, attention_participation, rules_routines, self_regulation_effort, respect_collaboration, responsibility, observations, updatedAt) VALUES " +
                "(1, 1, 1, 1, 5, 4, 5, 5, 4, 'Excelente desempeño y participación activa en clase.', '2025-01-22 10:00:00'), " +
                "(2, 1, 1, 2, 4, 5, 4, 4, 5, 'Muy bien en trabajo colaborativo.', '2025-01-22 10:00:00'), " +
                "(3, 2, 1, 1, 5, 5, 5, 5, 5, 'Comportamiento sobresaliente.', '2025-01-22 10:00:00');");

        db.execSQL("INSERT OR IGNORE INTO attendance_states (value, label) VALUES " +
                "(0, 'Sin evaluar'), (1, 'Asistió'), (2, 'No asistió'), (3, 'Justificado');");

        db.execSQL("INSERT OR IGNORE INTO academic_periods (period, startDate, endDate) VALUES " +
                "(1, '2025-01-16', '2025-05-08'), " +
                "(2, '2025-05-09', '2025-08-29'), " +
                "(3, '2025-08-30', '2025-12-20');");

        db.execSQL("INSERT OR IGNORE INTO non_school_days (id, day, reason) VALUES " +
                "(1, '2025-05-01', 'Día del Trabajo'), " +
                "(2, '2025-08-06', 'Fiestas Agostinas'), " +
                "(3, '2025-09-15', 'Día de la Independencia');");
    }
}
