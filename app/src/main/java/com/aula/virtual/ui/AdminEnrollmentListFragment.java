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
                showEditGradesDialog(info);
            }

            @Override
            public void onViewBlog(StudentGradeInfo info) {
                if (info == null || info.subject == null) return;
                Bundle args = new Bundle();
                args.putInt("subjectId", info.subject.id);
                Navigation.findNavController(requireView()).navigate(R.id.action_adminEnrollmentListFragment_to_subjectBlogFragment, args);
            }

            @Override
            public void onAddPeriodClick(StudentGradeInfo info) {
                showEditGradesDialog(info);
            }

            @Override
            public void onViewAttendance(StudentGradeInfo info) {
                showSubjectAttendanceDialog(info);
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

    private void showEditGradesDialog(StudentGradeInfo info) {
        if (info == null || info.enrollment == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_grades, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View btnClose = dialogView.findViewById(R.id.btnCloseDialog);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        TextView tvSubjectSubtitle = dialogView.findViewById(R.id.tvSubjectNameSubtitle);
        if (info.subject != null && info.subject.name != null) {
            tvSubjectSubtitle.setText("Materia: " + info.subject.name);
        }

        TextView tvPeriodsHeader = dialogView.findViewById(R.id.tvPeriodsCountHeader);
        View btnRemovePeriod = dialogView.findViewById(R.id.btnRemovePeriod);
        View btnAddPeriod = dialogView.findViewById(R.id.btnAddPeriod);

        LinearLayout layoutRowsContainer = dialogView.findViewById(R.id.layoutGradesRowsContainer);
        TextView tvCalcAvg = dialogView.findViewById(R.id.tvCalculatedAverage);
        TextView tvStatusBadge = dialogView.findViewById(R.id.tvCalculatedStatusBadge);

        View btnCancel = dialogView.findViewById(R.id.btnCancelDialog);
        View btnSave = dialogView.findViewById(R.id.btnSaveDialog);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        com.aula.virtual.data.entity.Enrollment e = info.enrollment;
        final int[] periodsCount = {e.periodsCount > 0 ? e.periodsCount : 5};
        List<EditText> inputFields = new ArrayList<>();

        String[] periodNames = {"Primer Parcial", "Segundo Parcial", "Tercer Parcial", "Cuarto Parcial", "Quinto Parcial", "Sexto Parcial", "Séptimo Parcial", "Octavo Parcial"};

        Runnable calculateAndRenderUI = () -> {
            tvPeriodsHeader.setText(periodsCount[0] + " Períodos evaluativos");
            layoutRowsContainer.removeAllViews();
            inputFields.clear();

            double sum = 0;
            int filledCount = 0;

            for (int i = 1; i <= periodsCount[0]; i++) {
                final int periodIndex = i;
                View rowView = LayoutInflater.from(requireContext()).inflate(R.layout.item_dialog_grade_row, null);

                TextView tvBadge = rowView.findViewById(R.id.tvPeriodBadge);
                TextView tvName = rowView.findViewById(R.id.tvPeriodName);
                EditText etGrade = rowView.findViewById(R.id.etPeriodGrade);

                tvBadge.setText("P" + periodIndex);
                tvName.setText("Nota " + periodIndex);

                Double currentVal = getGradeForPeriodIndex(e, periodIndex);
                if (currentVal != null) {
                    etGrade.setText(String.format(java.util.Locale.getDefault(), "%.1f", currentVal));
                    sum += currentVal;
                    filledCount++;
                } else {
                    etGrade.setText("");
                }

                inputFields.add(etGrade);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, (int) (10 * getResources().getDisplayMetrics().density));
                rowView.setLayoutParams(lp);

                layoutRowsContainer.addView(rowView);
            }

            // Real Average treating missing grades as 0
            double avgReal = sum / periodsCount[0];
            tvCalcAvg.setText(String.format(java.util.Locale.getDefault(), "%.1f / 10", avgReal));

            boolean hasPending = filledCount < periodsCount[0];
            if (hasPending) {
                tvStatusBadge.setText("🟡 En curso");
                tvStatusBadge.setTextColor(Color.parseColor("#F59E0B"));
            } else if (avgReal >= 6.0) {
                tvStatusBadge.setText("✓ Aprobado");
                tvStatusBadge.setTextColor(Color.parseColor("#10B981"));
            } else {
                tvStatusBadge.setText("✕ Reprobado");
                tvStatusBadge.setTextColor(Color.parseColor("#EF4444"));
            }
        };

        calculateAndRenderUI.run();

        btnRemovePeriod.setOnClickListener(v -> {
            if (periodsCount[0] > 1) {
                periodsCount[0]--;
                calculateAndRenderUI.run();
            } else {
                Toast.makeText(requireContext(), "El mínimo es 1 período", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddPeriod.setOnClickListener(v -> {
            if (periodsCount[0] < 8) {
                periodsCount[0]++;
                calculateAndRenderUI.run();
            } else {
                Toast.makeText(requireContext(), "El máximo es 8 períodos", Toast.LENGTH_SHORT).show();
            }
        });

        btnSave.setOnClickListener(v -> {
            e.periodsCount = periodsCount[0];

            for (int i = 0; i < inputFields.size(); i++) {
                int periodIndex = i + 1;
                String valStr = inputFields.get(i).getText().toString().trim();
                Double val = null;
                if (!valStr.isEmpty()) {
                    try { val = Double.parseDouble(valStr.replace(",", ".")); } catch (Exception ignored) {}
                }
                setGradeForPeriodIndex(e, periodIndex, val);
            }

            viewModel.performOnlineAction(() -> {
                viewModel.updateEnrollment(e);
                Toast.makeText(getContext(), "Calificaciones actualizadas exitosamente", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private Double getGradeForPeriodIndex(com.aula.virtual.data.entity.Enrollment e, int index) {
        if (e == null) return null;
        switch (index) {
            case 1: return e.grade1;
            case 2: return e.grade2;
            case 3: return e.grade3;
            case 4: return e.grade4;
            case 5: return e.grade5;
            case 6: return e.grade6;
            case 7: return e.grade7;
            case 8: return e.grade8;
            default: return null;
        }
    }

    private void setGradeForPeriodIndex(com.aula.virtual.data.entity.Enrollment e, int index, Double val) {
        if (e == null) return;
        switch (index) {
            case 1: e.grade1 = val; break;
            case 2: e.grade2 = val; break;
            case 3: e.grade3 = val; break;
            case 4: e.grade4 = val; break;
            case 5: e.grade5 = val; break;
            case 6: e.grade6 = val; break;
            case 7: e.grade7 = val; break;
            case 8: e.grade8 = val; break;
        }
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

    private void showSubjectAttendanceDialog(StudentGradeInfo info) {
        if (info == null || info.subject == null || getContext() == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_student_attendance, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvSubtitle = dialogView.findViewById(R.id.tvAttendanceSubtitle);
        if (tvSubtitle != null) {
            String studentName = (info.student != null && info.student.name != null) ? info.student.name : "Estudiante";
            tvSubtitle.setText("Asistencia de " + studentName + " en " + info.subject.name);
        }

        View btnClose = dialogView.findViewById(R.id.btnCloseDialog);
        View btnCancel = dialogView.findViewById(R.id.btnCancelDialog);
        View btnSave = dialogView.findViewById(R.id.btnSaveDialog);

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        LinearLayout layoutRows = dialogView.findViewById(R.id.layoutAttendanceRowsContainer);
        if (layoutRows != null) {
            layoutRows.removeAllViews();

            String[] dates = {"Lunes 13/10/2025", "Miércoles 15/10/2025", "Viernes 17/10/2025", "Lunes 20/10/2025", "Miércoles 22/10/2025"};
            String[] statuses = {"PRESENT", "PRESENT", "ABSENT", "EXCUSED", "PRESENT"};

            for (int i = 0; i < dates.length; i++) {
                View rowView = LayoutInflater.from(requireContext()).inflate(R.layout.item_attendance_row, null);

                TextView tvDate = rowView.findViewById(R.id.tvAttendanceDate);
                TextView tvSubj = rowView.findViewById(R.id.tvAttendanceSubject);
                com.google.android.material.button.MaterialButton btnToggle = rowView.findViewById(R.id.btnToggleAttendance);

                if (tvDate != null) tvDate.setText(dates[i]);
                if (tvSubj != null) tvSubj.setText(info.subject.name + " • Horario de clases");

                final String[] currentStatus = {statuses[i]};

                Runnable updateButtonUI = () -> {
                    if ("PRESENT".equals(currentStatus[0])) {
                        btnToggle.setText("🟢 Asistió");
                        btnToggle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#10B981")));
                    } else if ("ABSENT".equals(currentStatus[0])) {
                        btnToggle.setText("🔴 No asistió");
                        btnToggle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EF4444")));
                    } else {
                        btnToggle.setText("🟡 Justificado");
                        btnToggle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F59E0B")));
                    }
                };

                updateButtonUI.run();

                btnToggle.setOnClickListener(v -> {
                    if ("PRESENT".equals(currentStatus[0])) {
                        currentStatus[0] = "ABSENT";
                    } else if ("ABSENT".equals(currentStatus[0])) {
                        currentStatus[0] = "EXCUSED";
                    } else {
                        currentStatus[0] = "PRESENT";
                    }
                    updateButtonUI.run();
                });

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, (int) (8 * getResources().getDisplayMetrics().density));
                rowView.setLayoutParams(lp);

                layoutRows.addView(rowView);
            }
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Control de asistencia guardado exitosamente", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
