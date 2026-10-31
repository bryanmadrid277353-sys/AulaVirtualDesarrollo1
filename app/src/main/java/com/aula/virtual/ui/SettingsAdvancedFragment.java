package com.aula.virtual.ui;

import android.app.ProgressDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.os.Bundle;
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
import android.content.Context;
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
import com.aula.virtual.data.RetrofitClient;
import com.aula.virtual.data.VirtualAulaRepository;
import com.aula.virtual.data.local.LocalDatabaseBackupManager;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.aula.virtual.data.entity.Notification;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentSettingsAdvancedBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsAdvancedFragment extends Fragment {
    private FragmentSettingsAdvancedBinding binding;
    private MainViewModel viewModel;

    private final ActivityResultLauncher<String[]> importJsonLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    Context ctx = getContext();
                    if (ctx == null) return;

                    new MaterialAlertDialogBuilder(ctx, R.style.Theme_MyApplication_Dialog)
                            .setTitle("⚠️ Confirmar Restauración Local")
                            .setMessage("¿Estás seguro de que deseas restaurar los datos locales desde este archivo de copia de seguridad (.json)?\n\nATENCIÓN: Se reemplazarán los registros locales actuales de la base de datos.")
                            .setNegativeButton("Cancelar", null)
                            .setPositiveButton("Restaurar Ahora", (dialog, which) -> {
                                ProgressDialog pd = new ProgressDialog(ctx);
                                pd.setMessage("Restaurando datos locales...");
                                pd.show();

                                try {
                                    InputStream is = ctx.getContentResolver().openInputStream(uri);
                                    LocalDatabaseBackupManager.importBackupFromJsonStream(ctx, is, new LocalDatabaseBackupManager.BackupCallback() {
                                        @Override
                                        public void onSuccess(String message, String filePath) {
                                            if (getActivity() != null) {
                                                getActivity().runOnUiThread(() -> {
                                                    pd.dismiss();
                                                    new MaterialAlertDialogBuilder(ctx, R.style.Theme_MyApplication_Dialog)
                                                            .setTitle("¡Restauración Completada!")
                                                            .setMessage(message)
                                                            .setPositiveButton("Entendido", (d, w) -> {
                                                                if (getActivity() instanceof MainActivity) {
                                                                    ((MainActivity) getActivity()).navigateToHome();
                                                                }
                                                            })
                                                            .show();
                                                });
                                            }
                                        }

                                        @Override
                                        public void onError(String error) {
                                            if (getActivity() != null) {
                                                getActivity().runOnUiThread(() -> {
                                                    pd.dismiss();
                                                    new MaterialAlertDialogBuilder(ctx, R.style.Theme_MyApplication_Dialog)
                                                            .setTitle("⚠️ Error al Restaurar")
                                                            .setMessage(error)
                                                            .setPositiveButton("Entendido", null)
                                                            .show();
                                                });
                                            }
                                        }
                                    });
                                } catch (Exception e) {
                                    pd.dismiss();
                                    Toast.makeText(ctx, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            })
                            .show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsAdvancedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        binding.btnBackAdvancedSettings.setOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp());

        setupDataModeSelector();
        setupBiometricAutofillSwitch();
        binding.btnAcademicCalendar.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_settingsAdvancedFragment_to_academicCalendarFragment));
        binding.btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        binding.btnDownloadOfflineData.setOnClickListener(v -> downloadOfflineData());
        binding.btnUploadOfflineData.setOnClickListener(v -> confirmUploadOfflineData());
        binding.btnClearOfflineData.setOnClickListener(v -> confirmClearOfflineData());
        binding.btnConfigureServerUrl.setOnClickListener(v -> showServerUrlDialog());

        if (binding.btnExportJsonLocal != null) {
            binding.btnExportJsonLocal.setOnClickListener(v -> {
                Context ctx = getContext();
                if (ctx == null) return;
                ProgressDialog pd = new ProgressDialog(ctx);
                pd.setMessage("Exportando copia de seguridad local en JSON...");
                pd.show();

                LocalDatabaseBackupManager.generateAndAutoSaveBackupJson(ctx, new LocalDatabaseBackupManager.BackupCallback() {
                    @Override
                    public void onSuccess(String message, String filePath) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                pd.dismiss();
                                new MaterialAlertDialogBuilder(ctx, R.style.Theme_MyApplication_Dialog)
                                        .setTitle("¡Exportación Exitosa!")
                                        .setMessage(message + "\n\nUbicación:\n" + filePath)
                                        .setPositiveButton("Entendido", null)
                                        .show();
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                pd.dismiss();
                                new MaterialAlertDialogBuilder(ctx, R.style.Theme_MyApplication_Dialog)
                                        .setTitle("⚠️ Error en Exportación")
                                        .setMessage(error)
                                        .setPositiveButton("Entendido", null)
                                        .show();
                            });
                        }
                    }
                });
            });
        }

        if (binding.btnImportJsonLocal != null) {
            binding.btnImportJsonLocal.setOnClickListener(v -> {
                importJsonLauncher.launch(new String[]{"application/json", "text/plain", "*/*"});
            });
        }

        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            updateVisibilityForMode();
        });

        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });
    }

    private void updateVisibilityForMode() {
        if (binding == null || getContext() == null) return;
        String currentMode = ThemeHelper.getDataMode(requireContext());
        boolean isLocal = ThemeHelper.MODE_LOCAL.equals(currentMode);

        // 0. Organización: Visible SIEMPRE
        if (binding.layoutOrganizationCategory != null) {
            binding.layoutOrganizationCategory.setVisibility(View.VISIBLE);
        }

        // 1. Servidor y Conexión: Visible SOLO en Modo Servidor
        int serverConfigVisibility = isLocal ? View.GONE : View.VISIBLE;
        if (binding.layoutServerConfigCategory != null) {
            binding.layoutServerConfigCategory.setVisibility(serverConfigVisibility);
        } else if (binding.btnConfigureServerUrl != null) {
            binding.btnConfigureServerUrl.setVisibility(serverConfigVisibility);
        }

        // 2. Transferencia de Datos: Visible SOLO en Modo Servidor
        if (binding.layoutTransferCategory != null) {
            binding.layoutTransferCategory.setVisibility(isLocal ? View.GONE : View.VISIBLE);
        }

        // 3. Datos Descargados: Visible SOLO en Modo Local
        if (binding.layoutClearCategory != null) {
            binding.layoutClearCategory.setVisibility(isLocal ? View.VISIBLE : View.GONE);
        }

        // 4. Copia de Seguridad Local (JSON): Visible SOLO en Modo Local
        if (binding.layoutLocalBackupCategory != null) {
            binding.layoutLocalBackupCategory.setVisibility(isLocal ? View.VISIBLE : View.GONE);
        }
    }

    private void setupDataModeSelector() {
        String currentMode = ThemeHelper.getDataMode(requireContext());
        boolean isLocal = ThemeHelper.MODE_LOCAL.equals(currentMode);

        binding.ivRadioLocal.setImageResource(isLocal ? R.drawable.ic_radio_checked : R.drawable.ic_radio_unchecked);
        binding.ivRadioServer.setImageResource(isLocal ? R.drawable.ic_radio_unchecked : R.drawable.ic_radio_checked);

        updateVisibilityForMode();

        if (isLocal) {
            binding.tvDataModeTitle.setText("Estás trabajando en modo local");
            binding.tvDataModeDescription.setText("Usa los datos de este dispositivo sin depender del servidor.");
        } else {
            binding.tvDataModeTitle.setText("Estás trabajando en modo servidor");
            binding.tvDataModeDescription.setText("Conexión obligatoria con el servidor para la consulta y registro de datos.");
        }

        binding.btnModeLocalBox.setOnClickListener(v -> {
            if (!ThemeHelper.MODE_LOCAL.equals(ThemeHelper.getDataMode(requireContext()))) {
                ThemeHelper.setDataMode(requireContext(), ThemeHelper.MODE_LOCAL);
                Toast.makeText(getContext(), "Cambiado a Modo Local (Sin servidor)", Toast.LENGTH_SHORT).show();
                requireActivity().recreate();
            }
        });

        binding.btnModeServerBox.setOnClickListener(v -> {
            if (!ThemeHelper.MODE_SERVER.equals(ThemeHelper.getDataMode(requireContext()))) {
                ThemeHelper.setDataMode(requireContext(), ThemeHelper.MODE_SERVER);
                Toast.makeText(getContext(), "Cambiado a Modo Servidor. Verificando conexión...", Toast.LENGTH_SHORT).show();
                RetrofitClient.resetClient();
                viewModel.refreshData();
                requireActivity().recreate();
            }
        });
    }

    private boolean isUpdatingSwitch = false;

    private void setupBiometricAutofillSwitch() {
        boolean isEnabled = ThemeHelper.isBiometricAutofillEnabled(requireContext());
        binding.switchBiometricAutofill.setChecked(isEnabled);

        binding.switchBiometricAutofill.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingSwitch) return;

            if (!isChecked) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("⚠️ Desactivar Datos Biométricos")
                        .setMessage("Al desactivar los Datos Biométricos, se ELIMINARÁN TODAS las cuentas guardadas para inicio de sesión biométrico en este dispositivo.\n\n¿Deseas continuar?")
                        .setPositiveButton("Sí, Desactivar y Eliminar", (dialog, which) -> {
                            CredentialsManager manager = new CredentialsManager(requireContext());
                            manager.clearAllCredentials();
                            ThemeHelper.setBiometricAutofillEnabled(requireContext(), false);
                            Toast.makeText(getContext(), "Datos biométricos desactivados y cuentas guardadas eliminadas", Toast.LENGTH_LONG).show();
                        })
                        .setNegativeButton("Cancelar", (dialog, which) -> {
                            isUpdatingSwitch = true;
                            binding.switchBiometricAutofill.setChecked(true);
                            isUpdatingSwitch = false;
                        })
                        .setCancelable(false)
                        .show();
            } else {
                ThemeHelper.setBiometricAutofillEnabled(requireContext(), true);
                Toast.makeText(getContext(), "Inicio de sesión biométrico activado", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showServerUrlDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_configure_server, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View cardCloudflare = dialogView.findViewById(R.id.cardOptionCloudflare);
        View cardEmulator = dialogView.findViewById(R.id.cardOptionEmulator);
        View cardLocalNet = dialogView.findViewById(R.id.cardOptionLocalNet);

        ImageView ivRadioCloudflare = dialogView.findViewById(R.id.ivRadioCloudflare);
        ImageView ivRadioEmulator = dialogView.findViewById(R.id.ivRadioEmulator);
        ImageView ivRadioLocalNet = dialogView.findViewById(R.id.ivRadioLocalNet);

        TextView tvSslBadge = dialogView.findViewById(R.id.tvSslBadge);
        EditText etServerUrl = dialogView.findViewById(R.id.etServerUrl);

        ImageView btnToggleMask = dialogView.findViewById(R.id.btnToggleMask);

        View btnCancel = dialogView.findViewById(R.id.btnCancelDialog);
        View btnSave = dialogView.findViewById(R.id.btnSaveServerUrl);

        String currentUrl = RetrofitClient.getActiveUrl(requireContext());
        etServerUrl.setText(currentUrl);

        int colorPrimary = ContextCompat.getColor(requireContext(), R.color.bs_primary);
        int colorBorder = ContextCompat.getColor(requireContext(), R.color.input_border);

        Runnable updateSelectionUI = () -> {
            String url = etServerUrl.getText().toString().trim();

            boolean isCloudflare = url.equals(RetrofitClient.BASE_URL) || url.contains("trycloudflare.com") || url.contains("https://");
            boolean isEmulator = url.equals(RetrofitClient.EMULATOR_URL) || url.contains("10.0.2.2");
            boolean isLocalNet = url.contains("192.168.") || url.contains("localhost");

            if (!isCloudflare && !isEmulator && !isLocalNet) {
                isCloudflare = true;
            }

            if (cardCloudflare instanceof com.google.android.material.card.MaterialCardView) {
                ((com.google.android.material.card.MaterialCardView) cardCloudflare).setStrokeColor(isCloudflare ? colorPrimary : colorBorder);
            }
            if (cardEmulator instanceof com.google.android.material.card.MaterialCardView) {
                ((com.google.android.material.card.MaterialCardView) cardEmulator).setStrokeColor(isEmulator ? colorPrimary : colorBorder);
            }
            if (cardLocalNet instanceof com.google.android.material.card.MaterialCardView) {
                ((com.google.android.material.card.MaterialCardView) cardLocalNet).setStrokeColor(isLocalNet ? colorPrimary : colorBorder);
            }

            ImageView ivIconCloudflare = dialogView.findViewById(R.id.ivIconCloudflare);
            ImageView ivIconEmulator = dialogView.findViewById(R.id.ivIconEmulator);
            ImageView ivIconLocalNet = dialogView.findViewById(R.id.ivIconLocalNet);

            TypedValue typedValue = new TypedValue();
            requireContext().getTheme().resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
            int colorSecondary = typedValue.data;

            if (ivIconCloudflare != null) ivIconCloudflare.setImageTintList(ColorStateList.valueOf(isCloudflare ? colorPrimary : colorSecondary));
            if (ivIconEmulator != null) ivIconEmulator.setImageTintList(ColorStateList.valueOf(isEmulator ? colorPrimary : colorSecondary));
            if (ivIconLocalNet != null) ivIconLocalNet.setImageTintList(ColorStateList.valueOf(isLocalNet ? colorPrimary : colorSecondary));

            if (ivRadioCloudflare != null) {
                ivRadioCloudflare.setImageResource(isCloudflare ? R.drawable.ic_radio_checked : R.drawable.ic_radio_unchecked);
            }
            if (ivRadioEmulator != null) {
                ivRadioEmulator.setImageResource(isEmulator ? R.drawable.ic_radio_checked : R.drawable.ic_radio_unchecked);
            }
            if (ivRadioLocalNet != null) {
                ivRadioLocalNet.setImageResource(isLocalNet ? R.drawable.ic_radio_checked : R.drawable.ic_radio_unchecked);
            }

            if (tvSslBadge != null) {
                if (url.startsWith("https://")) {
                    tvSslBadge.setText("🟢 SSL ACTIVO");
                    tvSslBadge.setTextColor(Color.parseColor("#10B981"));
                    tvSslBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#064E3B")));
                } else {
                    tvSslBadge.setText("🟡 HTTP LOCAL");
                    tvSslBadge.setTextColor(Color.parseColor("#F59E0B"));
                    tvSslBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#451A03")));
                }
            }
        };

        updateSelectionUI.run();

        cardCloudflare.setOnClickListener(v -> {
            etServerUrl.setText(RetrofitClient.BASE_URL);
            updateSelectionUI.run();
            Toast.makeText(getContext(), "Seleccionado: Servidor Nube (Cloudflare)", Toast.LENGTH_SHORT).show();
        });

        cardEmulator.setOnClickListener(v -> {
            etServerUrl.setText(RetrofitClient.EMULATOR_URL);
            updateSelectionUI.run();
            Toast.makeText(getContext(), "Seleccionado: Emulador Android Studio", Toast.LENGTH_SHORT).show();
        });

        cardLocalNet.setOnClickListener(v -> {
            if (!etServerUrl.getText().toString().contains("192.168.")) {
                etServerUrl.setText("http://192.168.1.100:8000/");
            }
            updateSelectionUI.run();
            Toast.makeText(getContext(), "Seleccionado: Red Local Wi-Fi", Toast.LENGTH_SHORT).show();
        });

        ImageView btnCopyServerUrl = dialogView.findViewById(R.id.btnCopyServerUrl);
        if (btnCopyServerUrl != null) {
            btnCopyServerUrl.setOnClickListener(v -> {
                String urlToCopy = etServerUrl.getText().toString().trim();
                if (!urlToCopy.isEmpty()) {
                    ClipboardUtils.copyToClipboard(requireContext(), "Dirección del Servidor", urlToCopy);
                    Toast.makeText(getContext(), "Dirección copiada al portapapeles", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "No hay dirección para copiar", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View.OnClickListener toggleMaskAction = v -> {
            if (etServerUrl.getTransformationMethod() instanceof PasswordTransformationMethod) {
                etServerUrl.setTransformationMethod(null);
                if (btnToggleMask != null) btnToggleMask.setImageResource(R.drawable.ic_visibility);
            } else {
                etServerUrl.setTransformationMethod(PasswordTransformationMethod.getInstance());
                if (btnToggleMask != null) btnToggleMask.setImageResource(R.drawable.ic_visibility_off);
            }
            if (etServerUrl.getText() != null) {
                etServerUrl.setSelection(etServerUrl.getText().length());
            }
        };

        if (btnToggleMask != null) btnToggleMask.setOnClickListener(toggleMaskAction);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newUrl = etServerUrl.getText().toString().trim();
            if (!newUrl.isEmpty()) {
                RetrofitClient.setCustomUrl(requireContext(), newUrl);
                Toast.makeText(getContext(), "Configuración guardada. Probando conexión con el servidor...", Toast.LENGTH_SHORT).show();
                viewModel.refreshData();
                dialog.dismiss();
            } else {
                etServerUrl.setError("Ingrese una dirección válida");
            }
        });

        dialog.show();
    }

    private void showChangePasswordDialog() {
        User currentUser = viewModel.getCurrentUser().getValue();
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

    private void downloadOfflineData() {
        User currentUser = viewModel.getCurrentUser().getValue();
        if (ThemeHelper.isLocalMode(requireContext()) && currentUser != null && !"ADMIN".equals(currentUser.role)) {
            Toast.makeText(getContext(), "Acción no permitida: Los Profesores y Estudiantes no pueden descargar datos del servidor en Modo Local.", Toast.LENGTH_LONG).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_sync, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvServerHost = dialogView.findViewById(R.id.tvServerHostName);
        View btnCancel = dialogView.findViewById(R.id.btnCancelSync);
        View btnConfirm = dialogView.findViewById(R.id.btnConfirmSync);

        String activeUrl = RetrofitClient.getActiveUrl(requireContext());
        String hostLabel = activeUrl;
        try {
            java.net.URI uri = new java.net.URI(activeUrl);
            String host = uri.getHost();
            if (host != null && !host.isEmpty()) {
                hostLabel = host + " (Activo)";
            }
        } catch (Exception ignored) {}

        if (tvServerHost != null) tvServerHost.setText(hostLabel);

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();

                ProgressDialog progressDialog = new ProgressDialog(getContext());
                progressDialog.setMessage("Sincronizando y descargando datos del servidor...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                viewModel.downloadAllDataForOffline(new VirtualAulaRepository.SyncCallback() {
                    @Override
                    public void onSuccess(String message) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog)
                                        .setTitle("¡Descarga Exitosa!")
                                        .setMessage(message)
                                        .setPositiveButton("Entendido", null)
                                        .show();
                                viewModel.refreshData();
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog)
                                        .setTitle("⚠️ Error en Descarga")
                                        .setMessage(error)
                                        .setPositiveButton("Entendido", null)
                                        .show();
                            });
                        }
                    }
                });
            });
        }

        dialog.show();
    }

    private void confirmClearOfflineData() {
        User currentUser = viewModel.getCurrentUser().getValue();
        if (ThemeHelper.isLocalMode(requireContext()) && currentUser != null && !"ADMIN".equals(currentUser.role)) {
            Toast.makeText(getContext(), "Acción no permitida: Los Profesores y Estudiantes no pueden eliminar datos de la aplicación en Modo Local.", Toast.LENGTH_LONG).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_clear_offline, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View btnCancel = dialogView.findViewById(R.id.btnCancelClear);
        View btnConfirm = dialogView.findViewById(R.id.btnConfirmClear);

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                viewModel.clearAllOfflineData(new VirtualAulaRepository.SyncCallback() {
                    @Override
                    public void onSuccess(String message) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                                viewModel.refreshData();
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                            });
                        }
                    }
                });
            });
        }

        dialog.show();
    }

    private void confirmUploadOfflineData() {
        User currentUser = viewModel.getCurrentUser().getValue();
        if (ThemeHelper.isLocalMode(requireContext()) && currentUser != null && !"ADMIN".equals(currentUser.role)) {
            Toast.makeText(getContext(), "Acción no permitida: Los Profesores y Estudiantes no pueden subir datos al servidor en Modo Local.", Toast.LENGTH_LONG).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_upload_offline, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View btnCancel = dialogView.findViewById(R.id.btnCancelUpload);
        View btnConfirm = dialogView.findViewById(R.id.btnConfirmUpload);

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                uploadOfflineData();
            });
        }

        dialog.show();
    }

    private void uploadOfflineData() {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Subiendo registros locales al servidor...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        viewModel.uploadAllOfflineDataToOnline(new VirtualAulaRepository.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                        viewModel.refreshData();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Error al subir registros: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void showContactAdminDialog(User user) {
        if (user == null || getContext() == null) return;

        MaterialAlertDialogBuilder builder = DialogUtils.createMaterialDialog(requireContext(), "Contactar con Administrador");
        LinearLayout layout = DialogUtils.createDialogContainer(requireContext());

        final EditText etTitle = DialogUtils.createStyledEditText(requireContext(), "Asunto / Título del Problema", 0);
        layout.addView(etTitle);

        final EditText etMessage = DialogUtils.createStyledEditText(requireContext(), "Detalles del problema o consulta", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        layout.addView(etMessage);

        builder.setView(layout);
        builder.setPositiveButton("Enviar Reporte", null);
        builder.setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String message = etMessage.getText().toString().trim();

            boolean isValid = true;
            if (title.isEmpty()) {
                etTitle.setError("El asunto es obligatorio");
                isValid = false;
            }
            if (message.isEmpty()) {
                etMessage.setError("El mensaje es obligatorio");
                isValid = false;
            }

            if (isValid) {
                String senderInfo = user.name + " (" + user.carnet + ")";
                Notification notif = new Notification(title, message, "SUPPORT", null, senderInfo, "");

                viewModel.performOnlineAction(() -> {
                    viewModel.insertNotification(notif, () -> {
                        Toast.makeText(getContext(), "¡Reporte enviado exitosamente a los Administradores!", Toast.LENGTH_SHORT).show();
                        Toast.makeText(getContext(), "¡Reporte enviado exitosamente a los Administradores!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
