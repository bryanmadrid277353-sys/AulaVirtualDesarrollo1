package com.aula.virtual.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.aula.virtual.R;
import com.aula.virtual.data.ScheduleInfo;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.databinding.FragmentProfessorHomeBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProfessorHomeFragment extends Fragment {
    private FragmentProfessorHomeBinding binding;
    private MainViewModel viewModel;
    private List<Subject> allSubjects = new ArrayList<>();
    private final Map<Integer, String> facilityMap = new HashMap<>();
    private final Map<Integer, ScheduleInfo> scheduleMap = new HashMap<>();
    private String currentSearchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfessorHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        binding.btnGoToProfileProf.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_professorHomeFragment_to_studentProfileFragment));

        binding.btnSearchHeaderProf.setOnClickListener(v -> binding.etSearchSubjectProf.requestFocus());

        if (viewModel.isImpersonating()) {
            binding.cardReturnToAdmin.setVisibility(View.VISIBLE);
            binding.cardReturnToAdmin.setOnClickListener(v -> {
                viewModel.stopImpersonation();
                Navigation.findNavController(view).popBackStack();
            });
        }

        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                String nameStr = user.name != null ? user.name : "Profesor";
                if (!nameStr.toLowerCase().startsWith("prof")) {
                    binding.tvProfessorWelcomeName.setText("Prof. " + nameStr);
                } else {
                    binding.tvProfessorWelcomeName.setText(nameStr);
                }

                String carnetStr = user.carnet != null ? user.carnet : "DOC9842101";
                binding.tvProfessorCarnetBadge.setText(carnetStr);

                ImageUtils.setProfileImage(binding.ivProfileImageProf, user.profile_image, user.name);

                // Fetch subjects assigned to professor
                viewModel.getSubjectsByProfessor(user.id).observe(getViewLifecycleOwner(), subjects -> {
                    allSubjects = subjects != null ? subjects : new ArrayList<>();
                    renderSubjectCards();
                });

                // Fetch professor's schedules for accurate days and times
                viewModel.getProfessorSchedules(user.id).observe(getViewLifecycleOwner(), schedules -> {
                    scheduleMap.clear();
                    if (schedules != null) {
                        for (ScheduleInfo info : schedules) {
                            if (info != null && info.schedule != null) {
                                scheduleMap.put(info.schedule.subjectId, info);
                            }
                        }
                    }
                    renderSubjectCards();
                });
            }
        });

        viewModel.fetchSubjectFacilityMap(map -> {
            if (map != null) {
                facilityMap.clear();
                facilityMap.putAll(map);
                renderSubjectCards();
            }
        });

        binding.etSearchSubjectProf.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim().toLowerCase() : "";
                renderSubjectCards();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void renderSubjectCards() {
        if (binding == null || getContext() == null || !isAdded()) return;
        binding.layoutSubjectCardsContainer.removeAllViews();

        List<Subject> filtered = allSubjects.stream().filter(s -> {
            if (s == null) return false;
            if (currentSearchQuery.isEmpty()) return true;
            String name = s.name != null ? s.name.toLowerCase() : "";
            String desc = s.description != null ? s.description.toLowerCase() : "";
            String sec = s.section != null ? s.section.toLowerCase() : "";
            return name.contains(currentSearchQuery) || desc.contains(currentSearchQuery) || sec.contains(currentSearchQuery);
        }).collect(Collectors.toList());

        int count = filtered.size();
        binding.tvAssignedSubjectsHeader.setText("MATERIAS ASIGNADAS (" + count + ")");

        if (filtered.isEmpty()) {
            if (getContext() == null) return;
            TextView tvEmpty = new TextView(getContext());
            tvEmpty.setText("No hay materias asignadas encontradas.");
            tvEmpty.setTextColor(0xFF8E9BB0);
            tvEmpty.setPadding(0, 16, 0, 16);
            binding.layoutSubjectCardsContainer.addView(tvEmpty);
            return;
        }

        for (Subject s : filtered) {
            View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_professor_subject_card, null);

            TextView tvTitle = cardView.findViewById(R.id.tvSubjectTitle);
            TextView tvSubtitle = cardView.findViewById(R.id.tvSubjectSubtitle);
            TextView tvScheduleDays = cardView.findViewById(R.id.tvScheduleDays);
            View btnChevron = cardView.findViewById(R.id.btnChevronRight);

            tvTitle.setText(s.name != null ? s.name : "Materia");

            String facilityName = facilityMap.get(s.id);
            String secText = (s.section != null && !s.section.trim().isEmpty()) ? ("Sección " + s.section.trim()) : "";
            String descText = (s.description != null && !s.description.trim().isEmpty()) ? s.description.trim() : "";

            StringBuilder sb = new StringBuilder();
            if (!descText.isEmpty()) {
                sb.append(descText);
            }
            if (facilityName != null && !facilityName.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(" • ");
                sb.append(facilityName.trim());
            }
            if (!secText.isEmpty()) {
                if (sb.length() > 0) sb.append(" • ");
                sb.append(secText);
            }
            if (sb.length() == 0) {
                sb.append("Materia asignada");
            }
            tvSubtitle.setText(sb.toString());

            // Real Schedule Days & Times from Database
            ScheduleInfo schInfo = scheduleMap.get(s.id);
            if (schInfo != null && schInfo.schedule != null && schInfo.schedule.days != null && !schInfo.schedule.days.trim().isEmpty()) {
                String daysStr = schInfo.schedule.days.trim();
                if (schInfo.schedule.startTime != null && schInfo.schedule.endTime != null) {
                    daysStr += " (" + schInfo.schedule.startTime + " - " + schInfo.schedule.endTime + ")";
                }
                tvScheduleDays.setText(daysStr);
            } else {
                tvScheduleDays.setText("Horario por definir");
            }

            // Navigation to Subject Management (Blog, Foro, Alumnos, Notas, etc.)
            View.OnClickListener openSubjectManagement = v -> {
                Bundle args = new Bundle();
                args.putInt("subjectId", s.id);
                Navigation.findNavController(requireView()).navigate(R.id.action_professorHomeFragment_to_subjectBlogFragment, args);
            };

            cardView.setOnClickListener(openSubjectManagement);
            if (btnChevron != null) {
                btnChevron.setOnClickListener(openSubjectManagement);
            }

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, (int) (12 * getResources().getDisplayMetrics().density));
            cardView.setLayoutParams(lp);

            binding.layoutSubjectCardsContainer.addView(cardView);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
