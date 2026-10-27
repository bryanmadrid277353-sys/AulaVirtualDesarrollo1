package com.aula.virtual.ui;

import android.text.method.PasswordTransformationMethod;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentLoginBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;
import java.util.concurrent.Executor;

public class LoginFragment extends Fragment {
    private FragmentLoginBinding binding;
    private MainViewModel viewModel;
    private CredentialsManager credentialsManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        viewModel.logout();
        credentialsManager = new CredentialsManager(requireContext());

        boolean autofillEnabled = ThemeHelper.isBiometricAutofillEnabled(requireContext());
        binding.btnSavedAccounts.setVisibility(autofillEnabled ? View.VISIBLE : View.GONE);
        binding.cbRemember.setVisibility(autofillEnabled ? View.VISIBLE : View.GONE);
        binding.layoutBiometricHint.setVisibility(autofillEnabled ? View.VISIBLE : View.GONE);
        if (!autofillEnabled) {
            binding.cbRemember.setChecked(false);
        }

        binding.btnTogglePassword.setOnClickListener(v -> {
            if (binding.etPassword.getTransformationMethod() instanceof PasswordTransformationMethod) {
                binding.etPassword.setTransformationMethod(null);
                binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility);
            } else {
                binding.etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
            }
            binding.etPassword.setSelection(binding.etPassword.getText().length());
        });

        binding.btnLogin.setOnClickListener(v -> {
            String carnet = binding.etCarnet.getText().toString();
            String password = binding.etPassword.getText().toString();

            if (carnet.isEmpty()) {
                Toast.makeText(getContext(), "El carné es obligatorio", Toast.LENGTH_SHORT).show();
                return;
            }

            performLogin(carnet, password, binding.cbRemember.isChecked());
        });

        binding.btnSavedAccounts.setOnClickListener(v -> showSavedAccountsDialog());
    }

    private void performLogin(String carnet, String password, boolean remember) {
        viewModel.logout();
        viewModel.login(carnet, password, new MainViewModel.LoginCallback() {
            @Override
            public void onSuccess(User user) {
                if (remember && ThemeHelper.isBiometricAutofillEnabled(requireContext())) {
                    credentialsManager.saveCredentials(carnet, password, user.name);
                }
                
                if (getActivity() == null || getView() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    if (getView() == null || Navigation.findNavController(getView()).getCurrentDestination() == null || 
                        Navigation.findNavController(getView()).getCurrentDestination().getId() != R.id.loginFragment) {
                        return;
                    }
                    if ("ADMIN".equals(user.role)) {
                        Navigation.findNavController(getView()).navigate(R.id.action_loginFragment_to_adminHomeFragment);
                    } else if ("PROFESSOR".equals(user.role)) {
                        Navigation.findNavController(getView()).navigate(R.id.action_loginFragment_to_professorHomeFragment);
                    } else {
                        Navigation.findNavController(getView()).navigate(R.id.action_loginFragment_to_studentHomeFragment);
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (getContext() != null) Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showSavedAccountsDialog() {
        if (!ThemeHelper.isBiometricAutofillEnabled(requireContext())) {
            Toast.makeText(getContext(), "El autocompletado de contraseñas está desactivado en Ajustes", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> accounts = credentialsManager.getRememberedAccounts();
        if (accounts.isEmpty()) {
            Toast.makeText(getContext(), "No hay cuentas guardadas", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_saved_accounts, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ListView listView = dialogView.findViewById(R.id.listSavedAccounts);
        View btnClose = dialogView.findViewById(R.id.btnCloseDialog);
        View btnCancel = dialogView.findViewById(R.id.btnCancelDialog);
        View btnAuthAndFill = dialogView.findViewById(R.id.btnAuthenticateAndFill);

        final String[] selectedCarnetRef = new String[]{accounts.get(0)};

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(), R.layout.item_saved_account, accounts) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = convertView;
                if (view == null) {
                    view = LayoutInflater.from(getContext()).inflate(R.layout.item_saved_account, parent, false);
                }

                String carnet = getItem(position);
                TextView tvName = view.findViewById(R.id.tvAccountName);
                TextView tvCarnet = view.findViewById(R.id.tvAccountCarnet);
                ImageView ivIndicator = view.findViewById(R.id.ivSelectionIndicator);
                View btnDelete = view.findViewById(R.id.btnDeleteAccount);

                String name = credentialsManager.getUserName(carnet);
                if (name != null && !name.equals(carnet)) {
                    tvName.setText(name);
                    tvCarnet.setText("Carné: " + carnet);
                    tvCarnet.setVisibility(View.VISIBLE);
                } else {
                    tvName.setText(carnet);
                    tvCarnet.setVisibility(View.GONE);
                }

                boolean isSelected = carnet.equals(selectedCarnetRef[0]);
                if (isSelected) {
                    ivIndicator.setImageResource(R.drawable.ic_radio_checked);
                } else {
                    ivIndicator.setImageResource(R.drawable.ic_radio_unchecked);
                }

                view.setOnClickListener(v -> {
                    selectedCarnetRef[0] = carnet;
                    notifyDataSetChanged();
                });

                btnDelete.setFocusable(false);
                btnDelete.setOnClickListener(v -> {
                    showDeleteAccountConfirmation(carnet, name, () -> {
                        credentialsManager.removeCredentials(carnet);
                        Toast.makeText(getContext(), "Cuenta eliminada", Toast.LENGTH_SHORT).show();
                        remove(carnet);
                        notifyDataSetChanged();
                        if (isEmpty()) {
                            dialog.dismiss();
                            Toast.makeText(getContext(), "No quedan cuentas guardadas", Toast.LENGTH_SHORT).show();
                        } else if (!accounts.contains(selectedCarnetRef[0])) {
                            selectedCarnetRef[0] = accounts.get(0);
                        }
                    });
                });

                return view;
            }
        };

        listView.setAdapter(adapter);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAuthAndFill.setOnClickListener(v -> {
            dialog.dismiss();
            String carnet = selectedCarnetRef[0];
            binding.etCarnet.setText(carnet);
            binding.etPassword.setText("");
            authenticateBiometrically(carnet);
        });

        dialog.show();
    }

    private void showDeleteAccountConfirmation(String carnet, String name, Runnable onDelete) {
        String displayName = (name != null && !name.equals(carnet)) ? name : carnet;
        DialogUtils.showConfirmDeleteDialog(
            requireContext(),
            "Confirmar Eliminación",
            "¿Estás seguro de que deseas eliminar las credenciales guardadas para esta cuenta en este dispositivo?",
            displayName,
            "Cuenta guardada para acceso rápido",
            carnet,
            null,
            0,
            onDelete::run
        );
    }

    private void authenticateBiometrically(String carnet) {
        String name = credentialsManager.getUserName(carnet);
        String displayName = (name != null && !name.equals(carnet)) ? name + " (" + carnet + ")" : carnet;

        Executor executor = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                String password = credentialsManager.getPassword(carnet);
                if (password != null) {
                    performLogin(carnet, password, false);
                }
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getContext(), "Autenticación fallida: " + errString, Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Inicio de sesión biométrico")
                .setSubtitle("Usa tu huella o rostro para entrar como " + displayName)
                .setNegativeButtonText("Cancelar")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
