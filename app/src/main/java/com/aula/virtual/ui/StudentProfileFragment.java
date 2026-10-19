package com.aula.virtual.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.aula.virtual.MainActivity;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentStudentProfileBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.IOException;

public class StudentProfileFragment extends Fragment {
    private FragmentStudentProfileBinding binding;
    private MainViewModel viewModel;
    private User currentUser;

    private final ActivityResultLauncher<Void> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    ImageUtils.showCropAndAdjustDialog(requireContext(), bitmap, this::updateProfilePicture);
                }
            }
    );

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    cameraLauncher.launch(null);
                } else {
                    Toast.makeText(getContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
                        ImageUtils.showCropAndAdjustDialog(requireContext(), bitmap, this::updateProfilePicture);
                    } catch (IOException e) {
                        Toast.makeText(getContext(), "Error al cargar imagen", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        binding.btnBackProfile.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToHome();
            } else {
                Navigation.findNavController(v).navigateUp();
            }
        });

        int studentId = -1;
        if (getArguments() != null) {
            studentId = getArguments().getInt("studentId", -1);
        }

        final int targetStudentId = studentId;

        if (targetStudentId == -1) {
            // My own profile
            viewModel.getCurrentUser().observe(getViewLifecycleOwner(), this::displayUser);
            binding.cardProfilePicture.setOnClickListener(v -> showImageSourceDialog());
            binding.btnChangeAvatar.setOnClickListener(v -> showImageSourceDialog());
            
            viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
                if (error != null) {
                    Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    viewModel.clearModificationError();
                }
            });
        } else {
            // Viewing another user's profile (student, professor, admin)
            viewModel.getUserById(targetStudentId, user -> {
                if (user != null) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> displayUser(user));
                    }
                } else {
                    viewModel.fetchStudentById(targetStudentId);
                }
            });

            viewModel.getSelectedStudentRecord().observe(getViewLifecycleOwner(), student -> {
                if (student != null) {
                    User u = new User();
                    u.id = student.id;
                    u.carnet = student.carnet;
                    u.name = student.name;
                    u.role = "STUDENT";
                    u.address = student.grade != null ? student.grade : "Estudiante de primaria";
                    u.profile_image = student.profile_image;
                    displayUser(u);
                }
            });

            binding.btnChangeAvatar.setVisibility(View.GONE);
            binding.cardProfilePicture.setClickable(false);
        }

        setupCopyButtons();
        setupMenuActions();
    }

    private void setupCopyButtons() {
        binding.btnCopyCarnet.setOnClickListener(v -> ClipboardUtils.copyToClipboard(getContext(), "Usuario", binding.tvProfileCarnet.getText().toString()));
        binding.btnCopyAddress.setOnClickListener(v -> ClipboardUtils.copyToClipboard(getContext(), "Dirección", binding.tvValAddress.getText().toString()));
        binding.btnCopyPersonalEmail.setOnClickListener(v -> ClipboardUtils.copyToClipboard(getContext(), "Correo Personal", binding.tvValPersonalEmail.getText().toString()));
        binding.btnCopyInstEmail.setOnClickListener(v -> ClipboardUtils.copyToClipboard(getContext(), "Correo Institucional", binding.tvValInstitutionalEmail.getText().toString()));
    }

    private void setupMenuActions() {
        binding.btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        binding.btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        boolean isBiometricEnabled = ThemeHelper.isBiometricAutofillEnabled(requireContext());
        binding.switchBiometricAutofill.setChecked(isBiometricEnabled);
        binding.switchBiometricAutofill.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ThemeHelper.setBiometricAutofillEnabled(requireContext(), isChecked);
            Toast.makeText(getContext(), isChecked ? "Datos biométricos activados" : "Datos biométricos desactivados", Toast.LENGTH_SHORT).show();
        });

        binding.btnLogout.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Cerrar sesión")
                    .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                    .setPositiveButton("Sí, Cerrar sesión", (dialog, which) -> {
                        viewModel.logout();
                        Navigation.findNavController(v).navigate(R.id.loginFragment);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    private void displayUser(User user) {
        if (user != null) {
            currentUser = user;
            binding.tvProfileName.setText(user.name);
            binding.tvProfileCarnet.setText(user.carnet);
            binding.tvValAddress.setText(user.address != null ? user.address : "No registrada");
            binding.tvValPersonalEmail.setText(user.personal_email != null ? user.personal_email : "No registrado");

            // Role label
            String roleText = "Estudiante de primaria";
            if ("ADMIN".equals(user.role)) {
                roleText = "Administrador principal";
            } else if ("PROFESSOR".equals(user.role)) {
                roleText = "Equipo docente";
            } else if ("ENCARGADO".equals(user.role)) {
                roleText = "Familia / Tutor";
            }
            binding.tvProfileRole.setText(roleText);

            // Institutional Email
            String emailSuffix = ("PROFESSOR".equals(user.role) || "ADMIN".equals(user.role)) ? "@mail.AulaV.edu.sv" : "@mail.AulaV.sv";
            binding.tvValInstitutionalEmail.setText(user.carnet + emailSuffix);

            // Avatar Initials vs Image
            String initials = getInitials(user.name);
            binding.tvAvatarInitials.setText(initials);

            if (user.profile_image != null && !user.profile_image.isEmpty()) {
                binding.tvAvatarInitials.setVisibility(View.GONE);
                binding.ivProfilePicture.setVisibility(View.VISIBLE);
                ImageUtils.setProfileImage(binding.ivProfilePicture, user.profile_image, 0);
            } else {
                binding.ivProfilePicture.setVisibility(View.GONE);
                binding.tvAvatarInitials.setVisibility(View.VISIBLE);
            }
        }
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

    private void showImageSourceDialog() {
        if (currentUser != null && currentUser.can_change_photo == 0) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Acceso Restringido")
                    .setMessage("Tu función de cambio de foto ha sido deshabilitada por subir contenido inapropiado.")
                    .setPositiveButton("Entendido", null)
                    .show();
            return;
        }
        String[] options = {"Cámara", "Galería", "Eliminar foto"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cambiar avatar de perfil")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) checkCameraPermission();
                    else if (which == 1) galleryLauncher.launch("image/*");
                    else removeProfilePicture();
                }).show();
    }

    private void showEditProfileDialog() {
        if (currentUser == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etAddress = dialogView.findViewById(R.id.etAddress);
        EditText etEmail = dialogView.findViewById(R.id.etEmail);
        View btnCancel = dialogView.findViewById(R.id.btnCancelDialog);
        View btnSave = dialogView.findViewById(R.id.btnSaveDialog);

        if (currentUser.name != null) etName.setText(currentUser.name);
        if (currentUser.address != null) etAddress.setText(currentUser.address);
        if (currentUser.personal_email != null) etEmail.setText(currentUser.personal_email);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            if (newName.isEmpty()) {
                etName.setError("El nombre es obligatorio");
                return;
            }

            currentUser.name = newName;
            currentUser.address = etAddress.getText().toString().trim();
            currentUser.personal_email = etEmail.getText().toString().trim();

            viewModel.performOnlineAction(() -> {
                viewModel.updateUser(currentUser);
                if (getContext() != null) {
                    CredentialsManager credentialsManager = new CredentialsManager(requireContext());
                    credentialsManager.saveCredentials(currentUser.carnet, currentUser.password, currentUser.name);
                }
                Toast.makeText(getContext(), "Perfil actualizado exitosamente", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void showChangePasswordDialog() {
        if (currentUser == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etCurrentPass = dialogView.findViewById(R.id.etCurrentPass);
        EditText etNewPass = dialogView.findViewById(R.id.etNewPass);
        EditText etConfirmPass = dialogView.findViewById(R.id.etConfirmPass);

        TextView tvStrength = dialogView.findViewById(R.id.tvPasswordStrength);
        ImageView btnToggleCurrent = dialogView.findViewById(R.id.btnToggleCurrentPass);
        ImageView btnToggleNew = dialogView.findViewById(R.id.btnToggleNewPass);
        ImageView btnToggleConfirm = dialogView.findViewById(R.id.btnToggleConfirmPass);

        View btnCancel = dialogView.findViewById(R.id.btnCancelDialog);
        View btnUpdate = dialogView.findViewById(R.id.btnUpdateDialog);

        btnToggleCurrent.setOnClickListener(v -> {
            if (etCurrentPass.getTransformationMethod() instanceof PasswordTransformationMethod) {
                etCurrentPass.setTransformationMethod(null);
                btnToggleCurrent.setImageResource(R.drawable.ic_visibility);
            } else {
                etCurrentPass.setTransformationMethod(PasswordTransformationMethod.getInstance());
                btnToggleCurrent.setImageResource(R.drawable.ic_visibility_off);
            }
            etCurrentPass.setSelection(etCurrentPass.getText().length());
        });

        btnToggleNew.setOnClickListener(v -> {
            if (etNewPass.getTransformationMethod() instanceof PasswordTransformationMethod) {
                etNewPass.setTransformationMethod(null);
                btnToggleNew.setImageResource(R.drawable.ic_visibility);
            } else {
                etNewPass.setTransformationMethod(PasswordTransformationMethod.getInstance());
                btnToggleNew.setImageResource(R.drawable.ic_visibility_off);
            }
            etNewPass.setSelection(etNewPass.getText().length());
        });

        btnToggleConfirm.setOnClickListener(v -> {
            if (etConfirmPass.getTransformationMethod() instanceof PasswordTransformationMethod) {
                etConfirmPass.setTransformationMethod(null);
                btnToggleConfirm.setImageResource(R.drawable.ic_visibility);
            } else {
                etConfirmPass.setTransformationMethod(PasswordTransformationMethod.getInstance());
                btnToggleConfirm.setImageResource(R.drawable.ic_visibility_off);
            }
            etConfirmPass.setSelection(etConfirmPass.getText().length());
        });

        etNewPass.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    tvStrength.setVisibility(View.GONE);
                } else {
                    tvStrength.setVisibility(View.VISIBLE);
                    ValidationUtils.PasswordStrength strength = ValidationUtils.getPasswordStrength(s.toString());
                    tvStrength.setText(strength.label);
                    tvStrength.setTextColor(strength.color);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnUpdate.setOnClickListener(v -> {
            String currentPass = etCurrentPass.getText().toString();
            String newPass = etNewPass.getText().toString();
            String confirmPass = etConfirmPass.getText().toString();

            boolean isValid = true;

            if (!currentPass.equals(currentUser.password)) {
                etCurrentPass.setError("Contraseña actual incorrecta");
                isValid = false;
            }

            if (newPass.isEmpty()) {
                etNewPass.setError("Ingrese la nueva contraseña");
                isValid = false;
            } else if (ValidationUtils.getPasswordStrength(newPass) == ValidationUtils.PasswordStrength.WEAK) {
                etNewPass.setError("Contraseña demasiado débil (mínimo 4 caracteres)");
                isValid = false;
            }

            if (!newPass.equals(confirmPass)) {
                etConfirmPass.setError("Las contraseñas no coinciden");
                isValid = false;
            }

            if (isValid) {
                viewModel.performOnlineAction(() -> {
                    currentUser.password = newPass;
                    viewModel.updateUser(currentUser);
                    if (getContext() != null) {
                        CredentialsManager credentialsManager = new CredentialsManager(requireContext());
                        credentialsManager.saveCredentials(currentUser.carnet, currentUser.password, currentUser.name);
                    }
                    Toast.makeText(getContext(), "Contraseña actualizada exitosamente", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }
        });

        dialog.show();
    }



    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null);
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void updateProfilePicture(Bitmap bitmap) {
        if (currentUser != null) {
            String base64 = ImageUtils.bitmapToBase64(bitmap);
            currentUser.profile_image = base64;
            
            binding.ivProfilePicture.setPadding(0, 0, 0, 0);
            binding.ivProfilePicture.setImageBitmap(bitmap);
            binding.ivProfilePicture.setImageTintList(null);
            binding.ivProfilePicture.setVisibility(View.VISIBLE);
            binding.tvAvatarInitials.setVisibility(View.GONE);
            
            viewModel.performOnlineAction(() -> {
                viewModel.updateUser(currentUser);
                Toast.makeText(getContext(), "Foto de perfil actualizada", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void removeProfilePicture() {
        if (currentUser != null) {
            viewModel.performOnlineAction(() -> {
                currentUser.profile_image = null;
                viewModel.updateUser(currentUser);
                binding.ivProfilePicture.setVisibility(View.GONE);
                binding.tvAvatarInitials.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), "Foto de perfil eliminada", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
