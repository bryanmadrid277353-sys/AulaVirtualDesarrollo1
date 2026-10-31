package com.aula.virtual.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.aula.virtual.data.RetrofitClient;
import com.aula.virtual.data.entity.AttendanceRecord;
import com.aula.virtual.data.entity.Student;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.local.AppDatabase;
import com.aula.virtual.databinding.FragmentStudentAttendanceBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;

public class StudentAttendanceFragment extends Fragment {
    private FragmentStudentAttendanceBinding binding;
    private MainViewModel viewModel;
    private AttendanceDateAdapter adapter;

    private int studentId = -1;
    private int subjectId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentAttendanceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        if (getArguments() != null) {
            studentId = getArguments().getInt("studentId", -1);
            subjectId = getArguments().getInt("subjectId", -1);
        }

        setupUI();
        loadData();
    }

    private void setupUI() {
        binding.btnBackAttendance.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        adapter = new AttendanceDateAdapter();
        binding.rvAttendanceDates.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvAttendanceDates.setAdapter(adapter);
    }

    private void loadData() {
        android.content.Context ctx = getContext();
        if (ctx == null || studentId == -1 || subjectId == -1) {
            binding.tvEmptyAttendance.setVisibility(View.VISIBLE);
            return;
        }

        DialogUtils.showLoadingOverlay(binding.getRoot(), ctx);
        boolean isLocal = ThemeHelper.isLocalMode(ctx);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<AttendanceRecord> dbRecords = new ArrayList<>();
            List<com.aula.virtual.data.entity.AcademicPeriod> periods = new ArrayList<>();
            List<com.aula.virtual.data.entity.NonSchoolDay> nsdList = new ArrayList<>();
            List<com.aula.virtual.data.entity.FacilitySchedule> schedules = new ArrayList<>();
            String subtitle = null;

            if (!isLocal) {
                try {
                    retrofit2.Response<List<AttendanceRecord>> attResp =
                            RetrofitClient.getApiService(ctx).getStudentAttendanceRecords(studentId, subjectId).execute();
                    if (attResp.isSuccessful() && attResp.body() != null && !attResp.body().isEmpty()) {
                        dbRecords = attResp.body();
                    }

                    retrofit2.Response<List<com.aula.virtual.data.entity.AcademicPeriod>> pResp =
                            RetrofitClient.getApiService(ctx).getAcademicPeriods().execute();
                    if (pResp.isSuccessful() && pResp.body() != null) {
                        periods = pResp.body();
                    }

                    retrofit2.Response<List<com.aula.virtual.data.entity.NonSchoolDay>> nsdResp =
                            RetrofitClient.getApiService(ctx).getNonSchoolDays().execute();
                    if (nsdResp.isSuccessful() && nsdResp.body() != null) {
                        nsdList = nsdResp.body();
                    }

                    retrofit2.Response<List<com.aula.virtual.data.entity.FacilitySchedule>> schResp =
                            RetrofitClient.getApiService(ctx).getFacilitySchedulesBySubject(subjectId).execute();
                    if (schResp.isSuccessful() && schResp.body() != null) {
                        schedules = schResp.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (dbRecords.isEmpty()) {
                    try {
                        dbRecords = AppDatabase.getInstance(ctx).attendanceRecordDao().getRecordsByStudentAndSubject(studentId, subjectId);
                    } catch (Exception ignored) {}
                }
                if (periods.isEmpty()) {
                    try {
                        periods = AppDatabase.getInstance(ctx).academicCalendarDao().getAllPeriods();
                    } catch (Exception ignored) {}
                }
                if (nsdList.isEmpty()) {
                    try {
                        nsdList = AppDatabase.getInstance(ctx).academicCalendarDao().getAllNonSchoolDays();
                    } catch (Exception ignored) {}
                }
                if (schedules.isEmpty()) {
                    try {
                        schedules = AppDatabase.getInstance(ctx).facilityScheduleDao().getBySubjectId(subjectId);
                    } catch (Exception ignored) {}
                }
            } else {
                AppDatabase db = AppDatabase.getInstance(ctx);
                Student student = db.studentDao().getStudentById(studentId);
                Subject subject = db.subjectDao().getSubjectById(subjectId);
                dbRecords = db.attendanceRecordDao().getRecordsByStudentAndSubject(studentId, subjectId);
                periods = db.academicCalendarDao().getAllPeriods();
                nsdList = db.academicCalendarDao().getAllNonSchoolDays();
                schedules = db.facilityScheduleDao().getBySubjectId(subjectId);

                if (student != null && subject != null) {
                    subtitle = student.name + " • " + subject.name;
                } else if (student != null) {
                    subtitle = student.name;
                } else if (subject != null) {
                    subtitle = subject.name;
                }
            }

            Map<String, AttendanceRecord> recordMap = new HashMap<>();
            if (dbRecords != null) {
                for (AttendanceRecord ar : dbRecords) {
                    if (ar.classDate != null) {
                        recordMap.put(ar.classDate, ar);
                    }
                }
            }

            Set<String> nonSchoolDaysSet = new HashSet<>();
            if (nsdList != null) {
                for (com.aula.virtual.data.entity.NonSchoolDay nsd : nsdList) {
                    if (nsd.day != null) nonSchoolDaysSet.add(nsd.day);
                }
            }

            Set<Integer> scheduleDaysOfWeek = new HashSet<>();
            if (schedules != null) {
                for (com.aula.virtual.data.entity.FacilitySchedule fs : schedules) {
                    scheduleDaysOfWeek.addAll(parseScheduleDays(fs.days));
                }
            }

            SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            TreeSet<String> allDatesSet = new TreeSet<>();

            if (periods != null && !scheduleDaysOfWeek.isEmpty()) {
                for (com.aula.virtual.data.entity.AcademicPeriod period : periods) {
                    if (period.startDate == null || period.endDate == null) continue;
                    try {
                        Calendar startCal = Calendar.getInstance();
                        startCal.setTime(ymdFormat.parse(period.startDate));

                        Calendar endCal = Calendar.getInstance();
                        endCal.setTime(ymdFormat.parse(period.endDate));

                        while (!startCal.after(endCal)) {
                            int dow = startCal.get(Calendar.DAY_OF_WEEK);
                            String dateStr = ymdFormat.format(startCal.getTime());

                            if (scheduleDaysOfWeek.contains(dow) && !nonSchoolDaysSet.contains(dateStr)) {
                                allDatesSet.add(dateStr);
                            }
                            startCal.add(Calendar.DAY_OF_MONTH, 1);
                        }
                    } catch (Exception ignored) {}
                }
            }

            allDatesSet.addAll(recordMap.keySet());

            List<AttendanceRecord> fullDisplayList = new ArrayList<>();
            for (String classDate : allDatesSet) {
                AttendanceRecord ar = recordMap.get(classDate);
                if (ar == null) {
                    ar = new AttendanceRecord();
                    ar.studentId = studentId;
                    ar.subjectId = subjectId;
                    ar.classDate = classDate;
                    ar.status = 0;
                }
                fullDisplayList.add(ar);
            }

            final String finalSubtitle = subtitle;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    DialogUtils.hideLoadingOverlay(binding.getRoot());

                    if (finalSubtitle != null) {
                        binding.tvStudentSubjectSubtitle.setText(finalSubtitle);
                    }

                    if (fullDisplayList.isEmpty()) {
                        binding.rvAttendanceDates.setVisibility(View.GONE);
                        binding.tvEmptyAttendance.setVisibility(View.VISIBLE);
                        binding.tvOverallAttendancePercentage.setText("Asistencia: - %");
                    } else {
                        binding.tvEmptyAttendance.setVisibility(View.GONE);
                        binding.rvAttendanceDates.setVisibility(View.VISIBLE);
                        adapter.setRecords(fullDisplayList);

                        updateSummary(fullDisplayList);
                    }
                });
            }
        });
    }

    private Set<Integer> parseScheduleDays(String daysStr) {
        Set<Integer> daysSet = new HashSet<>();
        if (daysStr == null) return daysSet;
        String clean = java.text.Normalizer.normalize(daysStr, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        if (clean.contains("lunes")) daysSet.add(Calendar.MONDAY);
        if (clean.contains("martes")) daysSet.add(Calendar.TUESDAY);
        if (clean.contains("miercoles")) daysSet.add(Calendar.WEDNESDAY);
        if (clean.contains("jueves")) daysSet.add(Calendar.THURSDAY);
        if (clean.contains("viernes")) daysSet.add(Calendar.FRIDAY);
        if (clean.contains("sabado")) daysSet.add(Calendar.SATURDAY);
        if (clean.contains("domingo")) daysSet.add(Calendar.SUNDAY);
        return daysSet;
    }

    private void updateSummary(List<AttendanceRecord> records) {
        if (records == null || records.isEmpty()) return;

        int attended = 0;
        int absent = 0;
        int excused = 0;
        int unevaluated = 0;

        for (AttendanceRecord r : records) {
            switch (r.status) {
                case 1: attended++; break;
                case 2: absent++; break;
                case 3: excused++; break;
                case 0:
                default: unevaluated++; break;
            }
        }

        binding.tvAttendedCount.setText("Asistió: " + attended);
        binding.tvAbsentCount.setText("No asistió: " + absent);
        binding.tvExcusedCount.setText("Justificado: " + excused);
        binding.tvUnevaluatedCount.setText("Sin evaluar: " + unevaluated);

        int totalEvaluated = attended + absent + excused;
        if (totalEvaluated > 0) {
            double pct = ((double) (attended + excused) / totalEvaluated) * 100.0;
            binding.tvOverallAttendancePercentage.setText(String.format(Locale.US, "Asistencia: %.1f %%", pct));
        } else {
            binding.tvOverallAttendancePercentage.setText("Asistencia: - %");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
