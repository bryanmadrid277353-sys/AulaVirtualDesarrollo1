package com.aula.virtual.ui;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.aula.virtual.data.CommentInfo;
import com.aula.virtual.data.ScheduleInfo;
import com.aula.virtual.data.StudentGradeInfo;
import com.aula.virtual.data.StudentInSubjectInfo;
import com.aula.virtual.data.VirtualAulaRepository;
import com.aula.virtual.data.entity.BlogComment;
import com.aula.virtual.data.entity.BlogEntry;
import com.aula.virtual.data.entity.Enrollment;
import com.aula.virtual.data.entity.Facility;
import com.aula.virtual.data.entity.FacilitySchedule;
import com.aula.virtual.data.entity.Notification;
import com.aula.virtual.data.entity.Student;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.entity.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainViewModel extends AndroidViewModel {
    private final VirtualAulaRepository repository;
    private final MutableLiveData<List<User>> allStudents = new MutableLiveData<>();
    private final MutableLiveData<List<Student>> allStudentRecords = new MutableLiveData<>();
    private final MutableLiveData<Student> selectedStudentRecord = new MutableLiveData<>();
    private final MutableLiveData<List<User>> allEncargados = new MutableLiveData<>();
    private final MutableLiveData<List<User>> allAdmins = new MutableLiveData<>();
    private final MutableLiveData<List<User>> allProfessors = new MutableLiveData<>();
    private final MutableLiveData<List<Subject>> allSubjects = new MutableLiveData<>();
    private final MutableLiveData<List<StudentGradeInfo>> currentStudentGrades = new MutableLiveData<>();
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<List<Student>> myStudents = new MutableLiveData<>();
    private final MutableLiveData<Student> selectedStudent = new MutableLiveData<>();
    private User adminUserBeforeImpersonation = null;

    private final MutableLiveData<User> selectedUser = new MutableLiveData<>();
    private final MutableLiveData<Subject> selectedSubject = new MutableLiveData<>();
    private final MutableLiveData<Enrollment> selectedEnrollment = new MutableLiveData<>();
    private final MutableLiveData<List<BlogEntry>> subjectBlog = new MutableLiveData<>();
    private final MutableLiveData<List<CommentInfo>> blogComments = new MutableLiveData<>();
    private final MutableLiveData<List<Subject>> professorSubjects = new MutableLiveData<>();
    private final MutableLiveData<List<StudentGradeInfo>> subjectStudents = new MutableLiveData<>();
    private final MutableLiveData<List<User>> subjectParticipants = new MutableLiveData<>();
    private final MutableLiveData<List<Facility>> allFacilities = new MutableLiveData<>();
    private final MutableLiveData<List<ScheduleInfo>> facilitySchedules = new MutableLiveData<>();
    private final MutableLiveData<List<ScheduleInfo>> professorSchedulesList = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isServerConnected = new MutableLiveData<>(true);
    private final MutableLiveData<String> modificationError = new MutableLiveData<>();

    private final Handler keepAliveHandler = new Handler(Looper.getMainLooper());
    private boolean isKeepAliveRunning = false;
    private final Runnable keepAliveRunnable = new Runnable() {
        @Override
        public void run() {
            if (isKeepAliveRunning) {
                if (!ThemeHelper.isLocalMode(getApplication())) {
                    repository.checkServerHealth(null);
                }
                keepAliveHandler.postDelayed(this, 20000);
            }
        }
    };

    public void startKeepAlive() {
        if (!isKeepAliveRunning) {
            isKeepAliveRunning = true;
            keepAliveHandler.removeCallbacks(keepAliveRunnable);
            keepAliveHandler.post(keepAliveRunnable);
        }
    }

    public void stopKeepAlive() {
        isKeepAliveRunning = false;
        keepAliveHandler.removeCallbacks(keepAliveRunnable);
    }

    public MainViewModel(Application application) {
        super(application);
        repository = new VirtualAulaRepository(application.getApplicationContext());
        repository.setConnectionStatusListener(connected -> isServerConnected.postValue(connected));
        refreshData();
    }

    public void refreshData() {
        fetchStudents();
        fetchStudentRecords();
        fetchEncargados();
        fetchAdmins();
        fetchProfessors();
        fetchSubjects();
        fetchFacilities();
    }

    public LiveData<List<User>> getAllStudents() { return allStudents; }
    public LiveData<List<Student>> getAllStudentRecords() {
        fetchStudentRecords();
        return allStudentRecords;
    }
    public LiveData<List<User>> getAllEncargados() { 
        fetchEncargados();
        return allEncargados; 
    }
    public LiveData<List<User>> getAllAdmins() { return allAdmins; }
    public LiveData<List<User>> getAllProfessors() { return allProfessors; }
    public LiveData<List<Subject>> getAllSubjects() { return allSubjects; }
    public LiveData<User> getCurrentUser() { return currentUser; }
    public void setCurrentUser(User user) { 
        currentUser.setValue(user); 
        if (user != null) {
            fetchStudentsForEncargado(user.id);
        }
    }

    public LiveData<List<Student>> getMyStudents() { return myStudents; }
    public LiveData<Student> getSelectedStudent() { return selectedStudent; }
    public void setSelectedStudent(Student student) { selectedStudent.setValue(student); }

    public void fetchStudentsForEncargado(int encargadoId) {
        repository.getStudentsByEncargado(encargadoId, new Callback<List<Student>>() {
            @Override
            public void onResponse(Call<List<Student>> call, Response<List<Student>> response) {
                if (response != null && response.body() != null) {
                    List<Student> list = response.body();
                    myStudents.postValue(list);
                    if (!list.isEmpty() && selectedStudent.getValue() == null) {
                        selectedStudent.postValue(list.get(0));
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Student>> call, Throwable t) {}
        });
    }

    public void fetchEncargados() {
        repository.getEncargadosOnly(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response != null && response.body() != null) {
                    allEncargados.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {}
        });
    }

    public void fetchStudentRecords() {
        repository.getAllStudentRecords(new Callback<List<Student>>() {
            @Override
            public void onResponse(Call<List<Student>> call, Response<List<Student>> response) {
                if (response != null && response.body() != null) {
                    allStudentRecords.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<Student>> call, Throwable t) {}
        });
    }

    public void insertStudent(Student student, Callback<Student> callback) {
        repository.insertStudent(student, callback);
    }

    public LiveData<Student> getSelectedStudentRecord() { return selectedStudentRecord; }

    public void fetchStudentById(int studentId) {
        repository.getStudentById(studentId, new Callback<Student>() {
            @Override
            public void onResponse(Call<Student> call, Response<Student> response) {
                if (response != null && response.body() != null) {
                    selectedStudentRecord.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<Student> call, Throwable t) {}
        });
    }

    public void updateStudentRecord(Student student, Callback<Student> callback) {
        repository.updateStudent(student, callback);
    }

    public void deleteStudentRecord(int studentId, Callback<Void> callback) {
        repository.deleteStudent(studentId, callback);
    }
    
    public void startImpersonation(User student) {
        adminUserBeforeImpersonation = currentUser.getValue();
        currentUser.setValue(student);
    }

    public void stopImpersonation() {
        if (adminUserBeforeImpersonation != null) {
            currentUser.setValue(adminUserBeforeImpersonation);
            adminUserBeforeImpersonation = null;
        }
    }

    public boolean isImpersonating() {
        return adminUserBeforeImpersonation != null;
    }

    public void logout() {
        currentUser.postValue(null);
        adminUserBeforeImpersonation = null;
        selectedUser.postValue(null);
        selectedSubject.postValue(null);
        selectedEnrollment.postValue(null);
        subjectBlog.postValue(null);
    }

    public LiveData<User> getSelectedUser() { return selectedUser; }
    public LiveData<Subject> getSelectedSubject() { return selectedSubject; }
    public LiveData<Enrollment> getSelectedEnrollment() { return selectedEnrollment; }
    public LiveData<List<BlogEntry>> getSubjectBlog() { return subjectBlog; }
    public LiveData<List<CommentInfo>> getBlogComments() { return blogComments; }
    public LiveData<Boolean> isServerConnected() { return isServerConnected; }
    public LiveData<String> getModificationError() { return modificationError; }

    public void clearModificationError() { modificationError.setValue(null); }

    public boolean isProfOrStudentInLocalMode() {
        if (!ThemeHelper.isLocalMode(getApplication())) return false;
        User user = currentUser.getValue();
        if (user == null) return false;
        return "PROFESSOR".equals(user.role) || "STUDENT".equals(user.role);
    }

    public boolean performOnlineAction(Runnable action) {
        if (ThemeHelper.isLocalMode(getApplication()) || Boolean.TRUE.equals(isServerConnected.getValue())) {
            action.run();
            return true;
        } else {
            modificationError.setValue("Operación no permitida: fuera de línea o sin conexión con el servidor.");
            return false;
        }
    }

    public LiveData<List<Facility>> getAllFacilities() { return allFacilities; }

    private void fetchFacilities() {
        repository.getAllFacilities(new Callback<List<Facility>>() {
            @Override public void onResponse(@NonNull Call<List<Facility>> call, @NonNull Response<List<Facility>> response) {
                if (response.isSuccessful()) allFacilities.setValue(response.body());
            }
            @Override public void onFailure(@NonNull Call<List<Facility>> call, @NonNull Throwable t) {}
        });
    }

    public void insertFacility(Facility facility) {
        repository.insertFacility(facility, new Callback<Facility>() {
            @Override public void onResponse(@NonNull Call<Facility> call, @NonNull Response<Facility> response) { refreshData(); }
            @Override public void onFailure(@NonNull Call<Facility> call, @NonNull Throwable t) {}
        });
    }

    public void updateFacility(Facility facility) {
        repository.updateFacility(facility, new Callback<Facility>() {
            @Override public void onResponse(@NonNull Call<Facility> call, @NonNull Response<Facility> response) { refreshData(); }
            @Override public void onFailure(@NonNull Call<Facility> call, @NonNull Throwable t) {}
        });
    }

    public void deleteFacility(int facId) {
        repository.deleteFacility(facId, new Callback<Void>() {
            @Override public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) { refreshData(); }
            @Override public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {}
        });
    }

    public LiveData<List<ScheduleInfo>> getFacilitySchedules(int facId) {
        fetchSchedules(facId);
        return facilitySchedules;
    }

    public void fetchSchedules(int facId) {
        repository.getFacilitySchedules(facId, new Callback<List<ScheduleInfo>>() {
            @Override public void onResponse(@NonNull Call<List<ScheduleInfo>> call, @NonNull Response<List<ScheduleInfo>> response) {
                if (response.isSuccessful()) facilitySchedules.setValue(response.body());
            }
            @Override public void onFailure(@NonNull Call<List<ScheduleInfo>> call, @NonNull Throwable t) {}
        });
    }

    public LiveData<List<ScheduleInfo>> getStudentSchedules(int studentId) {
        repository.getStudentSchedules(studentId, new Callback<List<ScheduleInfo>>() {
            @Override public void onResponse(@NonNull Call<List<ScheduleInfo>> call, @NonNull Response<List<ScheduleInfo>> response) {
                if (response.isSuccessful()) facilitySchedules.setValue(response.body());
            }
            @Override public void onFailure(@NonNull Call<List<ScheduleInfo>> call, @NonNull Throwable t) {}
        });
        return facilitySchedules;
    }

    public LiveData<List<ScheduleInfo>> getProfessorSchedules(int profId) {
        repository.getProfessorSchedules(profId, new Callback<List<ScheduleInfo>>() {
            @Override public void onResponse(@NonNull Call<List<ScheduleInfo>> call, @NonNull Response<List<ScheduleInfo>> response) {
                if (response.isSuccessful()) professorSchedulesList.setValue(response.body());
            }
            @Override public void onFailure(@NonNull Call<List<ScheduleInfo>> call, @NonNull Throwable t) {}
        });
        return professorSchedulesList;
    }

    public void insertSchedule(FacilitySchedule schedule) {
        repository.insertSchedule(schedule, new Callback<FacilitySchedule>() {
            @Override public void onResponse(@NonNull Call<FacilitySchedule> call, @NonNull Response<FacilitySchedule> response) { fetchSchedules(schedule.facilityId); }
            @Override public void onFailure(@NonNull Call<FacilitySchedule> call, @NonNull Throwable t) {}
        });
    }

    public void updateSchedule(FacilitySchedule schedule) {
        repository.updateSchedule(schedule, new Callback<FacilitySchedule>() {
            @Override public void onResponse(@NonNull Call<FacilitySchedule> call, @NonNull Response<FacilitySchedule> response) { fetchSchedules(schedule.facilityId); }
            @Override public void onFailure(@NonNull Call<FacilitySchedule> call, @NonNull Throwable t) {}
        });
    }

    public void updateSchedulesProfessorBySubject(int subjectId, int newProfId) {
        repository.updateSchedulesProfessorBySubject(subjectId, newProfId);
    }

    public void deleteSchedule(FacilitySchedule schedule) {
        repository.deleteSchedule(schedule.id, new Callback<Void>() {
            @Override public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) { fetchSchedules(schedule.facilityId); }
            @Override public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {}
        });
    }

    public void updateUserSubjectColor(int userId, int subjectId, String color) {
        repository.updateUserSubjectColor(userId, subjectId, color, new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                User user = currentUser.getValue();
                if (user != null) {
                    if ("PROFESSOR".equals(user.role)) {
                        getProfessorSchedules(user.id);
                    } else if ("STUDENT".equals(user.role)) {
                        getStudentSchedules(user.id);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                User user = currentUser.getValue();
                if (user != null) {
                    if ("PROFESSOR".equals(user.role)) {
                        getProfessorSchedules(user.id);
                    } else if ("STUDENT".equals(user.role)) {
                        getStudentSchedules(user.id);
                    }
                }
            }
        });
    }

    public void getUserSubjectColor(int userId, int subjectId, DataCallback<String> callback) {
        repository.getUserSubjectColor(userId, subjectId, new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.body() != null) callback.onResult(response.body());
                else callback.onResult("BLUE");
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                callback.onResult("BLUE");
            }
        });
    }

    public void fetchBlogComments(int entryId) {
        repository.getBlogComments(entryId, new Callback<List<CommentInfo>>() {
            @Override public void onResponse(@NonNull Call<List<CommentInfo>> call, @NonNull Response<List<CommentInfo>> response) {
                if (response.isSuccessful()) blogComments.setValue(response.body());
            }
            @Override public void onFailure(@NonNull Call<List<CommentInfo>> call, @NonNull Throwable t) {}
        });
    }

    public void insertBlogComment(BlogComment comment) {
        repository.insertBlogComment(comment, new Callback<BlogComment>() {
            @Override public void onResponse(@NonNull Call<BlogComment> call, @NonNull Response<BlogComment> response) {
                if (response.isSuccessful()) fetchBlogComments(comment.blogEntryId);
            }
            @Override public void onFailure(@NonNull Call<BlogComment> call, @NonNull Throwable t) {}
        });
    }

    public void deleteBlogComment(BlogComment comment) {
        repository.deleteBlogComment(comment.id, new Callback<Void>() {
            @Override public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) fetchBlogComments(comment.blogEntryId);
            }
            @Override public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {}
        });
    }

    public void clearBlogDiscussion(int entryId) {
        repository.clearBlogDiscussion(entryId, new Callback<Void>() {
            @Override public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) fetchBlogComments(entryId);
            }
            @Override public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {}
        });
    }

    public LiveData<List<Subject>> getSubjectsByProfessor(int professorId) {
        repository.getSubjectsByProfessor(professorId, new Callback<List<Subject>>() {
            @Override public void onResponse(@NonNull Call<List<Subject>> call, @NonNull Response<List<Subject>> response) {
                if (response.isSuccessful()) professorSubjects.setValue(response.body());
            }
            @Override public void onFailure(@NonNull Call<List<Subject>> call, @NonNull Throwable t) {}
        });
        return professorSubjects;
    }

    public LiveData<List<StudentGradeInfo>> getStudentsBySubject(int subjectId) {
        repository.getSubjectEnrollments(subjectId, new Callback<List<StudentGradeInfo>>() {
            @Override public void onResponse(@NonNull Call<List<StudentGradeInfo>> call, @NonNull Response<List<StudentGradeInfo>> response) {
                if (response.isSuccessful()) subjectStudents.setValue(response.body());
            }
            @Override public void onFailure(@NonNull Call<List<StudentGradeInfo>> call, @NonNull Throwable t) {}
        });
        return subjectStudents;
    }

    public LiveData<List<User>> getSubjectParticipants(int subjectId) {
        repository.getSubjectParticipants(subjectId, new Callback<List<User>>() {
            @Override public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful()) subjectParticipants.setValue(response.body());
            }
            @Override public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {}
        });
        return subjectParticipants;
    }

    public void fetchSubjectBlog(int subjectId) {
        repository.getSubjectBlog(subjectId, new Callback<List<BlogEntry>>() {
            @Override public void onResponse(@NonNull Call<List<BlogEntry>> call, @NonNull Response<List<BlogEntry>> response) {
                if (response.isSuccessful()) subjectBlog.setValue(response.body());
            }
            @Override public void onFailure(@NonNull Call<List<BlogEntry>> call, @NonNull Throwable t) {}
        });
    }

    public void insertBlogEntry(BlogEntry entry, DataCallback<String> onResult) {
        repository.insertBlogEntry(entry, new Callback<BlogEntry>() {
            @Override public void onResponse(@NonNull Call<BlogEntry> call, @NonNull Response<BlogEntry> response) {
                if (response.isSuccessful()) {
                    fetchSubjectBlog(entry.subjectId);
                    onResult.onResult("OK");
                } else {
                    onResult.onResult("Error en el servidor: " + response.code());
                }
            }
            @Override public void onFailure(@NonNull Call<BlogEntry> call, @NonNull Throwable t) {
                onResult.onResult("Error de conexión: No se pudo contactar con el servidor");
            }
        });
    }

    public void updateBlogEntry(BlogEntry entry, DataCallback<String> onResult) {
        repository.updateBlogEntry(entry, new Callback<BlogEntry>() {
            @Override public void onResponse(@NonNull Call<BlogEntry> call, @NonNull Response<BlogEntry> response) {
                if (response.isSuccessful()) {
                    fetchSubjectBlog(entry.subjectId);
                    onResult.onResult("OK");
                } else {
                    onResult.onResult("Error al actualizar: " + response.code());
                }
            }
            @Override public void onFailure(@NonNull Call<BlogEntry> call, @NonNull Throwable t) {
                onResult.onResult("Error de conexión");
            }
        });
    }

    public void updateBlogOrder(List<BlogEntry> entries) {
        repository.reorderBlog(entries, new Callback<Void>() {
            @Override public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {}
            @Override public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {}
        });
    }

    public void deleteBlogEntry(BlogEntry entry) {
        repository.deleteBlogEntry(entry.id, new Callback<Void>() {
            @Override public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) fetchSubjectBlog(entry.subjectId);
            }
            @Override public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {}
        });
    }

    private void fetchStudents() {
        repository.getAllStudents(new Callback<List<User>>() {
            @Override public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) allStudents.setValue(response.body());
            }
            @Override public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {}
        });
    }

    private void fetchAdmins() {
        repository.getAllAdmins(new Callback<List<User>>() {
            @Override public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) allAdmins.setValue(response.body());
            }
            @Override public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {}
        });
    }

    private void fetchProfessors() {
        repository.getAllProfessors(new Callback<List<User>>() {
            @Override public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) allProfessors.setValue(response.body());
            }
            @Override public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {}
        });
    }

    private void fetchSubjects() {
        repository.getAllSubjects(new Callback<List<Subject>>() {
            @Override public void onResponse(@NonNull Call<List<Subject>> call, @NonNull Response<List<Subject>> response) {
                if (response.isSuccessful() && response.body() != null) allSubjects.setValue(response.body());
            }
            @Override public void onFailure(@NonNull Call<List<Subject>> call, @NonNull Throwable t) {}
        });
    }

    public void login(String carnet, String password, LoginCallback callback) {
        repository.login(carnet, password, new Callback<User>() {
            @Override public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser.setValue(response.body());
                    callback.onSuccess(response.body());
                } else {
                    if (response.code() >= 500) {
                        callback.onError("Error del servidor: Los servicios de datos no responden");
                    } else {
                        callback.onError("Credenciales incorrectas");
                    }
                }
            }
            @Override public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                callback.onError("Error de conexión: No se pudo contactar con el servidor");
            }
        });
    }

    public void insertUser(User user) {
        repository.insertUser(user, new Callback<User>() {
            @Override public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) { refreshData(); }
            @Override public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {}
        });
    }

    public void updateUser(User user) {
        repository.updateUser(user, new Callback<User>() {
            @Override public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User updated = response.body();
                    User current = currentUser.getValue();
                    if (current != null && (current.id == updated.id || (current.carnet != null && current.carnet.equalsIgnoreCase(updated.carnet)))) {
                        currentUser.postValue(updated);
                    }
                    // Also update selected user for Detail screens
                    User selected = selectedUser.getValue();
                    if (selected != null && (selected.id == updated.id || (selected.carnet != null && selected.carnet.equalsIgnoreCase(updated.carnet)))) {
                        selectedUser.postValue(updated);
                    }
                }
                refreshData();
            }
            @Override public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {}
        });
    }

    public void deleteUser(User user) {
        repository.deleteUser(user.id, new Callback<Void>() {
            @Override public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) { refreshData(); }
            @Override public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {}
        });
    }

    public void insertSubject(Subject subject) {
        repository.insertSubject(subject, new Callback<Subject>() {
            @Override public void onResponse(@NonNull Call<Subject> call, @NonNull Response<Subject> response) { refreshData(); }
            @Override public void onFailure(@NonNull Call<Subject> call, @NonNull Throwable t) {}
        });
    }

    public void updateSubject(Subject subject) {
        repository.updateSubject(subject, new Callback<Subject>() {
            @Override public void onResponse(@NonNull Call<Subject> call, @NonNull Response<Subject> response) { refreshData(); }
            @Override public void onFailure(@NonNull Call<Subject> call, @NonNull Throwable t) {}
        });
    }

    public void deleteSubject(Subject subject) {
        repository.deleteSubject(subject.id, new Callback<Void>() {
            @Override public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) { refreshData(); }
            @Override public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {}
        });
    }

    public void enrollStudent(int studentId, int subjectId, DataCallback<String> errorCallback) {
        enrollStudentWithCycle(studentId, subjectId, "Ciclo 2025", errorCallback);
    }

    public void enrollStudentWithCycle(int studentId, int subjectId, String cycle, DataCallback<String> errorCallback) {
        Enrollment en = new Enrollment(studentId, subjectId, cycle);
        repository.enrollStudent(en, new Callback<Enrollment>() {
            @Override public void onResponse(@NonNull Call<Enrollment> call, @NonNull Response<Enrollment> response) {
                if (response.isSuccessful()) {
                    fetchGradesForStudent(studentId);
                } else {
                    if (errorCallback != null) errorCallback.onResult("El alumno ya está inscrito o hubo un error");
                }
            }
            @Override public void onFailure(@NonNull Call<Enrollment> call, @NonNull Throwable t) {
                if (errorCallback != null) errorCallback.onResult("Error de conexión");
            }
        });
    }

    public void updateEnrollment(Enrollment enrollment) {
        repository.updateEnrollment(enrollment, new Callback<Enrollment>() {
            @Override public void onResponse(@NonNull Call<Enrollment> call, @NonNull Response<Enrollment> response) {
                if (response.isSuccessful()) fetchGradesForStudent(enrollment.studentId);
            }
            @Override public void onFailure(@NonNull Call<Enrollment> call, @NonNull Throwable t) {}
        });
    }

    public void deleteEnrollment(Enrollment enrollment) {
        repository.deleteEnrollment(enrollment.id, new Callback<Void>() {
            @Override public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) fetchGradesForStudent(enrollment.studentId);
            }
            @Override public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {}
        });
    }

    public LiveData<List<StudentGradeInfo>> getGradeInfoForStudent(int studentId) {
        fetchGradesForStudent(studentId);
        return currentStudentGrades;
    }

    public void fetchGradesForStudent(int studentId) {
        repository.getGradeInfoForStudent(studentId, new Callback<List<StudentGradeInfo>>() {
            @Override public void onResponse(@NonNull Call<List<StudentGradeInfo>> call, @NonNull Response<List<StudentGradeInfo>> response) {
                if (response.isSuccessful() && response.body() != null) currentStudentGrades.setValue(response.body());
            }
            @Override public void onFailure(@NonNull Call<List<StudentGradeInfo>> call, @NonNull Throwable t) {}
        });
    }

    public void getUserById(int id, DataCallback<User> callback) {
        repository.getUserById(id, new Callback<User>() {
            @Override public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) { if (response.isSuccessful()) callback.onResult(response.body()); }
            @Override public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {}
        });
    }

    public void fetchUserById(int id) {
        selectedUser.setValue(null);
        repository.getUserById(id, new Callback<User>() {
            @Override public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful() && response.body() != null) selectedUser.setValue(response.body());
            }
            @Override public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {}
        });
    }

    public void getSubjectById(int id, DataCallback<Subject> callback) {
        repository.getSubjectById(id, new Callback<Subject>() {
            @Override public void onResponse(@NonNull Call<Subject> call, @NonNull Response<Subject> response) { if (response.isSuccessful()) callback.onResult(response.body()); }
            @Override public void onFailure(@NonNull Call<Subject> call, @NonNull Throwable t) {}
        });
    }

    public void fetchSubjectById(int id) {
        selectedSubject.setValue(null);
        repository.getSubjectById(id, new Callback<Subject>() {
            @Override public void onResponse(@NonNull Call<Subject> call, @NonNull Response<Subject> response) {
                if (response.isSuccessful() && response.body() != null) selectedSubject.setValue(response.body());
            }
            @Override public void onFailure(@NonNull Call<Subject> call, @NonNull Throwable t) {}
        });
    }

    public void getFacilityById(int id, DataCallback<Facility> callback) {
        repository.getFacilityById(id, new Callback<Facility>() {
            @Override public void onResponse(@NonNull Call<Facility> call, @NonNull Response<Facility> response) {
                if (response.isSuccessful() && response.body() != null) callback.onResult(response.body());
                else callback.onResult(null);
            }
            @Override public void onFailure(@NonNull Call<Facility> call, @NonNull Throwable t) {
                callback.onResult(null);
            }
        });
    }

    public void getEnrollmentById(int id, DataCallback<Enrollment> callback) {
        repository.getEnrollmentById(id, new Callback<Enrollment>() {
            @Override public void onResponse(@NonNull Call<Enrollment> call, @NonNull Response<Enrollment> response) { if (response.isSuccessful()) callback.onResult(response.body()); }
            @Override public void onFailure(@NonNull Call<Enrollment> call, @NonNull Throwable t) {}
        });
    }

    public void fetchEnrollmentById(int id) {
        selectedEnrollment.setValue(null);
        repository.getEnrollmentById(id, new Callback<Enrollment>() {
            @Override public void onResponse(@NonNull Call<Enrollment> call, @NonNull Response<Enrollment> response) {
                if (response.isSuccessful() && response.body() != null) selectedEnrollment.setValue(response.body());
            }
            @Override public void onFailure(@NonNull Call<Enrollment> call, @NonNull Throwable t) {}
        });
    }

    public void downloadAllDataForOffline(VirtualAulaRepository.SyncCallback callback) {
        if (isProfOrStudentInLocalMode()) {
            if (callback != null) callback.onError("Acción no permitida: Los Profesores y Estudiantes no pueden descargar datos del servidor en Modo Local.");
            return;
        }
        repository.downloadAllDataForOffline(callback);
    }

    public void uploadAllOfflineDataToOnline(VirtualAulaRepository.SyncCallback callback) {
        if (isProfOrStudentInLocalMode()) {
            if (callback != null) callback.onError("Acción no permitida: Los Profesores y Estudiantes no pueden subir datos al servidor en Modo Local.");
            return;
        }
        repository.uploadAllOfflineDataToOnline(callback);
    }

    public void clearAllOfflineData(VirtualAulaRepository.SyncCallback callback) {
        if (isProfOrStudentInLocalMode()) {
            if (callback != null) callback.onError("Acción no permitida: Los Profesores y Estudiantes no pueden eliminar datos de la aplicación en Modo Local.");
            return;
        }
        repository.clearAllOfflineData(callback);
    }

    public void fetchSubjectFacilityMap(DataCallback<Map<Integer, String>> callback) {
        Map<Integer, String> map = new HashMap<>();
        repository.getAllFacilities(new Callback<List<Facility>>() {
            @Override
            public void onResponse(@NonNull Call<List<Facility>> call, @NonNull Response<List<Facility>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Facility> facs = response.body();
                    if (facs.isEmpty()) {
                        callback.onResult(map);
                        return;
                    }
                    int[] pending = {facs.size()};
                    for (Facility f : facs) {
                        repository.getFacilitySchedules(f.id, new Callback<List<ScheduleInfo>>() {
                            @Override
                            public void onResponse(@NonNull Call<List<ScheduleInfo>> call, @NonNull Response<List<ScheduleInfo>> schResp) {
                                if (schResp.isSuccessful() && schResp.body() != null) {
                                    for (ScheduleInfo info : schResp.body()) {
                                        if (info.schedule != null) {
                                            map.put(info.schedule.subjectId, f.name);
                                        }
                                    }
                                }
                                pending[0]--;
                                if (pending[0] <= 0) {
                                    callback.onResult(map);
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<List<ScheduleInfo>> call, @NonNull Throwable t) {
                                pending[0]--;
                                if (pending[0] <= 0) {
                                    callback.onResult(map);
                                }
                            }
                        });
                    }
                } else {
                    callback.onResult(map);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Facility>> call, @NonNull Throwable t) {
                callback.onResult(map);
            }
        });
    }

    public void fetchSubjectProfessorMap(DataCallback<Map<Integer, String>> callback) {
        Map<Integer, String> map = new HashMap<>();
        repository.getAllProfessors(new Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<Integer, String> profMap = new HashMap<>();
                    for (User u : response.body()) {
                        profMap.put(u.id, u.name);
                    }
                    repository.getAllSubjects(new Callback<List<Subject>>() {
                        @Override
                        public void onResponse(@NonNull Call<List<Subject>> call, @NonNull Response<List<Subject>> subResponse) {
                            if (subResponse.isSuccessful() && subResponse.body() != null) {
                                for (Subject s : subResponse.body()) {
                                    if (s.professorId != null && profMap.containsKey(s.professorId)) {
                                        map.put(s.id, profMap.get(s.professorId));
                                    }
                                }
                            }
                            callback.onResult(map);
                        }
                        @Override
                        public void onFailure(@NonNull Call<List<Subject>> call, @NonNull Throwable t) {
                            callback.onResult(map);
                        }
                    });
                } else {
                    callback.onResult(map);
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {
                callback.onResult(map);
            }
        });
    }

    private final MutableLiveData<List<Notification>> allNotifications = new MutableLiveData<>();

    public LiveData<List<Notification>> getAllNotifications() {
        return allNotifications;
    }

    public void fetchNotifications() {
        allNotifications.setValue(null);
        repository.getNotifications(new Callback<List<Notification>>() {
            @Override
            public void onResponse(@NonNull Call<List<Notification>> call, @NonNull Response<List<Notification>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allNotifications.setValue(response.body());
                } else {
                    allNotifications.setValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Notification>> call, @NonNull Throwable t) {
                allNotifications.setValue(new ArrayList<>());
            }
        });
    }

    public void insertNotification(Notification notification, Runnable onSuccess) {
        repository.insertNotification(notification, new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                fetchNotifications();
                if (onSuccess != null) onSuccess.run();
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (t != null && t.getMessage() != null && !t.getMessage().isEmpty()) {
                    modificationError.postValue(t.getMessage());
                } else {
                    fetchNotifications();
                    if (onSuccess != null) onSuccess.run();
                }
            }
        });
    }

    public void updateNotification(Notification notification, Runnable onSuccess) {
        repository.updateNotification(notification, new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                fetchNotifications();
                if (onSuccess != null) onSuccess.run();
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                fetchNotifications();
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public void deleteNotification(int id, Runnable onSuccess) {
        repository.deleteNotification(id, new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                fetchNotifications();
                if (onSuccess != null) onSuccess.run();
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                fetchNotifications();
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public interface LoginCallback {
        void onSuccess(User user);
        void onError(String message);
    }

    public interface DataCallback<T> {
        void onResult(T data);
    }
}
