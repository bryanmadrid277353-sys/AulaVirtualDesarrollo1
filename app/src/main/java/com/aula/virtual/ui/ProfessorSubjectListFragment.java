package com.aula.virtual.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aula.virtual.MainActivity;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.GradeImportLog;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.data.local.AppDatabase;
import com.aula.virtual.databinding.FragmentAdminProfessorDetailBinding;
import com.aula.virtual.excel.ExcelTemplateGenerator;
import com.aula.virtual.excel.GradeSyncManager;
import com.aula.virtual.excel.ImportResult;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProfessorSubjectListFragment extends Fragment {
    private FragmentAdminProfessorDetailBinding binding;
    private MainViewModel viewModel;
    private SubjectAdapter adapter;
    private List<Subject> allProfSubjects = new ArrayList<>();
    private Subject activeExcelSubject = null;

    private final ActivityResultLauncher<String[]> openExcelFileLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null && activeExcelSubject != null) {
                    GradeSyncManager.savePersistableUri(requireContext(), activeExcelSubject.id, uri);
                    Toast.makeText(getContext(), "Archivo .xlsx vinculado correctamente", Toast.LENGTH_SHORT).show();
                    showExcelGradeManagerDialog(activeExcelSubject);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminProfessorDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        binding.btnBackList.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToHome();
            } else {
                Navigation.findNavController(v).navigateUp();
            }
        });

        binding.tvHeaderTitle.setText("Gestión del profesor");
        binding.tvTitle.setText("Materias asignadas");
        binding.tvSubtitle.setText("Consulta y gestiona las materias del profesor.");

        adapter = new SubjectAdapter();
        adapter.setManageButtonText("Gestionar");
        adapter.setShowDeleteButton(false);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        int targetProfId = -1;
        if (getArguments() != null && getArguments().containsKey("professorId")) {
            targetProfId = getArguments().getInt("professorId");
        } else if (getArguments() != null && getArguments().containsKey("adminId")) {
            targetProfId = getArguments().getInt("adminId");
        } else {
            User prof = viewModel.getCurrentUser().getValue();
            if (prof != null) targetProfId = prof.id;
        }

        if (targetProfId != -1) {
            final int profId = targetProfId;

            viewModel.getUserById(profId, user -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (binding != null && user != null) {
                            binding.tvProfessorName.setText(user.name);
                            binding.tvProfessorCarnet.setText("Carnet: " + (user.carnet != null ? user.carnet : "-"));
                            binding.tvProfessorInitials.setText(getInitials(user.name));
                        }
                    });
                }
            });

            // Horario Button
            binding.btnHeaderAction.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putInt("professorId", profId);
                Navigation.findNavController(view).navigate(R.id.action_professorSubjectListFragment_to_adminFacilityTimetableFragment, args);
            });

            // Detalle Button
            binding.btnHeaderDetail.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putInt("adminId", profId);
                Navigation.findNavController(view).navigate(R.id.action_professorSubjectListFragment_to_adminDetailFragment, args);
            });

            binding.btnAdd.setText("+ Inscribir en materia");
            binding.btnAdd.setOnClickListener(v -> showAssignSubjectDialog(profId));

            viewModel.getSubjectsByProfessor(profId).observe(getViewLifecycleOwner(), subjects -> {
                allProfSubjects = subjects != null ? subjects : new ArrayList<>();
                applyFilter();
            });

            viewModel.fetchSubjectFacilityMap(map -> {
                if (adapter != null && map != null) {
                    adapter.setFacilityMap(map);
                }
            });
        }

        setupSearch();

        adapter.setOnItemClickListener(subject -> {
            Bundle args = new Bundle();
            args.putInt("subjectId", subject.id);
            Navigation.findNavController(view).navigate(R.id.action_professorSubjectListFragment_to_subjectBlogFragment, args);
        });
    }

    public void showExcelGradeManagerDialog(Subject subject) {
        if (subject == null || getContext() == null) return;
        activeExcelSubject = subject;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_excel_grade_manager, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View btnClose = dialogView.findViewById(R.id.btnCloseExcelDialog);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        TextView tvSubjectCode = dialogView.findViewById(R.id.tvSubjectExcelCode);
        if (tvSubjectCode != null) {
            tvSubjectCode.setText(subject.name + " (" + (subject.code != null ? subject.code : "S" + subject.id) + ")");
        }

        MaterialButton btnGenerate = dialogView.findViewById(R.id.btnGenerateExcelTemplate);
        MaterialButton btnLink = dialogView.findViewById(R.id.btnLinkExcelFile);
        TextView tvLinkedFile = dialogView.findViewById(R.id.tvLinkedFileName);
        TextView tvLastSync = dialogView.findViewById(R.id.tvLastSyncTime);
        TextView tvRowsOk = dialogView.findViewById(R.id.tvRowsOk);
        TextView tvRowsError = dialogView.findViewById(R.id.tvRowsError);
        View layoutErrorBox = dialogView.findViewById(R.id.layoutErrorDetailBox);
        TextView tvErrorDetailText = dialogView.findViewById(R.id.tvErrorDetailText);
        MaterialButton btnSync = dialogView.findViewById(R.id.btnSyncNow);

        Uri linkedUri = GradeSyncManager.getLinkedUri(requireContext(), subject.id);
        if (linkedUri != null && tvLinkedFile != null) {
            tvLinkedFile.setText(linkedUri.getLastPathSegment());
        } else if (tvLinkedFile != null) {
            tvLinkedFile.setText("Ningún archivo vinculado actualmente.");
        }

        AppDatabase db = AppDatabase.getInstance(requireContext());
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            GradeImportLog lastLog = db.gradeImportLogDao().getLastLogForSubject(subject.id);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (lastLog != null) {
                        if (tvLastSync != null) tvLastSync.setText("Fecha: " + lastLog.importedAt);
                        if (tvRowsOk != null) tvRowsOk.setText("Correctos: " + lastLog.rowsOk);
                        if (tvRowsError != null) tvRowsError.setText("Errores: " + lastLog.rowsError);

                        if (lastLog.errorDetail != null && !lastLog.errorDetail.isEmpty()) {
                            if (layoutErrorBox != null) layoutErrorBox.setVisibility(View.VISIBLE);
                            if (tvErrorDetailText != null) tvErrorDetailText.setText(lastLog.errorDetail);
                        } else if (layoutErrorBox != null) {
                            layoutErrorBox.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });

        if (btnGenerate != null) {
            btnGenerate.setOnClickListener(v -> {
                final android.content.Context ctx = getContext();
                if (ctx == null) return;
                java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                    boolean isLocal = com.aula.virtual.ui.ThemeHelper.isLocalMode(ctx);
                    List<com.aula.virtual.data.entity.AcademicPeriod> periods = new java.util.ArrayList<>();
                    if (!isLocal) {
                        try {
                            retrofit2.Response<List<com.aula.virtual.data.entity.AcademicPeriod>> resp =
                                    com.aula.virtual.data.RetrofitClient.getApiService(ctx).getAcademicPeriods().execute();
                            if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                                periods = resp.body();
                                try {
                                    db.academicCalendarDao().deleteAllPeriods();
                                    db.academicCalendarDao().insertPeriods(periods);
                                } catch (Exception ignored) {}
                            }
                        } catch (Exception ignored) {}
                    }

                    if (periods == null || periods.isEmpty()) {
                        periods = db.academicCalendarDao().getAllPeriods();
                    }

                    boolean isValid = periods != null && !periods.isEmpty();
                    if (isValid) {
                        for (com.aula.virtual.data.entity.AcademicPeriod p : periods) {
                            if (p.startDate == null || p.startDate.trim().isEmpty() ||
                                p.endDate == null || p.endDate.trim().isEmpty()) {
                                isValid = false;
                                break;
                            }
                        }
                    }
                    final boolean calendarValid = isValid;
                    final int periodCount = (periods != null) ? periods.size() : 0;
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (!calendarValid) {
                                String msg;
                                if (periodCount == 0) {
                                    msg = "No se ha configurado el Calendario Académico.\n\nUn Administrador debe ingresar al menú 'Calendario Académico' para definir las fechas del ciclo escolar antes de generar la plantilla Excel.";
                                } else {
                                    msg = "Los períodos establecidos en el Calendario Académico (" + periodCount + (periodCount == 1 ? " período" : " períodos") + ") no tienen seleccionadas sus fechas de inicio y fin completas.\n\nUn Administrador debe ingresar al menú 'Calendario Académico' para definir las fechas de todos los períodos antes de generar la plantilla Excel.";
                                }
                                new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog)
                                        .setTitle("⚠️ Calendario Académico Requerido")
                                        .setMessage(msg)
                                        .setPositiveButton("Entendido", null)
                                        .show();
                            } else {
                                ProgressDialog pd = new ProgressDialog(ctx);
                                pd.setMessage("Generando plantilla Excel automáticamente...");
                                pd.show();

                                java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                                    try {
                                        com.aula.virtual.excel.ExcelTemplateGenerator.AutoSaveResult result =
                                                com.aula.virtual.excel.ExcelTemplateGenerator.generateAndAutoSaveTemplate(ctx, subject);

                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(() -> {
                                                pd.dismiss();
                                                if (tvLinkedFile != null) {
                                                    tvLinkedFile.setText(result.fileName);
                                                }
                                                new MaterialAlertDialogBuilder(ctx, R.style.Theme_MyApplication_Dialog)
                                                        .setTitle("¡Plantilla Generada Exitosamente!")
                                                        .setMessage("Se ha generado y vinculado automáticamente el archivo Excel en:\n\n" + result.displayPath)
                                                        .setPositiveButton("Entendido", null)
                                                        .show();
                                            });
                                        }
                                    } catch (Exception e) {
                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(() -> {
                                                pd.dismiss();
                                                new MaterialAlertDialogBuilder(ctx, R.style.Theme_MyApplication_Dialog)
                                                        .setTitle("⚠️ Error al generar plantilla")
                                                        .setMessage(e.getMessage() != null ? e.getMessage() : "Ocurrió un error inesperado.")
                                                        .setPositiveButton("Entendido", null)
                                                        .show();
                                            });
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
            });
        }

        if (btnLink != null) {
            btnLink.setOnClickListener(v -> {
                openExcelFileLauncher.launch(new String[]{
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel"
                });
            });
        }

        if (btnSync != null) {
            btnSync.setOnClickListener(v -> {
                Uri curUri = GradeSyncManager.getLinkedUri(requireContext(), subject.id);
                if (curUri == null) {
                    Toast.makeText(getContext(), "Primero debes vincular un archivo Excel (.xlsx)", Toast.LENGTH_SHORT).show();
                    return;
                }
                ProgressDialog pd = new ProgressDialog(getContext());
                pd.setMessage("Sincronizando notas desde Excel...");
                pd.show();

                GradeSyncManager.syncNowInternal(requireContext(), subject, curUri, new GradeSyncManager.SyncCallback() {
                    @Override
                    public void onSyncCompleted(ImportResult result) {
                        pd.dismiss();
                        Toast.makeText(getContext(), result.message, Toast.LENGTH_LONG).show();
                        viewModel.refreshData();
                        dialog.dismiss();
                    }

                    @Override
                    public void onSyncSkipped(String reason) {
                        pd.dismiss();
                        Toast.makeText(getContext(), reason, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        pd.dismiss();
                        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    }
                });
            });
        }

        dialog.show();
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
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { applyFilter(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void applyFilter() {
        String query = binding.etSearch.getText().toString().toLowerCase().trim();
        if (query.isEmpty()) {
            adapter.setSubjects(allProfSubjects);
            binding.tvRecordCount.setText(allProfSubjects.size() + (allProfSubjects.size() == 1 ? " materia" : " materias"));
            binding.tvNoSubjectsMessage.setVisibility(allProfSubjects.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }

        List<Subject> filtered = allProfSubjects.stream().filter(s -> {
            if (s == null) return false;
            String name = s.name != null ? s.name.toLowerCase() : "";
            String desc = s.description != null ? s.description.toLowerCase() : "";
            return name.contains(query) || desc.contains(query);
        }).collect(Collectors.toList());

        adapter.setSubjects(filtered);
        binding.tvRecordCount.setText(filtered.size() + (filtered.size() == 1 ? " materia" : " materias"));
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

    private void showAssignSubjectDialog(int profId) {
        observeOnce(viewModel.getAllSubjects(), allSubjects -> {
            if (allSubjects == null || allSubjects.isEmpty()) {
                Toast.makeText(getContext(), "No hay materias registradas", Toast.LENGTH_SHORT).show();
                return;
            }
            observeOnce(viewModel.getSubjectsByProfessor(profId), assignedSubjects -> {
                List<Integer> assignedSubjectIds = assignedSubjects != null ? 
                    assignedSubjects.stream().map(s -> s.id).collect(Collectors.toList()) : 
                    new ArrayList<>();

                List<Subject> availableSubjects = allSubjects.stream()
                    .filter(s -> !assignedSubjectIds.contains(s.id))
                    .collect(Collectors.toList());

                if (availableSubjects.isEmpty()) {
                    Toast.makeText(getContext(), "El profesor ya está inscrito en todas las materias disponibles.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] names = new String[availableSubjects.size()];
                for (int i = 0; i < availableSubjects.size(); i++) {
                    Subject s = availableSubjects.get(i);
                    String sec = s.section != null ? s.section : "01";
                    names[i] = s.name + " (Sección: " + sec + ")";
                }

                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Inscribir Materia a Profesor")
                    .setItems(names, (dialog, which) -> {
                        Subject selected = availableSubjects.get(which);
                        if (selected.professorId != null && selected.professorId != 0 && selected.professorId != profId) {
                            viewModel.getUserById(selected.professorId, existingProf -> {
                                String profName = existingProf != null ? existingProf.name : "otro profesor";
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> 
                                        new MaterialAlertDialogBuilder(requireContext())
                                            .setTitle("⚠️ Asignación Restringida")
                                            .setMessage("La materia \"" + selected.name + "\" ya está asignada al profesor " + profName + ".\n\nSolo se permite UN Profesor por Materia.")
                                            .setPositiveButton("Entendido", null)
                                            .show()
                                    );
                                }
                            });
                            return;
                        }

                        selected.professorId = profId;
                        viewModel.performOnlineAction(() -> {
                            viewModel.updateSubject(selected);
                            viewModel.updateSchedulesProfessorBySubject(selected.id, profId);
                            Toast.makeText(getContext(), "¡Profesor inscrito/asignado a la materia!", Toast.LENGTH_SHORT).show();
                            viewModel.getSubjectsByProfessor(profId);
                            viewModel.refreshData();
                        });
                    })
                    .show();
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
