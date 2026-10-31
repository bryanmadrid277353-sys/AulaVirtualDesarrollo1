package com.aula.virtual.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.aula.virtual.R;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.aula.virtual.data.entity.BlogEntry;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentSubjectBlogBinding;
import java.util.ArrayList;
import java.util.List;

public class SubjectBlogFragment extends Fragment {
    private FragmentSubjectBlogBinding binding;
    private MainViewModel viewModel;
    private BlogAdapter adapter;
    private int subjectId;
    private boolean isAdmin;
    private ItemTouchHelper touchHelper;
    private Subject currentSubject = null;

    private final ActivityResultLauncher<String[]> openExcelFileLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null && currentSubject != null) {
                    com.aula.virtual.excel.GradeSyncManager.savePersistableUri(requireContext(), currentSubject.id, uri);
                    Toast.makeText(getContext(), "Archivo .xlsx vinculado correctamente", Toast.LENGTH_SHORT).show();
                    showExcelGradeManagerDialog(currentSubject);
                }
            });

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private static final int POLL_INTERVAL = 4000; // Poll blog every 4 seconds

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewModel != null && isResumed()) {
                viewModel.fetchSubjectBlog(subjectId);
                pollHandler.postDelayed(this, POLL_INTERVAL);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSubjectBlogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        
        if (getArguments() != null) {
            subjectId = getArguments().getInt("subjectId");
        }
        
        User user = viewModel.getCurrentUser().getValue();
        isAdmin = user != null && ("ADMIN".equals(user.role) || "PROFESSOR".equals(user.role));

        setupRecyclerView();
        observeViewModel();
        
        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        binding.btnBackList.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        if (binding.btnToggleSearch != null && binding.layoutSearchContainer != null) {
            binding.btnToggleSearch.setOnClickListener(v -> {
                boolean isVisible = binding.layoutSearchContainer.getVisibility() == View.VISIBLE;
                binding.layoutSearchContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                if (!isVisible) {
                    binding.etSearch.requestFocus();
                }
            });
        }

        if (isAdmin) {
            binding.btnAddEntryTop.setVisibility(View.VISIBLE);
            binding.btnAddEntryTop.setOnClickListener(v -> showEntryDialog(null));
        } else {
            binding.btnAddEntryTop.setVisibility(View.GONE);
        }
        
        viewModel.fetchSubjectBlog(subjectId);
        viewModel.getSubjectById(subjectId, sub -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null && sub != null) {
                        currentSubject = sub;
                        binding.tvSubjectName.setText(sub.name);
                        String sec = (sub.section != null && !sub.section.isEmpty()) ? sub.section : "01";
                        binding.tvSubjectSection.setText("Sección: " + sec);
                        String initials = sub.name != null && sub.name.length() >= 2 ? sub.name.substring(0, 2).toUpperCase() : "MA";
                        binding.tvSubjectInitials.setText(initials);
                    }
                });
            }
        });

        if (binding.btnSubjectAttendanceBlog != null) {
            binding.btnSubjectAttendanceBlog.setVisibility(View.VISIBLE);
            binding.btnSubjectAttendanceBlog.setOnClickListener(v -> {
                if (currentSubject != null) {
                    showExcelGradeManagerDialog(currentSubject);
                } else {
                    viewModel.getSubjectById(subjectId, sub -> {
                        if (sub != null && getActivity() != null) {
                            getActivity().runOnUiThread(() -> showExcelGradeManagerDialog(sub));
                        }
                    });
                }
            });
        }

        binding.btnViewParticipantsBlog.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("subjectId", subjectId);
            Navigation.findNavController(view).navigate(R.id.action_subjectBlogFragment_to_subjectParticipantsFragment, args);
        });

        binding.btnSubjectDetailBlog.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("subjectId", subjectId);
            Navigation.findNavController(view).navigate(R.id.action_subjectBlogFragment_to_subjectDetailFragment, args);
        });

        // Start polling
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL);
    }

    public void showExcelGradeManagerDialog(Subject subject) {
        if (subject == null || getContext() == null) return;
        currentSubject = subject;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_excel_grade_manager, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
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

        android.net.Uri linkedUri = com.aula.virtual.excel.GradeSyncManager.getLinkedUri(requireContext(), subject.id);
        if (linkedUri != null && tvLinkedFile != null) {
            tvLinkedFile.setText(linkedUri.getLastPathSegment());
        } else if (tvLinkedFile != null) {
            tvLinkedFile.setText("Ningún archivo vinculado actualmente.");
        }

        com.aula.virtual.data.local.AppDatabase db = com.aula.virtual.data.local.AppDatabase.getInstance(requireContext());
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            com.aula.virtual.data.entity.GradeImportLog lastLog = db.gradeImportLogDao().getLastLogForSubject(subject.id);
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
                                android.app.ProgressDialog pd = new android.app.ProgressDialog(ctx);
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
                android.net.Uri curUri = com.aula.virtual.excel.GradeSyncManager.getLinkedUri(requireContext(), subject.id);
                if (curUri == null) {
                    Toast.makeText(getContext(), "Primero debes vincular un archivo Excel (.xlsx)", Toast.LENGTH_SHORT).show();
                    return;
                }
                android.app.ProgressDialog pd = new android.app.ProgressDialog(getContext());
                pd.setMessage("Sincronizando notas desde Excel...");
                pd.show();

                com.aula.virtual.excel.GradeSyncManager.syncNowInternal(requireContext(), subject, curUri, new com.aula.virtual.excel.GradeSyncManager.SyncCallback() {
                    @Override
                    public void onSyncCompleted(com.aula.virtual.excel.ImportResult result) {
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

    private void setupRecyclerView() {
        adapter = new BlogAdapter(isAdmin, new BlogAdapter.OnBlogActionListener() {
            @Override public void onEdit(BlogEntry entry) { showEntryDialog(entry); }
            @Override public void onDelete(BlogEntry entry) { showDeleteDialog(entry); }
            @Override public void onOrderChanged(List<BlogEntry> entries) {
                viewModel.performOnlineAction(() -> viewModel.updateBlogOrder(entries));
            }
            @Override public void onDiscussionClick(BlogEntry entry) {
                Bundle args = new Bundle();
                args.putInt("blogEntryId", entry.id);
                args.putString("blogTitle", entry.title);
                args.putInt("subjectId", subjectId);
                Navigation.findNavController(requireView()).navigate(R.id.action_subjectBlogFragment_to_blogDiscussionFragment, args);
            }
        });
        binding.rvBlog.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvBlog.setAdapter(adapter);

        if (isAdmin) {
            setupDragAndDrop();
        }
    }

    private void setupDragAndDrop() {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewModel.updateBlogOrder(adapter.getEntries());
            }
        };
        touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(binding.rvBlog);
        adapter.setOnStartDragListener(touchHelper::startDrag);
    }

    private void observeViewModel() {
        viewModel.getSubjectBlog().observe(getViewLifecycleOwner(), entries -> {
            if (binding == null || !isAdded()) return;
            if (entries != null) {
                adapter.setEntries(entries);
                int count = entries.size();
                if (binding.tvBlogEntryCount != null) {
                    binding.tvBlogEntryCount.setText(count + (count == 1 ? " entrada" : " entradas"));
                    binding.tvBlogEntryCount.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void showEntryDialog(@Nullable BlogEntry existing) {
        MaterialAlertDialogBuilder builder = DialogUtils.createMaterialDialog(requireContext(), existing == null ? "Nueva Asignación" : "Editar Asignación");

        LinearLayout layout = DialogUtils.createDialogContainer(requireContext());

        final String[] selectedCategory = {existing != null ? existing.category : "Aviso"};
        final TextView tvCategory = DialogUtils.createDialogOptionButton(requireContext(), existing != null ? "Categoría: " + existing.category : "Categoría: Aviso", existing == null);
        String[] categories = {"Aviso", "Parcial", "Tarea"};
        tvCategory.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Categoría de Asignación")
                .setItems(categories, (dialog1, which) -> {
                    selectedCategory[0] = categories[which];
                    DialogUtils.setOptionState(tvCategory, "Categoría: " + selectedCategory[0], false, requireContext());
                }).show();
        });
        layout.addView(tvCategory);

        final EditText etTitle = DialogUtils.createStyledEditText(requireContext(), "Título", 0);
        if (existing != null) etTitle.setText(existing.title);
        layout.addView(etTitle);

        final EditText etContent = DialogUtils.createStyledEditText(requireContext(), "Contenido/Instrucciones", 0);
        if (existing != null) etContent.setText(existing.content);
        layout.addView(etContent);

        builder.setView(layout);
        builder.setPositiveButton("Guardar", null); 
        builder.setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String cat = selectedCategory[0];
            String title = etTitle.getText().toString();
            String content = etContent.getText().toString();

            boolean isValid = true;
            if (title.isEmpty()) {
                etTitle.setError("El título es obligatorio");
                isValid = false;
            }
            if (content.isEmpty()) {
                etContent.setError("El contenido es obligatorio");
                isValid = false;
            }

            if (isValid) {
                if (existing == null) {
                    viewModel.performOnlineAction(() -> {
                        viewModel.insertBlogEntry(new BlogEntry(subjectId, cat, title, content), result -> {
                            if (!"OK".equals(result)) Toast.makeText(getContext(), result, Toast.LENGTH_LONG).show();
                        });
                        dialog.dismiss();
                    });
                } else {
                    existing.category = cat;
                    existing.title = title;
                    existing.content = content;
                    viewModel.performOnlineAction(() -> {
                        viewModel.updateBlogEntry(existing, result -> {
                            if (!"OK".equals(result)) Toast.makeText(getContext(), result, Toast.LENGTH_LONG).show();
                        });
                        dialog.dismiss();
                    });
                }
            }
        });
    }

    private void showDeleteDialog(BlogEntry entry) {
        if (entry == null) return;
        String categoryStr = (entry.category != null && !entry.category.isEmpty()) ? entry.category : "Publicación";
        DialogUtils.showConfirmDeleteDialog(
            requireContext(),
            "Confirmar Eliminación",
            "¿Estás seguro de que deseas eliminar esta entrada del foro? Se perderán las discusiones y comentarios asociados de forma permanente.",
            entry.title,
            categoryStr + " • Foro de la materia",
            "FORO",
            null,
            R.drawable.ic_megaphone,
            () -> viewModel.performOnlineAction(() -> viewModel.deleteBlogEntry(entry))
        );
    }

    @Override
    public void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (subjectId != 0) {
            pollHandler.postDelayed(pollRunnable, POLL_INTERVAL);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pollHandler.removeCallbacks(pollRunnable);
        binding = null;
    }
}
