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
        AttendanceRecord.class
    },
    version = 6,
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
                "(1, 'ADMIN12345', 'Admin Maestro', 'ASD###', 'ADMIN', 1);");
        db.execSQL("INSERT OR IGNORE INTO conduct_levels (value, label) VALUES " +
                "(1, 'Malo'), (2, 'Regular'), (3, 'Bueno'), (4, 'Muy bueno'), (5, 'Excelente');");
        db.execSQL("INSERT OR IGNORE INTO attendance_states (value, label) VALUES " +
                "(0, 'Sin evaluar'), (1, 'Asistió'), (2, 'No asistió'), (3, 'Justificado');");
    }
}
