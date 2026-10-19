package com.aula.virtual.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.aula.virtual.MainActivity;
import com.aula.virtual.R;
import com.aula.virtual.databinding.FragmentSettingsBinding;

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

        binding.btnNotificationSettings.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_settingsFragment_to_notificationSettingsFragment));

        binding.btnGoToAdvancedSettings.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_settingsFragment_to_settingsAdvancedFragment));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
