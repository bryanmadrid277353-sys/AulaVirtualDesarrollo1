package com.aula.virtual.data;

import com.aula.virtual.data.entity.BlogComment;
import com.aula.virtual.data.entity.BlogEntry;
import com.aula.virtual.data.entity.DirectMessage;
import com.aula.virtual.data.entity.Facility;
import com.aula.virtual.data.entity.FacilitySchedule;
import com.aula.virtual.data.entity.Enrollment;
import com.aula.virtual.data.entity.Notification;
import com.aula.virtual.data.entity.Student;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.entity.User;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @GET("/")
    Call<Map<String, Object>> healthCheck();

    // --- Users & Students ---
    @GET("users/students")
    Call<List<User>> getStudents();

    @GET("users/admins")
    Call<List<User>> getAdmins();

    @GET("users/professors")
    Call<List<User>> getProfessors();

    @GET("users/encargados")
    Call<List<User>> getEncargados();

    @GET("students")
    Call<List<Student>> getAllStudentRecords();

    @GET("students/encargado/{encargado_id}")
    Call<List<Student>> getStudentsByEncargado(@Path("encargado_id") int encargadoId);

    @POST("students")
    Call<Student> createStudent(@Body Student student);

    @GET("students/{id}")
    Call<Student> getStudentById(@Path("id") int id);

    @PUT("students/{id}")
    Call<Student> updateStudent(@Path("id") int id, @Body Student student);

    @DELETE("students/{id}")
    Call<Void> deleteStudent(@Path("id") int id);

    @GET("users/student/{id}/schedules")
    Call<List<ScheduleInfo>> getStudentSchedules(@Path("id") int studentId);

    @GET("users/professor/{id}/schedules")
    Call<List<ScheduleInfo>> getProfessorSchedules(@Path("id") int professorId);

    @PUT("users/{userId}/subjects/{subjectId}/color")
    Call<Void> updateUserSubjectColor(@Path("userId") int userId, @Path("subjectId") int subjectId, @Query("color") String color);

    @GET("users/{userId}/subjects/{subjectId}/color")
    Call<Map<String, String>> getUserSubjectColor(@Path("userId") int userId, @Path("subjectId") int subjectId);

    @POST("users")
    Call<User> createUser(@Body User user);

    @PUT("users/{id}")
    Call<User> updateUser(@Path("id") int id, @Body User user);

    @DELETE("users/{id}")
    Call<Void> deleteUser(@Path("id") int id);

    @GET("login")
    Call<User> login(@Query("carnet") String carnet, @Query("password") String password);

    @GET("users/{id}")
    Call<User> getUserById(@Path("id") int id);

    // --- Subjects ---
    @GET("subjects")
    Call<List<Subject>> getSubjects();

    @POST("subjects")
    Call<Subject> createSubject(@Body Subject subject);

    @PUT("subjects/{id}")
    Call<Subject> updateSubject(@Path("id") int id, @Body Subject subject);

    @DELETE("subjects/{id}")
    Call<Void> deleteSubject(@Path("id") int id);

    @GET("subjects/{id}")
    Call<Subject> getSubjectById(@Path("id") int id);

    @GET("subjects/professor/{prof_id}")
    Call<List<Subject>> getSubjectsByProfessor(@Path("prof_id") int professorId);

    @GET("subjects/{sub_id}/enrollments")
    Call<List<StudentGradeInfo>> getSubjectEnrollments(@Path("sub_id") int subjectId);

    @GET("subjects/{sub_id}/participants")
    Call<List<User>> getSubjectParticipants(@Path("sub_id") int subjectId);

    // --- Enrollments ---
    @GET("enrollments/student/{student_id}")
    Call<List<StudentGradeInfo>> getGradeInfoForStudent(@Path("student_id") int studentId);

    @POST("enrollments")
    Call<Enrollment> enrollStudent(@Body Enrollment enrollment);

    @PUT("enrollments/{id}")
    Call<Enrollment> updateEnrollment(@Path("id") int id, @Body Enrollment enrollment);

    @DELETE("enrollments/{id}")
    Call<Void> deleteEnrollment(@Path("id") int id);

    @GET("enrollments/{id}")
    Call<Enrollment> getEnrollmentById(@Path("id") int id);

    // --- Blog ---
    @GET("subjects/{sub_id}/blog")
    Call<List<BlogEntry>> getSubjectBlog(@Path("sub_id") int subjectId);

    @POST("blog")
    Call<BlogEntry> createBlogEntry(@Body BlogEntry entry);

    @PUT("blog/{id}")
    Call<BlogEntry> updateBlogEntry(@Path("id") int id, @Body BlogEntry entry);

    @PUT("blog/reorder")
    Call<Void> reorderBlog(@Body List<BlogEntry> entries);

    @DELETE("blog/{id}")
    Call<Void> deleteBlogEntry(@Path("id") int id);

    // --- Facilities ---
    @GET("facilities")
    Call<List<Facility>> getFacilities();

    @GET("facilities/{id}")
    Call<Facility> getFacilityById(@Path("id") int id);

    @POST("facilities")
    Call<Facility> createFacility(@Body Facility facility);

    @PUT("facilities/{id}")
    Call<Facility> updateFacility(@Path("id") int id, @Body Facility facility);

    @DELETE("facilities/{id}")
    Call<Void> deleteFacility(@Path("id") int id);

    // --- Facility Schedules ---
    @GET("facilities/{id}/schedules")
    Call<List<ScheduleInfo>> getFacilitySchedules(@Path("id") int facilityId);

    @POST("facilities/schedules")
    Call<FacilitySchedule> createSchedule(@Body FacilitySchedule schedule);

    @PUT("facilities/schedules/{id}")
    Call<FacilitySchedule> updateSchedule(@Path("id") int scheduleId, @Body FacilitySchedule schedule);

    @DELETE("facilities/schedules/{id}")
    Call<Void> deleteSchedule(@Path("id") int scheduleId);

    // --- Blog Comments ---
    @GET("blog/{entry_id}/comments")
    Call<List<CommentInfo>> getBlogComments(@Path("entry_id") int entryId);

    @POST("blog/comments")
    Call<BlogComment> createBlogComment(@Body BlogComment comment);

    @DELETE("blog/comments/{id}")
    Call<Void> deleteBlogComment(@Path("id") int commentId);

    @DELETE("blog/entries/{id}/comments")
    Call<Void> clearBlogDiscussion(@Path("id") int entryId);

    // --- Notifications ---
    @GET("notifications")
    Call<List<Notification>> getNotifications();

    @POST("notifications")
    Call<Notification> createNotification(@Body Notification notification);

    @PUT("notifications/{id}")
    Call<Notification> updateNotification(@Path("id") int id, @Body Notification notification);

    @DELETE("notifications/{id}")
    Call<Void> deleteNotification(@Path("id") int id);

    // --- Direct Messages & Private Chats ---
    @GET("direct-messages/rooms/{user_id}")
    Call<List<ChatRoomInfo>> getUserChatRooms(@Path("user_id") int userId);

    @GET("direct-messages/history/{user_id}/{contact_id}")
    Call<List<DirectMessage>> getDirectMessagesHistory(@Path("user_id") int userId, @Path("contact_id") int contactId);

    @POST("direct-messages")
    Call<DirectMessage> sendDirectMessage(@Body DirectMessage message);

    @DELETE("direct-messages/rooms/{chat_room_id}")
    Call<Void> deleteChatRoom(@Path("chat_room_id") String chatRoomId);
}
