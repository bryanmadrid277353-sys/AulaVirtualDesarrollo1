package com.aula.virtual.ui;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.aula.virtual.R;
import com.aula.virtual.databinding.FragmentNotificationSettingsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class NotificationSettingsFragment extends Fragment {
    private FragmentNotificationSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBackNotificationSettings.setOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp());

        loadSettings();
        setupListeners();
    }

    private void loadSettings() {
        if (getContext() == null || binding == null) return;

        // 1. General Options
        boolean isPush = NotificationPrefs.isPushEnabled(requireContext());
        binding.switchPushNotifications.setChecked(isPush);
        binding.tvPushStatus.setText(isPush ? "Recibir avisos en tiempo real (Activadas)" : "Notificaciones deshabilitadas globalmente");

        binding.switchSound.setChecked(NotificationPrefs.isSoundEnabled(requireContext()));
        binding.switchVibration.setChecked(NotificationPrefs.isVibrationEnabled(requireContext()));

        // 2. Channels
        binding.switchChannelGeneral.setChecked(NotificationPrefs.isChannelGeneralEnabled(requireContext()));
        binding.switchChannelSubjects.setChecked(NotificationPrefs.isChannelSubjectsEnabled(requireContext()));
        binding.switchChannelFacilities.setChecked(NotificationPrefs.isChannelFacilitiesEnabled(requireContext()));
        binding.switchChannelInstitutional.setChecked(NotificationPrefs.isChannelInstitutionalEnabled(requireContext()));

        // 3. Priority & Quiet Hours
        binding.switchPriorityAlerts.setChecked(NotificationPrefs.isPriorityAlertsEnabled(requireContext()));

        boolean isQuiet = NotificationPrefs.isQuietHoursEnabled(requireContext());
        binding.switchQuietHours.setChecked(isQuiet);
        binding.layoutQuietHoursDetails.setVisibility(isQuiet ? View.VISIBLE : View.GONE);

        binding.tvQuietHoursStartValue.setText(NotificationPrefs.getQuietHoursStart(requireContext()));
        binding.tvQuietHoursEndValue.setText(NotificationPrefs.getQuietHoursEnd(requireContext()));

        // 4. Frequency
        updateFrequencyDisplay();
    }

    private void updateFrequencyDisplay() {
        String freq = NotificationPrefs.getFrequency(requireContext());
        if (NotificationPrefs.FREQUENCY_DAILY.equals(freq)) {
            binding.tvFrequencyValue.setText("Resumen Diario Consolidado");
        } else {
            binding.tvFrequencyValue.setText("Instantánea (Al momento)");
        }
    }

    private void setupListeners() {
        if (binding == null) return;

        // General Switches
        binding.switchPushNotifications.setOnCheckedChangeListener((btn, isChecked) -> {
            NotificationPrefs.setPushEnabled(requireContext(), isChecked);
            binding.tvPushStatus.setText(isChecked ? "Recibir avisos en tiempo real (Activadas)" : "Notificaciones deshabilitadas globalmente");
            Toast.makeText(getContext(), isChecked ? "Notificaciones Push activadas" : "Notificaciones Push desactivadas", Toast.LENGTH_SHORT).show();
        });

        binding.switchSound.setOnCheckedChangeListener((btn, isChecked) -> {
            NotificationPrefs.setSoundEnabled(requireContext(), isChecked);
        });

        binding.switchVibration.setOnCheckedChangeListener((btn, isChecked) -> {
            NotificationPrefs.setVibrationEnabled(requireContext(), isChecked);
        });

        // Channels Switches
        binding.switchChannelGeneral.setOnCheckedChangeListener((btn, isChecked) -> {
            NotificationPrefs.setChannelGeneralEnabled(requireContext(), isChecked);
        });

        binding.switchChannelSubjects.setOnCheckedChangeListener((btn, isChecked) -> {
            NotificationPrefs.setChannelSubjectsEnabled(requireContext(), isChecked);
        });

        binding.switchChannelFacilities.setOnCheckedChangeListener((btn, isChecked) -> {
            NotificationPrefs.setChannelFacilitiesEnabled(requireContext(), isChecked);
        });

        binding.switchChannelInstitutional.setOnCheckedChangeListener((btn, isChecked) -> {
            NotificationPrefs.setChannelInstitutionalEnabled(requireContext(), isChecked);
        });

        // Priority & Quiet Hours
        binding.switchPriorityAlerts.setOnCheckedChangeListener((btn, isChecked) -> {
            NotificationPrefs.setPriorityAlertsEnabled(requireContext(), isChecked);
            Toast.makeText(getContext(), isChecked ? "Avisos de Alerta y Sistema priorizados" : "Priorización de alertas desactivada", Toast.LENGTH_SHORT).show();
        });

        binding.switchQuietHours.setOnCheckedChangeListener((btn, isChecked) -> {
            NotificationPrefs.setQuietHoursEnabled(requireContext(), isChecked);
            binding.layoutQuietHoursDetails.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            Toast.makeText(getContext(), isChecked ? "Horario Silencioso activado" : "Horario Silencioso desactivado", Toast.LENGTH_SHORT).show();
        });

        binding.btnQuietHoursStart.setOnClickListener(v -> showTimePicker(true));
        binding.btnQuietHoursEnd.setOnClickListener(v -> showTimePicker(false));

        // Frequency selector
        binding.btnNotificationFrequency.setOnClickListener(v -> showFrequencyDialog());
    }

    private void showTimePicker(boolean isStart) {
        String currentTime = isStart ? 
            NotificationPrefs.getQuietHoursStart(requireContext()) : 
            NotificationPrefs.getQuietHoursEnd(requireContext());

        int hour = 21;
        int minute = 0;

        try {
            String[] parts = currentTime.split(":");
            if (parts.length == 2) {
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
            }
        } catch (Exception ignored) {}

        TimePickerDialog timePicker = new TimePickerDialog(requireContext(), (view, hourOfDay, minuteOfHour) -> {
            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour);
            if (isStart) {
                NotificationPrefs.setQuietHoursStart(requireContext(), formattedTime);
                binding.tvQuietHoursStartValue.setText(formattedTime);
            } else {
                NotificationPrefs.setQuietHoursEnd(requireContext(), formattedTime);
                binding.tvQuietHoursEndValue.setText(formattedTime);
            }
        }, hour, minute, true);

        timePicker.setTitle(isStart ? "Seleccionar Hora de Inicio" : "Seleccionar Hora de Fin");
        timePicker.show();
    }

    private void showFrequencyDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_notification_frequency, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View cardInstant = dialogView.findViewById(R.id.cardOptionInstant);
        View cardDaily = dialogView.findViewById(R.id.cardOptionDaily);
        ImageView ivRadioInstant = dialogView.findViewById(R.id.ivRadioInstant);
        ImageView ivRadioDaily = dialogView.findViewById(R.id.ivRadioDaily);
        View btnCancel = dialogView.findViewById(R.id.btnCancelFrequency);

        String currentFreq = NotificationPrefs.getFrequency(requireContext());
        boolean isDaily = NotificationPrefs.FREQUENCY_DAILY.equals(currentFreq);

        updateRadioState(isDaily, ivRadioInstant, ivRadioDaily);

        cardInstant.setOnClickListener(v -> {
            NotificationPrefs.setFrequency(requireContext(), NotificationPrefs.FREQUENCY_INSTANT);
            updateFrequencyDisplay();
            Toast.makeText(getContext(), "Frecuencia actualizada: Instantánea", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        cardDaily.setOnClickListener(v -> {
            NotificationPrefs.setFrequency(requireContext(), NotificationPrefs.FREQUENCY_DAILY);
            updateFrequencyDisplay();
            Toast.makeText(getContext(), "Frecuencia actualizada: Resumen Diario", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateRadioState(boolean isDaily, ImageView ivInstant, ImageView ivDaily) {
        if (ivInstant == null || ivDaily == null || getContext() == null) return;
        if (isDaily) {
            ivInstant.setImageResource(R.drawable.ic_radio_unchecked);
            ivInstant.setImageTintList(android.content.res.ColorStateList.valueOf(0xFF8E9BB0));
            ivDaily.setImageResource(R.drawable.ic_radio_checked);
            ivDaily.setImageTintList(android.content.res.ColorStateList.valueOf(0xFF38BDF8));
        } else {
            ivInstant.setImageResource(R.drawable.ic_radio_checked);
            ivInstant.setImageTintList(android.content.res.ColorStateList.valueOf(0xFF38BDF8));
            ivDaily.setImageResource(R.drawable.ic_radio_unchecked);
            ivDaily.setImageTintList(android.content.res.ColorStateList.valueOf(0xFF8E9BB0));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
