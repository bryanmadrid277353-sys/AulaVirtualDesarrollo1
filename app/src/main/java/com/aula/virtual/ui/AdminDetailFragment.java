package com.aula.virtual.ui;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
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
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentDetailBinding;

public class AdminDetailFragment extends Fragment {
    private FragmentDetailBinding binding;
    private MainViewModel viewModel;
    private User admin;

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
        int adminId = getArguments().getInt("adminId");

        setupUI();
        observeViewModel();
        
        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        DialogUtils.showLoadingOverlay(binding.getRoot(), requireContext());
        viewModel.fetchUserById(adminId);
    }

    private void setupUI() {
        binding.btnBackDetail.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        binding.tvDetailTitle.setText("Detalle"); // Will be updated in observeViewModel
        
        binding.etField1.setHint("Nombre Completo");
        
        binding.etField2.setHint("Carnet");
        binding.etField2.setInputType(InputType.TYPE_CLASS_TEXT);
        binding.etField2.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15)});

        binding.etField3.setHint("Contraseña");
        binding.etField3.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        binding.etField3.setVisibility(View.VISIBLE);
        binding.layoutCopyToggles.setVisibility(View.VISIBLE);
        binding.btnTogglePassword.setVisibility(View.VISIBLE);
        binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);

        binding.btnTogglePassword.setOnClickListener(v -> {
            int inputType = binding.etField3.getInputType();
            if (inputType == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                binding.etField3.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility);
            } else {
                binding.etField3.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
            }
            binding.etField3.setSelection(binding.etField3.getText().length());
        });

        binding.tvPasswordStrength.setVisibility(View.VISIBLE);
        binding.etField3.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                ValidationUtils.PasswordStrength strength = ValidationUtils.getPasswordStrength(s.toString());
                binding.tvPasswordStrength.setText(strength.label);
                binding.tvPasswordStrength.setTextColor(strength.color);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.btnSave.setOnClickListener(v -> saveChanges());
        binding.btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        binding.btnImpersonateAction.setOnClickListener(v -> showImpersonateConfirmationDialog());

        setupCopyButtons();
    }

    private void setupCopyButtons() {
        binding.btnCopyField1.setOnClickListener(v -> ClipboardUtils.copyToClipboard(getContext(), "Nombre", binding.etField1.getText().toString()));
        binding.btnCopyField2.setOnClickListener(v -> {
            String carnetBody = binding.etField2.getText().toString();
            String prefix = "";
            if (binding.tvRolePrefix.getVisibility() == View.VISIBLE) {
                prefix = binding.tvRolePrefix.getText().toString();
            }
            ClipboardUtils.copyToClipboard(getContext(), "Carnet", prefix + carnetBody);
        });
        binding.btnCopyField3.setOnClickListener(v -> ClipboardUtils.copyToClipboard(getContext(), "Contraseña", binding.etField3.getText().toString()));
        binding.btnCopyAddress.setOnClickListener(v -> ClipboardUtils.copyToClipboard(getContext(), "Dirección", binding.etAddress.getText().toString()));
        binding.btnCopyPersonalEmail.setOnClickListener(v -> ClipboardUtils.copyToClipboard(getContext(), "Correo Personal", binding.etPersonalEmail.getText().toString()));
    }

    private void showImpersonateConfirmationDialog() {
        if (admin == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_impersonate_user, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvImpersonateTitle);
        TextView tvUserInitials = dialogView.findViewById(R.id.tvUserInitials);
        TextView tvUserName = dialogView.findViewById(R.id.tvUserName);
        TextView tvUserCarnet = dialogView.findViewById(R.id.tvUserCarnet);
        TextView tvQuestion = dialogView.findViewById(R.id.tvImpersonateQuestion);
        TextView tvNotice = dialogView.findViewById(R.id.tvNoticeText);
        View btnCancel = dialogView.findViewById(R.id.btnCancel);
        View btnAcceder = dialogView.findViewById(R.id.btnAcceder);

        boolean isProfessor = "PROFESSOR".equals(admin.role);
        String roleTitle = isProfessor ? "profesor" : "encargado del estudiante";
        String roleNoticeName = isProfessor ? "profesor" : "encargado";

        tvTitle.setText("Acceder como " + roleTitle);
        tvUserInitials.setText(getInitials(admin.name));
        tvUserName.setText(admin.name != null ? admin.name : "-");
        tvUserCarnet.setText(admin.carnet != null ? admin.carnet : "-");
        tvQuestion.setText("¿Deseas ingresar a la aplicación utilizando el perfil y menú de " + admin.name + "?");
        tvNotice.setText("Opción exclusiva del Administrador. No se modificará el registro del " + roleNoticeName + ".");

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnAcceder.setOnClickListener(v -> {
            dialog.dismiss();
            impersonateUser();
        });

        dialog.show();
    }

    private void impersonateUser() {
        if (admin != null) {
            viewModel.startImpersonation(admin);
            if ("PROFESSOR".equals(admin.role)) {
                Navigation.findNavController(requireView()).navigate(R.id.action_adminDetailFragment_to_professorHomeFragment);
            } else {
                Navigation.findNavController(requireView()).navigate(R.id.action_adminDetailFragment_to_studentHomeFragment);
            }
        }
    }

    private void observeViewModel() {
        viewModel.getSelectedUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                admin = user;
                
                String title = "Detalle";
                String roleHeaderTitle = "Usuario del sistema";
                String roleName = "encargado";
                if ("PROFESSOR".equals(admin.role)) {
                    title = "Detalle del profesor";
                    roleHeaderTitle = "Equipo docente";
                    roleName = "profesor";
                } else if ("ADMIN".equals(admin.role)) {
                    title = "Detalle del administrador";
                    roleHeaderTitle = "Administrador principal";
                    roleName = "administrador";
                } else if ("ENCARGADO".equals(admin.role)) {
                    title = "Detalle del encargado";
                    roleHeaderTitle = "Encargado del estudiante";
                    roleName = "encargado";
                } else if ("STUDENT".equals(admin.role)) {
                    title = "Detalle del alumno";
                    roleHeaderTitle = "Estudiante de primaria";
                    roleName = "estudiante";
                }
                binding.tvDetailTitle.setText(title);
                binding.tvDetailRoleHeader.setText(roleHeaderTitle);
                binding.tvDetailCarnetHeader.setText(admin.carnet != null ? admin.carnet : "-");
                binding.tvDetailInitials.setText(getInitials(admin.name));
                binding.tvImpersonateDescription.setText("Ingresa con el perfil y menú del " + roleName + ". Esta acción no edita su registro.");
                binding.btnImpersonateAction.setText("Acceder como " + roleName);

                binding.etField1.setText(admin.name);
                binding.etField2.setText(admin.carnet);
                binding.etField3.setText(admin.password);
                
                if ("PROFESSOR".equals(admin.role) || "ADMIN".equals(admin.role) || "ENCARGADO".equals(admin.role)) {
                    binding.layoutAddress.setVisibility(View.VISIBLE);
                    binding.layoutPersonalEmail.setVisibility(View.VISIBLE);
                    binding.switchLockPhoto.setVisibility(View.VISIBLE);
                    binding.cardLockPhotoContainer.setVisibility(View.VISIBLE);

                    binding.tvRolePrefix.setVisibility(View.VISIBLE);
                    if ("PROFESSOR".equals(admin.role)) {
                        binding.tvRolePrefix.setText("PROF");
                        binding.etField2.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
                        if (admin.carnet != null && admin.carnet.startsWith("PROF")) {
                            binding.etField2.setText(admin.carnet.substring(4));
                        } else {
                            binding.etField2.setText(admin.carnet);
                        }
                    } else if ("ENCARGADO".equals(admin.role)) {
                        binding.tvRolePrefix.setText("ENC");
                        binding.etField2.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
                        if (admin.carnet != null && admin.carnet.startsWith("ENC")) {
                            binding.etField2.setText(admin.carnet.substring(3));
                        } else {
                            binding.etField2.setText(admin.carnet);
                        }
                    } else {
                        binding.tvRolePrefix.setText("ADMIN");
                        binding.etField2.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
                        if (admin.carnet != null && admin.carnet.startsWith("ADMIN")) {
                            binding.etField2.setText(admin.carnet.substring(5));
                        } else {
                            binding.etField2.setText(admin.carnet);
                        }
                    }
                    binding.etField2.setInputType(InputType.TYPE_CLASS_NUMBER);
                    
                    binding.etAddress.setText(admin.address != null ? admin.address : "");
                    binding.etPersonalEmail.setText(admin.personal_email != null ? admin.personal_email : "");

                    boolean locked = admin.can_change_photo != null && admin.can_change_photo == 0;
                    binding.switchLockPhoto.setChecked(locked);

                    binding.layoutCopyToggles.setVisibility(View.VISIBLE);
                    binding.layoutProfileHeaderContainer.setVisibility(View.VISIBLE);

                    if ("PROFESSOR".equals(admin.role) || "ENCARGADO".equals(admin.role)) {
                        binding.cardImpersonateContainer.setVisibility(View.VISIBLE);
                    } else {
                        binding.cardImpersonateContainer.setVisibility(View.GONE);
                    }
                } else {
                    binding.tvRolePrefix.setVisibility(View.GONE);
                    binding.layoutAddress.setVisibility(View.GONE);
                    binding.layoutPersonalEmail.setVisibility(View.GONE);
                    binding.switchLockPhoto.setVisibility(View.GONE);
                    binding.cardLockPhotoContainer.setVisibility(View.GONE);
                    binding.cardImpersonateContainer.setVisibility(View.GONE);
                }

                ImageUtils.setProfileImage(binding.ivProfileImageDetail, admin.profile_image, admin.name);

                if ("ADMIN12345".equals(admin.carnet)) {
                    binding.btnDelete.setVisibility(View.GONE);
                    binding.etField2.setEnabled(false);
                }

                DialogUtils.arrangeGridButtons(binding.layoutActionButtons);
                DialogUtils.hideLoadingOverlay(binding.getRoot());
            }
        });
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

    private void saveChanges() {
        if (admin != null) {
            String name = binding.etField1.getText().toString().trim();
            String rawCarnetInput = binding.etField2.getText().toString().trim();
            String pass = binding.etField3.getText().toString();

            String carnetDigits = rawCarnetInput;
            if (carnetDigits.startsWith("ADMIN")) carnetDigits = carnetDigits.substring(5);
            else if (carnetDigits.startsWith("PROF")) carnetDigits = carnetDigits.substring(4);
            else if (carnetDigits.startsWith("ENC")) carnetDigits = carnetDigits.substring(3);
            else if (carnetDigits.startsWith("EST")) carnetDigits = carnetDigits.substring(3);

            boolean isValid = true;
            if (name.isEmpty()) {
                binding.etField1.setError("El nombre es obligatorio");
                isValid = false;
            }

            if ("ADMIN".equals(admin.role)) {
                if (!carnetDigits.matches("\\d{5}")) {
                    binding.etField2.setError("Debe ingresar exactamente 5 dígitos numéricos");
                    isValid = false;
                }
            } else if ("PROFESSOR".equals(admin.role)) {
                if (!carnetDigits.matches("\\d{6}")) {
                    binding.etField2.setError("Debe ingresar exactamente 6 dígitos numéricos");
                    isValid = false;
                }
            } else if ("ENCARGADO".equals(admin.role)) {
                if (!carnetDigits.matches("\\d{7}")) {
                    binding.etField2.setError("Debe ingresar exactamente 7 dígitos numéricos");
                    isValid = false;
                }
            } else {
                if (carnetDigits.isEmpty()) {
                    binding.etField2.setError("El carnet es obligatorio");
                    isValid = false;
                }
            }

            if (ValidationUtils.getPasswordStrength(pass) == ValidationUtils.PasswordStrength.WEAK) {
                binding.tvPasswordStrength.setText("Contraseña muy débil");
                binding.tvPasswordStrength.setTextColor(0xFFFF4444);
                isValid = false;
            }

            if (isValid) {
                admin.name = name;
                
                if ("PROFESSOR".equals(admin.role)) {
                    admin.carnet = "PROF" + carnetDigits;
                } else if ("ADMIN".equals(admin.role)) {
                    admin.carnet = "ADMIN" + carnetDigits;
                } else if ("ENCARGADO".equals(admin.role)) {
                    admin.carnet = "ENC" + carnetDigits;
                } else {
                    admin.carnet = carnetDigits;
                }
                
                admin.password = pass;
                
                if ("PROFESSOR".equals(admin.role) || "ADMIN".equals(admin.role) || "ENCARGADO".equals(admin.role)) {
                    admin.address = binding.etAddress.getText().toString();
                    admin.personal_email = binding.etPersonalEmail.getText().toString();
                    
                    boolean isLocked = binding.switchLockPhoto.isChecked();
                    admin.can_change_photo = isLocked ? 0 : 1;
                    if (isLocked) admin.profile_image = null;
                }

                viewModel.performOnlineAction(() -> {
                    viewModel.updateUser(admin);
                    Toast.makeText(getContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).popBackStack();
                });
            }
        }
    }

    private void showDeleteConfirmation() {
        if (admin == null) return;
        String roleTitle = "PROFESSOR".equals(admin.role) ? "Profesor" : ("ENCARGADO".equals(admin.role) ? "Encargado" : "Administrador");
        String badge = admin.carnet != null ? admin.carnet : "";
        DialogUtils.showConfirmDeleteDialog(
            requireContext(),
            "Confirmar Eliminación",
            "¿Estás seguro de que deseas eliminar este perfil? Se perderán las credenciales de acceso institucional y los datos asociados de forma permanente.",
            admin.name,
            roleTitle + " • AulaV",
            badge,
            null,
            0,
            () -> {
                viewModel.performOnlineAction(() -> {
                    viewModel.deleteUser(admin);
                    Toast.makeText(getContext(), "Perfil eliminado", Toast.LENGTH_SHORT).show();
                    if (!Navigation.findNavController(requireView()).popBackStack(R.id.adminProfessorListFragment, false)) {
                        Navigation.findNavController(requireView()).popBackStack();
                    }
                });
            }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
