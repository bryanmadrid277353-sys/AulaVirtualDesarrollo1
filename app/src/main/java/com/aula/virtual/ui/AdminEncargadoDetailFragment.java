package com.aula.virtual.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
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
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.aula.virtual.MainActivity;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.Student;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentAdminEncargadoDetailBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminEncargadoDetailFragment extends Fragment {
    private FragmentAdminEncargadoDetailBinding binding;
    private MainViewModel viewModel;
    private User encargado;
    private StudentRecordAdapter adapter;
    private List<Student> encargadoStudents = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminEncargadoDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        binding.btnBackEncargadoDetail.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToHome();
            } else {
                Navigation.findNavController(v).navigateUp();
            }
        });

        int encargadoId = getArguments() != null ? getArguments().getInt("encargadoId", -1) : -1;

        adapter = new StudentRecordAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

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

        if (encargadoId != -1) {
            viewModel.fetchUserById(encargadoId);
            viewModel.getSelectedUser().observe(getViewLifecycleOwner(), user -> {
                if (user != null) {
                    encargado = user;
                    if (binding.tvDetailTitle != null) {
                        binding.tvDetailTitle.setText("Gestión del encargado");
                    }
                    binding.tvEncargadoName.setText(user.name);
                    binding.tvEncargadoCarnet.setText(user.carnet != null ? user.carnet : "-");
                    binding.tvEncargadoInitials.setText(getInitials(user.name));
                    loadEncargadoStudents(user.id);

                    binding.btnHeaderDetail.setOnClickListener(v -> {
                        Bundle args = new Bundle();
                        args.putInt("adminId", user.id);
                        Navigation.findNavController(requireView()).navigate(R.id.action_adminEncargadoDetailFragment_to_adminDetailFragment, args);
                    });
                }
            });
        }

        binding.btnAddStudentToEncargado.setOnClickListener(v -> {
            if (encargado != null) {
                showAddStudentDialog(encargado);
            }
        });

        adapter.setOnItemClickListener(student -> {
            Bundle args = new Bundle();
            args.putInt("studentId", student.id);
            Navigation.findNavController(view).navigate(R.id.action_adminEncargadoDetailFragment_to_adminEnrollmentListFragment, args);
        });
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

    private void setupSearch() {
        String[] options = {"Todo", "Carnet", "Nombre"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, options);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFilter.setAdapter(spinnerAdapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { applyFilter(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadEncargadoStudents(int encargadoId) {
        viewModel.fetchStudentsForEncargado(encargadoId);
        viewModel.getMyStudents().observe(getViewLifecycleOwner(), students -> {
            encargadoStudents = students != null ? students : new ArrayList<>();
            applyFilter();
        });
    }

    private void applyFilter() {
        String query = binding.etSearch.getText().toString().toLowerCase().trim();
        String filterType = binding.spinnerFilter.getSelectedItem() != null ? binding.spinnerFilter.getSelectedItem().toString() : "Todo";

        if (query.isEmpty()) {
            adapter.setStudents(encargadoStudents);
            binding.tvRecordCount.setText(encargadoStudents.size() + (encargadoStudents.size() == 1 ? " alumno" : " alumnos"));
            binding.tvNoStudentsMessage.setVisibility(encargadoStudents.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }

        List<Student> filtered = encargadoStudents.stream().filter(s -> {
            if (s == null) return false;
            String name = s.name != null ? s.name.toLowerCase() : "";
            String carnet = s.carnet != null ? s.carnet.toLowerCase() : "";

            if (filterType.equals("Todo")) {
                return name.contains(query) || carnet.contains(query);
            } else if (filterType.equals("Carnet")) {
                return carnet.contains(query);
            } else if (filterType.equals("Nombre")) {
                return name.contains(query);
            }
            return false;
        }).collect(Collectors.toList());

        adapter.setStudents(filtered);
        binding.tvRecordCount.setText(filtered.size() + (filtered.size() == 1 ? " alumno" : " alumnos"));
        binding.tvNoStudentsMessage.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAddStudentDialog(User targetEncargado) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_student_to_encargado, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvEncargadoName = dialogView.findViewById(R.id.tvEncargadoName);
        EditText etStudentName = dialogView.findViewById(R.id.etStudentName);
        EditText etStudentCarnet = dialogView.findViewById(R.id.etStudentCarnet);
        View btnSelectGrade = dialogView.findViewById(R.id.btnSelectGrade);
        TextView tvSelectedGrade = dialogView.findViewById(R.id.tvSelectedGrade);
        View btnCancel = dialogView.findViewById(R.id.btnCancel);
        View btnAddStudent = dialogView.findViewById(R.id.btnAddStudent);

        tvEncargadoName.setText(targetEncargado.name != null ? targetEncargado.name : "-");

        TextView tvNameError = dialogView.findViewById(R.id.tvNameError);
        TextView tvCarnetError = dialogView.findViewById(R.id.tvCarnetError);
        TextView tvGradeError = dialogView.findViewById(R.id.tvGradeError);

        final String[] gradeOptions = {
            "1º Primaria", "2º Primaria", "3º Primaria", "4º Primaria",
            "5º Primaria", "6º Primaria", "7º Primaria", "8º Primaria", "9º Primaria",
            "1º Bachillerato", "2º Bachillerato", "3º Bachillerato"
        };
        final String[] selectedGrade = {null};

        btnSelectGrade.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Seleccionar Grado y Nivel")
                    .setItems(gradeOptions, (d, which) -> {
                        selectedGrade[0] = gradeOptions[which];
                        tvSelectedGrade.setText(selectedGrade[0]);
                        tvSelectedGrade.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                        if (tvGradeError != null) tvGradeError.setVisibility(View.GONE);
                    }).show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAddStudent.setOnClickListener(v1 -> {
            String name = etStudentName.getText().toString().trim();
            String carnet = etStudentCarnet.getText().toString().trim();

            boolean isValid = true;

            if (name.isEmpty()) {
                if (tvNameError != null) { tvNameError.setText("Obligatorio"); tvNameError.setVisibility(View.VISIBLE); }
                isValid = false;
            } else {
                if (tvNameError != null) tvNameError.setVisibility(View.GONE);
            }

            if (!carnet.matches("\\d{7}")) {
                if (tvCarnetError != null) { tvCarnetError.setText("Obligatorio"); tvCarnetError.setVisibility(View.VISIBLE); }
                isValid = false;
            } else {
                if (tvCarnetError != null) tvCarnetError.setVisibility(View.GONE);
            }

            if (selectedGrade[0] == null) {
                if (tvGradeError != null) { tvGradeError.setText("Obligatorio"); tvGradeError.setVisibility(View.VISIBLE); }
                isValid = false;
            } else {
                if (tvGradeError != null) tvGradeError.setVisibility(View.GONE);
            }

            if (isValid) {
                String fullCarnet = "EST" + carnet;
                String grade = selectedGrade[0];
                Student newStudent = new Student(fullCarnet, name, grade, targetEncargado.id);
                viewModel.performOnlineAction(() -> {
                    viewModel.insertStudent(newStudent, new retrofit2.Callback<Student>() {
                        @Override
                        public void onResponse(@NonNull retrofit2.Call<Student> call, @NonNull retrofit2.Response<Student> response) {
                            Toast.makeText(getContext(), "Estudiante registrado exitosamente", Toast.LENGTH_SHORT).show();
                            loadEncargadoStudents(targetEncargado.id);
                            dialog.dismiss();
                        }

                        @Override
                        public void onFailure(@NonNull retrofit2.Call<Student> call, @NonNull Throwable t) {
                            Toast.makeText(getContext(), "Error al guardar estudiante", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }
        });

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
