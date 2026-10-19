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
import com.aula.virtual.data.entity.BlogEntry;
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
            if (isAdmin) {
                binding.btnSubjectAttendanceBlog.setVisibility(View.GONE);
            } else {
                binding.btnSubjectAttendanceBlog.setVisibility(View.VISIBLE);
                binding.btnSubjectAttendanceBlog.setOnClickListener(v -> showSubjectAttendanceDialog(subjectId));
            }
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

    private void showSubjectAttendanceDialog(int subjectId) {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_student_attendance, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvSubtitle = dialogView.findViewById(R.id.tvAttendanceSubtitle);
        if (tvSubtitle != null) {
            tvSubtitle.setText("Control de asistencia según los días de clase de esta materia.");
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
                MaterialButton btnToggle = rowView.findViewById(R.id.btnToggleAttendance);

                if (tvDate != null) tvDate.setText(dates[i]);
                if (tvSubj != null) tvSubj.setText("Día de clase programado");

                final String[] currentStatus = {statuses[i]};

                Runnable updateButtonUI = () -> {
                    if ("PRESENT".equals(currentStatus[0])) {
                        btnToggle.setText("🟢 Asistió");
                        btnToggle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#10B981")));
                    } else if ("ABSENT".equals(currentStatus[0])) {
                        btnToggle.setText("🔴 No asistió");
                        btnToggle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EF4444")));
                    } else {
                        btnToggle.setText("🟡 Justificado");
                        btnToggle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F59E0B")));
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
                Toast.makeText(requireContext(), "Asistencia de la materia guardada exitosamente", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
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
            if (entries != null) {
                adapter.setEntries(entries);
                int count = entries.size();
                if (binding != null && binding.tvBlogEntryCount != null) {
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
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Entrada")
            .setMessage("¿Estás seguro de eliminar esta asignación?")
            .setPositiveButton("Eliminar", (dialog, which) -> {
                viewModel.performOnlineAction(() -> viewModel.deleteBlogEntry(entry));
            })
            .setNegativeButton("Cancelar", null)
            .show();
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
