package com.aula.virtual.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.aula.virtual.MainActivity;
import com.aula.virtual.R;
import com.aula.virtual.databinding.FragmentSettingsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBackSettings.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToHome();
            } else {
                Navigation.findNavController(v).navigateUp();
            }
        });

        boolean isDark = ThemeHelper.isDarkMode(requireContext());
        binding.switchDarkMode.setChecked(isDark);
        binding.tvDarkModeStatus.setText(isDark ? "Activado" : "Desactivado");

        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ThemeHelper.setDarkMode(requireContext(), isChecked);
            binding.tvDarkModeStatus.setText(isChecked ? "Activado" : "Desactivado");
            requireActivity().recreate();
        });

        boolean isHaptic = ThemeHelper.isHapticFeedbackEnabled(requireContext());
        if (binding.switchHapticFeedback != null) {
            binding.switchHapticFeedback.setChecked(isHaptic);
            binding.switchHapticFeedback.setOnCheckedChangeListener((buttonView, isChecked) -> {
                ThemeHelper.setHapticFeedbackEnabled(requireContext(), isChecked);
            });
        }

        setupBiometricAutofillSwitch();

        MainViewModel viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (binding == null) return;
            boolean isAdmin = user != null && "ADMIN".equalsIgnoreCase(user.role);
            if (binding.layoutAdministrationCategory != null) {
                binding.layoutAdministrationCategory.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            }
        });

        binding.btnNotificationSettings.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_settingsFragment_to_notificationSettingsFragment));

        if (binding.btnGoToAdvancedSettings != null) {
            binding.btnGoToAdvancedSettings.setOnClickListener(v -> 
                Navigation.findNavController(v).navigate(R.id.action_settingsFragment_to_settingsAdvancedFragment));
        }
    }

    private boolean isUpdatingSwitch = false;

    private void setupBiometricAutofillSwitch() {
        if (binding.switchBiometricAutofill == null) return;
        boolean isEnabled = ThemeHelper.isBiometricAutofillEnabled(requireContext());
        binding.switchBiometricAutofill.setChecked(isEnabled);

        binding.switchBiometricAutofill.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingSwitch) return;

            if (!isChecked) {
                new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog)
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
