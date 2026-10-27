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
import com.aula.virtual.data.entity.AttendanceRecord;
import com.aula.virtual.data.entity.Student;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.local.AppDatabase;
import com.aula.virtual.databinding.FragmentStudentAttendanceBinding;

import java.util.List;
import java.util.Locale;
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
        if (getContext() == null || studentId == -1 || subjectId == -1) {
            binding.tvEmptyAttendance.setVisibility(View.VISIBLE);
            return;
        }

        DialogUtils.showLoadingOverlay(binding.getRoot(), requireContext());

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());

            Student student = db.studentDao().getStudentById(studentId);
            Subject subject = db.subjectDao().getSubjectById(subjectId);
            List<AttendanceRecord> records = db.attendanceRecordDao().getRecordsByStudentAndSubject(studentId, subjectId);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    DialogUtils.hideLoadingOverlay(binding.getRoot());

                    if (student != null && subject != null) {
                        binding.tvStudentSubjectSubtitle.setText(student.name + " • " + subject.name);
                    } else if (student != null) {
                        binding.tvStudentSubjectSubtitle.setText(student.name);
                    } else if (subject != null) {
                        binding.tvStudentSubjectSubtitle.setText(subject.name);
                    }

                    if (records == null || records.isEmpty()) {
                        binding.rvAttendanceDates.setVisibility(View.GONE);
                        binding.tvEmptyAttendance.setVisibility(View.VISIBLE);
                        binding.tvOverallAttendancePercentage.setText("Asistencia: - %");
                    } else {
                        binding.tvEmptyAttendance.setVisibility(View.GONE);
                        binding.rvAttendanceDates.setVisibility(View.VISIBLE);
                        adapter.setRecords(records);

                        updateSummary(records);
                    }
                });
            }
        });
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
