package com.aula.virtual.ui;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.aula.virtual.R;
import com.aula.virtual.data.ScheduleInfo;
import com.aula.virtual.data.entity.Facility;
import com.aula.virtual.data.entity.FacilitySchedule;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentAdminFacilitySchedulesBinding;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminFacilitySchedulesFragment extends Fragment {
    private FragmentAdminFacilitySchedulesBinding binding;
    private MainViewModel viewModel;
    private ScheduleAdapter adapter;
    private int facilityId;
    private List<ScheduleInfo> currentSchedules = new ArrayList<>();
    private List<ScheduleInfo> professorSchedules = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminFacilitySchedulesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        facilityId = getArguments().getInt("facilityId");

        if (binding.btnBackList != null) {
            binding.btnBackList.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
        }

        adapter = new ScheduleAdapter(new ScheduleAdapter.OnScheduleActionListener() {
            @Override public void onDelete(ScheduleInfo info) { showDeleteScheduleConfirmation(info); }
            @Override public void onEdit(ScheduleInfo info) { showAddScheduleDialog(info.schedule); }
        });

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        observeViewModel();
        
        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        binding.btnAdd.setText("+ Programar Clase");
        binding.btnAdd.setOnClickListener(v -> showAddScheduleDialog(null));
        setupSearch();

        if (binding.btnToggleSearch != null && binding.layoutSearchContainer != null) {
            binding.btnToggleSearch.setOnClickListener(v -> {
                boolean isVisible = binding.layoutSearchContainer.getVisibility() == View.VISIBLE;
                binding.layoutSearchContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                if (!isVisible) {
                    binding.etSearch.requestFocus();
                }
            });
        }

        View.OnClickListener openTimetable = v -> {
            Bundle args = new Bundle();
            args.putInt("facilityId", facilityId);
            Navigation.findNavController(view).navigate(R.id.action_adminFacilitySchedulesFragment_to_adminFacilityTimetableFragment, args);
        };
        if (binding.btnViewTimetable != null) {
            binding.btnViewTimetable.setOnClickListener(openTimetable);
        }

        View.OnClickListener openFacilityDetail = v -> {
            Bundle args = new Bundle();
            args.putInt("facilityId", facilityId);
            Navigation.findNavController(view).navigate(R.id.action_adminFacilitySchedulesFragment_to_adminFacilityDetailFragment, args);
        };
        if (binding.btnHeaderDetail != null) {
            binding.btnHeaderDetail.setOnClickListener(openFacilityDetail);
        }
    }

    private void setupSearch() {
        String[] options = {"Todo", "Materia", "Profesor", "Días"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, options);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFilter.setAdapter(spinnerAdapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilter();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void applyFilter() {
        if (currentSchedules == null) return;
        String query = binding.etSearch.getText().toString().toLowerCase().trim();
        String filterType = binding.spinnerFilter.getSelectedItem() != null ? binding.spinnerFilter.getSelectedItem().toString() : "Todo";

        if (query.isEmpty()) {
            adapter.setSchedules(currentSchedules);
            binding.tvRecordCount.setText(currentSchedules.size() + (currentSchedules.size() == 1 ? " horario programado" : " horarios programados"));
            return;
        }

        List<ScheduleInfo> filtered = currentSchedules.stream().filter(info -> {
            if (info == null) return false;
            String subject = info.subjectName != null ? info.subjectName.toLowerCase() : "";
            String professor = info.professorName != null ? info.professorName.toLowerCase() : "";
            String days = (info.schedule != null && info.schedule.days != null) ? info.schedule.days.toLowerCase() : "";
            String time = (info.schedule != null && info.schedule.startTime != null && info.schedule.endTime != null) ? (info.schedule.startTime + " " + info.schedule.endTime).toLowerCase() : "";

            if (filterType.equals("Todo")) {
                return subject.contains(query) || professor.contains(query) || days.contains(query) || time.contains(query);
            } else if (filterType.equals("Materia")) {
                return subject.contains(query);
            } else if (filterType.equals("Profesor")) {
                return professor.contains(query);
            } else if (filterType.equals("Días")) {
                return days.contains(query);
            }
            return false;
        }).collect(Collectors.toList());

        adapter.setSchedules(filtered);
        binding.tvRecordCount.setText(filtered.size() + (filtered.size() == 1 ? " horario programado" : " horarios programados"));
    }

    private Facility currentFacility;

    private void observeViewModel() {
        viewModel.getAllFacilities().observe(getViewLifecycleOwner(), facilities -> {
            for (Facility f : facilities) {
                if (f.id == facilityId) {
                    currentFacility = f;
                    if (binding.tvFacilityName != null) {
                        binding.tvFacilityName.setText(f.name);
                    }
                    if (binding.tvFacilityTypePill != null) {
                        binding.tvFacilityTypePill.setText(f.type != null ? f.type.toUpperCase(Locale.ROOT) : "AULA");
                    }
                    if (binding.tvFacilityDescription != null) {
                        String desc = (f.description != null && !f.description.isEmpty()) ? f.description : "Espacio educativo";
                        binding.tvFacilityDescription.setText((f.type != null ? f.type : "Aulas") + " • " + desc);
                    }
                    break;
                }
            }
        });

        viewModel.getFacilitySchedules(facilityId).observe(getViewLifecycleOwner(), schedules -> {
            currentSchedules = schedules != null ? schedules : new ArrayList<>();
            applyFilter();
        });
    }

    private void showAddScheduleDialog(@Nullable FacilitySchedule existing) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_schedule, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvFacilityCodeBadge = dialogView.findViewById(R.id.tvFacilityCodeBadge);
        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvDialogSubtitle = dialogView.findViewById(R.id.tvDialogSubtitle);

        TextView tvSubjectText = dialogView.findViewById(R.id.tvSubjectText);
        TextView tvProfessorText = dialogView.findViewById(R.id.tvProfessorText);
        TextView tvDaysText = dialogView.findViewById(R.id.tvDaysText);
        TextView tvStartText = dialogView.findViewById(R.id.tvStartText);
        TextView tvEndText = dialogView.findViewById(R.id.tvEndText);
        TextView tvColorText = dialogView.findViewById(R.id.tvColorText);

        View btnSelectSubject = dialogView.findViewById(R.id.btnSelectSubject);
        View btnSelectProfessor = dialogView.findViewById(R.id.btnSelectProfessor);
        View btnSelectDays = dialogView.findViewById(R.id.btnSelectDays);
        View btnSelectStart = dialogView.findViewById(R.id.btnSelectStart);
        View btnSelectEnd = dialogView.findViewById(R.id.btnSelectEnd);
        View btnSelectColor = dialogView.findViewById(R.id.btnSelectColor);

        View btnCancel = dialogView.findViewById(R.id.btnCancelDialog);
        View btnSave = dialogView.findViewById(R.id.btnSaveScheduleDialog);

        String fCode = (currentFacility != null && currentFacility.code != null && !currentFacility.code.trim().isEmpty())
                ? currentFacility.code
                : ("INSTA" + facilityId);
        String fName = (currentFacility != null && currentFacility.name != null) ? currentFacility.name : "Instalación";

        if (tvFacilityCodeBadge != null) tvFacilityCodeBadge.setText(fCode);
        if (tvDialogTitle != null) tvDialogTitle.setText(existing == null ? "Nueva Programación" : "Editar Programación");
        if (tvDialogSubtitle != null) tvDialogSubtitle.setText(fName + " (" + fCode + ") • Configuración de Horario");

        final int[] selectedSubId = {existing != null ? existing.subjectId : -1};
        final int[] selectedProfId = {existing != null ? existing.professorId : -1};
        final String[] selectedDays = {existing != null ? existing.days : ""};
        final String[] selectedColor = {existing != null ? existing.color : null};

        professorSchedules = new ArrayList<>();

        if (existing != null) {
            if (existing.days != null && !existing.days.trim().isEmpty()) {
                tvDaysText.setText("Días: " + existing.days);
            }
            if (existing.startTime != null) {
                tvStartText.setText("Hora Inicio: " + existing.startTime);
            }
            if (existing.endTime != null) {
                tvEndText.setText("Hora Fin: " + existing.endTime);
            }
            if (existing.color != null) {
                tvColorText.setText("Color en esta Aula: " + existing.color);
            }
        }

        btnSelectProfessor.setOnClickListener(v ->
                Toast.makeText(getContext(), "El profesor único se asigna desde el menú de Inscripción de Materias del Profesor", Toast.LENGTH_SHORT).show()
        );

        MainViewModel.DataCallback<Integer> loadProfForSubject = (subId) -> {
            viewModel.getSubjectById(subId, s -> {
                if (getActivity() == null) return;
                if (s != null && s.professorId != null && s.professorId > 0) {
                    selectedProfId[0] = s.professorId;
                    viewModel.getUserById(s.professorId, u -> {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if (u != null) {
                                tvProfessorText.setError(null);
                                tvProfessorText.setText("Profesor Asignado: " + u.name + " [" + u.carnet + "]");
                                tvProfessorText.setTextColor(ContextCompat.getColor(requireContext(), R.color.entity_profesor));
                            } else {
                                selectedProfId[0] = 0;
                                tvProfessorText.setError(null);
                                tvProfessorText.setText("Profesor: Sin Profesor Asignado");
                            }
                        });
                    });
                    viewModel.getProfessorSchedules(s.professorId).observe(getViewLifecycleOwner(), schedules -> {
                        professorSchedules = schedules;
                    });
                } else {
                    selectedProfId[0] = 0;
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            tvProfessorText.setError(null);
                            tvProfessorText.setText("Profesor: Sin Profesor Asignado");
                        });
                    }
                    professorSchedules = new ArrayList<>();
                }
            });
        };

        if (existing != null) {
            viewModel.getSubjectById(existing.subjectId, s -> {
                if (s != null) {
                    String sec = s.section != null ? s.section : "01";
                    tvSubjectText.setText("Materia: " + s.name + " (Sec " + sec + ")");
                    loadProfForSubject.onResult(s.id);
                }
            });
        }

        btnSelectSubject.setOnClickListener(v -> showSubjectSearchDialog((item) -> {
            selectedSubId[0] = item.id;
            tvSubjectText.setError(null);
            tvSubjectText.setText("Materia: " + item.text + " (" + item.subtext + ")");
            loadProfForSubject.onResult(item.id);
        }));

        btnSelectDays.setOnClickListener(v -> showDaysPickerDialog(selectedDays[0], result -> {
            selectedDays[0] = result;
            tvDaysText.setError(null);
            tvDaysText.setText("Días: " + result);
        }));

        btnSelectStart.setOnClickListener(v -> {
            tvStartText.setError(null);
            showTimePicker(tvStartText);
        });

        btnSelectEnd.setOnClickListener(v -> {
            tvEndText.setError(null);
            showTimePicker(tvEndText);
        });

        btnSelectColor.setOnClickListener(v -> showColorPickerDialog(selectedColor[0] != null ? selectedColor[0] : "BLUE", colorName -> {
            selectedColor[0] = colorName;
            tvColorText.setError(null);
            tvColorText.setText("Color en esta Aula: " + colorName);
        }));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            boolean isValid = true;

            if (selectedSubId[0] == -1) {
                tvSubjectText.setError("Debe seleccionar una materia");
                isValid = false;
            }
            if (selectedProfId[0] < 0) {
                selectedProfId[0] = 0;
            }
            if (selectedDays[0].isEmpty()) {
                tvDaysText.setError("Debe seleccionar al menos un día");
                isValid = false;
            }

            String startStr = tvStartText.getText().toString();
            String endStr = tvEndText.getText().toString();

            if (startStr.contains("Click") || startStr.equalsIgnoreCase("Hora Inicio")) {
                tvStartText.setError("Debe seleccionar la hora de inicio");
                isValid = false;
            }
            if (endStr.contains("Click") || endStr.equalsIgnoreCase("Hora Fin")) {
                tvEndText.setError("Debe seleccionar la hora de fin");
                isValid = false;
            }

            if (selectedColor[0] == null) {
                tvColorText.setError("Debe seleccionar un color");
                isValid = false;
            }

            if (!isValid) {
                return;
            }

            String start = startStr.replace("Hora Inicio: ", "").replace("Hora Inicio", "").trim();
            String end = endStr.replace("Hora Fin: ", "").replace("Hora Fin", "").trim();

            if (start.isEmpty() || end.isEmpty() || start.equals("00:00") || end.equals("00:00")) {
                Toast.makeText(getContext(), "Debe definir un horario válido", Toast.LENGTH_SHORT).show();
                return;
            }

            String conflict = checkScheduleConflicts(existing != null ? existing.id : -1, selectedSubId[0], selectedProfId[0], selectedDays[0], start, end);
            if (conflict != null) {
                Toast.makeText(getContext(), "CONFLICTO: " + conflict, Toast.LENGTH_LONG).show();
                return;
            }

            FacilitySchedule sch = existing != null ? existing : new FacilitySchedule();
            sch.facilityId = facilityId;
            sch.subjectId = selectedSubId[0];
            sch.professorId = selectedProfId[0];
            sch.days = selectedDays[0];
            sch.startTime = start;
            sch.endTime = end;
            sch.color = selectedColor[0];

            if (existing == null) {
                viewModel.performOnlineAction(() -> {
                    viewModel.insertSchedule(sch);
                    dialog.dismiss();
                });
            } else {
                viewModel.performOnlineAction(() -> {
                    viewModel.updateSchedule(sch);
                    dialog.dismiss();
                });
            }
        });

        dialog.show();
    }

    private void showColorPickerDialog(String currentColor, MainViewModel.DataCallback<String> onSelected) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("Seleccionar Color del Horario");

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_detail, null);
        dialogView.findViewById(R.id.tvDetailTitle).setVisibility(View.GONE);
        dialogView.findViewById(R.id.etField1).setVisibility(View.GONE);
        dialogView.findViewById(R.id.btnCopyField1).setVisibility(View.GONE);
        dialogView.findViewById(R.id.etField2).setVisibility(View.GONE);
        dialogView.findViewById(R.id.btnCopyField2).setVisibility(View.GONE);
        dialogView.findViewById(R.id.btnSave).setVisibility(View.GONE);
        dialogView.findViewById(R.id.btnDelete).setVisibility(View.GONE);
        
        View scrollViewChild = ((ViewGroup) dialogView).getChildAt(0);
        if (scrollViewChild instanceof LinearLayout) {
            LinearLayout innerLayout = (LinearLayout) scrollViewChild;
            innerLayout.setGravity(Gravity.CENTER);
            int p = (int) (24 * getResources().getDisplayMetrics().density);
            int pTopBottom = (int) (48 * getResources().getDisplayMetrics().density);
            innerLayout.setPadding(p, pTopBottom, p, pTopBottom);
        }

        LinearLayout layoutColorSelector = dialogView.findViewById(R.id.layoutColorSelector);
        if (layoutColorSelector != null) {
            layoutColorSelector.setVisibility(View.VISIBLE);
            layoutColorSelector.setGravity(Gravity.CENTER);
            TextView tvColorLabel = dialogView.findViewById(R.id.tvColorLabel);
            if (tvColorLabel != null) {
                tvColorLabel.setText("Seleccionar Color del Horario");
                tvColorLabel.setGravity(Gravity.CENTER);
                tvColorLabel.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ));
            }
        }

        Map<String, MaterialButton> colorButtons = new HashMap<>();
        colorButtons.put("BLUE_LIGHT", dialogView.findViewById(R.id.btnColor1));
        colorButtons.put("RED_LIGHT", dialogView.findViewById(R.id.btnColor2));
        colorButtons.put("GREEN_LIGHT", dialogView.findViewById(R.id.btnColor3));
        colorButtons.put("PURPLE_LIGHT", dialogView.findViewById(R.id.btnColor4));
        colorButtons.put("CYAN_LIGHT", dialogView.findViewById(R.id.btnColor5));
        colorButtons.put("YELLOW_LIGHT", dialogView.findViewById(R.id.btnColor6));

        colorButtons.put("BLUE", dialogView.findViewById(R.id.btnColor7));
        colorButtons.put("RED", dialogView.findViewById(R.id.btnColor8));
        colorButtons.put("GREEN", dialogView.findViewById(R.id.btnColor9));
        colorButtons.put("PURPLE", dialogView.findViewById(R.id.btnColor10));
        colorButtons.put("CYAN", dialogView.findViewById(R.id.btnColor11));
        colorButtons.put("YELLOW", dialogView.findViewById(R.id.btnColor12));

        colorButtons.put("BLUE_DARK", dialogView.findViewById(R.id.btnColor13));
        colorButtons.put("RED_DARK", dialogView.findViewById(R.id.btnColor14));
        colorButtons.put("GREEN_DARK", dialogView.findViewById(R.id.btnColor15));
        colorButtons.put("PURPLE_DARK", dialogView.findViewById(R.id.btnColor16));
        colorButtons.put("CYAN_DARK", dialogView.findViewById(R.id.btnColor17));
        colorButtons.put("YELLOW_DARK", dialogView.findViewById(R.id.btnColor18));

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        for (Map.Entry<String, MaterialButton> entry : colorButtons.entrySet()) {
            if (entry.getValue() != null) {
                if (entry.getKey().equals(currentColor)) {
                    entry.getValue().setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
                    entry.getValue().setIconResource(R.drawable.ic_check);
                    int colorInt = ThemeHelper.getSubjectColor(requireContext(), currentColor);
                    int tint = ThemeHelper.isColorLight(colorInt) ? 0xFF000000 : 0xFFFFFFFF;
                    entry.getValue().setIconTint(ColorStateList.valueOf(tint));
                }
                entry.getValue().setOnClickListener(v -> {
                    onSelected.onResult(entry.getKey());
                    dialog.dismiss();
                });
            }
        }

        dialog.show();
    }

    private String checkScheduleConflicts(int currentSchId, int selectedSubId, int selectedProfId, String newDays, String newStart, String newEnd) {
        // 0. Restriction: No same Subject in the same Facility
        for (ScheduleInfo info : currentSchedules) {
            if (info.schedule != null && info.schedule.id == currentSchId) continue;
            if (info.schedule != null && info.schedule.facilityId == facilityId && info.schedule.subjectId == selectedSubId) {
                return "Ya existe una clase programada para esta misma Materia en esta Instalación.";
            }
        }

        List<String> newDaysList = Arrays.asList(newDays.split(", "));
        int newStartMin = timeToMinutes(newStart);
        int newEndMin = timeToMinutes(newEnd);

        // 1. Conflict in this Facility (Aula)
        for (ScheduleInfo info : currentSchedules) {
            if (info.schedule != null && info.schedule.id == currentSchId) continue;
            if (overlaps(info, newDaysList, newStartMin, newEndMin)) {
                return "Esta aula ya tiene una clase en este horario (" + info.subjectName + ")";
            }
        }

        // 2. Conflict for the Professor in ANY Facility (only if professor assigned)
        if (selectedProfId > 0) {
            for (ScheduleInfo info : professorSchedules) {
                if (info.schedule != null && info.schedule.id == currentSchId) continue;
                if (overlaps(info, newDaysList, newStartMin, newEndMin)) {
                    return "El profesor ya tiene una clase en este horario (" + info.subjectName + ")";
                }
            }
        }
        return null;
    }

    private boolean overlaps(ScheduleInfo info, List<String> newDaysList, int newStartMin, int newEndMin) {
        List<String> existingDays = Arrays.asList(info.schedule.days.split(", "));
        boolean overlapDay = false;
        for (String day : newDaysList) {
            if (existingDays.contains(day)) {
                overlapDay = true;
                break;
            }
        }

        if (overlapDay) {
            int exStart = timeToMinutes(info.schedule.startTime);
            int exEnd = timeToMinutes(info.schedule.endTime);
            return (newStartMin < exEnd && newEndMin > exStart);
        }
        return false;
    }

    private int timeToMinutes(String time) {
        try {
            // Soporta formatos "HH:mm AM/PM" o "HH:mm" (24h)
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

    private void showSubjectSearchDialog(SearchableAdapter.OnItemClickListener onSelected) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_searchable_list, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext()).setView(dialogView).create();
        EditText etSearch = dialogView.findViewById(R.id.etSearchDialog);
        RecyclerView rvList = dialogView.findViewById(R.id.rvDialogList);
        rvList.setLayoutManager(new LinearLayoutManager(getContext()));

        List<SearchableAdapter.SearchableItem> allItems = new ArrayList<>();
        List<Subject> subjects = viewModel.getAllSubjects().getValue();
        if (subjects != null) {
            for (Subject s : subjects) {
                String sec = s.section != null ? s.section : "01";
                allItems.add(new SearchableAdapter.SearchableItem(s.id, s.name, "Sección " + sec));
            }
        }

        SearchableAdapter sAdapter = new SearchableAdapter(item -> { onSelected.onItemClick(item); dialog.dismiss(); });
        sAdapter.setItems(allItems);
        rvList.setAdapter(sAdapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString().toLowerCase();
                sAdapter.setItems(allItems.stream().filter(i -> i.text.toLowerCase().contains(q)).collect(Collectors.toList()));
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        dialog.show();
    }

    private void showDaysPickerDialog(String currentDays, MainViewModel.DataCallback<String> onResult) {
        String[] days = {"Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo"};
        boolean[] checked = new boolean[7];
        if (!currentDays.isEmpty()) {
            List<String> currentList = Arrays.asList(currentDays.split(", "));
            for (int i = 0; i < 7; i++) checked[i] = currentList.contains(days[i]);
        }

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Seleccionar Días")
            .setMultiChoiceItems(days, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
            .setPositiveButton("OK", (dialog, which) -> {
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < 7; i++) {
                    if (checked[i]) {
                        if (result.length() > 0) result.append(", ");
                        result.append(days[i]);
                    }
                }
                onResult.onResult(result.toString());
            }).show();
    }

    private TextView createValueButton(String text) {
        return DialogUtils.createDialogOptionButton(requireContext(), text);
    }

    private void showDeleteScheduleConfirmation(ScheduleInfo info) {
        if (info == null || info.schedule == null) return;
        String daysStr = info.schedule.days != null ? info.schedule.days : "Días por definir";
        String timeStr = (info.schedule.startTime != null && info.schedule.endTime != null) ? (info.schedule.startTime + " - " + info.schedule.endTime) : "";
        String titleStr = (info.subjectName != null ? info.subjectName : "Materia") + " (" + daysStr + ")";
        DialogUtils.showConfirmDeleteDialog(
            requireContext(),
            "Confirmar Eliminación",
            "¿Estás seguro de eliminar esta programación de clase? El bloque de horario quedará liberado en la instalación.",
            titleStr,
            timeStr + " • Horario Asignado",
            "HORARIO",
            null,
            R.drawable.ic_timetable,
            () -> viewModel.performOnlineAction(() -> viewModel.deleteSchedule(info.schedule))
        );
    }

    private void showTimePicker(TextView target) {
        Calendar c = Calendar.getInstance();
        int initialHour = c.get(Calendar.HOUR_OF_DAY);
        int initialMinute = c.get(Calendar.MINUTE);

        // Intentar inicializar con la hora actual del TextView si existe
        try {
            String currentText = target.getText().toString();
            String timePart = currentText.substring(currentText.indexOf(":") + 2);
            int totalMins = timeToMinutes(timePart);
            if (totalMins > 0 || timePart.startsWith("00") || timePart.startsWith("12")) {
                initialHour = totalMins / 60;
                initialMinute = totalMins % 60;
            }
        } catch (Exception ignored) {}

        new TimePickerDialog(getContext(), (view, hour, minute) -> {
            String ampm = (hour < 12) ? "AM" : "PM";
            int hour12 = hour % 12;
            if (hour12 == 0) hour12 = 12;
            
            String time = String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, ampm);
            if (target.getText().toString().contains("Inicio")) {
                DialogUtils.setOptionState(target, "Hora Inicio: " + time, false, requireContext());
            } else {
                DialogUtils.setOptionState(target, "Hora Fin: " + time, false, requireContext());
            }
        }, initialHour, initialMinute, false).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
