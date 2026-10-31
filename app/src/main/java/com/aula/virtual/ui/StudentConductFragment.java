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
import com.aula.virtual.data.entity.ConductRecord;
import com.aula.virtual.data.entity.Student;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.local.AppDatabase;
import com.aula.virtual.databinding.FragmentStudentConductBinding;

import java.util.ArrayList;
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
        android.content.Context ctx = getContext();
        if (ctx == null || studentId == -1 || subjectId == -1) {
            binding.tvEmptyConduct.setVisibility(View.VISIBLE);
            return;
        }

        DialogUtils.showLoadingOverlay(binding.getRoot(), ctx);
        boolean isLocal = ThemeHelper.isLocalMode(ctx);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<ConductRecord> records = new ArrayList<>();
            String subtitle = null;

            if (!isLocal) {
                try {
                    retrofit2.Response<List<ConductRecord>> resp =
                            RetrofitClient.getApiService(ctx).getStudentConductRecords(studentId, subjectId).execute();
                    if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                        records = resp.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (records.isEmpty()) {
                    try {
                        records = AppDatabase.getInstance(ctx).conductRecordDao().getRecordsByStudentAndSubject(studentId, subjectId);
                    } catch (Exception ignored) {}
                }
            } else {
                AppDatabase db = AppDatabase.getInstance(ctx);
                Student student = db.studentDao().getStudentById(studentId);
                Subject subject = db.subjectDao().getSubjectById(subjectId);
                records = db.conductRecordDao().getRecordsByStudentAndSubject(studentId, subjectId);

                if (student != null && subject != null) {
                    subtitle = student.name + " • " + subject.name;
                } else if (student != null) {
                    subtitle = student.name;
                } else if (subject != null) {
                    subtitle = subject.name;
                }
            }

            final List<ConductRecord> finalRecords = records;
            final String finalSubtitle = subtitle;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    DialogUtils.hideLoadingOverlay(binding.getRoot());

                    if (finalSubtitle != null) {
                        binding.tvStudentSubjectSubtitle.setText(finalSubtitle);
                    }

                    if (finalRecords == null || finalRecords.isEmpty()) {
                        binding.rvConductPeriods.setVisibility(View.GONE);
                        binding.tvEmptyConduct.setVisibility(View.VISIBLE);
                        binding.tvOverallAverage.setText("Promedio: Sin evaluar");
                    } else {
                        binding.tvEmptyConduct.setVisibility(View.GONE);
                        binding.rvConductPeriods.setVisibility(View.VISIBLE);
                        adapter.setRecords(finalRecords);

                        updateSummary(finalRecords);
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
