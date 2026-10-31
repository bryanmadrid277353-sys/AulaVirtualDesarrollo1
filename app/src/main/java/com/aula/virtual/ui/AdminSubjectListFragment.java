package com.aula.virtual.ui;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.aula.virtual.R;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.databinding.FragmentAdminDashboardBinding;

public class AdminSubjectListFragment extends Fragment {
    private FragmentAdminDashboardBinding binding;
    private MainViewModel viewModel;
    private SubjectAdapter adapter;
    private List<Subject> allSubjects = new ArrayList<>();

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

        binding.tvHeaderTitle.setText("Materias");
        binding.tvTitle.setText("Listado de Materias");
        binding.tvSubtitle.setText("Gestiona el catálogo de materias y asignaturas del ciclo lectivo.");
        adapter = new SubjectAdapter();
        adapter.setManageButtonText("Gestionar");
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

        viewModel.getAllSubjects().observe(getViewLifecycleOwner(), subjects -> {
            allSubjects = subjects;
            applyFilter();
        });

        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        viewModel.refreshData();

        adapter.setOnItemClickListener(subject -> {
            Bundle args = new Bundle();
            args.putInt("subjectId", subject.id);
            Navigation.findNavController(view).navigate(R.id.action_adminSubjectListFragment_to_subjectBlogFragment, args);
        });

        binding.btnAdd.setOnClickListener(v -> showAddSubjectDialog());
    }

    private void setupSearch() {
        String[] options = {"Todo", "Nombre"};
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
        if (binding == null || !isAdded()) return;
        String query = binding.etSearch.getText().toString().toLowerCase().trim();
        String filterType = binding.spinnerFilter.getSelectedItem() != null ? binding.spinnerFilter.getSelectedItem().toString() : "Todo";

        if (query.isEmpty()) {
            adapter.setSubjects(allSubjects);
            binding.tvRecordCount.setText(allSubjects.size() + (allSubjects.size() == 1 ? " registro" : " registros"));
            return;
        }

        List<Subject> filtered = allSubjects.stream().filter(s -> {
            if (s == null) return false;
            String name = s.name != null ? s.name.toLowerCase() : "";

            if (filterType.equals("Todo") || filterType.equals("Nombre")) {
                return name.contains(query);
            }
            return false;
        }).collect(Collectors.toList());

        adapter.setSubjects(filtered);
        binding.tvRecordCount.setText(filtered.size() + (filtered.size() == 1 ? " registro" : " registros"));
    }

    private void showAddSubjectDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_subject, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etCodeDigits = dialogView.findViewById(R.id.etCodeDigits);
        EditText etDesc = dialogView.findViewById(R.id.etDesc);
        EditText etSection = dialogView.findViewById(R.id.etSection);
        View btnCancel = dialogView.findViewById(R.id.btnCancelDialog);
        View btnAdd = dialogView.findViewById(R.id.btnAddDialog);

        TextView tvNameError = dialogView.findViewById(R.id.tvNameError);
        TextView tvCodeError = dialogView.findViewById(R.id.tvCodeError);
        TextView tvSectionError = dialogView.findViewById(R.id.tvSectionError);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v1 -> {
            String name = etName.getText().toString().trim();
            String codeDigits = etCodeDigits.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            String sectionStr = etSection.getText().toString().trim();

            boolean isValid = true;
            if (name.isEmpty()) {
                if (tvNameError != null) { tvNameError.setText("Obligatorio"); tvNameError.setVisibility(View.VISIBLE); }
                isValid = false;
            } else {
                if (tvNameError != null) tvNameError.setVisibility(View.GONE);
            }

            if (!codeDigits.matches("\\d{5}")) {
                if (tvCodeError != null) { tvCodeError.setText("Obligatorio"); tvCodeError.setVisibility(View.VISIBLE); }
                isValid = false;
            } else {
                if (tvCodeError != null) tvCodeError.setVisibility(View.GONE);
            }

            String finalSection = "01";
            if (sectionStr.isEmpty()) {
                if (tvSectionError != null) { tvSectionError.setText("Obligatorio"); tvSectionError.setVisibility(View.VISIBLE); }
                isValid = false;
            } else {
                try {
                    int val = Integer.parseInt(sectionStr);
                    if (val < 1 || val > 9) {
                        if (tvSectionError != null) { tvSectionError.setText("Obligatorio"); tvSectionError.setVisibility(View.VISIBLE); }
                        isValid = false;
                    } else {
                        finalSection = "0" + val;
                        if (tvSectionError != null) tvSectionError.setVisibility(View.GONE);
                    }
                } catch (NumberFormatException e) {
                    if (tvSectionError != null) { tvSectionError.setText("Obligatorio"); tvSectionError.setVisibility(View.VISIBLE); }
                    isValid = false;
                }
            }

            if (isValid) {
                String fullCode = "ASIGN" + codeDigits;
                String sectionParam = finalSection;
                viewModel.performOnlineAction(() -> {
                    Subject newSub = new Subject(fullCode, name, desc, null);
                    newSub.section = sectionParam;
                    viewModel.insertSubject(newSub);
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
