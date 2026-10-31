package com.aula.virtual.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aula.virtual.MainActivity;
import com.aula.virtual.R;
import com.aula.virtual.data.StudentGradeInfo;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.databinding.FragmentAdminStudentDetailBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminEnrollmentListFragment extends Fragment {
    private FragmentAdminStudentDetailBinding binding;
    private MainViewModel viewModel;
    private GradeAdapter adapter;
    private int studentId;
    private List<StudentGradeInfo> allGradeInfos = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminStudentDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        if (getArguments() == null) {
            Navigation.findNavController(view).popBackStack();
            return;
        }

        studentId = getArguments().getInt("studentId");

        binding.btnBackList.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToHome();
            } else {
                Navigation.findNavController(v).navigateUp();
            }
        });

        binding.tvHeaderTitle.setText("Gestión del estudiante");
        binding.tvTitle.setText("Materias inscritas");
        binding.tvSubtitle.setText("Consulta y gestiona las materias del estudiante.");

        if (binding.btnToggleSearch != null && binding.layoutSearchContainer != null) {
            binding.btnToggleSearch.setOnClickListener(v -> {
                boolean isVisible = binding.layoutSearchContainer.getVisibility() == View.VISIBLE;
                binding.layoutSearchContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                if (!isVisible) {
                    binding.etSearch.requestFocus();
                }
            });
        }

        viewModel.fetchStudentById(studentId);
        viewModel.getSelectedStudentRecord().observe(getViewLifecycleOwner(), student -> {
            if (student != null && binding != null) {
                binding.tvStudentName.setText(student.name != null ? student.name : "");
                String carnetStr = student.carnet != null ? student.carnet : "-";
                String gradeStr = student.grade != null ? student.grade : "";
                String details = "Carnet: " + carnetStr + (!gradeStr.isEmpty() ? " • " + gradeStr : "");
                binding.tvStudentCarnet.setText(details);
                binding.tvStudentInitials.setText(getInitials(student.name));
            }
        });

        // Timetable Header Action for Student
        binding.btnHeaderAction.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("studentId", studentId);
            Navigation.findNavController(view).navigate(R.id.action_adminEnrollmentListFragment_to_adminFacilityTimetableFragment, args);
        });

        // Carnet / Student Detail Header Action
        binding.btnHeaderDetail.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("studentId", studentId);
            Navigation.findNavController(view).navigate(R.id.action_adminEnrollmentListFragment_to_studentDetailFragment, args);
        });

        adapter = new GradeAdapter(true);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        viewModel.getAcademicPeriods().observe(getViewLifecycleOwner(), periods -> {
            if (adapter != null) {
                adapter.setActivePeriods(periods);
            }
        });
        viewModel.fetchAcademicPeriods(periods -> {
            if (adapter != null && periods != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> adapter.setActivePeriods(periods));
            }
        });

        setupSearch();

        viewModel.fetchSubjectFacilityMap(map -> {
            if (adapter != null && map != null) {
                adapter.setFacilityMap(map);
            }
        });

        viewModel.fetchSubjectProfessorMap(map -> {
            if (adapter != null && map != null) {
                adapter.setProfessorMap(map);
            }
        });

        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        viewModel.getGradeInfoForStudent(studentId).observe(getViewLifecycleOwner(), gradeInfos -> {
            if (gradeInfos != null) {
                allGradeInfos = new ArrayList<>(gradeInfos);
            } else {
                allGradeInfos = new ArrayList<>();
            }
            applyFilter();
        });

        adapter.setOnGradeActionListener(new GradeAdapter.OnGradeActionListener() {
            @Override
            public void onEditGrades(StudentGradeInfo info) {
                // Manual grade editing disabled
            }

            @Override
            public void onViewBlog(StudentGradeInfo info) {
                if (info == null || info.subject == null) return;
                Bundle args = new Bundle();
                args.putInt("subjectId", info.subject.id);
                Navigation.findNavController(requireView()).navigate(R.id.action_adminEnrollmentListFragment_to_subjectBlogFragment, args);
            }

            @Override
            public void onViewConduct(StudentGradeInfo info) {
                if (info == null || info.subject == null) return;
                Bundle args = new Bundle();
                args.putInt("studentId", studentId);
                args.putInt("subjectId", info.subject.id);
                Navigation.findNavController(requireView()).navigate(R.id.action_global_studentConductFragment, args);
            }

            @Override
            public void onAddPeriodClick(StudentGradeInfo info) {
                // Period count modification disabled
            }

            @Override
            public void onViewAttendance(StudentGradeInfo info) {
                if (info == null || info.subject == null) return;
                Bundle args = new Bundle();
                args.putInt("studentId", studentId);
                args.putInt("subjectId", info.subject.id);
                Navigation.findNavController(requireView()).navigate(R.id.action_global_studentAttendanceFragment, args);
            }

            @Override
            public void onDeleteEnrollment(StudentGradeInfo info) {
                if (info == null || info.enrollment == null) return;
                String subjName = (info.subject != null ? info.subject.name : "esta materia");
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Desinscribir Materia")
                    .setMessage("¿Estás seguro de desinscribir a este alumno de la materia \"" + subjName + "\"? Se eliminará su inscripción y sus calificaciones.")
                    .setPositiveButton("Desinscribir", (dialog, which) -> {
                        viewModel.performOnlineAction(() -> {
                            viewModel.deleteEnrollment(info.enrollment);
                            Toast.makeText(getContext(), "Inscripción eliminada", Toast.LENGTH_SHORT).show();
                        });
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
            }
        });

        binding.btnAdd.setText("+ Añadir inscripción");
        binding.btnAdd.setOnClickListener(v -> showEnrollDialog());
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "AV";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        } else {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
    }

    private void setupSearch() {
        String[] options = {"Todo", "Materia"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, options);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFilter.setAdapter(spinnerAdapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilter();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void applyFilter() {
        String query = binding.etSearch.getText().toString().toLowerCase().trim();
        List<StudentGradeInfo> filtered;

        if (query.isEmpty()) {
            filtered = new ArrayList<>(allGradeInfos);
        } else {
            filtered = allGradeInfos.stream().filter(info -> {
                if (info == null || info.subject == null) return false;
                String name = info.subject.name != null ? info.subject.name.toLowerCase() : "";
                String desc = info.subject.description != null ? info.subject.description.toLowerCase() : "";
                return name.contains(query) || desc.contains(query);
            }).collect(Collectors.toList());
        }

        // Sort by Cycle: Current cycle (Ciclo 2025) first, older cycles below!
        filtered.sort((a, b) -> {
            String ca = (a != null && a.enrollment != null && a.enrollment.cycle != null) ? a.enrollment.cycle : "";
            String cb = (b != null && b.enrollment != null && b.enrollment.cycle != null) ? b.enrollment.cycle : "";
            return cb.compareTo(ca);
        });

        adapter.setGradeInfos(filtered);
        binding.tvRecordCount.setText(filtered.size() + (filtered.size() == 1 ? " materia registrada" : " materias registradas"));
        binding.tvNoSubjectsMessage.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }



    private <T> void observeOnce(androidx.lifecycle.LiveData<T> liveData, androidx.lifecycle.Observer<T> observer) {
        liveData.observe(getViewLifecycleOwner(), new androidx.lifecycle.Observer<T>() {
            @Override
            public void onChanged(T t) {
                if (t != null) {
                    liveData.removeObserver(this);
                    observer.onChanged(t);
                }
            }
        });
    }

    private void showEnrollDialog() {
        observeOnce(viewModel.getAllSubjects(), subjects -> {
            List<Integer> enrolledSubjectIds = allGradeInfos.stream()
                .filter(info -> info != null && info.subject != null)
                .map(info -> info.subject.id)
                .collect(Collectors.toList());

            List<Subject> availableSubjects = subjects.stream()
                .filter(s -> !enrolledSubjectIds.contains(s.id))
                .collect(Collectors.toList());

            if (availableSubjects.isEmpty()) {
                Toast.makeText(getContext(), "El alumno ya está inscrito en todas las materias disponibles.", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] names = new String[availableSubjects.size()];
            for (int i = 0; i < availableSubjects.size(); i++) {
                Subject s = availableSubjects.get(i);
                names[i] = s.name + " (Sección: " + (s.section != null ? s.section : "01") + ")";
            }

            final String[] cycles = {"Ciclo 2025 (Actual)", "Ciclo 2024 (Pasado)", "Ciclo 2023 (Pasado)", "Ciclo 2022 (Pasado)"};
            final String[] cycleKeys = {"Ciclo 2025", "Ciclo 2024", "Ciclo 2023", "Ciclo 2022"};

            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Seleccionar Materia")
                .setItems(names, (dialog1, whichSubject) -> {
                    Subject chosenSub = availableSubjects.get(whichSubject);

                    new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Seleccionar Ciclo de Inscripción")
                        .setItems(cycles, (dialog2, whichCycle) -> {
                            String chosenCycle = cycleKeys[whichCycle];
                            viewModel.performOnlineAction(() -> {
                                viewModel.enrollStudentWithCycle(studentId, chosenSub.id, chosenCycle, error -> {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            if (error != null) {
                                                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                                            } else {
                                                Toast.makeText(getContext(), "Alumno inscrito exitosamente en " + chosenCycle, Toast.LENGTH_SHORT).show();
                                                viewModel.refreshData();
                                            }
                                        });
                                    }
                                });
                            });
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
