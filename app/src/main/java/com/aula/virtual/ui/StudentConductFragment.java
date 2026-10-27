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
import com.aula.virtual.data.entity.ConductRecord;
import com.aula.virtual.data.entity.Student;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.local.AppDatabase;
import com.aula.virtual.databinding.FragmentStudentConductBinding;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class StudentConductFragment extends Fragment {
    private FragmentStudentConductBinding binding;
    private MainViewModel viewModel;
    private ConductAdapter adapter;

    private int studentId = -1;
    private int subjectId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentConductBinding.inflate(inflater, container, false);
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
        binding.btnBackConduct.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        adapter = new ConductAdapter();
        binding.rvConductPeriods.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvConductPeriods.setAdapter(adapter);
    }

    private void loadData() {
        if (getContext() == null || studentId == -1 || subjectId == -1) {
            binding.tvEmptyConduct.setVisibility(View.VISIBLE);
            return;
        }

        DialogUtils.showLoadingOverlay(binding.getRoot(), requireContext());

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());

            Student student = db.studentDao().getStudentById(studentId);
            Subject subject = db.subjectDao().getSubjectById(subjectId);
            List<ConductRecord> records = db.conductRecordDao().getRecordsByStudentAndSubject(studentId, subjectId);

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
                        binding.rvConductPeriods.setVisibility(View.GONE);
                        binding.tvEmptyConduct.setVisibility(View.VISIBLE);
                        binding.tvOverallAverage.setText("Promedio: Sin evaluar");
                    } else {
                        binding.tvEmptyConduct.setVisibility(View.GONE);
                        binding.rvConductPeriods.setVisibility(View.VISIBLE);
                        adapter.setRecords(records);

                        updateSummary(records);
                    }
                });
            }
        });
    }

    private void updateSummary(List<ConductRecord> records) {
        if (records == null || records.isEmpty()) return;

        double sum = 0;
        int count = 0;

        for (ConductRecord r : records) {
            Double avg = r.average;
            if (avg == null) avg = r.calculateAverage();
            if (avg != null) {
                sum += avg;
                count++;
            }
        }

        if (count == 0) {
            binding.tvOverallAverage.setText("Promedio: Sin evaluar");
            return;
        }

        double overallAvg = sum / count;
        binding.tvOverallAverage.setText(String.format(Locale.US, "Promedio: %.2f / 5.0", overallAvg));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
