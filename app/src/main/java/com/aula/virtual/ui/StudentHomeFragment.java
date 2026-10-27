package com.aula.virtual.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.aula.virtual.R;
import com.aula.virtual.data.StudentGradeInfo;
import com.aula.virtual.data.entity.Student;
import com.aula.virtual.databinding.FragmentStudentHomeBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Locale;

public class StudentHomeFragment extends Fragment {
    private FragmentStudentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MainViewModel viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        binding.btnGoToProfileHome.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_studentHomeFragment_to_studentProfileFragment));

        if (viewModel.isImpersonating()) {
            binding.cardReturnToAdmin.setVisibility(View.VISIBLE);
            binding.cardReturnToAdmin.setOnClickListener(v -> {
                viewModel.stopImpersonation();
                Navigation.findNavController(view).popBackStack();
            });
        }

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

        for (Student s : students) {
            View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_student_card, null);

            TextView tvInitials = cardView.findViewById(R.id.tvStudentInitials);
            TextView tvName = cardView.findViewById(R.id.tvStudentName);
            TextView tvCarnetGrade = cardView.findViewById(R.id.tvStudentCarnetGrade);
            TextView tvStatusPill = cardView.findViewById(R.id.tvStudentStatusPill);
            TextView tvAveragePill = cardView.findViewById(R.id.tvStudentAveragePill);
            View btnChevronRight = cardView.findViewById(R.id.btnChevronRight);

            MaterialButton btnSchedule = cardView.findViewById(R.id.btnStudentSchedule);

            tvInitials.setText(getInitials(s.name));
            tvName.setText(s.name != null ? s.name : "Estudiante");
            String carnetStr = s.carnet != null ? s.carnet : "-";
            String gradeStr = s.grade != null ? s.grade : "Primaria";
            tvCarnetGrade.setText("Carnet: " + carnetStr + " • " + gradeStr);

            tvStatusPill.setText("• Regular");

            // Calculate student's average
            viewModel.getGradeInfoForStudent(s.id).observe(getViewLifecycleOwner(), gradeInfos -> {
                if (gradeInfos != null && !gradeInfos.isEmpty()) {
                    double sum = 0;
                    int totalPeriods = 0;
                    for (StudentGradeInfo info : gradeInfos) {
                        if (info != null && info.enrollment != null) {
                            int pCount = info.enrollment.periodsCount > 0 ? info.enrollment.periodsCount : 5;
                            totalPeriods += pCount;
                            if (info.enrollment.grade1 != null) sum += info.enrollment.grade1;
                            if (info.enrollment.grade2 != null) sum += info.enrollment.grade2;
                            if (info.enrollment.grade3 != null) sum += info.enrollment.grade3;
                            if (info.enrollment.grade4 != null) sum += info.enrollment.grade4;
                            if (info.enrollment.grade5 != null) sum += info.enrollment.grade5;
                        }
                    }
                    double avg = totalPeriods > 0 ? (sum / totalPeriods) : 0;
                    tvAveragePill.setText(String.format(Locale.getDefault(), "☆ Promedio: %.1f", avg));
                } else {
                    tvAveragePill.setText("☆ Promedio: N/A");
                }
            });

            // Card Click & Chevron Right -> Open Student Management (Gestión del estudiante)
            View.OnClickListener openManagement = v -> {
                Bundle args = new Bundle();
                args.putInt("studentId", s.id);
                Navigation.findNavController(requireView()).navigate(R.id.action_studentHomeFragment_to_adminEnrollmentListFragment, args);
            };

            cardView.setOnClickListener(openManagement);
            if (btnChevronRight != null) btnChevronRight.setOnClickListener(openManagement);

            // Right Button: Horario
            btnSchedule.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putInt("studentId", s.id);
                Navigation.findNavController(requireView()).navigate(R.id.action_studentHomeFragment_to_adminFacilityTimetableFragment, args);
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, (int) (12 * getResources().getDisplayMetrics().density));
            cardView.setLayoutParams(lp);

            binding.layoutStudentCardsContainer.addView(cardView);
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
