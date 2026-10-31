package com.aula.virtual.data.local;

import com.aula.virtual.data.entity.*;
import java.util.ArrayList;
import java.util.List;

public class LocalBackupDto {
    public int version = 1;
    public String app = "Aula Virtual Local Backup";
    public String exportedAt;

    public List<User> users = new ArrayList<>();
    public List<Student> students = new ArrayList<>();
    public List<Subject> subjects = new ArrayList<>();
    public List<Facility> facilities = new ArrayList<>();
    public List<Enrollment> enrollments = new ArrayList<>();
    public List<FacilitySchedule> facilitySchedules = new ArrayList<>();
    public List<BlogEntry> blogEntries = new ArrayList<>();
    public List<BlogComment> blogComments = new ArrayList<>();
    public List<Notification> notifications = new ArrayList<>();
    public List<UserSubjectColor> userSubjectColors = new ArrayList<>();
    public List<DirectMessage> directMessages = new ArrayList<>();
    public List<GradeImportLog> gradeImportLogs = new ArrayList<>();
    public List<ConductLevel> conductLevels = new ArrayList<>();
    public List<ConductRecord> conductRecords = new ArrayList<>();
    public List<AcademicPeriod> academicPeriods = new ArrayList<>();
    public List<NonSchoolDay> nonSchoolDays = new ArrayList<>();
    public List<AttendanceState> attendanceStates = new ArrayList<>();
    public List<AttendanceRecord> attendanceRecords = new ArrayList<>();
    public List<ModificationLog> modificationLogs = new ArrayList<>();
}