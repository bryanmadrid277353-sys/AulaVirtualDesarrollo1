package com.aula.virtual.ui;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.Facility;
import com.aula.virtual.databinding.FragmentAdminDashboardBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminFacilityListFragment extends Fragment {
    private FragmentAdminDashboardBinding binding;
    private MainViewModel viewModel;
    private FacilityAdapter adapter;
    private List<Facility> allFacilities = new ArrayList<>();

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

        binding.tvTitle.setText("Listado de Instalaciones");
        adapter = new FacilityAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        setupSearch();

        viewModel.getAllFacilities().observe(getViewLifecycleOwner(), facilities -> {
            allFacilities = facilities;
            applyFilter();
        });

        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        adapter.setOnItemClickListener(facility -> {
            Bundle args = new Bundle();
            args.putInt("facilityId", facility.id);
            Navigation.findNavController(view).navigate(R.id.action_adminFacilityListFragment_to_adminFacilitySchedulesFragment, args);
        });

        binding.btnAdd.setOnClickListener(v -> showAddFacilityDialog());
    }

    private void setupSearch() {
        String[] options = {"Todo", "Nombre", "Tipo"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, options);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFilter.setAdapter(spinnerAdapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void applyFilter() {
        String query = binding.etSearch.getText().toString().toLowerCase().trim();
        String filterType = binding.spinnerFilter.getSelectedItem() != null ? binding.spinnerFilter.getSelectedItem().toString() : "Todo";

        if (query.isEmpty()) {
            adapter.setFacilities(allFacilities);
            binding.tvRecordCount.setText(allFacilities.size() + (allFacilities.size() == 1 ? " registro" : " registros"));
            return;
        }

        List<Facility> filtered = allFacilities.stream().filter(f -> {
            if (f == null) return false;
            String name = f.name != null ? f.name.toLowerCase() : "";
            String type = f.type != null ? f.type.toLowerCase() : "";

            if (filterType.equals("Todo")) return name.contains(query) || type.contains(query);
            else if (filterType.equals("Nombre")) return name.contains(query);
            else return type.contains(query);
        }).collect(Collectors.toList());

        adapter.setFacilities(filtered);
        binding.tvRecordCount.setText(filtered.size() + (filtered.size() == 1 ? " registro" : " registros"));
    }

    private void showAddFacilityDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_facility, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etType = dialogView.findViewById(R.id.etType);
        EditText etDesc = dialogView.findViewById(R.id.etDesc);
        View btnCancel = dialogView.findViewById(R.id.btnCancelDialog);
        View btnAdd = dialogView.findViewById(R.id.btnAddDialog);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v1 -> {
            String name = etName.getText().toString().trim();
            String type = etType.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("El nombre es obligatorio");
                return;
            }

            viewModel.performOnlineAction(() -> {
                viewModel.insertFacility(new Facility(name, type, desc));
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
