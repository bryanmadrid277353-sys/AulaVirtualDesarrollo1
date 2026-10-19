package com.aula.virtual.ui;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentAdminDashboardBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminProfessorListFragment extends Fragment {
    private FragmentAdminDashboardBinding binding;
    private MainViewModel viewModel;
    private StudentAdapter adapter;
    private List<User> allProfessors = new ArrayList<>();

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

        binding.tvTitle.setText("Listado de Profesores");
        adapter = new StudentAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        setupSearch();

        viewModel.getAllProfessors().observe(getViewLifecycleOwner(), professors -> {
            allProfessors = professors;
            applyFilter();
        });

        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        viewModel.refreshData();

        adapter.setOnItemClickListener(prof -> {
            Bundle args = new Bundle();
            args.putInt("professorId", prof.id);
            Navigation.findNavController(view).navigate(R.id.action_adminProfessorListFragment_to_professorSubjectListFragment, args);
        });

        binding.btnAdd.setOnClickListener(v -> showAddProfessorDialog());
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

    private void applyFilter() {
        String query = binding.etSearch.getText().toString().toLowerCase().trim();
        String filterType = binding.spinnerFilter.getSelectedItem() != null ? binding.spinnerFilter.getSelectedItem().toString() : "Todo";

        if (query.isEmpty()) {
            adapter.setStudents(allProfessors);
            binding.tvRecordCount.setText(allProfessors.size() + (allProfessors.size() == 1 ? " registro" : " registros"));
            return;
        }

        List<User> filtered = allProfessors.stream().filter(p -> {
            if (p == null) return false;
            String name = p.name != null ? p.name.toLowerCase() : "";
            String carnet = p.carnet != null ? p.carnet.toLowerCase() : "";

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
    }

    private void showAddProfessorDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_professor, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etCarnet = dialogView.findViewById(R.id.etCarnet);
        EditText etPass = dialogView.findViewById(R.id.etPass);
        TextView tvStrength = dialogView.findViewById(R.id.tvPasswordStrength);
        ImageView btnTogglePass = dialogView.findViewById(R.id.btnTogglePassword);
        EditText etAddress = dialogView.findViewById(R.id.etAddress);
        EditText etEmail = dialogView.findViewById(R.id.etEmail);
        View btnCancel = dialogView.findViewById(R.id.btnCancelDialog);
        View btnAdd = dialogView.findViewById(R.id.btnAddDialog);

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
            String pass = etPass.getText().toString().trim();

            boolean isValid = true;
            if (name.isEmpty()) {
                etName.setError("El nombre es obligatorio");
                isValid = false;
            }
            if (carnet.length() != 6) {
                etCarnet.setError("Debe ingresar exactamente 6 dígitos");
                isValid = false;
            }
            if (ValidationUtils.getPasswordStrength(pass) == ValidationUtils.PasswordStrength.WEAK) {
                etPass.setError("Contraseña muy débil");
                isValid = false;
            }

            if (isValid) {
                String fullCarnet = "PROF" + carnet;
                String address = etAddress.getText().toString().trim();
                String email = etEmail.getText().toString().trim();

                viewModel.performOnlineAction(() -> {
                    User newProf = new User(fullCarnet, name, pass, "PROFESSOR");
                    newProf.address = address.isEmpty() ? null : address;
                    newProf.personal_email = email.isEmpty() ? null : email;
                    viewModel.insertUser(newProf);
                    dialog.dismiss();
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
