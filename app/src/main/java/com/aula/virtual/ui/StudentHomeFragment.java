package com.aula.virtual.ui;

import android.os.Bundle;
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
import com.aula.virtual.data.StudentGradeInfo;
import com.aula.virtual.data.entity.Student;
import com.aula.virtual.databinding.FragmentStudentHomeBinding;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class StudentHomeFragment extends Fragment {
    private FragmentStudentHomeBinding binding;
    private MainViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        binding.btnGoToProfileHome.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_studentHomeFragment_to_studentProfileFragment));

        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (binding == null || !isAdded()) return;
            if (user != null) {
                binding.tvWelcomeName.setText(user.name != null ? user.name : "Usuario");
                binding.tvRoleTag.setText("• Encargado de Familia");
                String carnetStr = user.carnet != null ? user.carnet : "-";
                binding.tvCarnetTag.setText("Carnet: " + carnetStr);

                ImageUtils.setProfileImage(binding.ivProfileImageHome, user.profile_image, user.name);

                // Fetch students for this Encargado
                viewModel.fetchStudentsForEncargado(user.id);
            }
        });

        viewModel.getMyStudents().observe(getViewLifecycleOwner(), students -> {
            if (binding == null || !isAdded()) return;
            renderStudentCards(viewModel, students);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null && viewModel.getMyStudents().getValue() != null) {
            renderStudentCards(viewModel, viewModel.getMyStudents().getValue());
        }
    }

    private void renderStudentCards(MainViewModel viewModel, List<Student> students) {
        if (binding == null || getContext() == null || !isAdded()) return;
        binding.layoutStudentCardsContainer.removeAllViews();

        int count = students != null ? students.size() : 0;
        binding.tvStudentsCountHeader.setText("MIS ESTUDIANTES (" + count + (count == 1 ? " REGISTRO)" : " REGISTROS)"));

        if (students == null || students.isEmpty()) {
            if (getContext() == null) return;
            TextView tvEmpty = new TextView(getContext());
            tvEmpty.setText("No tienes estudiantes asignados a tu cargo.");
            tvEmpty.setTextColor(0xFF8E9BB0);
            tvEmpty.setPadding(0, 16, 0, 16);
            binding.layoutStudentCardsContainer.addView(tvEmpty);
            return;
        }

        boolean isGrid = ThemeHelper.isGridHomeLayout(requireContext());

        if (isGrid) {
            float density = getResources().getDisplayMetrics().density;
            for (int i = 0; i < count; i += 2) {
                LinearLayout rowLayout = new LinearLayout(requireContext());
                rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rowLp.setMargins(0, 0, 0, (int) (12 * density));
                rowLayout.setLayoutParams(rowLp);

                Student s1 = students.get(i);
                View cardView1 = LayoutInflater.from(requireContext()).inflate(R.layout.item_student_grid_card, rowLayout, false);
                populateStudentCard(viewModel, cardView1, s1);
                LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
                lp1.setMarginEnd((int) (6 * density));
                cardView1.setLayoutParams(lp1);
                rowLayout.addView(cardView1);

                if (i + 1 < count) {
                    Student s2 = students.get(i + 1);
                    View cardView2 = LayoutInflater.from(requireContext()).inflate(R.layout.item_student_grid_card, rowLayout, false);
                    populateStudentCard(viewModel, cardView2, s2);
                    LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
                    lp2.setMarginStart((int) (6 * density));
                    cardView2.setLayoutParams(lp2);
                    rowLayout.addView(cardView2);
                } else {
                    View dummy = new View(requireContext());
                    LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
                    lp2.setMarginStart((int) (6 * density));
                    dummy.setLayoutParams(lp2);
                    rowLayout.addView(dummy);
                }

                binding.layoutStudentCardsContainer.addView(rowLayout);
            }
        } else {
            for (Student s : students) {
                View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_student_card, null);
                populateStudentCard(viewModel, cardView, s);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, (int) (12 * getResources().getDisplayMetrics().density));
                cardView.setLayoutParams(lp);

                binding.layoutStudentCardsContainer.addView(cardView);
            }
        }
    }

    private void populateStudentCard(MainViewModel viewModel, View cardView, Student s) {
        TextView tvInitials = cardView.findViewById(R.id.tvStudentInitials);
        TextView tvName = cardView.findViewById(R.id.tvStudentName);
        TextView tvCarnetGrade = cardView.findViewById(R.id.tvStudentCarnetGrade);
        TextView tvStatusPill = cardView.findViewById(R.id.tvStudentStatusPill);
        TextView tvAveragePill = cardView.findViewById(R.id.tvStudentAveragePill);
        View btnChevronRight = cardView.findViewById(R.id.btnChevronRight);

        MaterialButton btnSchedule = cardView.findViewById(R.id.btnStudentSchedule);

        if (tvInitials != null) tvInitials.setText(getInitials(s.name));
        if (tvName != null) tvName.setText(s.name != null ? s.name : "Estudiante");
        String carnetStr = s.carnet != null ? s.carnet : "-";
        String gradeStr = s.grade != null ? s.grade : "Primaria";
        if (tvCarnetGrade != null) tvCarnetGrade.setText("Carnet: " + carnetStr + " • " + gradeStr);

        if (tvStatusPill != null) tvStatusPill.setText("• Regular");

        if (tvAveragePill != null) {
            viewModel.getAcademicPeriods().observe(getViewLifecycleOwner(), periods -> {
                if (periods == null || periods.isEmpty()) {
                    tvAveragePill.setText("☆ Promedio: Sin Calendario");
                    return;
                }
                int activePeriodsCount = periods.size();
                viewModel.getGradeInfoForStudent(s.id).observe(getViewLifecycleOwner(), gradeInfos -> {
                    if (gradeInfos != null && !gradeInfos.isEmpty()) {
                        double sum = 0;
                        int totalPeriods = 0;
                        for (StudentGradeInfo info : gradeInfos) {
                            if (info != null && info.enrollment != null) {
                                totalPeriods += activePeriodsCount;
                                if (activePeriodsCount >= 1 && info.enrollment.grade1 != null) sum += info.enrollment.grade1;
                                if (activePeriodsCount >= 2 && info.enrollment.grade2 != null) sum += info.enrollment.grade2;
                                if (activePeriodsCount >= 3 && info.enrollment.grade3 != null) sum += info.enrollment.grade3;
                                if (activePeriodsCount >= 4 && info.enrollment.grade4 != null) sum += info.enrollment.grade4;
                                if (activePeriodsCount >= 5 && info.enrollment.grade5 != null) sum += info.enrollment.grade5;
                            }
                        }
                        double avg = totalPeriods > 0 ? (sum / totalPeriods) : 0;
                        tvAveragePill.setText(String.format(Locale.getDefault(), "☆ Promedio: %.1f", avg));
                    } else {
                        tvAveragePill.setText("☆ Promedio: N/A");
                    }
                });
            });
        }

        // Card Click & Chevron Right -> Open Student Management (Gestión del estudiante)
        View.OnClickListener openManagement = v -> {
            Bundle args = new Bundle();
            args.putInt("studentId", s.id);
            Navigation.findNavController(requireView()).navigate(R.id.action_studentHomeFragment_to_adminEnrollmentListFragment, args);
        };

        cardView.setOnClickListener(openManagement);
        if (btnChevronRight != null) btnChevronRight.setOnClickListener(openManagement);

        // Right Button: Horario
        if (btnSchedule != null) {
            btnSchedule.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putInt("studentId", s.id);
                Navigation.findNavController(requireView()).navigate(R.id.action_studentHomeFragment_to_adminFacilityTimetableFragment, args);
            });
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
