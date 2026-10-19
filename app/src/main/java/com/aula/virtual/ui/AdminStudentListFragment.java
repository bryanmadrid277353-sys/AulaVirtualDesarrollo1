package com.aula.virtual.ui;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.aula.virtual.MainActivity;
import com.aula.virtual.R;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;
import com.aula.virtual.data.entity.Student;
import com.aula.virtual.data.entity.User;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.aula.virtual.databinding.FragmentAdminDashboardBinding;

public class AdminStudentListFragment extends Fragment {
    private FragmentAdminDashboardBinding binding;
    private MainViewModel viewModel;
    private StudentAdapter adapter;
    private StudentRecordAdapter studentRecordAdapter;
    private List<User> allEncargados = new ArrayList<>();
    private List<Student> allStudentRecords = new ArrayList<>();
    private boolean isEncargadoMode = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        binding.btnBackList.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToHome();
            } else {
                Navigation.findNavController(v).navigateUp();
            }
        });

        String filterRole = (getArguments() != null) ? getArguments().getString("filterRole", "ENCARGADO") : "ENCARGADO";
        isEncargadoMode = "ENCARGADO".equals(filterRole);

        adapter = new StudentAdapter();
        studentRecordAdapter = new StudentRecordAdapter();

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        if (isEncargadoMode) {
            binding.recyclerView.setAdapter(adapter);
            binding.tvHeaderTitle.setText("Encargados");
            binding.tvTitle.setText("Listado de encargados");
            binding.tvSubtitle.setText("Gestiona los registros de familias y tutores de tus estudiantes.");
            binding.btnAdd.setText("+ Añadir encargado");
        } else {
            binding.recyclerView.setAdapter(studentRecordAdapter);
            binding.tvHeaderTitle.setText("Alumnos");
            binding.tvTitle.setText("Listado de alumnos");
            binding.tvSubtitle.setText("Gestiona los registros de estudiantes de primaria.");
            binding.btnAdd.setText("+ Añadir alumno");
        }

        setupSearch();

        viewModel.getAllEncargados().observe(getViewLifecycleOwner(), users -> {
            allEncargados = users != null ? users : new ArrayList<>();
            if (isEncargadoMode) applyFilter();
        });

        if (!isEncargadoMode) {
            viewModel.getAllStudentRecords().observe(getViewLifecycleOwner(), students -> {
                allStudentRecords = students != null ? students : new ArrayList<>();
                applyFilter();
            });
        }

        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        viewModel.refreshData(); // Force refresh when entering list

        adapter.setOnItemClickListener(userItem -> {
            Bundle args = new Bundle();
            args.putInt("encargadoId", userItem.id);
            Navigation.findNavController(view).navigate(R.id.action_adminStudentListFragment_to_adminEncargadoDetailFragment, args);
        });

        studentRecordAdapter.setOnItemClickListener(studentItem -> {
            Bundle args = new Bundle();
            args.putInt("studentId", studentItem.id);
            Navigation.findNavController(view).navigate(R.id.action_adminStudentListFragment_to_adminEnrollmentListFragment, args);
        });

        binding.btnAdd.setOnClickListener(v -> {
            if (isEncargadoMode) {
                showAddUserDialog();
            } else {
                showAddStudentGeneralDialog();
            }
        });
    }

    private void setupSearch() {
        String[] options = {"Todo", "Carnet", "Nombre"};
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
        String query = binding.etSearch.getText().toString().toLowerCase().trim();
        String filterType = binding.spinnerFilter.getSelectedItem() != null ? binding.spinnerFilter.getSelectedItem().toString() : "Todo";

        if (isEncargadoMode) {
            if (query.isEmpty()) {
                adapter.setStudents(allEncargados);
                binding.tvRecordCount.setText(allEncargados.size() + (allEncargados.size() == 1 ? " registro" : " registros"));
                return;
            }

            List<User> filtered = allEncargados.stream().filter(s -> {
                if (s == null) return false;
                String name = s.name != null ? s.name.toLowerCase() : "";
                String carnet = s.carnet != null ? s.carnet.toLowerCase() : "";

                if (filterType.equals("Todo")) {
                    return name.contains(query) || carnet.contains(query);
                } else if (filterType.equals("Carnet")) {
                    return carnet.contains(query);
                } else {
                    return name.contains(query);
                }
            }).collect(Collectors.toList());

            adapter.setStudents(filtered);
            binding.tvRecordCount.setText(filtered.size() + (filtered.size() == 1 ? " registro" : " registros"));
        } else {
            if (query.isEmpty()) {
                studentRecordAdapter.setStudents(allStudentRecords);
                binding.tvRecordCount.setText(allStudentRecords.size() + (allStudentRecords.size() == 1 ? " registro" : " registros"));
                return;
            }

            List<Student> filtered = allStudentRecords.stream().filter(s -> {
                if (s == null) return false;
                String name = s.name != null ? s.name.toLowerCase() : "";
                String carnet = s.carnet != null ? s.carnet.toLowerCase() : "";

                if (filterType.equals("Todo")) {
                    return name.contains(query) || carnet.contains(query);
                } else if (filterType.equals("Carnet")) {
                    return carnet.contains(query);
                } else {
                    return name.contains(query);
                }
            }).collect(Collectors.toList());

            studentRecordAdapter.setStudents(filtered);
            binding.tvRecordCount.setText(filtered.size() + (filtered.size() == 1 ? " registro" : " registros"));
        }
    }

    private void showAddUserDialog() {
        String filterRole = (getArguments() != null) ? getArguments().getString("filterRole", "ENCARGADO") : "ENCARGADO";
        boolean isEncargado = "ENCARGADO".equals(filterRole);
        String dialogTitle = isEncargado ? "Añadir encargado del estudiante" : "Añadir Alumno";

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_encargado, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etCarnet = dialogView.findViewById(R.id.etCarnet);
        EditText etPass = dialogView.findViewById(R.id.etPass);
        TextView tvStrength = dialogView.findViewById(R.id.tvPasswordStrength);
        ImageView btnTogglePass = dialogView.findViewById(R.id.btnTogglePassword);
        EditText etAddress = dialogView.findViewById(R.id.etAddress);
        EditText etEmail = dialogView.findViewById(R.id.etEmail);
        View btnCancel = dialogView.findViewById(R.id.btnCancelDialog);
        View btnAdd = dialogView.findViewById(R.id.btnAddDialog);

        tvTitle.setText(dialogTitle);

        btnTogglePass.setOnClickListener(v -> {
            if (etPass.getTransformationMethod() instanceof PasswordTransformationMethod) {
                etPass.setTransformationMethod(null);
                btnTogglePass.setImageResource(R.drawable.ic_visibility);
            } else {
                etPass.setTransformationMethod(PasswordTransformationMethod.getInstance());
                btnTogglePass.setImageResource(R.drawable.ic_visibility_off);
            }
            etPass.setSelection(etPass.getText().length());
        });

        etPass.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    tvStrength.setVisibility(View.GONE);
                } else {
                    tvStrength.setVisibility(View.VISIBLE);
                    ValidationUtils.PasswordStrength strength = ValidationUtils.getPasswordStrength(s.toString());
                    tvStrength.setText(strength.label);
                    tvStrength.setTextColor(strength.color);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v1 -> {
            String name = etName.getText().toString().trim();
            String carnet = etCarnet.getText().toString().trim();
            String pass = etPass.getText().toString();
            
            boolean isValid = true;

            if (name.isEmpty()) {
                etName.setError("El nombre es obligatorio");
                isValid = false;
            }
            if (!carnet.matches("\\d{7}")) {
                etCarnet.setError("Carnet inválido (7 dígitos numéricos)");
                isValid = false;
            }
            if (isEncargado && ValidationUtils.getPasswordStrength(pass) == ValidationUtils.PasswordStrength.WEAK) {
                etPass.setError("Contraseña muy débil");
                isValid = false;
            }

            if (isValid) {
                if (isEncargado) {
                    String fullCarnet = "ENC" + carnet;
                    String address = etAddress.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    viewModel.performOnlineAction(() -> {
                        User newUser = new User(fullCarnet, name, pass, "ENCARGADO");
                        newUser.address = address.isEmpty() ? null : address;
                        newUser.personal_email = email.isEmpty() ? null : email;
                        viewModel.insertUser(newUser);
                        dialog.dismiss();
                    });
                } else {
                    String fullCarnet = "EST" + carnet;
                    String grade = "1º Primaria";
                    Student newStudent = new Student(fullCarnet, name, grade, 1);
                    viewModel.performOnlineAction(() -> {
                        viewModel.insertStudent(newStudent, new retrofit2.Callback<Student>() {
                            @Override
                            public void onResponse(@NonNull retrofit2.Call<Student> call, @NonNull retrofit2.Response<Student> response) {
                                Toast.makeText(getContext(), "Alumno registrado exitosamente", Toast.LENGTH_SHORT).show();
                                viewModel.refreshData();
                                dialog.dismiss();
                            }

                            @Override
                            public void onFailure(@NonNull retrofit2.Call<Student> call, @NonNull Throwable t) {
                                Toast.makeText(getContext(), "Error al guardar alumno", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                }
            }
        });

        dialog.show();
    }

    private void showAddStudentGeneralDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_student_general, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etStudentName = dialogView.findViewById(R.id.etStudentName);
        EditText etStudentCarnet = dialogView.findViewById(R.id.etStudentCarnet);
        View btnSelectGrade = dialogView.findViewById(R.id.btnSelectGrade);
        TextView tvSelectedGrade = dialogView.findViewById(R.id.tvSelectedGrade);
        View btnSelectEncargado = dialogView.findViewById(R.id.btnSelectEncargado);
        TextView tvSelectedEncargado = dialogView.findViewById(R.id.tvSelectedEncargado);
        View btnCancel = dialogView.findViewById(R.id.btnCancelDialog);
        View btnAdd = dialogView.findViewById(R.id.btnAddDialog);

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
                    }).show();
        });

        final Integer[] selectedEncargadoId = {null};
        btnSelectEncargado.setOnClickListener(v -> {
            if (allEncargados == null || allEncargados.isEmpty()) {
                Toast.makeText(requireContext(), "Cargando lista de encargados...", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] encNames = new String[allEncargados.size()];
            for (int i = 0; i < allEncargados.size(); i++) {
                User enc = allEncargados.get(i);
                String carnet = enc.carnet != null ? enc.carnet : "-";
                encNames[i] = enc.name + " (" + carnet + ")";
            }

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Seleccionar Encargado Asignado")
                    .setItems(encNames, (d, which) -> {
                        User chosen = allEncargados.get(which);
                        selectedEncargadoId[0] = chosen.id;
                        tvSelectedEncargado.setText(chosen.name);
                        tvSelectedEncargado.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                    }).show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v1 -> {
            String name = etStudentName.getText().toString().trim();
            String carnet = etStudentCarnet.getText().toString().trim();

            boolean isValid = true;

            if (name.isEmpty()) {
                etStudentName.setError("El nombre es obligatorio");
                isValid = false;
            }
            if (!carnet.matches("\\d{7}")) {
                etStudentCarnet.setError("Carnet inválido (7 dígitos numéricos)");
                isValid = false;
            }
            if (selectedGrade[0] == null) {
                Toast.makeText(requireContext(), "Seleccione un grado y nivel", Toast.LENGTH_SHORT).show();
                isValid = false;
            }

            int encId = (selectedEncargadoId[0] != null) ? selectedEncargadoId[0] : (allEncargados != null && !allEncargados.isEmpty() ? allEncargados.get(0).id : 1);

            if (isValid) {
                String fullCarnet = "EST" + carnet;
                String grade = selectedGrade[0];
                Student newStudent = new Student(fullCarnet, name, grade, encId);
                viewModel.performOnlineAction(() -> {
                    viewModel.insertStudent(newStudent, new retrofit2.Callback<Student>() {
                        @Override
                        public void onResponse(@NonNull retrofit2.Call<Student> call, @NonNull retrofit2.Response<Student> response) {
                            Toast.makeText(getContext(), "Alumno registrado exitosamente", Toast.LENGTH_SHORT).show();
                            viewModel.refreshData();
                            dialog.dismiss();
                        }

                        @Override
                        public void onFailure(@NonNull retrofit2.Call<Student> call, @NonNull Throwable t) {
                            Toast.makeText(getContext(), "Error al guardar alumno", Toast.LENGTH_SHORT).show();
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
