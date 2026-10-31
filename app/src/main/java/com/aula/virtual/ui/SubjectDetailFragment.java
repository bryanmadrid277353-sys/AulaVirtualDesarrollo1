package com.aula.virtual.ui;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.aula.virtual.R;
import com.aula.virtual.data.StudentGradeInfo;
import com.aula.virtual.data.entity.Enrollment;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentSubjectDetailBinding;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class SubjectDetailFragment extends Fragment {
    private FragmentSubjectDetailBinding binding;
    private MainViewModel viewModel;
    private Subject subject;
    private String selectedColor = "BLUE";
    private final Map<String, MaterialButton> colorButtons = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSubjectDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        int subjectId = getArguments().getInt("subjectId");
        
        setupUI();
        setupColorSelector();
        observeViewModel();
        
        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        DialogUtils.showLoadingOverlay(binding.getRoot(), requireContext());
        viewModel.fetchSubjectById(subjectId);
    }

    private void setupUI() {
        binding.btnBackDetail.setOnClickListener(v -> popBackToManagementMenu());
        binding.btnSave.setOnClickListener(v -> saveChanges());
        binding.btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        binding.btnManageStudents.setOnClickListener(v -> navigateToStudents());
        binding.btnUnenrollMember.setOnClickListener(v -> showUnenrollMemberDialog());

        binding.etField4.setInputType(InputType.TYPE_CLASS_NUMBER);
        binding.etField4.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(1)});
    }

    private void setupColorSelector() {
        colorButtons.put("BLUE_LIGHT", binding.btnColor1);
        colorButtons.put("RED_LIGHT", binding.btnColor2);
        colorButtons.put("GREEN_LIGHT", binding.btnColor3);
        colorButtons.put("PURPLE_LIGHT", binding.btnColor4);
        colorButtons.put("CYAN_LIGHT", binding.btnColor5);
        colorButtons.put("YELLOW_LIGHT", binding.btnColor6);

        colorButtons.put("BLUE", binding.btnColor7);
        colorButtons.put("RED", binding.btnColor8);
        colorButtons.put("GREEN", binding.btnColor9);
        colorButtons.put("PURPLE", binding.btnColor10);
        colorButtons.put("CYAN", binding.btnColor11);
        colorButtons.put("YELLOW", binding.btnColor12);

        colorButtons.put("BLUE_DARK", binding.btnColor13);
        colorButtons.put("RED_DARK", binding.btnColor14);
        colorButtons.put("GREEN_DARK", binding.btnColor15);
        colorButtons.put("PURPLE_DARK", binding.btnColor16);
        colorButtons.put("CYAN_DARK", binding.btnColor17);
        colorButtons.put("YELLOW_DARK", binding.btnColor18);

        for (Map.Entry<String, MaterialButton> entry : colorButtons.entrySet()) {
            entry.getValue().setOnClickListener(v -> selectColor(entry.getKey()));
        }
    }

    private void selectColor(String colorName) {
        if (!ThemeHelper.isLocalMode(requireContext()) && !Boolean.TRUE.equals(viewModel.isServerConnected().getValue())) {
            User currentUser = viewModel.getCurrentUser().getValue();
            if (currentUser != null && !"ADMIN".equals(currentUser.role)) {
                Toast.makeText(getContext(), "No se puede cambiar el color fuera de línea", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        selectedColor = colorName;
        resetColorIcons();
        MaterialButton btn = colorButtons.get(colorName);
        if (btn != null) {
            btn.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
            btn.setIconResource(R.drawable.ic_check);
            int colorInt = ThemeHelper.getSubjectColor(requireContext(), colorName);
            int tint = ThemeHelper.isColorLight(colorInt) ? 0xFF000000 : 0xFFFFFFFF;
            btn.setIconTint(ColorStateList.valueOf(tint));
        }
    }

    private void resetColorIcons() {
        for (MaterialButton btn : colorButtons.values()) {
            btn.setIcon(null);
        }
    }

    private void navigateToStudents() {
        if (subject == null) return;
        Bundle args = new Bundle();
        args.putInt("subjectId", subject.id);
        Navigation.findNavController(requireView()).navigate(R.id.action_subjectDetailFragment_to_professorStudentListFragment, args);
    }

    private void observeViewModel() {
        User currentUser = viewModel.getCurrentUser().getValue();
        if (currentUser != null && !"ADMIN".equals(currentUser.role)) {
            binding.etField1.setFocusable(false);
            binding.etField1.setClickable(false);
            binding.etField2.setFocusable(false);
            binding.etField2.setClickable(false);
            binding.etField4.setFocusable(false);
            binding.etField4.setClickable(false);
            
            binding.btnDelete.setVisibility(View.GONE);
            binding.layoutColorSelector.setVisibility(View.VISIBLE);
            binding.btnSave.setText("Guardar Color");
            binding.btnSave.setVisibility(View.VISIBLE);

            if ("PROFESSOR".equals(currentUser.role)) {
                binding.tvColorLabel.setText("Color Personalizado para tu Horario Semanal");
                binding.btnManageStudents.setVisibility(View.GONE);
            } else {
                binding.tvColorLabel.setText("Color Materia");
                binding.btnManageStudents.setVisibility(View.GONE);

                viewModel.getGradeInfoForStudent(currentUser.id).observe(getViewLifecycleOwner(), gradeInfos -> {
                    if (gradeInfos != null && subject != null && binding != null) {
                        for (StudentGradeInfo info : gradeInfos) {
                            if (info.subject != null && info.subject.id == subject.id && info.enrollment != null) {
                                Enrollment e = info.enrollment;
                                viewModel.getAcademicPeriods().observe(getViewLifecycleOwner(), periods -> {
                                    displayStudentGrades(e, periods);
                                });
                                break;
                            }
                        }
                    }
                });
            }
        } else {
            binding.layoutColorSelector.setVisibility(View.GONE);
            binding.btnDelete.setVisibility(View.VISIBLE);
            binding.btnSave.setText("Guardar Cambios");
            binding.btnSave.setVisibility(View.VISIBLE);
            binding.btnManageStudents.setVisibility(View.GONE);
        }

        boolean canManageMembers = currentUser != null && "ADMIN".equals(currentUser.role);
        if (canManageMembers) {
            binding.btnUnenrollMember.setVisibility(View.VISIBLE);
        } else {
            binding.btnUnenrollMember.setVisibility(View.GONE);
        }

        viewModel.getSelectedSubject().observe(getViewLifecycleOwner(), sub -> {
            if (sub != null) {
                subject = sub;
                binding.etField1.setText(subject.name);

                String codeStr = (subject.code != null && !subject.code.isEmpty()) ? subject.code : ("ASIGN" + (10000 + subject.id));
                if (codeStr.startsWith("ASIGN")) {
                    codeStr = codeStr.substring(5);
                }
                binding.etField2.setText(codeStr);

                String descStr = (subject.description != null) ? subject.description : "";
                binding.etDesc.setText(descStr);

                String sec = (subject.section != null && !subject.section.isEmpty()) ? subject.section : "01";
                String secNum = sec.replace("Sección", "").trim();
                if (secNum.startsWith("0") && secNum.length() > 1) {
                    secNum = secNum.substring(1);
                }
                binding.etField4.setText(secNum);

                binding.btnCopyField1.setOnClickListener(v -> 
                    ClipboardUtils.copyToClipboard(getContext(), "Nombre de materia", binding.etField1.getText().toString()));
                binding.btnCopyField2.setOnClickListener(v -> 
                    ClipboardUtils.copyToClipboard(getContext(), "ID Materia", "ASIGN" + binding.etField2.getText().toString().trim()));
                binding.btnCopyDesc.setOnClickListener(v -> 
                    ClipboardUtils.copyToClipboard(getContext(), "Descripción de materia", binding.etDesc.getText().toString()));

                ModificationLogManager.loadLogTextAsync(requireContext(), "subject_" + subject.id, logText -> {
                    if (binding != null && binding.tvLastModifiedFooter != null) {
                        binding.tvLastModifiedFooter.setText(logText);
                    }
                });

                DialogUtils.hideLoadingOverlay(binding.getRoot());

                if (currentUser != null && !"ADMIN".equals(currentUser.role)) {
                    viewModel.getUserSubjectColor(currentUser.id, subject.id, color -> {
                        if (binding != null) selectColor(color);
                    });

                    if ("STUDENT".equals(currentUser.role)) {
                        viewModel.getGradeInfoForStudent(currentUser.id).observe(getViewLifecycleOwner(), gradeInfos -> {
                            if (gradeInfos != null && binding != null) {
                                for (StudentGradeInfo info : gradeInfos) {
                                    if (info.subject != null && info.subject.id == subject.id && info.enrollment != null) {
                                        Enrollment e = info.enrollment;
                                        viewModel.getAcademicPeriods().observe(getViewLifecycleOwner(), periods -> {
                                            displayStudentGrades(e, periods);
                                        });
                                        break;
                                    }
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    private void displayStudentGrades(Enrollment e, List<com.aula.virtual.data.entity.AcademicPeriod> periods) {
        if (binding == null) return;
        binding.layoutStudentGrades.setVisibility(View.VISIBLE);

        if (periods == null || periods.isEmpty()) {
            ((View) binding.tvDetailP1.getParent()).setVisibility(View.GONE);
            ((View) binding.tvDetailP2.getParent()).setVisibility(View.GONE);
            ((View) binding.tvDetailP3.getParent()).setVisibility(View.GONE);
            ((View) binding.tvDetailP4.getParent()).setVisibility(View.GONE);
            ((View) binding.tvDetailP5.getParent()).setVisibility(View.GONE);
            binding.tvDetailAverage.setText("Sin Calendario");
            binding.tvDetailAverage.setTextColor(ContextCompat.getColor(requireContext(), R.color.bs_warning));
            return;
        }

        int activeCount = periods.size();

        TextView[] textViews = new TextView[]{
            binding.tvDetailP1, binding.tvDetailP2, binding.tvDetailP3, binding.tvDetailP4, binding.tvDetailP5
        };
        Double[] grades = new Double[]{
            e.grade1, e.grade2, e.grade3, e.grade4, e.grade5
        };

        double sum = 0;
        for (int i = 0; i < 5; i++) {
            boolean isActive = (i < activeCount);
            View parentView = (View) textViews[i].getParent();
            if (isActive) {
                parentView.setVisibility(View.VISIBLE);
                textViews[i].setText(formatGrade(grades[i]));
                setGradeColor(textViews[i], grades[i]);
                if (grades[i] != null) sum += grades[i];
            } else {
                parentView.setVisibility(View.GONE);
            }
        }

        double avg = sum / activeCount;
        binding.tvDetailAverage.setText(String.format(Locale.getDefault(), "%.1f", avg));
        setGradeColor(binding.tvDetailAverage, avg);
    }

    private void setGradeColor(TextView tv, Double grade) {
        if (grade == null) {
            tv.setTextColor(ContextCompat.getColor(tv.getContext(), R.color.bs_secondary));
            return;
        }
        int colorRes;
        if (grade <= 3.0) {
            colorRes = R.color.bs_danger;
        } else if (grade <= 6.0) {
            colorRes = R.color.bs_warning;
        } else {
            colorRes = R.color.bs_success;
        }
        tv.setTextColor(ContextCompat.getColor(tv.getContext(), colorRes));
    }

    private String formatGrade(Double grade) {
        if (grade == null) return "-";
        return String.format(Locale.getDefault(), "%.1f", grade);
    }

    private void saveChanges() {
        if (subject != null) {
            User currentUser = viewModel.getCurrentUser().getValue();
            if (currentUser != null && !"ADMIN".equals(currentUser.role)) {
                viewModel.performOnlineAction(() -> {
                    viewModel.updateUserSubjectColor(currentUser.id, subject.id, selectedColor);
                    Toast.makeText(getContext(), "Color guardado para tu horario", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).popBackStack();
                });
                return;
            }

            String name = binding.etField1.getText().toString().trim();
            if (name.isEmpty()) {
                binding.etField1.setError("El nombre es obligatorio");
                return;
            }

            String codeInput = binding.etField2.getText().toString().trim();
            String codeDigits = codeInput;
            if (codeDigits.startsWith("ASIGN")) codeDigits = codeDigits.substring(5);

            if (!codeDigits.matches("\\d{5}")) {
                binding.etField2.setError("ID Materia inválido (debe ingresar exactamente 5 dígitos numéricos)");
                return;
            }
            
            String sectionInput = binding.etField4.getText().toString().trim();
            if (sectionInput.isEmpty()) {
                binding.etField4.setError("La sección es obligatoria");
                return;
            }
            
            String formattedSection = "01";
            try {
                int val = Integer.parseInt(sectionInput);
                if (val < 1 || val > 9) {
                    binding.etField4.setError("Debe ser un número entero entre 1 y 9");
                    return;
                }
                formattedSection = "0" + val;
            } catch (NumberFormatException e) {
                binding.etField4.setError("Debe ser un número del 1 al 9");
                return;
            }

            subject.name = name;
            subject.code = "ASIGN" + codeDigits;
            subject.description = binding.etDesc.getText().toString().trim();
            subject.section = formattedSection;
            
            String authorName = (currentUser != null && currentUser.name != null) ? currentUser.name : "Admin Maestro";
            ModificationLogManager.saveLog(requireContext(), "subject_" + subject.id, authorName);

            viewModel.performOnlineAction(() -> {
                viewModel.updateSubject(subject);
                Toast.makeText(getContext(), "Materia actualizada", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).popBackStack();
            });
        }
    }

    private void popBackToManagementMenu() {
        NavController navController = Navigation.findNavController(requireView());
        if (navController.popBackStack(R.id.adminSubjectListFragment, false)) {
            return;
        }
        if (navController.popBackStack(R.id.adminEnrollmentListFragment, false)) {
            return;
        }
        if (navController.popBackStack(R.id.professorSubjectListFragment, false)) {
            return;
        }
        if (navController.popBackStack(R.id.studentDashboardFragment, false)) {
            return;
        }
        navController.popBackStack();
    }

    private void showDeleteConfirmation() {
        if (subject == null) return;
        String secStr = "Sección " + (subject.section != null ? subject.section : "01");
        String badge = subject.code != null ? subject.code : ("ASIGN" + (10000 + subject.id));
        DialogUtils.showConfirmDeleteDialog(
            requireContext(),
            "Confirmar Eliminación",
            "¿Estás seguro de que deseas eliminar esta materia? Se perderán las inscripciones, notas y publicaciones asociadas de forma permanente.",
            subject.name,
            secStr + " • Asignatura",
            badge,
            null,
            R.drawable.ic_open_book,
            () -> {
                viewModel.performOnlineAction(() -> {
                    viewModel.deleteSubject(subject);
                    Toast.makeText(getContext(), "Materia eliminada", Toast.LENGTH_SHORT).show();
                    popBackToManagementMenu();
                });
            }
        );
    }

    private void showUnenrollMemberDialog() {
        if (subject == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_searchable_list, null);
        MaterialAlertDialogBuilder builder = DialogUtils.createMaterialDialog(requireContext(), "Desinscribir Integrante");
        builder.setView(dialogView);
        builder.setNegativeButton("Cerrar", null);

        AlertDialog dialog = builder.create();

        EditText etSearch = dialogView.findViewById(R.id.etSearchDialog);
        RecyclerView rvList = dialogView.findViewById(R.id.rvDialogList);
        rvList.setLayoutManager(new LinearLayoutManager(requireContext()));

        etSearch.setHint("Buscar integrante...");

        viewModel.getStudentsBySubject(subject.id).observe(getViewLifecycleOwner(), studentGrades -> {
            List<StudentGradeInfo> list = new ArrayList<>();

            Runnable updateAdapter = () -> {
                UnenrollAdapter adapter = new UnenrollAdapter(list, info -> {
                    boolean isProf = (info.enrollment == null && info.student != null);
                    String studentName = (info.student != null && info.student.name != null) ? info.student.name : "este integrante";
                    String msg = isProf ?
                        "¿Estás seguro de desinscribir al profesor \"" + studentName + "\" de la materia \"" + subject.name + "\"? Se eliminará la asignación del profesor." :
                        "¿Estás seguro de desinscribir a \"" + studentName + "\" de la materia \"" + subject.name + "\"? Se eliminará su inscripción.";

                    new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Desinscribir Integrante")
                        .setMessage(msg)
                        .setPositiveButton("Desinscribir", (confirmDialog, which) -> {
                            viewModel.performOnlineAction(() -> {
                                if (isProf) {
                                    subject.professorId = null;
                                    viewModel.updateSubject(subject);
                                    viewModel.updateSchedulesProfessorBySubject(subject.id, 0);
                                    Toast.makeText(getContext(), "Profesor desinscrito exitosamente", Toast.LENGTH_SHORT).show();
                                    viewModel.refreshData();
                                    dialog.dismiss();
                                } else if (info.enrollment != null) {
                                    viewModel.deleteEnrollment(info.enrollment);
                                    Toast.makeText(getContext(), "Integrante desinscrito exitosamente", Toast.LENGTH_SHORT).show();
                                    viewModel.getStudentsBySubject(subject.id);
                                }
                            });
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
                });

                rvList.setAdapter(adapter);

                etSearch.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        adapter.filter(s.toString());
                    }
                    @Override public void afterTextChanged(Editable s) {}
                });
            };

            if (studentGrades != null) {
                list.addAll(studentGrades);
            }

            if (subject.professorId != null && subject.professorId > 0) {
                viewModel.getUserById(subject.professorId, prof -> {
                    if (prof != null) {
                        StudentGradeInfo profInfo = new StudentGradeInfo();
                        profInfo.student = prof;
                        profInfo.enrollment = null;
                        profInfo.subject = subject;
                        if (list.stream().noneMatch(i -> i.enrollment == null && i.student != null && i.student.id == prof.id)) {
                            list.add(0, profInfo);
                        }
                    }
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(updateAdapter);
                    }
                });
            } else {
                updateAdapter.run();
            }
        });

        dialog.show();
    }

    static class UnenrollAdapter extends RecyclerView.Adapter<UnenrollAdapter.ViewHolder> {
        private final List<StudentGradeInfo> originalList;
        private List<StudentGradeInfo> filteredList;
        private final OnUnenrollClickListener listener;

        interface OnUnenrollClickListener {
            void onUnenroll(StudentGradeInfo info);
        }

        UnenrollAdapter(List<StudentGradeInfo> list, OnUnenrollClickListener listener) {
            this.originalList = list;
            this.filteredList = new ArrayList<>(list);
            this.listener = listener;
        }

        void filter(String query) {
            String q = query.toLowerCase().trim();
            if (q.isEmpty()) {
                filteredList = new ArrayList<>(originalList);
            } else {
                filteredList = originalList.stream().filter(info -> {
                    if (info == null) return false;
                    String name = "";
                    String carnet = "";
                    if (info.student != null) {
                        name = info.student.name != null ? info.student.name.toLowerCase() : "";
                        carnet = info.student.carnet != null ? info.student.carnet.toLowerCase() : "";
                    } else if (info.enrollment != null) {
                        name = "alumno " + info.enrollment.studentId;
                    }
                    return name.contains(q) || carnet.contains(q);
                }).collect(Collectors.toList());
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout container = new LinearLayout(parent.getContext());
            container.setOrientation(LinearLayout.HORIZONTAL);
            container.setGravity(Gravity.CENTER_VERTICAL);
            int paddingH = (int) (12 * parent.getContext().getResources().getDisplayMetrics().density);
            int paddingV = (int) (8 * parent.getContext().getResources().getDisplayMetrics().density);
            container.setPadding(paddingH, paddingV, paddingH, paddingV);

            LinearLayout textLayout = new LinearLayout(parent.getContext());
            textLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            textLayout.setLayoutParams(lpText);

            TextView tvName = new TextView(parent.getContext());
            tvName.setTextSize(15f);
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setTextColor(ThemeHelper.isDarkMode(parent.getContext()) ? 0xFFFFFFFF : 0xFF000000);

            TextView tvSub = new TextView(parent.getContext());
            tvSub.setTextSize(13f);
            tvSub.setTextColor(0xFF888888);

            textLayout.addView(tvName);
            textLayout.addView(tvSub);

            MaterialButton btn = new MaterialButton(parent.getContext(), null, android.R.attr.borderlessButtonStyle);
            btn.setText("Desinscribir");
            btn.setTextSize(12f);
            btn.setAllCaps(false);
            btn.setTextColor(0xFFFFFFFF);
            btn.setBackgroundTintList(ColorStateList.valueOf(0xFFDC3545));
            int btnPad = (int) (8 * parent.getContext().getResources().getDisplayMetrics().density);
            btn.setPadding(btnPad, 0, btnPad, 0);

            container.addView(textLayout);
            container.addView(btn);

            container.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new ViewHolder(container, tvName, tvSub, btn);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StudentGradeInfo info = filteredList.get(position);
            if (info == null) return;

            String name = "Alumno";
            String carnetStr = "";
            boolean isProf = (info.enrollment == null && info.student != null);

            if (info.student != null) {
                if (info.student.name != null && !info.student.name.isEmpty()) name = info.student.name;
                if (info.student.carnet != null) carnetStr = info.student.carnet;
            } else if (info.enrollment != null) {
                name = "Alumno ID: " + info.enrollment.studentId;
            }

            holder.tvName.setText(name);
            String subtext;
            if (isProf) {
                subtext = "Profesor Asignado";
            } else {
                subtext = carnetStr;
                if (subtext.isEmpty() && info.enrollment != null) {
                    subtext = "Inscripción #" + info.enrollment.id;
                }
            }
            holder.tvSub.setText(subtext);
            holder.btnUnenroll.setOnClickListener(v -> listener.onUnenroll(info));
        }

        @Override
        public int getItemCount() {
            return filteredList.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvSub;
            MaterialButton btnUnenroll;

            ViewHolder(View itemView, TextView tvName, TextView tvSub, MaterialButton btnUnenroll) {
                super(itemView);
                this.tvName = tvName;
                this.tvSub = tvSub;
                this.btnUnenroll = btnUnenroll;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
