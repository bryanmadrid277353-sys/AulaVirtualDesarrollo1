package com.aula.virtual.ui;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.content.res.ColorStateList;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.aula.virtual.R;
import com.aula.virtual.data.ScheduleInfo;
import com.aula.virtual.databinding.FragmentFacilityTimetableBinding;
import com.aula.virtual.databinding.ItemTimetableBlockBinding;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminFacilityTimetableFragment extends Fragment {
    private FragmentFacilityTimetableBinding binding;
    private MainViewModel viewModel;
    private int facilityId = -1;
    private int studentId = -1;
    private int professorId = -1;
    private static final int HOUR_HEIGHT_DP = 60;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFacilityTimetableBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        
        if (getArguments() != null) {
            facilityId = getArguments().getInt("facilityId", -1);
            studentId = getArguments().getInt("studentId", -1);
            professorId = getArguments().getInt("professorId", -1);
        }

        populateTimeLabels();
        
        if (facilityId != -1) {
            viewModel.getFacilitySchedules(facilityId).observe(getViewLifecycleOwner(), this::renderSchedules);
        } else if (studentId != -1) {
            binding.tvTimetableHeaderTitle.setText("Mi Horario Semanal");
            viewModel.getStudentSchedules(studentId).observe(getViewLifecycleOwner(), this::renderSchedules);
        } else if (professorId != -1) {
            binding.tvTimetableHeaderTitle.setText("Mi Horario Semanal");
            viewModel.getProfessorSchedules(professorId).observe(getViewLifecycleOwner(), this::renderSchedules);
        }
    }

    private void populateTimeLabels() {
        binding.layoutTimeLabels.removeAllViews();
        for (int i = 0; i < 24; i++) {
            TextView tv = new TextView(getContext());
            String label;
            int displayHour = i % 12;
            if (displayHour == 0) displayHour = 12;
            String ampm = (i < 12) ? "am" : "pm";
            label = displayHour + "\n" + ampm;

            tv.setText(label);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tv.setPadding(0, 0, 8, 0);
            tv.setGravity(Gravity.END);
            
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 
                    dpToPx(HOUR_HEIGHT_DP));
            binding.layoutTimeLabels.addView(tv, lp);
        }
    }

    private void renderSchedules(List<ScheduleInfo> schedules) {
        binding.layoutTimetableContent.removeAllViews();
        drawGridLines();

        if (schedules == null) return;

        binding.layoutTimetableContent.post(() -> {
            if (binding == null) return;
            int totalWidth = binding.layoutTimetableContent.getWidth();
            int colWidth = totalWidth / 7;

            for (ScheduleInfo info : schedules) {
                List<String> days = Arrays.asList(info.schedule.days.split(", "));
                int startMin = timeToMinutes(info.schedule.startTime);
                int endMin = timeToMinutes(info.schedule.endTime);
                int duration = endMin - startMin;

                for (String day : days) {
                    int dayIndex = getDayIndex(day);
                    if (dayIndex == -1) continue;

                    ItemTimetableBlockBinding blockBinding = ItemTimetableBlockBinding.inflate(getLayoutInflater());
                    blockBinding.tvBlockSubject.setText(info.subjectName);
                    blockBinding.tvBlockProfessor.setText(info.professorName);

                    // Apply subject color
                    int color = getSubjectColor(info.subjectColor);
                    blockBinding.getRoot().setCardBackgroundColor(color);
                    
                    // Contrast text
                    int textColor = isColorLight(color) ? 0xFF000000 : 0xFFFFFFFF;
                    blockBinding.tvBlockSubject.setTextColor(textColor);
                    blockBinding.tvBlockProfessor.setTextColor(textColor);

                    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(colWidth - 4, dpToPx(duration));
                    lp.topMargin = dpToPx(startMin);
                    lp.leftMargin = (dayIndex * colWidth) + 2;

                    blockBinding.getRoot().setOnClickListener(v -> showScheduleDetailDialog(info));

                    binding.layoutTimetableContent.addView(blockBinding.getRoot(), lp);
                }
            }
        });
    }

    private void showScheduleDetailDialog(ScheduleInfo info) {
        if (info == null || info.schedule == null || getContext() == null) return;

        final boolean[] dialogShown = {false};
        viewModel.getSubjectById(info.schedule.subjectId, subject -> {
            viewModel.getFacilityById(info.schedule.facilityId, facility -> {
                if (getContext() == null || dialogShown[0]) return;
                dialogShown[0] = true;
                requireActivity().runOnUiThread(() -> {
                    View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_schedule_detail, null);

                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
                    builder.setView(dialogView);

                    AlertDialog dialog = builder.create();
                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    }

                    TextView tvSectionBadge = dialogView.findViewById(R.id.tvSectionBadge);
                    TextView tvSubjectName = dialogView.findViewById(R.id.tvSubjectName);
                    TextView tvFacilityName = dialogView.findViewById(R.id.tvFacilityName);
                    TextView tvFacilityCode = dialogView.findViewById(R.id.tvFacilityCode);
                    View layoutDescRow = dialogView.findViewById(R.id.layoutDescRow);
                    TextView tvSubjectDesc = dialogView.findViewById(R.id.tvSubjectDesc);
                    TextView tvProfessorName = dialogView.findViewById(R.id.tvProfessorName);
                    TextView tvDays = dialogView.findViewById(R.id.tvDays);
                    TextView tvTime = dialogView.findViewById(R.id.tvTime);

                    View btnCancel = dialogView.findViewById(R.id.btnCancelDialog);
                    MaterialButton btnSaveColor = dialogView.findViewById(R.id.btnSaveColorDialog);

                    // Bind Fields
                    String secStr = (subject != null && subject.section != null && !subject.section.isEmpty()) ? subject.section : "01";
                    tvSectionBadge.setText("Sección " + secStr);

                    tvSubjectName.setText(info.subjectName != null && !info.subjectName.isEmpty() ? info.subjectName : "Sin Materia");

                    String facName = (info.facilityName != null && !info.facilityName.isEmpty() && !"N/A".equalsIgnoreCase(info.facilityName))
                            ? info.facilityName
                            : ((facility != null && facility.name != null && !facility.name.isEmpty()) ? facility.name : "N/A");
                    tvFacilityName.setText(facName);

                    if (tvFacilityCode != null) {
                        String fCode = (facility != null && facility.code != null && !facility.code.trim().isEmpty())
                                ? facility.code
                                : ("INSTA" + (facility != null ? facility.id : info.schedule.facilityId));
                        tvFacilityCode.setText(fCode);
                    }

                    if (subject != null && subject.description != null && !subject.description.trim().isEmpty()) {
                        tvSubjectDesc.setText(subject.description.trim());
                        layoutDescRow.setVisibility(View.VISIBLE);
                    } else {
                        layoutDescRow.setVisibility(View.GONE);
                    }

                    tvProfessorName.setText(info.professorName != null && !info.professorName.isEmpty() ? info.professorName : "Sin Profesor Asignado");
                    tvDays.setText(info.schedule.days != null ? info.schedule.days : "N/A");
                    tvTime.setText((info.schedule.startTime != null ? info.schedule.startTime : "") + " - " + (info.schedule.endTime != null ? info.schedule.endTime : ""));

                    // Setup Color Palette Selection
                    final String[] selectedColorHolder = {info.schedule.color != null ? info.schedule.color : "BLUE"};

                    Map<String, MaterialButton> colorButtons = new HashMap<>();
                    colorButtons.put("BLUE", dialogView.findViewById(R.id.btnColorBlue));
                    colorButtons.put("GREEN", dialogView.findViewById(R.id.btnColorGreen));
                    colorButtons.put("YELLOW_DARK", dialogView.findViewById(R.id.btnColorYellow));
                    colorButtons.put("PURPLE", dialogView.findViewById(R.id.btnColorPurple));
                    colorButtons.put("RED", dialogView.findViewById(R.id.btnColorRed));
                    colorButtons.put("CYAN", dialogView.findViewById(R.id.btnColorCyan));

                    Runnable highlightSelectedColor = () -> {
                        for (Map.Entry<String, MaterialButton> entry : colorButtons.entrySet()) {
                            MaterialButton btn = entry.getValue();
                            if (btn == null) continue;
                            if (entry.getKey().equalsIgnoreCase(selectedColorHolder[0])) {
                                btn.setIconResource(R.drawable.ic_check);
                                btn.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
                                int colorInt = ThemeHelper.getSubjectColor(requireContext(), entry.getKey());
                                int tint = ThemeHelper.isColorLight(colorInt) ? 0xFF000000 : 0xFFFFFFFF;
                                btn.setIconTint(ColorStateList.valueOf(tint));
                            } else {
                                btn.setIcon(null);
                            }
                        }
                    };

                    for (Map.Entry<String, MaterialButton> entry : colorButtons.entrySet()) {
                        MaterialButton btn = entry.getValue();
                        if (btn != null) {
                            btn.setOnClickListener(v -> {
                                selectedColorHolder[0] = entry.getKey();
                                highlightSelectedColor.run();
                            });
                        }
                    }

                    highlightSelectedColor.run();

                    btnCancel.setOnClickListener(v -> dialog.dismiss());

                    btnSaveColor.setOnClickListener(v -> {
                        info.schedule.color = selectedColorHolder[0];
                        viewModel.performOnlineAction(() -> {
                            viewModel.updateSchedule(info.schedule);
                            Toast.makeText(getContext(), "Color de la clase actualizado", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        });
                    });

                    dialog.show();
                });
            });
        });
    }

    private int getSubjectColor(String colorName) {
        return ThemeHelper.getSubjectColor(requireContext(), colorName);
    }

    private boolean isColorLight(int color) {
        return ThemeHelper.isColorLight(color);
    }

    private void drawGridLines() {
        boolean isDark = ThemeHelper.isDarkMode(requireContext());
        int lineColor = isDark ? 0x33FFFFFF : 0x33000000;

        for (int i = 0; i < 24; i++) {
            View line = new View(getContext());
            line.setBackgroundColor(lineColor);
            RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 
                    1);
            rlp.topMargin = i * dpToPx(HOUR_HEIGHT_DP);
            binding.layoutTimetableContent.addView(line, rlp);
        }

        binding.layoutTimetableContent.post(() -> {
            if (binding == null) return;
            int width = binding.layoutTimetableContent.getWidth();
            int colWidth = width / 7;
            for (int i = 0; i < 7; i++) {
                View line = new View(getContext());
                line.setBackgroundColor(lineColor);
                RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                        1, 
                        ViewGroup.LayoutParams.MATCH_PARENT);
                rlp.leftMargin = i * colWidth;
                binding.layoutTimetableContent.addView(line, rlp);
            }
        });
    }

    private int getDayIndex(String day) {
        switch (day) {
            case "Lunes": return 0;
            case "Martes": return 1;
            case "Miercoles": return 2;
            case "Jueves": return 3;
            case "Viernes": return 4;
            case "Sabado": return 5;
            case "Domingo": return 6;
            default: return -1;
        }
    }

    private int timeToMinutes(String time) {
        try {
            String[] parts = time.trim().split("[: ]");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            
            if (parts.length > 2) {
                String ampm = parts[2].toUpperCase();
                if (ampm.equals("PM") && hour < 12) hour += 12;
                else if (ampm.equals("AM") && hour == 12) hour = 0;
            }
            return hour * 60 + minute;
        } catch (Exception e) {
            return 0;
        }
    }

    private int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.densityDpi / (float) DisplayMetrics.DENSITY_DEFAULT));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
