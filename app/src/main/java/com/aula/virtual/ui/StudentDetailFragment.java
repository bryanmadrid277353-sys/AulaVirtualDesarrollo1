package com.aula.virtual.ui;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.Student;
import com.aula.virtual.databinding.FragmentDetailBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class StudentDetailFragment extends Fragment {
    private FragmentDetailBinding binding;
    private MainViewModel viewModel;
    private Student studentRecord;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        int studentId = getArguments() != null ? getArguments().getInt("studentId", -1) : -1;

        setupUI();
        observeViewModel();

        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        DialogUtils.showLoadingOverlay(binding.getRoot(), requireContext());
        if (studentId != -1) {
            viewModel.fetchStudentById(studentId);
        } else {
            DialogUtils.hideLoadingOverlay(binding.getRoot());
        }
    }

    private void setupUI() {
        binding.btnBackDetail.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        binding.tvDetailTitle.setText("Detalle del estudiante");
        binding.cardProfileImage.setVisibility(View.GONE);

        binding.tvLabelField1.setText("Nombre completo");
        binding.etField1.setHint("Nombre Completo del Alumno");

        binding.tvLabelField2.setText("Carnet");
        binding.etField2.setHint("Carnet (7 dígitos)");
        binding.etField2.setInputType(InputType.TYPE_CLASS_NUMBER);
        binding.etField2.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});

        // Hide unused fields for minor students
        binding.tvLabelField3.setVisibility(View.GONE);
        binding.etField3.setVisibility(View.GONE);
        binding.tvPasswordStrength.setVisibility(View.GONE);
        binding.btnTogglePassword.setVisibility(View.GONE);
        binding.layoutCopyToggles.setVisibility(View.GONE);
        binding.switchLockPhoto.setVisibility(View.GONE);
        binding.cardLockPhotoContainer.setVisibility(View.GONE);
        binding.btnImpersonateAction.setVisibility(View.GONE);
        binding.btnImpersonate.setVisibility(View.GONE);

        // Field 4 hidden
        binding.etField4.setVisibility(View.GONE);

        // Address layout repurposed for Grade / Level
        binding.layoutAddress.setVisibility(View.VISIBLE);
        binding.tvLabelAddress.setText("Grado y nivel");
        binding.etAddress.setHint("Grado y Nivel (Click para cambiar)");
        binding.etAddress.setFocusable(false);
        binding.etAddress.setClickable(true);

        final String[] gradeOptions = {
            "1º Primaria", "2º Primaria", "3º Primaria", "4º Primaria",
            "5º Primaria", "6º Primaria", "7º Primaria", "8º Primaria", "9º Primaria",
            "1º Bachillerato", "2º Bachillerato", "3º Bachillerato"
        };
        binding.etAddress.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Seleccionar Grado y Nivel")
                    .setItems(gradeOptions, (dialog, which) -> {
                        binding.etAddress.setText(gradeOptions[which]);
                    }).show();
        });

        // Personal email layout repurposed for Encargado info
        binding.layoutPersonalEmail.setVisibility(View.VISIBLE);
        binding.tvLabelPersonalEmail.setText("Encargado a cargo");
        binding.etPersonalEmail.setHint("Encargado a Cargo");
        binding.etPersonalEmail.setEnabled(false);
        binding.etPersonalEmail.setFocusable(false);

        // We can use tvRolePrefix for EST
        binding.tvRolePrefix.setVisibility(View.VISIBLE);
        binding.tvRolePrefix.setText("EST");

        binding.btnSave.setEnabled(false);
        binding.btnDelete.setEnabled(false);
        binding.btnManageGrades.setEnabled(false);
        binding.btnManageGrades.setText("Gestionar Calificaciones / Inscripciones");

        binding.btnSave.setOnClickListener(v -> saveChanges());
        binding.btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        binding.btnManageGrades.setOnClickListener(v -> navigateToGrades());

        setupCopyButtons();
    }

    private void setupCopyButtons() {
        binding.btnCopyField1.setOnClickListener(v -> ClipboardUtils.copyToClipboard(getContext(), "Nombre", binding.etField1.getText().toString()));
        binding.btnCopyField2.setOnClickListener(v -> ClipboardUtils.copyToClipboard(getContext(), "Carnet", "EST" + binding.etField2.getText().toString()));
        binding.btnCopyAddress.setOnClickListener(v -> ClipboardUtils.copyToClipboard(getContext(), "Grado", binding.etAddress.getText().toString()));
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

    private void observeViewModel() {
        viewModel.getSelectedStudentRecord().observe(getViewLifecycleOwner(), student -> {
            if (student != null && binding != null) {
                studentRecord = student;

                binding.tvDetailTitle.setText("Detalle del estudiante");
                binding.tvDetailRoleHeader.setText(student.name != null ? student.name : "Estudiante");
                binding.tvDetailCarnetHeader.setText(student.carnet != null ? student.carnet : "-");
                binding.tvDetailInitials.setText(getInitials(student.name));
                binding.layoutProfileHeaderContainer.setVisibility(View.VISIBLE);

                binding.etField1.setText(student.name != null ? student.name : "");

                if (student.carnet != null && student.carnet.startsWith("EST")) {
                    binding.etField2.setText(student.carnet.substring(3));
                } else {
                    binding.etField2.setText(student.carnet != null ? student.carnet : "");
                }

                binding.etAddress.setText(student.grade != null ? student.grade : "");

                if (student.encargadoId != null) {
                    viewModel.getUserById(student.encargadoId, encargadoUser -> {
                        if (encargadoUser != null && binding != null) {
                            String encCarnet = encargadoUser.carnet != null ? 
                                (encargadoUser.carnet.startsWith("ENC") ? encargadoUser.carnet : "ENC" + encargadoUser.carnet) : "-";
                            binding.etPersonalEmail.setText(encargadoUser.name + " (" + encCarnet + ")");
                        }
                    });
                } else {
                    binding.etPersonalEmail.setText("Sin encargado asignado");
                }

                binding.btnSave.setEnabled(true);
                binding.btnDelete.setEnabled(true);
                binding.btnManageGrades.setEnabled(true);

                DialogUtils.arrangeGridButtons(binding.layoutActionButtons);
                DialogUtils.hideLoadingOverlay(binding.getRoot());
            }
        });
    }

    private void saveChanges() {
        if (studentRecord != null) {
            String name = binding.etField1.getText().toString().trim();
            String carnetInput = binding.etField2.getText().toString().trim();
            String grade = binding.etAddress.getText().toString().trim();

            String carnetDigits = carnetInput;
            if (carnetDigits.startsWith("EST")) carnetDigits = carnetDigits.substring(3);

            boolean isValid = true;
            if (name.isEmpty()) {
                binding.etField1.setError("El nombre es obligatorio");
                isValid = false;
            }
            if (!carnetDigits.matches("\\d{7}")) {
                binding.etField2.setError("Carnet inválido (debe ingresar exactamente 7 dígitos numéricos)");
                isValid = false;
            }
            if (grade.isEmpty()) {
                binding.etAddress.setError("El grado es obligatorio");
                isValid = false;
            }

            if (isValid) {
                studentRecord.name = name;
                studentRecord.carnet = "EST" + carnetDigits;
                studentRecord.grade = grade;

                viewModel.performOnlineAction(() -> {
                    viewModel.updateStudentRecord(studentRecord, new retrofit2.Callback<Student>() {
                        @Override
                        public void onResponse(@NonNull retrofit2.Call<Student> call, @NonNull retrofit2.Response<Student> response) {
                            Toast.makeText(getContext(), "Alumno actualizado exitosamente", Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(requireView()).popBackStack();
                        }

                        @Override
                        public void onFailure(@NonNull retrofit2.Call<Student> call, @NonNull Throwable t) {
                            Toast.makeText(getContext(), "Error al actualizar alumno", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }
        }
    }

    private void showDeleteConfirmation() {
        if (studentRecord == null) return;
        String gradeStr = studentRecord.grade != null ? studentRecord.grade : "Estudiante";
        String badge = studentRecord.carnet != null ? studentRecord.carnet : "";
        DialogUtils.showConfirmDeleteDialog(
            requireContext(),
            "Confirmar Eliminación",
            "¿Estás seguro de que deseas eliminar este registro de alumno? Se perderán las inscripciones y notas asociadas de forma permanente.",
            studentRecord.name,
            gradeStr + " • AulaV",
            badge,
            null,
            0,
            () -> {
                viewModel.performOnlineAction(() -> {
                    viewModel.deleteStudentRecord(studentRecord.id, new retrofit2.Callback<Void>() {
                        @Override
                        public void onResponse(@NonNull retrofit2.Call<Void> call, @NonNull retrofit2.Response<Void> response) {
                            Toast.makeText(getContext(), "Alumno eliminado", Toast.LENGTH_SHORT).show();
                            if (!Navigation.findNavController(requireView()).popBackStack(R.id.adminStudentListFragment, false)) {
                                Navigation.findNavController(requireView()).popBackStack();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull retrofit2.Call<Void> call, @NonNull Throwable t) {
                            Toast.makeText(getContext(), "Error al eliminar alumno", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }
        );
    }

    private void navigateToGrades() {
        if (studentRecord == null) return;
        Bundle args = new Bundle();
        args.putInt("studentId", studentRecord.id);
        Navigation.findNavController(requireView()).navigate(R.id.action_studentDetailFragment_to_adminEnrollmentListFragment, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
